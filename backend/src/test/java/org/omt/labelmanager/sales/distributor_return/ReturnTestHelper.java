package org.omt.labelmanager.sales.distributor_return;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnCommandApi;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test return data.
 * Used by integration tests in other modules that need return fixtures.
 */
@Component
public class ReturnTestHelper {

    private final DistributorReturnCommandApi returnCommandApi;

    public ReturnTestHelper(DistributorReturnCommandApi returnCommandApi) {
        this.returnCommandApi = returnCommandApi;
    }

    /**
     * Creates a return with a single line item.
     */
    public DistributorReturn createReturn(
            Long labelId,
            Long distributorId,
            Long releaseId,
            ReleaseFormat format,
            int quantity
    ) {
        return returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.now(),
                null,
                List.of(new ReturnLineItemInput(releaseId, format, quantity))
        );
    }
}
