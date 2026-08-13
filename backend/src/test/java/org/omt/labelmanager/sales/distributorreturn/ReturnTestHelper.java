package org.omt.labelmanager.sales.distributorreturn;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.sales.distributorreturn.api.DistributorReturnCommandApi;
import org.omt.labelmanager.sales.distributorreturn.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributorreturn.domain.ReturnLineItemInput;
import org.omt.labelmanager.shared.Format;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test return data. Used by integration tests in other modules that need
 * return fixtures.
 */
@Component
public class ReturnTestHelper {

    private final DistributorReturnCommandApi returnCommandApi;

    public ReturnTestHelper(DistributorReturnCommandApi returnCommandApi) {
        this.returnCommandApi = returnCommandApi;
    }

    /** Creates a return with a single line item. */
    public DistributorReturn createReturn(
            Long labelId, Long distributorId, Long releaseId, Format format, int quantity) {
        return returnCommandApi.registerReturn(
                labelId,
                distributorId,
                LocalDate.now(),
                null,
                List.of(new ReturnLineItemInput(releaseId, format, quantity)));
    }
}
