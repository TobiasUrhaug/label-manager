package org.omt.labelmanager.catalog.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class LabelRepositoryIntegrationTest {

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
    void labelWithAddress_persistsAndRetrievesAddress() {
        var address = new AddressEmbeddable(
                "123 Main St",
                "Suite 100",
                "Oslo",
                "0123",
                "Norway"
        );
        var label = new LabelEntity("Label With Address", null, null);
        label.setAddress(address);
        repo.save(label);

        var retrieved = repo.findByName("Label With Address");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAddress()).isNotNull();
        assertThat(retrieved.get().getAddress().getStreet())
                .isEqualTo("123 Main St");
        assertThat(retrieved.get().getAddress().getCity())
                .isEqualTo("Oslo");
        assertThat(retrieved.get().getAddress().getCountry())
                .isEqualTo("Norway");
    }

    @Test
    void labelWithOwner_persistsAndRetrievesOwner() {
        var owner = new PersonEmbeddable("John Doe");
        var label = new LabelEntity("Label With Owner", null, null);
        label.setOwner(owner);
        repo.save(label);

        var retrieved = repo.findByName("Label With Owner");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getOwner()).isNotNull();
        assertThat(retrieved.get().getOwner().getName())
                .isEqualTo("John Doe");
    }
}
