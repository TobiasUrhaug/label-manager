package org.omt.labelmanager.sales.sale.application;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
class SaleQueryApiImpl implements SaleQueryApi {

    private final SaleRepository saleRepository;

    SaleQueryApiImpl(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Override
    public List<Sale> getSalesForLabel(Long labelId) {
        return saleRepository.findByLabelIdOrderBySaleDateDesc(labelId).stream()
                .map(this::convertToSale)
                .toList();
    }

    @Override
    @Transactional
    public Optional<Sale> findById(Long saleId) {
        return saleRepository.findById(saleId)
                .map(this::convertToSale);
    }

    @Override
    public Money getTotalRevenueForLabel(Long labelId) {
        BigDecimal total = saleRepository.sumTotalAmountByLabelId(labelId);
        return new Money(total, "EUR");
    }

    private Sale convertToSale(SaleEntity entity) {
        List<SaleLineItem> lineItems = entity.getLineItems().stream()
                .map(item -> new SaleLineItem(
                        item.getId(),
                        item.getReleaseId(),
                        item.getFormat(),
                        item.getQuantity(),
                        new Money(item.getUnitPrice(), item.getCurrency()),
                        new Money(item.getLineTotal(), item.getCurrency())
                ))
                .toList();

        return new Sale(
                entity.getId(),
                entity.getLabelId(),
                entity.getSaleDate(),
                entity.getChannel(),
                entity.getNotes(),
                lineItems,
                new Money(entity.getTotalAmount(), entity.getCurrency())
        );
    }
}
