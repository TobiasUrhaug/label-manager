package org.omt.labelmanager.catalog.label.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.omt.labelmanager.catalog.label.infrastructure.LabelEntity;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class LabelRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelRepository repo;

    @Autowired
    UserRepository userRepository;

    @Test
    void labelWithAddress_persistsAndRetrievesAddress() {
        var address = new AddressEmbeddable(
                "123 Main St",
                "Suite 100",
                "Oslo",
                "0123",
                "Norway"
        );
        var label = new LabelEntity("Label With Address", null, null);
        label.setAddress(address);
        repo.save(label);

        var retrieved = repo.findByName("Label With Address");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAddress()).isNotNull();
        assertThat(retrieved.get().getAddress().getStreet())
                .isEqualTo("123 Main St");
        assertThat(retrieved.get().getAddress().getCity())
                .isEqualTo("Oslo");
        assertThat(retrieved.get().getAddress().getCountry())
                .isEqualTo("Norway");
    }

    @Test
    void labelWithOwner_persistsAndRetrievesOwner() {
        var owner = new PersonEmbeddable("John Doe");
        var label = new LabelEntity("Label With Owner", null, null);
        label.setOwner(owner);
        repo.save(label);

        var retrieved = repo.findByName("Label With Owner");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getOwner()).isNotNull();
        assertThat(retrieved.get().getOwner().getName())
                .isEqualTo("John Doe");
    }

    @Test
    void findByUserId_onlyReturnsLabelsForSpecifiedUser() {
        var user1 = userRepository.save(
                new UserEntity("u1@test.com", "password", "User 1"));
        var user2 = userRepository.save(
                new UserEntity("u2@test.com", "password", "User 2"));

        var label1 = new LabelEntity("User 1 Label", null, null);
        label1.setUserId(user1.getId());
        repo.save(label1);

        var label2 = new LabelEntity("User 2 Label", null, null);
        label2.setUserId(user2.getId());
        repo.save(label2);

        var label3 = new LabelEntity(
                "Another User 1 Label", null, null);
        label3.setUserId(user1.getId());
        repo.save(label3);

        var result = repo.findByUserId(user2.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName())
                .isEqualTo("User 2 Label");
    }
}
