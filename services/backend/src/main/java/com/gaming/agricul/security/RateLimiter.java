package com.gaming.agricul.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * In-memory fixed-window counters (one backend instance, so no shared store is needed).
 * Counters are lost on restart; the per-account lock stored in app_users covers that case
 * for logins.
 */
@Component
public class RateLimiter {

    private static final int CLEANUP_THRESHOLD = 50_000;
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    private record Window(long startMillis, long lengthMillis, int count) {
        boolean expired(long now) {
            return now - startMillis >= lengthMillis;
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong();
    private final LongSupplier clock;

    public RateLimiter() {
        this(System::currentTimeMillis);
    }

    RateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /** Counts one hit and says whether it is still within the limit. */
    public boolean tryAcquire(String key, int limit, Duration window) {
        return increment(key, window) <= limit;
    }

    /** True once the key has reached the limit in the current window (does not count a hit). */
    public boolean isExhausted(String key, int limit) {
        Window w = windows.get(key);
        return w != null && !w.expired(clock.getAsLong()) && w.count() >= limit;
    }

    /** Counts one hit without checking. */
    public void record(String key, Duration window) {
        increment(key, window);
    }

    public void reset(String key) {
        windows.remove(key);
    }

    /** Seconds until the key's window ends (for Retry-After). */
    public long secondsUntilReset(String key) {
        Window w = windows.get(key);
        if (w == null) {
            return 0;
        }
        long left = w.startMillis() + w.lengthMillis() - clock.getAsLong();
        return Math.max(1, (left + 999) / 1000);
    }

    private int increment(String key, Duration window) {
        long now = clock.getAsLong();
        // At most one full scan per minute: while an attack keeps many windows alive, a scan
        // frees nothing, and doing it on every hit would make each request O(size)
        long last = lastCleanup.get();
        if (windows.size() > CLEANUP_THRESHOLD && now - last >= CLEANUP_INTERVAL_MILLIS
                && lastCleanup.compareAndSet(last, now)) {
            windows.values().removeIf(w -> w.expired(now));
        }
        Window updated = windows.compute(key, (k, w) -> w == null || w.expired(now)
                ? new Window(now, window.toMillis(), 1)
                : new Window(w.startMillis(), w.lengthMillis(), w.count() + 1));
        return updated.count();
    }
}
