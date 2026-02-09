package org.omt.labelmanager.catalog.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.omt.labelmanager.inventory.application.SalesChannelQueryService;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
public class LabelCommandHandlerIntegrationTest {

    @Autowired
    LabelCommandHandler labelCommandHandler;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SalesChannelQueryService salesChannelQueryService;

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
    void createLabel_createsDefaultDirectSalesChannel() {
        var user = userRepository.save(
                new UserEntity(
                        "sales-channel-test@example.com",
                        "password",
                        "Test User"
                )
        );

        var label = labelCommandHandler.createLabel(
                "Label With Default Channel",
                null,
                null,
                null,
                null,
                user.getId()
        );

        var salesChannels =
                salesChannelQueryService.getSalesChannelsForLabel(
                        label.id()
                );
        assertThat(salesChannels).hasSize(1);
        assertThat(salesChannels.getFirst().name())
                .isEqualTo("Direct Sales");
        assertThat(salesChannels.getFirst().channelType())
                .isEqualTo(ChannelType.DIRECT);
    }
}
