package org.omt.labelmanager.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an RFC 9457 ProblemDetail from a servlet filter, where no {@code HttpMessageConverter} is
 * in play.
 *
 * <p>Uses the application's own {@link JsonMapper} rather than a fresh one, so Spring's
 * ProblemDetail mixin applies and the extension members are flattened into the body instead of
 * nested under {@code "properties"}.
 */
class ProblemDetailWriter {

    private final JsonMapper jsonMapper;

    ProblemDetailWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(
                response.getWriter(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
