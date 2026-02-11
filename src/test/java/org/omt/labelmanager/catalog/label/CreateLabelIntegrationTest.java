package org.omt.labelmanager.catalog.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.springframework.beans.factory.annotation.Autowired;

public class CreateLabelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelCommandApi labelCommandApi;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DistributorQueryApi distributorQueryService;

    @Test
    void createLabel_persistsLabelWithAllFields() {
        var user = createTestUser("create-test@example.com");
        var address = new Address(
                "123 Main St",
                "Suite 100",
                "Oslo",
                "0123",
                "Norway"
        );
        var owner = new Person("John Doe");

        var label = labelCommandApi.createLabel(
                "Test Label",
                "test@label.com",
                "https://testlabel.com",
                address,
                owner,
                user.getId()
        );

        assertThat(label.id()).isNotNull();
        assertThat(label.name()).isEqualTo("Test Label");
        assertThat(label.email()).isEqualTo("test@label.com");
        assertThat(label.website()).isEqualTo("https://testlabel.com");
        assertThat(label.address()).isEqualTo(address);
        assertThat(label.owner()).isEqualTo(owner);
        assertThat(label.userId()).isEqualTo(user.getId());
    }

    @Test
    void createLabel_createsDefaultDirectDistributor() {
        var user = createTestUser("sales-channel-test@example.com");

        var label = labelCommandApi.createLabel(
                "Label With Default Channel",
                null,
                null,
                null,
                null,
                user.getId()
        );

        var distributors =
                distributorQueryService.findByLabelId(
                        label.id()
                );
        assertThat(distributors).hasSize(1);
        assertThat(distributors.getFirst().name())
                .isEqualTo("Direct Sales");
        assertThat(distributors.getFirst().channelType())
                .isEqualTo(ChannelType.DIRECT);
    }

    private UserEntity createTestUser(String email) {
        return userRepository.save(
                new UserEntity(email, "password", "Test User")
        );
    }
}
