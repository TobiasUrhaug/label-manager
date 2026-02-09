package org.omt.labelmanager.catalog.label;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LabelQueryServiceIntegrationTest {

    @Autowired
    LabelQueryService labelQueryService;

    @Autowired
    LabelTestHelper labelTestHelper;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void findById_returnsLabelWhenExists() {
        var label = labelTestHelper.createLabel(
                "Test Label", "test@example.com", "https://test.com");

        var result = labelQueryService.findById(label.id());

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Test Label");
        assertThat(result.get().email()).isEqualTo("test@example.com");
        assertThat(result.get().website()).isEqualTo("https://test.com");
    }

    @Test
    void findById_returnsEmptyWhenNotExists() {
        var result = labelQueryService.findById(99999L);

        assertThat(result).isEmpty();
    }

    @Test
    void exists_returnsTrueWhenLabelExists() {
        var label = labelTestHelper.createLabel("Existing Label");

        var result = labelQueryService.exists(label.id());

        assertThat(result).isTrue();
    }

    @Test
    void exists_returnsFalseWhenLabelDoesNotExist() {
        var result = labelQueryService.exists(99999L);

        assertThat(result).isFalse();
    }
}
