package org.omt.labelmanager.catalog.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class QueryLabelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelCommandApi labelCommandApi;

    @Autowired
    LabelQueryApi labelQueryApi;

    @Autowired
    UserRepository userRepository;

    @Test
    void queryLabelsForUser_returnsOnlyUserLabels() {
        var user1 = createTestUser("user1@example.com");
        var user2 = createTestUser("user2@example.com");

        labelCommandApi.createLabel(
                "User 1 Label A",
                null,
                null,
                null,
                null,
                user1.getId()
        );
        labelCommandApi.createLabel(
                "User 1 Label B",
                null,
                null,
                null,
                null,
                user1.getId()
        );
        labelCommandApi.createLabel(
                "User 2 Label",
                null,
                null,
                null,
                null,
                user2.getId()
        );

        var user1Labels = labelQueryApi.getLabelsForUser(user1.getId());
        assertThat(user1Labels).hasSize(2);
        assertThat(user1Labels)
                .extracting("name")
                .containsExactlyInAnyOrder("User 1 Label A", "User 1 Label B");
    }

    private UserEntity createTestUser(String email) {
        return userRepository.save(
                new UserEntity(email, "password", "Test User")
        );
    }
}
