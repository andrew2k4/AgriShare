package com.gaming.agricul.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpTest {

    @Test
    void trustsXRealIp_fromOurProxy() {
        // Nginx on the Docker network
        assertThat(ClientIp.of(request("172.18.0.5", "198.51.100.7"))).isEqualTo("198.51.100.7");
        assertThat(ClientIp.of(request("127.0.0.1", "198.51.100.7"))).isEqualTo("198.51.100.7");
    }

    @Test
    void ignoresXRealIp_fromAPublicPeer() {
        // Someone reaching the backend directly cannot choose the IP that gets rate-limited
        assertThat(ClientIp.of(request("198.51.100.9", "10.0.0.1"))).isEqualTo("198.51.100.9");
    }

    @Test
    void fallsBackToThePeer_withoutHeader() {
        assertThat(ClientIp.of(request("172.18.0.5", null))).isEqualTo("172.18.0.5");
    }

    private static MockHttpServletRequest request(String peer, String realIp) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (realIp != null) {
            request.addHeader("X-Real-IP", realIp);
        }
        return request;
    }
}
