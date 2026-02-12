package org.omt.labelmanager.catalog.label;

import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test label data.
 * Used by integration tests in other modules that need label fixtures.
 * Creates labels via the API to ensure proper setup (including DIRECT distributor).
 */
@Component
public class LabelTestHelper {

    private final LabelCommandApi labelCommandApi;
    private final UserRepository userRepository;

    public LabelTestHelper(
            LabelCommandApi labelCommandApi,
            UserRepository userRepository
    ) {
        this.labelCommandApi = labelCommandApi;
        this.userRepository = userRepository;
    }

    public Label createLabel(String name) {
        var user = createTestUser("test-" + System.nanoTime() + "@example.com");
        return labelCommandApi.createLabel(
                name, null, null, null, null, user.getId()
        );
    }

    public Label createLabel(String name, String email, String website) {
        var user = createTestUser("test-" + System.nanoTime() + "@example.com");
        return labelCommandApi.createLabel(
                name, email, website, null, null, user.getId()
        );
    }

    public Label createLabel(String name, Long userId) {
        return labelCommandApi.createLabel(
                name, null, null, null, null, userId
        );
    }

    private UserEntity createTestUser(String email) {
        return userRepository.save(
                new UserEntity(email, "password", "Test User")
        );
    }
}
