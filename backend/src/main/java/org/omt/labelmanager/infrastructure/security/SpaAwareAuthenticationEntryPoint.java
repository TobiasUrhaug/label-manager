package org.omt.labelmanager.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

public class SpaAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SpaApiAuthenticationEntryPoint apiEntryPoint = new SpaApiAuthenticationEntryPoint();
    private final LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if (request.getRequestURI().startsWith("/api/")) {
            apiEntryPoint.commence(request, response, authException);
        } else {
            loginEntryPoint.commence(request, response, authException);
        }
    }
}
