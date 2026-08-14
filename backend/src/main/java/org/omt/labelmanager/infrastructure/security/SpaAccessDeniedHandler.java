package org.omt.labelmanager.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders the 403s Spring Security raises for an <em>authenticated</em> request — most often a
 * missing or stale CSRF token — as a ProblemDetail.
 *
 * <p>Without this, those responses fall through to Spring Security's default handler, which sends
 * 403 with an empty body and no content type. Anonymous requests never reach here; they are routed
 * to {@link SpaApiAuthenticationEntryPoint} instead.
 */
public class SpaAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter problemDetailWriter;

    public SpaAccessDeniedHandler(JsonMapper jsonMapper) {
        this.problemDetailWriter = new ProblemDetailWriter(jsonMapper);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        problemDetailWriter.write(response, HttpStatus.FORBIDDEN, "Access denied.");
    }
}
