package org.omt.labelmanager.identity.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.identity.api.user.UserQueryApi;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class QueryUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired UserQueryApi userQueryApi;

    @Autowired UserRepository userRepository;

    @Test
    void exists_returnsTrue_forPersistedUser() {
        var user =
                userRepository.save(
                        new UserEntity("query-user-exists@example.com", "password", "Existing"));

        assertThat(userQueryApi.exists(user.getId())).isTrue();
    }

    @Test
    void exists_returnsFalse_forUnknownId() {
        assertThat(userQueryApi.exists(999999L)).isFalse();
    }
}
