package org.omt.labelmanager.identity.user;

import org.omt.labelmanager.identity.domain.user.User;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test user data. Used by integration tests in other modules that need a
 * user fixture without reaching into identity's persistence package.
 *
 * <p>Note: This helper bypasses the API for simplicity — it stores the password as given rather
 * than encoding it, so it is not suitable for tests that authenticate.
 */
@Component
public class UserTestHelper {

    private final UserRepository userRepository;

    public UserTestHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String email) {
        return User.fromEntity(userRepository.save(new UserEntity(email, "password", "Test User")));
    }
}
