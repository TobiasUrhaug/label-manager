package org.omt.labelmanager.sales.sale.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SaleQueryApiImpl implements SaleQueryApi {

    private final SaleRepository saleRepository;
    private final SaleConverter saleConverter;

    SaleQueryApiImpl(SaleRepository saleRepository, SaleConverter saleConverter) {
        this.saleRepository = saleRepository;
        this.saleConverter = saleConverter;
    }

    @Override
    @Transactional
    public List<Sale> getSalesForLabel(Long labelId) {
        return saleRepository.findByLabelIdOrderBySaleDateDesc(labelId).stream()
                .map(saleConverter::toSale)
                .toList();
    }

    @Override
    @Transactional
    public List<Sale> getSalesForDistributor(Long distributorId) {
        return saleRepository.findByDistributorIdOrderBySaleDateDesc(distributorId).stream()
                .map(saleConverter::toSale)
                .toList();
    }

    @Override
    @Transactional
    public List<Sale> getSalesForProductionRun(Long productionRunId) {
        return saleRepository.findByProductionRunIdOrderBySaleDateDesc(productionRunId).stream()
                .map(saleConverter::toSale)
                .toList();
    }

    @Override
    @Transactional
    public Optional<Sale> findById(Long saleId) {
        return saleRepository.findById(saleId)
                .map(saleConverter::toSale);
    }

    @Override
    public Money getTotalRevenueForLabel(Long labelId) {
        BigDecimal total = saleRepository.sumTotalAmountByLabelId(labelId);
        return new Money(total, "EUR");
    }
}
