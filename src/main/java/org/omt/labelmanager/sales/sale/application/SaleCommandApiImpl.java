package org.omt.labelmanager.sales.sale.application;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.springframework.stereotype.Service;

@Service
class SaleCommandApiImpl implements SaleCommandApi {

    private final RegisterSaleUseCase registerSale;
    private final UpdateSaleUseCase updateSale;
    private final DeleteSaleUseCase deleteSale;

    SaleCommandApiImpl(
            RegisterSaleUseCase registerSale,
            UpdateSaleUseCase updateSale,
            DeleteSaleUseCase deleteSale
    ) {
        this.registerSale = registerSale;
        this.updateSale = updateSale;
        this.deleteSale = deleteSale;
    }

    @Override
    public Sale registerSale(
            Long labelId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            Long distributorId,
            List<SaleLineItemInput> lineItems
    ) {
        return registerSale.execute(labelId, saleDate, channel, notes, distributorId, lineItems);
    }

    @Override
    public Sale updateSale(
            Long saleId,
            LocalDate saleDate,
            String notes,
            List<SaleLineItemInput> lineItems
    ) {
        return updateSale.execute(saleId, saleDate, notes, lineItems);
    }

    @Override
    public void deleteSale(Long saleId) {
        deleteSale.execute(saleId);
    }
}
