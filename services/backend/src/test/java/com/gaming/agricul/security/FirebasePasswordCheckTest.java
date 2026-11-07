package com.gaming.agricul.security;

import com.gaming.agricul.AppUser;
import com.gaming.agricul.Role;
import com.gaming.agricul.UserStatus;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Against a local fake of Firebase's signInWithPassword endpoint. */
class FirebasePasswordCheckTest {

    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastKey = new AtomicReference<>();
    private volatile int status;
    private volatile String reply;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/signIn", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastKey.set(exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private FirebasePasswordCheck check() {
        return new FirebasePasswordCheck("web-key", "http://127.0.0.1:" + server.getAddress().getPort() + "/signIn");
    }

    @Test
    void acceptedPasswordForTheSameUid_matches() {
        answer(200, "{\"kind\":\"x\",\"localId\":\"fb-1\",\"email\":\"a@example.com\",\"registered\":true}");

        assertThat(check().check("a@example.com", "pa\"ss\\word", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.MATCH);
        assertThat(lastKey.get()).isEqualTo("web-key");
        assertThat(lastBody.get())
                .isEqualTo("{\"email\":\"a@example.com\",\"password\":\"pa\\\"ss\\\\word\",\"returnSecureToken\":false}");
    }

    @Test
    void acceptedPasswordForAnotherUid_doesNotMatch() {
        answer(200, "{\"localId\":\"someone-else\"}");

        assertThat(check().check("a@example.com", "x", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.NO_MATCH);
    }

    @Test
    void wrongPassword_doesNotMatch() {
        answer(400, "{\"error\":{\"code\":400,\"message\":\"INVALID_LOGIN_CREDENTIALS\",\"errors\":[]}}");

        assertThat(check().check("a@example.com", "x", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.NO_MATCH);
    }

    @Test
    void firebaseThrottling_isUnavailable_notAWrongPassword() {
        answer(400, "{\"error\":{\"code\":400,\"message\":\"TOO_MANY_ATTEMPTS_TRY_LATER : Access disabled\"}}");

        assertThat(check().check("a@example.com", "x", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.UNAVAILABLE);
    }

    @Test
    void serverErrorOrNoConnection_isUnavailable() {
        answer(500, "oops");
        assertThat(check().check("a@example.com", "x", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.UNAVAILABLE);

        server.stop(0);
        assertThat(check().check("a@example.com", "x", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.UNAVAILABLE);
    }

    @Test
    void canMigrate_onlyFirebaseAccountsWithoutPassword_andOnlyWithAKey() {
        AppUser firebase = AppUser.create("a@example.com", "A", Role.INVESTOR, UserStatus.ACTIVE);
        firebase.setFirebaseUid("fb-1");
        assertThat(check().canMigrate(firebase)).isTrue();
        assertThat(new FirebasePasswordCheck("", "http://unused").canMigrate(firebase)).isFalse();

        firebase.setPasswordHash("{argon2}x");
        assertThat(check().canMigrate(firebase)).isFalse();

        AppUser local = AppUser.create("b@example.com", "B", Role.INVESTOR, UserStatus.ACTIVE);
        assertThat(check().canMigrate(local)).isFalse();
    }

    @Test
    void rejectedKey_isUnavailable() {
        // What a browser key restricted to HTTP referrers gets from a server
        answer(403, "{\"error\":{\"code\":403,\"message\":\"Requests from referer <empty> are blocked.\",\"status\":\"PERMISSION_DENIED\"}}");

        assertThat(check().check("a@example.com", "x", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.UNAVAILABLE);
    }

    @Test
    void passwordWithControlCharacters_isSentIntact() throws Exception {
        answer(200, "{\"localId\":\"fb-1\"}");

        check().check("a@example.com", "line\nbreak\u0001", "fb-1");

        assertThat(tools.jackson.databind.json.JsonMapper.builder().build().readTree(lastBody.get())
                .path("password").asString()).isEqualTo("line\nbreak\u0001");
    }

    @Test
    void garbageResponse_isUnavailable() {
        answer(502, "<html>bad gateway</html>");

        assertThat(check().check("a@example.com", "x", "fb-1")).isEqualTo(FirebasePasswordCheck.Outcome.UNAVAILABLE);
    }

    private void answer(int status, String body) {
        this.status = status;
        this.reply = body;
    }
}
