package org.omt.labelmanager.label;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class LabelCRUDIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private RestTestClient restClient;

    @Autowired
    LabelRepository repo;

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
    void createLabel() {
        restClient
                .post()
                .uri("/labels")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("labelName=My+Label")
                .exchange()
                .expectStatus()
                .is3xxRedirection();

        assertThat(repo.findByName("My Label")).isPresent();
    }

    @Test
    void deleteLabel() {
        var label = new LabelEntity("WronglyNamedLabel");
        repo.save(label);

        assertThat(repo
                .findAll()
                .stream()
                .map(LabelEntity::getName)
                .toList()
        )
                .contains("WronglyNamedLabel");

        restClient
                .delete()
                .uri("/labels/" + label.getId())
                .exchange()
                .expectStatus().is3xxRedirection();

        assertThat(repo
                .findAll()
                .stream()
                .map(LabelEntity::getName)
                .toList()
        )
                .doesNotContain("WronglyNamedLabel");
    }
}
