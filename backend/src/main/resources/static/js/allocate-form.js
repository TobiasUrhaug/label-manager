/**
 * Allocate inventory modal form management.
 * Handles location type toggle (Distributor / Bandcamp) and form action URL construction.
 */
export const AllocateForm = {
    buildActionUrl(labelId, releaseId, runId) {
        return `/labels/${labelId}/releases/${releaseId}/production-runs/${runId}/allocations`;
    },

    buildCancellationActionUrl(labelId, releaseId, runId) {
        return `/labels/${labelId}/releases/${releaseId}/production-runs/${runId}/bandcamp-cancellations`;
    },

    toggleDistributorField(locationType) {
        const distributorGroup = document.getElementById('distributorGroup');
        const distributorSelect = document.getElementById('allocationDistributorId');
        const isDistributor = locationType === 'DISTRIBUTOR';
        distributorGroup.style.display = isDistributor ? '' : 'none';
        distributorSelect.required = isDistributor;
        if (!isDistributor) {
            distributorSelect.value = '';
        }
    },

    bindMaxQuantity(maxQuantity) {
        const allocationQuantity = document.getElementById('allocationQuantity');
        const allocationAvailableDisplay = document.getElementById('allocationAvailableDisplay');
        allocationQuantity.max = maxQuantity;
        allocationQuantity.value = '';
        allocationAvailableDisplay.textContent = maxQuantity;
    },

    setup(labelId, releaseId) {
        const allocateForm = document.getElementById('allocateForm');
        const locationTypeSelect = document.getElementById('allocationLocationType');

        locationTypeSelect.addEventListener('change', function () {
            AllocateForm.toggleDistributorField(this.value);
        });

        document.querySelectorAll('.allocate-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const runId = this.dataset.runId;
                const maxQuantity = parseInt(this.dataset.maxQuantity, 10);

                allocateForm.action = AllocateForm.buildActionUrl(labelId, releaseId, runId);
                AllocateForm.bindMaxQuantity(maxQuantity);

                locationTypeSelect.value = 'DISTRIBUTOR';
                AllocateForm.toggleDistributorField('DISTRIBUTOR');
            });
        });

        const cancelBandcampForm = document.getElementById('cancelBandcampForm');
        document.querySelectorAll('.cancel-bandcamp-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const runId = this.dataset.runId;
                const maxQuantity = parseInt(this.dataset.maxQuantity, 10);

                cancelBandcampForm.action =
                    AllocateForm.buildCancellationActionUrl(labelId, releaseId, runId);
                const cancelQuantityInput = document.getElementById('cancelBandcampQuantity');
                cancelQuantityInput.max = maxQuantity;
                cancelQuantityInput.value = '';
            });
        });
    },
};
