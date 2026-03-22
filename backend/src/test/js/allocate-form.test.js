import { describe, it, expect, beforeEach } from 'vitest';
import { AllocateForm } from '../../main/resources/static/js/allocate-form.js';

describe('AllocateForm', () => {
    describe('buildActionUrl', () => {
        it('constructs allocation URL from label, release and run IDs', () => {
            expect(AllocateForm.buildActionUrl(1, 2, 3))
                .toBe('/labels/1/releases/2/production-runs/3/allocations');
        });

        it('handles numeric string IDs', () => {
            expect(AllocateForm.buildActionUrl('10', '20', '30'))
                .toBe('/labels/10/releases/20/production-runs/30/allocations');
        });
    });

    describe('buildCancellationActionUrl', () => {
        it('constructs bandcamp cancellation URL from label, release and run IDs', () => {
            expect(AllocateForm.buildCancellationActionUrl(1, 2, 3))
                .toBe('/labels/1/releases/2/production-runs/3/bandcamp-cancellations');
        });
    });

    describe('toggleDistributorField', () => {
        beforeEach(() => {
            document.body.innerHTML = `
                <div id="distributorGroup">
                    <select id="allocationDistributorId">
                        <option value="">Select...</option>
                        <option value="1">Distributor A</option>
                    </select>
                </div>
            `;
        });

        it('shows distributor dropdown and makes it required when location type is DISTRIBUTOR', () => {
            AllocateForm.toggleDistributorField('DISTRIBUTOR');

            const group = document.getElementById('distributorGroup');
            const select = document.getElementById('allocationDistributorId');
            expect(group.style.display).toBe('');
            expect(select.required).toBe(true);
        });

        it('hides distributor dropdown and removes required when location type is BANDCAMP', () => {
            AllocateForm.toggleDistributorField('BANDCAMP');

            const group = document.getElementById('distributorGroup');
            const select = document.getElementById('allocationDistributorId');
            expect(group.style.display).toBe('none');
            expect(select.required).toBe(false);
        });

        it('clears distributor selection when location type is BANDCAMP', () => {
            const select = document.getElementById('allocationDistributorId');
            select.value = '1';

            AllocateForm.toggleDistributorField('BANDCAMP');

            expect(select.value).toBe('');
        });

        it('does not clear distributor selection when location type is DISTRIBUTOR', () => {
            const select = document.getElementById('allocationDistributorId');
            select.value = '1';

            AllocateForm.toggleDistributorField('DISTRIBUTOR');

            expect(select.value).toBe('1');
        });
    });

    describe('bindMaxQuantity', () => {
        beforeEach(() => {
            document.body.innerHTML = `
                <input type="number" id="allocationQuantity" value="50">
                <span id="allocationAvailableDisplay">0</span>
            `;
        });

        it('sets max attribute on quantity input', () => {
            AllocateForm.bindMaxQuantity(300);

            expect(document.getElementById('allocationQuantity').max).toBe('300');
        });

        it('clears the quantity input value', () => {
            AllocateForm.bindMaxQuantity(300);

            expect(document.getElementById('allocationQuantity').value).toBe('');
        });

        it('updates the available display text', () => {
            AllocateForm.bindMaxQuantity(300);

            expect(document.getElementById('allocationAvailableDisplay').textContent).toBe('300');
        });
    });
});
