/**
 * Distributor return registration form management.
 * Handles dynamic line item add/remove without a price field.
 */
export const ReturnForm = {
    releases: [],
    formats: [],

    initialize(releases, formats) {
        this.releases = releases;
        this.formats = formats;
        this.updateItemNumbers();
    },

    addLineItem() {
        const container = document.getElementById('lineItemsContainer');
        const items = container.querySelectorAll('.line-item');
        const newIndex = items.length;

        const newItem = document.createElement('div');
        newItem.className = 'card mb-3 line-item';
        newItem.innerHTML = this.createLineItemHTML(newIndex);

        container.appendChild(newItem);
        this.updateItemNumbers();
    },

    createLineItemHTML(index) {
        const releaseOptions = this.releases
            .map(r => `<option value="${r.id}">${this.escapeHtml(r.name)}</option>`)
            .join('');

        const formatOptions = this.formats
            .map(f => `<option value="${f}">${f}</option>`)
            .join('');

        return `
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <strong>Item <span class="item-number">${index + 1}</span></strong>
                    <button type="button" class="btn btn-outline-danger btn-sm remove-item"
                            onclick="ReturnForm.removeLineItem(this)">
                        Remove
                    </button>
                </div>
                <div class="row">
                    <div class="col-md-4">
                        <label for="lineItems${index}.releaseId" class="form-label">Release*</label>
                        <select class="form-select release-select"
                                id="lineItems${index}.releaseId"
                                name="lineItems[${index}].releaseId"
                                required>
                            <option value="">Select release...</option>
                            ${releaseOptions}
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label for="lineItems${index}.format" class="form-label">Format*</label>
                        <select class="form-select format-select"
                                id="lineItems${index}.format"
                                name="lineItems[${index}].format"
                                required>
                            <option value="">Select...</option>
                            ${formatOptions}
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label for="lineItems${index}.quantity" class="form-label">Quantity*</label>
                        <input type="number" class="form-control quantity-input"
                               id="lineItems${index}.quantity"
                               name="lineItems[${index}].quantity"
                               min="1" required>
                    </div>
                </div>
            </div>
        `;
    },

    removeLineItem(button) {
        const container = document.getElementById('lineItemsContainer');
        const items = container.querySelectorAll('.line-item');

        if (items.length <= 1) {
            alert('At least one line item is required');
            return;
        }

        button.closest('.line-item').remove();

        this.reindexLineItems();
        this.updateItemNumbers();
    },

    reindexLineItems() {
        const container = document.getElementById('lineItemsContainer');
        const items = container.querySelectorAll('.line-item');

        items.forEach((item, index) => {
            item.querySelectorAll('select').forEach(select => {
                const oldName = select.getAttribute('name');
                if (oldName) {
                    select.setAttribute('name', oldName.replace(/\[\d+\]/, `[${index}]`));
                    const field = oldName.match(/\.(\w+)$/)?.[1] || '';
                    select.setAttribute('id', `lineItems${index}.${field}`);
                }
            });

            item.querySelectorAll('input[type="number"]').forEach(input => {
                const oldName = input.getAttribute('name');
                if (oldName) {
                    input.setAttribute('name', oldName.replace(/\[\d+\]/, `[${index}]`));
                    const field = oldName.match(/\.(\w+)$/)?.[1] || '';
                    input.setAttribute('id', `lineItems${index}.${field}`);
                }
            });
        });
    },

    updateItemNumbers() {
        document.querySelectorAll('.line-item').forEach((item, index) => {
            const numberSpan = item.querySelector('.item-number');
            if (numberSpan) {
                numberSpan.textContent = index + 1;
            }

            const removeBtn = item.querySelector('.remove-item');
            if (removeBtn) {
                removeBtn.style.display = index === 0 ? 'none' : 'inline-block';
            }
        });
    },

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};
