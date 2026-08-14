package org.omt.labelmanager.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.json.JsonMapper;

public class SpaAuthFailureHandler implements AuthenticationFailureHandler {

    private final ProblemDetailWriter problemDetailWriter;

    public SpaAuthFailureHandler(JsonMapper jsonMapper) {
        this.problemDetailWriter = new ProblemDetailWriter(jsonMapper);
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        problemDetailWriter.write(response, HttpStatus.UNAUTHORIZED, "Invalid credentials.");
    }
}
