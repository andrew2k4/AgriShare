package com.gaming.agricul.security;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The real client IP, for rate limiting. In production the backend is only reachable through
 * Nginx on the Docker network, and Nginx only accepts Cloudflare: Nginx puts Cloudflare's
 * CF-Connecting-IP in X-Real-IP. The header is trusted only when the direct peer is a
 * private or loopback address (i.e. our proxy); a client hitting the backend directly cannot
 * spoof its IP with it.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Real-IP");
        if (forwarded != null && !forwarded.isBlank() && isInternal(peer)) {
            return forwarded.trim();
        }
        return peer;
    }

    private static boolean isInternal(String address) {
        if (address == null) {
            return false;
        }
        try {
            // Literal IPs only: getByName does not do a DNS lookup for them
            if (!address.matches("[0-9a-fA-F.:]+")) {
                return false;
            }
            InetAddress inet = InetAddress.getByName(address);
            return inet.isLoopbackAddress() || inet.isSiteLocalAddress() || inet.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
