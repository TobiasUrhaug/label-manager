package org.omt.labelmanager.catalog.label;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LabelQueryHandlerIntegrationTest {

    @Autowired
    LabelQueryHandler labelQueryHandler;

    @Autowired
    LabelTestHelper labelTestHelper;

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
    void findById_returnsLabelWhenExists() {
        var label = labelTestHelper.createLabel(
                "Test Label", "test@example.com", "https://test.com");

        var result = labelQueryHandler.findById(label.id());

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Test Label");
        assertThat(result.get().email()).isEqualTo("test@example.com");
        assertThat(result.get().website()).isEqualTo("https://test.com");
    }

    @Test
    void findById_returnsEmptyWhenNotExists() {
        var result = labelQueryHandler.findById(99999L);

        assertThat(result).isEmpty();
    }

    @Test
    void exists_returnsTrueWhenLabelExists() {
        var label = labelTestHelper.createLabel("Existing Label");

        var result = labelQueryHandler.exists(label.id());

        assertThat(result).isTrue();
    }

    @Test
    void exists_returnsFalseWhenLabelDoesNotExist() {
        var result = labelQueryHandler.exists(99999L);

        assertThat(result).isFalse();
    }

    @Test
    void getLabelsForUser_returnsLabelsForSpecificUser() {
        var user = userRepository.save(
                new UserEntity("user1@test.com", "password", "User 1"));
        var label = labelTestHelper.createLabel("User Label", user.getId());

        var result = labelQueryHandler.getLabelsForUser(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(label.id());
        assertThat(result.get(0).name()).isEqualTo("User Label");
    }

    @Test
    void getLabelsForUser_returnsEmptyListWhenUserHasNoLabels() {
        var result = labelQueryHandler.getLabelsForUser(99999L);

        assertThat(result).isEmpty();
    }

    @Test
    void getLabelsForUser_returnsMultipleLabelsForUser() {
        var user = userRepository.save(
                new UserEntity("user2@test.com", "password", "User 2"));
        var label1 = labelTestHelper.createLabel("Label 1", user.getId());
        var label2 = labelTestHelper.createLabel("Label 2", user.getId());
        var label3 = labelTestHelper.createLabel("Label 3", user.getId());

        var result = labelQueryHandler.getLabelsForUser(user.getId());

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(Label::name)
                .containsExactlyInAnyOrder("Label 1", "Label 2", "Label 3");
    }

    @Test
    void getLabelsForUser_onlyReturnsLabelsForSpecifiedUser() {
        var user3 = userRepository.save(
                new UserEntity("user3@test.com", "password", "User 3"));
        var user4 = userRepository.save(
                new UserEntity("user4@test.com", "password", "User 4"));

        labelTestHelper.createLabel("User 3 Label", user3.getId());
        var user4Label = labelTestHelper.createLabel(
                "User 4 Label", user4.getId());
        labelTestHelper.createLabel("Another User 3 Label", user3.getId());

        var result = labelQueryHandler.getLabelsForUser(user4.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(user4Label.id());
        assertThat(result.get(0).name()).isEqualTo("User 4 Label");
    }
}
