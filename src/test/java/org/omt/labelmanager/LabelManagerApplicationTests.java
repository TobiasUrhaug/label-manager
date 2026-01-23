package org.omt.labelmanager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class LabelManagerApplicationTests {

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
    void contextLoads() {
    }

    @Test
    void loginPageLoads() {
        restClient.get()
                .uri("http://localhost:" + port + "/login")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .consumeWith(response ->
                        assertThat(response.getResponseBody()).contains("Login"));
    }

    @Test
    void dashboardRedirectsToLoginWhenUnauthenticated() {
        restClient.get()
                .uri("http://localhost:" + port + "/dashboard")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

}
