/**
 * Sale registration form management
 */
export const SaleForm = {
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
                            onclick="SaleForm.removeLineItem(this)">
                        Remove
                    </button>
                </div>
                <div class="row">
                    <div class="col-md-4">
                        <label for="lineItems${index}.releaseId" class="form-label">Release*</label>
                        <select class="form-select"
                                id="lineItems${index}.releaseId"
                                name="lineItems[${index}].releaseId"
                                required>
                            <option value="">Select release...</option>
                            ${releaseOptions}
                        </select>
                    </div>
                    <div class="col-md-2">
                        <label for="lineItems${index}.format" class="form-label">Format*</label>
                        <select class="form-select"
                                id="lineItems${index}.format"
                                name="lineItems[${index}].format"
                                required>
                            <option value="">Select...</option>
                            ${formatOptions}
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label for="lineItems${index}.quantity" class="form-label">Quantity*</label>
                        <input type="number" class="form-control"
                               id="lineItems${index}.quantity"
                               name="lineItems[${index}].quantity"
                               min="1" required>
                    </div>
                    <div class="col-md-3">
                        <label for="lineItems${index}.unitPrice" class="form-label">Unit Price*</label>
                        <input type="number" class="form-control"
                               id="lineItems${index}.unitPrice"
                               name="lineItems[${index}].unitPrice"
                               step="0.01" min="0" required>
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

        const item = button.closest('.line-item');
        item.remove();

        this.reindexLineItems();
        this.updateItemNumbers();
    },

    reindexLineItems() {
        const container = document.getElementById('lineItemsContainer');
        const items = container.querySelectorAll('.line-item');

        items.forEach((item, index) => {
            const selects = item.querySelectorAll('select');
            const inputs = item.querySelectorAll('input[type="number"]');

            selects.forEach(select => {
                const oldName = select.getAttribute('name');
                if (oldName) {
                    const newName = oldName.replace(/\[\d+\]/, `[${index}]`);
                    select.setAttribute('name', newName);
                    const newId = `lineItems${index}.${oldName.match(/\.(\w+)$/)?.[1] || ''}`;
                    select.setAttribute('id', newId);
                }
            });

            inputs.forEach(input => {
                const oldName = input.getAttribute('name');
                if (oldName) {
                    const newName = oldName.replace(/\[\d+\]/, `[${index}]`);
                    input.setAttribute('name', newName);
                    const newId = `lineItems${index}.${oldName.match(/\.(\w+)$/)?.[1] || ''}`;
                    input.setAttribute('id', newId);
                }
            });
        });
    },

    updateItemNumbers() {
        const items = document.querySelectorAll('.line-item');
        items.forEach((item, index) => {
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

// Make globally available
window.SaleForm = SaleForm;
