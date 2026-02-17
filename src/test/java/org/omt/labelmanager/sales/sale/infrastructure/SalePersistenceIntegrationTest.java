package org.omt.labelmanager.sales.sale.infrastructure;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SalePersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private DistributorRepository distributorRepository;

    private Long labelId;
    private Long releaseId;
    private Long distributorId;

    @BeforeEach
    void setUp() {
        saleRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        labelId = label.id();

        releaseId = releaseTestHelper.createReleaseEntity("Test Release", labelId);

        var distributor = distributorRepository.save(
                new DistributorEntity(labelId, "Test Distributor", ChannelType.DIRECT)
        );
        distributorId = distributor.getId();
    }

    @Test
    @Transactional
    void savesAndRetrievesSale() {
        var saleDate = LocalDate.of(2026, 2, 12);
        var sale = new SaleEntity(
                labelId,
                distributorId,
                saleDate,
                ChannelType.EVENT,
                "Concert at venue X",
                "EUR"
        );

        var lineItem = new SaleLineItemEntity(
                releaseId,
                ReleaseFormat.VINYL,
                5,
                new BigDecimal("15.00"),
                "EUR"
        );
        sale.addLineItem(lineItem);

        var saved = saleRepository.save(sale);

        var retrieved = saleRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLabelId()).isEqualTo(labelId);
        assertThat(retrieved.get().getDistributorId()).isEqualTo(distributorId);
        assertThat(retrieved.get().getSaleDate()).isEqualTo(saleDate);
        assertThat(retrieved.get().getChannel()).isEqualTo(ChannelType.EVENT);
        assertThat(retrieved.get().getNotes()).isEqualTo("Concert at venue X");
        assertThat(retrieved.get().getLineItems()).hasSize(1);
        assertThat(retrieved.get().getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void findsByLabelIdOrderedByDate() {
        var sale1 = new SaleEntity(
                labelId,
                distributorId,
                LocalDate.of(2026, 1, 15),
                ChannelType.EVENT,
                "Sale 1",
                "EUR"
        );
        var lineItem1 = new SaleLineItemEntity(
                releaseId,
                ReleaseFormat.VINYL,
                2,
                new BigDecimal("10.00"),
                "EUR"
        );
        sale1.addLineItem(lineItem1);
        saleRepository.save(sale1);

        var sale2 = new SaleEntity(
                labelId,
                distributorId,
                LocalDate.of(2026, 2, 20),
                ChannelType.DIRECT,
                "Sale 2",
                "EUR"
        );
        var lineItem2 = new SaleLineItemEntity(
                releaseId,
                ReleaseFormat.CD,
                3,
                new BigDecimal("12.00"),
                "EUR"
        );
        sale2.addLineItem(lineItem2);
        saleRepository.save(sale2);

        var sales = saleRepository.findByLabelIdOrderBySaleDateDesc(labelId);

        assertThat(sales).hasSize(2);
        assertThat(sales.get(0).getSaleDate()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(sales.get(1).getSaleDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void sumsTotalAmountByLabelId() {
        var sale1 = new SaleEntity(
                labelId,
                distributorId,
                LocalDate.of(2026, 1, 15),
                ChannelType.EVENT,
                "Sale 1",
                "EUR"
        );
        var lineItem1 = new SaleLineItemEntity(
                releaseId,
                ReleaseFormat.VINYL,
                2,
                new BigDecimal("10.00"),
                "EUR"
        );
        sale1.addLineItem(lineItem1);
        saleRepository.save(sale1);

        var sale2 = new SaleEntity(
                labelId,
                distributorId,
                LocalDate.of(2026, 2, 20),
                ChannelType.DIRECT,
                "Sale 2",
                "EUR"
        );
        var lineItem2 = new SaleLineItemEntity(
                releaseId,
                ReleaseFormat.CD,
                3,
                new BigDecimal("12.00"),
                "EUR"
        );
        sale2.addLineItem(lineItem2);
        saleRepository.save(sale2);

        var total = saleRepository.sumTotalAmountByLabelId(labelId);

        assertThat(total).isEqualByComparingTo(new BigDecimal("56.00"));
    }

    @Test
    void sumTotalAmountByLabelId_returnsZero_whenNoSales() {
        var total = saleRepository.sumTotalAmountByLabelId(labelId);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cascadeDeletesLineItems() {
        var sale = new SaleEntity(
                labelId,
                distributorId,
                LocalDate.of(2026, 2, 12),
                ChannelType.EVENT,
                "Sale with items",
                "EUR"
        );
        var lineItem = new SaleLineItemEntity(
                releaseId,
                ReleaseFormat.VINYL,
                2,
                new BigDecimal("10.00"),
                "EUR"
        );
        sale.addLineItem(lineItem);
        var saved = saleRepository.save(sale);

        assertThat(saved.getLineItems()).hasSize(1);

        saleRepository.deleteById(saved.getId());

        var retrieved = saleRepository.findById(saved.getId());
        assertThat(retrieved).isEmpty();
    }
}
