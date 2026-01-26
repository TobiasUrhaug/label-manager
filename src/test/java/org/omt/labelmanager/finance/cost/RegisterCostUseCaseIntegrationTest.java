package org.omt.labelmanager.finance.cost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.shared.Money;
import org.omt.labelmanager.finance.cost.persistence.CostRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RegisterCostUseCaseIntegrationTest {

    @Autowired
    RegisterCostUseCase registerCostUseCase;

    @Autowired
    CostRepository costRepository;

    @Autowired
    LabelRepository labelRepository;

    @Autowired
    ReleaseRepository releaseRepository;

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
    void registersCostForRelease() {
        var label = labelRepository.save(new LabelEntity("Test Label", null, null));
        var release = new ReleaseEntity();
        release.setName("Test Release");
        release.setLabel(label);
        release = releaseRepository.save(release);

        registerCostUseCase.registerCost(
                Money.of(new BigDecimal("100.00")),
                new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("125.00")),
                CostType.MASTERING,
                LocalDate.of(2024, 6, 15),
                "Mastering for album",
                CostOwner.release(release.getId()),
                "INV-2024-001"
        );

        var costs = costRepository.findByOwnerOwnerTypeAndOwnerOwnerId(
                CostOwnerType.RELEASE, release.getId());
        assertThat(costs).hasSize(1);
        assertThat(costs.getFirst().getNetAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(costs.getFirst().getDescription()).isEqualTo("Mastering for album");
    }

    @Test
    void registersCostForLabel() {
        var label = labelRepository.save(new LabelEntity("Label With Cost", null, null));

        registerCostUseCase.registerCost(
                Money.of(new BigDecimal("50.00")),
                new VatAmount(Money.of(new BigDecimal("12.50")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("62.50")),
                CostType.HOSTING,
                LocalDate.of(2024, 7, 1),
                "Website hosting",
                CostOwner.label(label.getId()),
                null
        );

        var costs = costRepository.findByOwnerOwnerTypeAndOwnerOwnerId(
                CostOwnerType.LABEL, label.getId());
        assertThat(costs).hasSize(1);
        assertThat(costs.getFirst().getCostType()).isEqualTo(CostType.HOSTING);
    }

    @Test
    void throwsWhenReleaseNotFound() {
        assertThatThrownBy(() -> registerCostUseCase.registerCost(
                Money.of(new BigDecimal("100.00")),
                new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("125.00")),
                CostType.MASTERING,
                LocalDate.of(2024, 6, 15),
                "Mastering",
                CostOwner.release(99999L),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenLabelNotFound() {
        assertThatThrownBy(() -> registerCostUseCase.registerCost(
                Money.of(new BigDecimal("100.00")),
                new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("125.00")),
                CostType.HOSTING,
                LocalDate.of(2024, 6, 15),
                "Hosting",
                CostOwner.label(99999L),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
