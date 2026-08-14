package org.omt.labelmanager.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

public class SpaApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailWriter problemDetailWriter;

    public SpaApiAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.problemDetailWriter = new ProblemDetailWriter(jsonMapper);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        problemDetailWriter.write(response, HttpStatus.UNAUTHORIZED, "Authentication required.");
    }
}
