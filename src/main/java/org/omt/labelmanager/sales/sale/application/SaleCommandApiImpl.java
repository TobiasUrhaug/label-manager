package org.omt.labelmanager.sales.sale.application;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
class SaleCommandApiImpl implements SaleCommandApi {

    private final RegisterSaleUseCase registerSale;

    SaleCommandApiImpl(RegisterSaleUseCase registerSale) {
        this.registerSale = registerSale;
    }

    @Override
    public Sale registerSale(
            Long labelId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            List<SaleLineItemInput> lineItems
    ) {
        return registerSale.execute(labelId, saleDate, channel, notes, lineItems);
    }
}
