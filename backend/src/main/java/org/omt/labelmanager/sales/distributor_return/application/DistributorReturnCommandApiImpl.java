package org.omt.labelmanager.sales.distributor_return.application;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnCommandApi;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.springframework.stereotype.Service;

@Service
class DistributorReturnCommandApiImpl implements DistributorReturnCommandApi {

    private final RegisterReturnUseCase registerReturn;
    private final UpdateReturnUseCase updateReturn;
    private final DeleteReturnUseCase deleteReturn;

    DistributorReturnCommandApiImpl(
            RegisterReturnUseCase registerReturn,
            UpdateReturnUseCase updateReturn,
            DeleteReturnUseCase deleteReturn
    ) {
        this.registerReturn = registerReturn;
        this.updateReturn = updateReturn;
        this.deleteReturn = deleteReturn;
    }

    @Override
    public DistributorReturn registerReturn(
            Long labelId,
            Long distributorId,
            LocalDate returnDate,
            String notes,
            List<ReturnLineItemInput> lineItems
    ) {
        return registerReturn.execute(labelId, distributorId, returnDate, notes, lineItems);
    }

    @Override
    public void updateReturn(
            Long returnId,
            LocalDate returnDate,
            String notes,
            List<ReturnLineItemInput> lineItems
    ) {
        updateReturn.execute(returnId, returnDate, notes, lineItems);
    }

    @Override
    public void deleteReturn(Long returnId) {
        deleteReturn.execute(returnId);
    }
}
