package com.gaming.agricul.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The Google flow with a client configured; Google itself is not called. */
@SpringBootTest(properties = {
        "app.google.client-id=test-client.apps.googleusercontent.com", "app.google.client-secret=test-secret"})
@AutoConfigureMockMvc
class GoogleLoginIntegrationTest {

    @Autowired
    private MockMvc mvc;



    @Test
    void startingTheFlow_redirectsToGoogle_withPkceAndTheBasicScopesOnly() throws Exception {
        String location = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound())
                .andReturn().getResponse().getRedirectedUrl();

        UriComponents google = UriComponentsBuilder.fromUriString(location).build();
        assertThat(google.getHost()).isEqualTo("accounts.google.com");
        assertThat(google.getQueryParams().getFirst("client_id")).isEqualTo("test-client.apps.googleusercontent.com");
        assertThat(google.getQueryParams().getFirst("redirect_uri")).isEqualTo("http://localhost/login/oauth2/code/google");
        assertThat(google.getQueryParams().getFirst("scope")).isEqualTo("openid%20email%20profile");
        assertThat(google.getQueryParams().getFirst("code_challenge_method")).isEqualTo("S256");
        assertThat(google.getQueryParams().getFirst("code_challenge")).isNotBlank();
        assertThat(google.getQueryParams().getFirst("state")).isNotBlank();
        assertThat(location).doesNotContain("test-secret");
    }

    @Test
    void callbackWithoutAPendingRequest_isRejected_andSentBackToTheFrontend() throws Exception {
        String location = mvc.perform(get("/login/oauth2/code/google").param("code", "x").param("state", "forged"))
                .andExpect(status().isFound())
                .andReturn().getResponse().getRedirectedUrl();

        assertThat(location).isEqualTo("http://localhost:3000/login?error=GOOGLE_LOGIN_FAILED");
    }

    @Test
    void startingTheFlow_isRateLimitedPerIp() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(get("/oauth2/authorization/google").with(fromIp("198.51.100.50")))
                    .andExpect(status().isFound());
        }
        // Over the limit: no redirect to Google, so no pending request (session) is stored
        mvc.perform(get("/oauth2/authorization/google").with(fromIp("198.51.100.50")))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/oauth2/authorization/google").with(fromIp("198.51.100.51")))
                .andExpect(status().isFound());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void springsGeneratedLoginPage_isNotExposed() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoints_stillAnswer401_notARedirectToGoogle() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
