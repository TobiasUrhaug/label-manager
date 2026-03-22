package org.omt.labelmanager.catalog.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class DeleteLabelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelCommandApi labelCommandApi;

    @Autowired
    LabelQueryApi labelQueryApi;

    @Autowired
    UserRepository userRepository;

    @Test
    void deleteLabel_removesLabelFromDatabase() {
        var user = createTestUser("delete-test@example.com");
        var label = labelCommandApi.createLabel(
                "Label To Delete",
                null,
                null,
                null,
                null,
                user.getId()
        );

        labelCommandApi.delete(label.id());

        assertThat(labelQueryApi.findById(label.id())).isEmpty();
    }

    private UserEntity createTestUser(String email) {
        return userRepository.save(
                new UserEntity(email, "password", "Test User")
        );
    }
}
