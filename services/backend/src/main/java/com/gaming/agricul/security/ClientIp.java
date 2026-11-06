package com.gaming.agricul.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The client IP, for rate limiting. Behind Nginx, Tomcat already replaces the peer address
 * with X-Real-IP (Cloudflare's CF-Connecting-IP), and only when the request comes from an
 * internal proxy address: see server.tomcat.remoteip.* in application.properties. A client
 * reaching the backend directly cannot choose its IP with that header.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
