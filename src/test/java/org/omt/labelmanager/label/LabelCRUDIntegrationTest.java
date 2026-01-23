package org.omt.labelmanager.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.common.Address;
import org.omt.labelmanager.common.Person;
import org.omt.labelmanager.common.persistence.AddressEmbeddable;
import org.omt.labelmanager.common.persistence.PersonEmbeddable;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.omt.labelmanager.user.persistence.UserEntity;
import org.omt.labelmanager.user.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LabelCRUDIntegrationTest {

    @Autowired
    LabelRepository repo;

    @Autowired
    LabelCRUDHandler labelCRUDHandler;

    @Autowired
    UserRepository userRepository;

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
        var user = userRepository.save(
                new UserEntity("test1@example.com", "password", "Test User"));

        labelCRUDHandler.createLabel(
                "My Label",
                "contact@mylabel.com",
                "https://mylabel.com",
                null,
                null,
                user.getId()
        );

        var savedLabel = repo.findByName("My Label");
        assertThat(savedLabel).isPresent();
        assertThat(savedLabel.get().getEmail()).isEqualTo("contact@mylabel.com");
        assertThat(savedLabel.get().getWebsite()).isEqualTo("https://mylabel.com");
        assertThat(savedLabel.get().getUserId()).isEqualTo(user.getId());
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
        var user = userRepository.save(
                new UserEntity("test2@example.com", "password", "Test User 2"));

        labelCRUDHandler.createLabel(
                "Label With Owner Via Handler",
                null,
                null,
                null,
                new Person("Jane Smith"),
                user.getId()
        );

        var savedLabel = repo.findByName("Label With Owner Via Handler");
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

        labelCRUDHandler.delete(label.getId());

        assertThat(repo
                .findAll()
                .stream()
                .map(LabelEntity::getName)
                .toList()
        )
                .doesNotContain("WronglyNamedLabel");
    }
}
