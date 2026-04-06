package org.omt.labelmanager.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.identity.application.UserCRUDHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SpaLoginLogoutIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserCRUDHandler userCRUDHandler;

    @BeforeEach
    void createTestUser() {
        try {
            userCRUDHandler.registerUser("login@example.com", "password123", "Login User");
        } catch (Exception ignored) {
            // user may already exist from a previous test run
        }
    }

    @Test
    void postLogin_withValidCredentials_returns200AndSessionCookie() {
        String xsrfToken = fetchXsrfToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + xsrfToken);
        headers.add("X-XSRF-TOKEN", xsrfToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", "login@example.com");
        body.add("password", "password123");

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/login", new HttpEntity<>(body, headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anyMatch(cookie -> cookie.startsWith("JSESSIONID="));
    }

    @Test
    void postLogin_withWrongPassword_returns401WithErrorBody() {
        String xsrfToken = fetchXsrfToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + xsrfToken);
        headers.add("X-XSRF-TOKEN", xsrfToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", "login@example.com");
        body.add("password", "wrongpassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/login", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody()).contains("message");
    }

    @Test
    void postLogout_whenAuthenticated_returns200() {
        String jsessionId = loginAndGetSessionId("login@example.com", "password123");
        String xsrfToken = fetchXsrfTokenForSession(jsessionId);

        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.add(HttpHeaders.COOKIE,
                "XSRF-TOKEN=" + xsrfToken + "; JSESSIONID=" + jsessionId);
        logoutHeaders.add("X-XSRF-TOKEN", xsrfToken);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/logout", HttpMethod.POST, new HttpEntity<>(logoutHeaders), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String loginAndGetSessionId(String username, String password) {
        String xsrfToken = fetchXsrfToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + xsrfToken);
        headers.add("X-XSRF-TOKEN", xsrfToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);

        ResponseEntity<Void> loginResponse = restTemplate.postForEntity(
                "/login", new HttpEntity<>(body, headers), Void.class);

        return extractCookieValue(loginResponse.getHeaders(), "JSESSIONID");
    }

    private String fetchXsrfTokenForSession(String jsessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "JSESSIONID=" + jsessionId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/login", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return extractCookieValue(response.getHeaders(), "XSRF-TOKEN");
    }

    private String fetchXsrfToken() {
        ResponseEntity<String> getResponse = restTemplate.getForEntity("/login", String.class);
        return extractCookieValue(getResponse.getHeaders(), "XSRF-TOKEN");
    }

    private String extractCookieValue(HttpHeaders headers, String cookieName) {
        return headers.get(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith(cookieName + "="))
                .map(c -> c.split(";")[0].substring(cookieName.length() + 1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie not found: " + cookieName));
    }
}
