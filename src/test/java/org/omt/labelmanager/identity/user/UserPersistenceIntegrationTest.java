package org.omt.labelmanager.identity.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.domain.user.User;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserPersistenceIntegrationTest {

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
    void createUser_persistsAllFields() {
        var user = new UserEntity("test@example.com", "hashedPassword", "Test User");

        userRepository.save(user);

        var found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getPassword()).isEqualTo("hashedPassword");
        assertThat(found.get().getDisplayName()).isEqualTo("Test User");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void existsByEmail_returnsTrue_whenUserExists() {
        var user = new UserEntity("exists@example.com", "hashedPassword", "Existing User");
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("notexists@example.com")).isFalse();
    }

    @Test
    void userFromEntity_mapsAllFields() {
        var entity = new UserEntity("mapping@example.com", "pwd123", "Mapped User");
        userRepository.save(entity);

        var found = userRepository.findByEmail("mapping@example.com").orElseThrow();
        var user = User.fromEntity(found);

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo("mapping@example.com");
        assertThat(user.password()).isEqualTo("pwd123");
        assertThat(user.displayName()).isEqualTo("Mapped User");
        assertThat(user.createdAt()).isNotNull();
    }
}
