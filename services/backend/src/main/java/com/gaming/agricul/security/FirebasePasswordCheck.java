package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Checks a password against Firebase, for accounts created there that have no password hash
 * on our side yet. Called once per account: on success the caller stores its own hash and
 * Firebase is never asked again for that user.
 *
 * <p>Uses Firebase Auth's public REST endpoint accounts:signInWithPassword with the project's
 * Web API key (FIREBASE_WEB_API_KEY, the same public key the frontend ships). Without the
 * key the migration is off: removing it is how the transition period ends.
 *
 * <p>The returned UID must be the one stored on the account, so a password that is valid
 * for some other Firebase account cannot unlock this one. The password is never logged.
 */
@Slf4j
@Component
public class FirebasePasswordCheck {

    public enum Outcome {
        /** Firebase accepted the password for this very account. */
        MATCH,
        /** Wrong password, unknown or disabled Firebase account. */
        NO_MATCH,
        /** Firebase could not answer (network, quota): the user should retry later. */
        UNAVAILABLE
    }

    private static final String DEFAULT_ENDPOINT =
            "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword";
    private static final Set<String> REJECTIONS = Set.of(
            "INVALID_PASSWORD", "EMAIL_NOT_FOUND", "INVALID_LOGIN_CREDENTIALS", "USER_DISABLED",
            "INVALID_EMAIL", "MISSING_PASSWORD");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String apiKey;
    private final URI endpoint;
    private final HttpClient http;

    public FirebasePasswordCheck(@Value("${app.firebase-migration.web-api-key:}") String apiKey,
                                 @Value("${app.firebase-migration.endpoint:" + DEFAULT_ENDPOINT + "}") String endpoint) {
        this.apiKey = apiKey.trim();
        this.endpoint = URI.create(endpoint);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        if (!this.apiKey.isEmpty()) {
            log.info("Firebase password migration is on: accounts without a local password are checked against Firebase");
        }
    }

    /** True when this account can still be migrated: it came from Firebase and has no password here. */
    public boolean canMigrate(AppUser user) {
        return !apiKey.isEmpty() && user.getFirebaseUid() != null && user.getEmail() != null && !user.hasPassword();
    }

    public Outcome check(String email, String password, String expectedUid) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("returnSecureToken", false);
        String body = JSON.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                // In a header rather than ?key=, so the key stays out of URLs and their logs
                .header("X-Goog-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode reply = parse(response.body());
            if (response.statusCode() == 200) {
                if (expectedUid.equals(reply.path("localId").asString(""))) {
                    return Outcome.MATCH;
                }
                log.warn("Firebase accepted the password of {} for another UID than the stored one", expectedUid);
                return Outcome.NO_MATCH;
            }
            // e.g. "INVALID_LOGIN_CREDENTIALS" or "TOO_MANY_ATTEMPTS_TRY_LATER : Access to this account..."
            String reason = reply.path("error").path("message").asString("HTTP " + response.statusCode())
                    .split("[ :]", 2)[0];
            if (response.statusCode() == 400 && REJECTIONS.contains(reason)) {
                return Outcome.NO_MATCH;
            }
            if (response.statusCode() == 403 || reason.startsWith("API_KEY") || reason.startsWith("PERMISSION")) {
                log.error("Firebase rejected FIREBASE_WEB_API_KEY ({}). A browser key restricted to HTTP referrers "
                        + "cannot be used by the server: use a key without referrer restriction, limited to the "
                        + "Identity Toolkit API", reason);
            } else {
                log.warn("Firebase password check unavailable: {}", reason);
            }
            return Outcome.UNAVAILABLE;
        } catch (IOException | JacksonException e) {
            log.warn("Firebase password check unavailable: {}", e.toString());
            return Outcome.UNAVAILABLE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Outcome.UNAVAILABLE;
        }
    }

    private static JsonNode parse(String body) {
        try {
            return JSON.readTree(body == null || body.isBlank() ? "{}" : body);
        } catch (JacksonException e) {
            return JSON.createObjectNode();
        }
    }
}
