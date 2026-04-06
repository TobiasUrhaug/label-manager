package org.omt.labelmanager.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

public class SpaAuthFailureHandler implements AuthenticationFailureHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    record ErrorResponse(String message) {}

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        MAPPER.writeValue(response.getWriter(), new ErrorResponse("Invalid username or password."));
    }
}
