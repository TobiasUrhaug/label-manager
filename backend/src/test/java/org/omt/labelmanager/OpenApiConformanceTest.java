package org.omt.labelmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Compares {@code contracts/openapi.yaml} — the stated source of truth — against the spec the
 * running application actually serves at {@code /v3/api-docs}.
 *
 * <p>Phase 2 documented all 31 paths by hand, and hand-matching is what let the document and the
 * code disagree in the first place. This is the check that stops them drifting again: an endpoint
 * added without a contract change fails here, and so does a contract entry with no endpoint behind
 * it.
 *
 * <p>Path and method only. Statuses are not comparable: springdoc reports {@code 200} for every
 * method returning a {@code ResponseEntity} — 26 operations that in fact answer 201 or 204 — and
 * infers no error responses at all, so the live spec is the less accurate of the two documents on
 * that point. The contract's statuses are pinned by the {@code @WebMvcTest} slices, which assert
 * the status the code really returns.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OpenApiConformanceTest extends AbstractIntegrationTest {

    /**
     * Served by Spring Security's form-login filter chain rather than a controller, so springdoc
     * cannot see them. They are real endpoints and belong in the contract; they just cannot be
     * confirmed from the live spec.
     */
    private static final Set<String> NOT_SERVED_BY_A_CONTROLLER =
            Set.of("POST /login", "POST /logout");

    private static final Path CONTRACT = Path.of("..", "contracts", "openapi.yaml");

    /** A path item also carries non-operation keys — {@code parameters}, {@code summary}. */
    private static final Set<String> HTTP_METHODS =
            Set.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

    @Autowired private TestRestTemplate restTemplate;

    @BeforeAll
    static void contractIsWhereWeThinkItIs() {
        assertThat(CONTRACT)
                .as("the contract is read relative to the backend project directory")
                .exists();
    }

    @Test
    void everyDocumentedOperationIsServed() throws IOException {
        Set<String> documented =
                operationsOf(new YAMLMapper().readTree(Files.readString(CONTRACT)));
        documented.removeAll(NOT_SERVED_BY_A_CONTROLLER);

        assertThat(documented)
                .as("operations in contracts/openapi.yaml that the application does not serve")
                .isSubsetOf(liveOperations());
    }

    @Test
    void everyServedOperationIsDocumented() throws IOException {
        Set<String> documented =
                operationsOf(new YAMLMapper().readTree(Files.readString(CONTRACT)));

        assertThat(liveOperations())
                .as(
                        "operations the application serves that contracts/openapi.yaml does not document")
                .isSubsetOf(documented);
    }

    private Set<String> liveOperations() throws IOException {
        return operationsOf(liveSpec());
    }

    private JsonNode liveSpec() throws IOException {
        return new ObjectMapper().readTree(restTemplate.getForObject("/v3/api-docs", String.class));
    }

    /** Flattens a spec into {@code "METHOD /path"} entries, the unit both directions compare. */
    private static Set<String> operationsOf(JsonNode spec) {
        Set<String> operations = new TreeSet<>();
        forEachOperation(spec, (method, path, operation) -> operations.add(method + " " + path));
        return operations;
    }

    private static void forEachOperation(JsonNode spec, OperationVisitor visitor) {
        for (Map.Entry<String, JsonNode> path : spec.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                if (HTTP_METHODS.contains(operation.getKey())) {
                    visitor.visit(
                            operation.getKey().toUpperCase(Locale.ROOT),
                            path.getKey(),
                            operation.getValue());
                }
            }
        }
    }

    private interface OperationVisitor {
        void visit(String method, String path, JsonNode operation);
    }
}
