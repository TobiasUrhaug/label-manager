import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SaleForm } from './sale-form.js';

describe('SaleForm', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        SaleForm.initialize([], []);
    });

    describe('initialize', () => {
        it('stores releases and formats', () => {
            const releases = [{ id: 1, name: 'Release 1' }];
            const formats = ['VINYL', 'CD'];

            SaleForm.initialize(releases, formats);

            expect(SaleForm.releases).toEqual(releases);
            expect(SaleForm.formats).toEqual(formats);
        });
    });

    describe('createLineItemHTML', () => {
        it('creates HTML with correct structure', () => {
            SaleForm.initialize(
                [{ id: 1, name: 'Test Release' }],
                ['VINYL', 'CD']
            );

            const html = SaleForm.createLineItemHTML(0);

            expect(html).toContain('Item <span class="item-number">1</span>');
            expect(html).toContain('lineItems0.releaseId');
            expect(html).toContain('lineItems[0].releaseId');
            expect(html).toContain('Test Release');
            expect(html).toContain('VINYL');
            expect(html).toContain('CD');
        });

        it('escapes HTML in release names', () => {
            SaleForm.initialize(
                [{ id: 1, name: '<script>alert("xss")</script>' }],
                ['VINYL']
            );

            const html = SaleForm.createLineItemHTML(0);

            expect(html).not.toContain('<script>');
            expect(html).toContain('&lt;script&gt;');
        });
    });

    describe('addLineItem', () => {
        beforeEach(() => {
            document.body.innerHTML = '<div id="lineItemsContainer"></div>';
            SaleForm.initialize(
                [{ id: 1, name: 'Release' }],
                ['VINYL']
            );
        });

        it('adds a new line item to the container', () => {
            const container = document.getElementById('lineItemsContainer');
            expect(container.querySelectorAll('.line-item')).toHaveLength(0);

            SaleForm.addLineItem();

            expect(container.querySelectorAll('.line-item')).toHaveLength(1);
        });

        it('creates item with correct index', () => {
            SaleForm.addLineItem();
            SaleForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const firstSelect = items[0].querySelector('select');
            const secondSelect = items[1].querySelector('select');

            expect(firstSelect.name).toBe('lineItems[0].releaseId');
            expect(secondSelect.name).toBe('lineItems[1].releaseId');
        });
    });

    describe('removeLineItem', () => {
        beforeEach(() => {
            document.body.innerHTML = '<div id="lineItemsContainer"></div>';
            SaleForm.initialize([{ id: 1, name: 'Release' }], ['VINYL']);
        });

        it('removes the line item', () => {
            SaleForm.addLineItem();
            SaleForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const removeButton = items[1].querySelector('.remove-item');

            SaleForm.removeLineItem(removeButton);

            expect(document.querySelectorAll('.line-item')).toHaveLength(1);
        });

        it('shows alert and does not remove if only one item', () => {
            SaleForm.addLineItem();
            const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

            const removeButton = document.querySelector('.remove-item');
            SaleForm.removeLineItem(removeButton);

            expect(alertSpy).toHaveBeenCalledWith('At least one line item is required');
            expect(document.querySelectorAll('.line-item')).toHaveLength(1);
        });

        it('reindexes remaining items after removal', () => {
            SaleForm.addLineItem();
            SaleForm.addLineItem();
            SaleForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const removeButton = items[1].querySelector('.remove-item');
            SaleForm.removeLineItem(removeButton);

            const remainingItems = document.querySelectorAll('.line-item');
            const firstSelect = remainingItems[0].querySelector('select');
            const secondSelect = remainingItems[1].querySelector('select');

            expect(firstSelect.name).toBe('lineItems[0].releaseId');
            expect(secondSelect.name).toBe('lineItems[1].releaseId');
        });
    });

    describe('updateItemNumbers', () => {
        beforeEach(() => {
            document.body.innerHTML = '<div id="lineItemsContainer"></div>';
            SaleForm.initialize([{ id: 1, name: 'Release' }], ['VINYL']);
        });

        it('updates item numbers correctly', () => {
            SaleForm.addLineItem();
            SaleForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const firstNumber = items[0].querySelector('.item-number');
            const secondNumber = items[1].querySelector('.item-number');

            expect(firstNumber.textContent).toBe('1');
            expect(secondNumber.textContent).toBe('2');
        });

        it('hides remove button on first item', () => {
            SaleForm.addLineItem();
            SaleForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const firstRemoveBtn = items[0].querySelector('.remove-item');
            const secondRemoveBtn = items[1].querySelector('.remove-item');

            expect(firstRemoveBtn.style.display).toBe('none');
            expect(secondRemoveBtn.style.display).toBe('inline-block');
        });
    });

    describe('escapeHtml', () => {
        it('escapes HTML special characters', () => {
            expect(SaleForm.escapeHtml('<script>alert("xss")</script>'))
                .toBe('&lt;script&gt;alert("xss")&lt;/script&gt;');

            expect(SaleForm.escapeHtml('Test & Co'))
                .toBe('Test &amp; Co');

            expect(SaleForm.escapeHtml('"quoted"'))
                .toContain('quoted');
        });

        it('handles plain text', () => {
            expect(SaleForm.escapeHtml('Normal text'))
                .toBe('Normal text');
        });
    });
});
