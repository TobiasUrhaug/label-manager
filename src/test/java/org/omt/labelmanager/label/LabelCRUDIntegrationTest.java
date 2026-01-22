package org.omt.labelmanager.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.common.Address;
import org.omt.labelmanager.common.Person;
import org.omt.labelmanager.common.persistence.AddressEmbeddable;
import org.omt.labelmanager.common.persistence.PersonEmbeddable;
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

    @Autowired
    LabelCRUDHandler labelCRUDHandler;

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
    void createLabel_persistsAllFields() {
        restClient
                .post()
                .uri("/labels")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("labelName=My+Label&email=contact%40mylabel.com"
                        + "&website=https%3A%2F%2Fmylabel.com")
                .exchange()
                .expectStatus()
                .is3xxRedirection();

        var savedLabel = repo.findByName("My Label");
        assertThat(savedLabel).isPresent();
        assertThat(savedLabel.get().getEmail()).isEqualTo("contact@mylabel.com");
        assertThat(savedLabel.get().getWebsite()).isEqualTo("https://mylabel.com");
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
        assertThat(retrieved.get().getAddress().getStreet()).isEqualTo("123 Main St");
        assertThat(retrieved.get().getAddress().getCity()).isEqualTo("Oslo");
        assertThat(retrieved.get().getAddress().getCountry()).isEqualTo("Norway");
    }

    @Test
    void updateAddress_setsAddressOnLabel() {
        var label = new LabelEntity("Label For Address Update", null, null);
        repo.save(label);

        var address = new Address("456 New Street", null, "Bergen", "5020", "Norway");
        labelCRUDHandler.updateAddress(label.getId(), address);

        var updated = repo.findById(label.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getAddress()).isNotNull();
        assertThat(updated.get().getAddress().getStreet()).isEqualTo("456 New Street");
        assertThat(updated.get().getAddress().getCity()).isEqualTo("Bergen");
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
        assertThat(retrieved.get().getOwner().getName()).isEqualTo("John Doe");
    }

    @Test
    void createLabel_persistsOwnerWhenProvided() {
        restClient
                .post()
                .uri("/labels")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("labelName=Label+With+Owner+Via+Form&ownerName=Jane+Smith")
                .exchange()
                .expectStatus()
                .is3xxRedirection();

        var savedLabel = repo.findByName("Label With Owner Via Form");
        assertThat(savedLabel).isPresent();
        assertThat(savedLabel.get().getOwner()).isNotNull();
        assertThat(savedLabel.get().getOwner().getName()).isEqualTo("Jane Smith");
    }

    @Test
    void updateLabel_updatesAllFields() {
        var label = new LabelEntity("Original Label", "old@email.com", "https://old.com");
        label.setAddress(new AddressEmbeddable("Old Street", null, "Oslo", "0123", "Norway"));
        label.setOwner(new PersonEmbeddable("Old Owner"));
        repo.save(label);

        var newAddress = new Address("New Street", "Apt 2", "Bergen", "5020", "Norway");
        var newOwner = new Person("New Owner");

        labelCRUDHandler.updateLabel(
                label.getId(),
                "Updated Label",
                "new@email.com",
                "https://new.com",
                newAddress,
                newOwner
        );

        var updated = repo.findById(label.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("Updated Label");
        assertThat(updated.get().getEmail()).isEqualTo("new@email.com");
        assertThat(updated.get().getWebsite()).isEqualTo("https://new.com");
        assertThat(updated.get().getAddress().getStreet()).isEqualTo("New Street");
        assertThat(updated.get().getAddress().getStreet2()).isEqualTo("Apt 2");
        assertThat(updated.get().getAddress().getCity()).isEqualTo("Bergen");
        assertThat(updated.get().getOwner().getName()).isEqualTo("New Owner");
    }

    @Test
    void deleteLabel() {
        var label = new LabelEntity("WronglyNamedLabel", null, null);
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
