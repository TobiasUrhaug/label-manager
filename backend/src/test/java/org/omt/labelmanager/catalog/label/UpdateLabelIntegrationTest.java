package org.omt.labelmanager.catalog.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateLabelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelCommandApi labelCommandApi;

    @Autowired
    LabelQueryApi labelQueryApi;

    @Autowired
    UserRepository userRepository;

    @Test
    void updateLabel_updatesAllFields() {
        var user = createTestUser("update-test@example.com");
        var label = labelCommandApi.createLabel(
                "Original Name",
                "original@email.com",
                "https://original.com",
                null,
                null,
                user.getId()
        );

        var newAddress = new Address(
                "456 Oak Ave",
                null,
                "Bergen",
                "5020",
                "Norway"
        );
        var newOwner = new Person("Jane Smith");

        labelCommandApi.updateLabel(
                label.id(),
                "Updated Name",
                "updated@email.com",
                "https://updated.com",
                newAddress,
                newOwner
        );

        var updatedLabel = labelQueryApi.findById(label.id()).orElseThrow();
        assertThat(updatedLabel.name()).isEqualTo("Updated Name");
        assertThat(updatedLabel.email()).isEqualTo("updated@email.com");
        assertThat(updatedLabel.website()).isEqualTo("https://updated.com");
        assertThat(updatedLabel.address()).isEqualTo(newAddress);
        assertThat(updatedLabel.owner()).isEqualTo(newOwner);
    }

    private UserEntity createTestUser(String email) {
        return userRepository.save(
                new UserEntity(email, "password", "Test User")
        );
    }
}
