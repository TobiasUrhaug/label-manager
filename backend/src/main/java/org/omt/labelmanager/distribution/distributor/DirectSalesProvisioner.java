package org.omt.labelmanager.distribution.distributor;

import org.omt.labelmanager.catalog.label.api.LabelCreated;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Every label sells directly as well as through distributors, so a new label gets a DIRECT
 * distributor. Distribution owns that policy; catalog only announces that a label now exists.
 *
 * <p>Runs BEFORE_COMMIT so the distributor is written in the same transaction as the label — if
 * provisioning fails, the label creation rolls back with it.
 */
@Component
class DirectSalesProvisioner {

    private static final String DIRECT_SALES_NAME = "Direct Sales";

    private static final Logger log = LoggerFactory.getLogger(DirectSalesProvisioner.class);

    private final DistributorCommandApi distributorCommandApi;

    DirectSalesProvisioner(DistributorCommandApi distributorCommandApi) {
        this.distributorCommandApi = distributorCommandApi;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void onLabelCreated(LabelCreated event) {
        log.debug("Provisioning direct sales distributor for label {}", event.labelId());
        distributorCommandApi.createDistributor(
                event.labelId(), DIRECT_SALES_NAME, ChannelType.DIRECT);
    }
}
