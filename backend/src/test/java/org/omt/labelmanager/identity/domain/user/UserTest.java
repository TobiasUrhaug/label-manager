package org.omt.labelmanager.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void user_createsWithAllFields() {
        Instant now = Instant.now();

        User user = new User(1L, "test@example.com", "hashedpwd", "Test User", now);

        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.email()).isEqualTo("test@example.com");
        assertThat(user.password()).isEqualTo("hashedpwd");
        assertThat(user.displayName()).isEqualTo("Test User");
        assertThat(user.createdAt()).isEqualTo(now);
    }

    @Test
    void userFactory_createsUserWithDefaults() {
        User user = UserFactory.createDefault();

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.displayName()).isEqualTo("Default User");
    }

    @Test
    void userFactory_builderOverridesDefaults() {
        User user = UserFactory.anUser()
                .email("custom@example.com")
                .displayName("Custom User")
                .build();

        assertThat(user.email()).isEqualTo("custom@example.com");
        assertThat(user.displayName()).isEqualTo("Custom User");
    }
}
