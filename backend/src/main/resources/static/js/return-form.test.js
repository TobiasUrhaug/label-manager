import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ReturnForm } from './return-form.js';

describe('ReturnForm', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        ReturnForm.initialize([], []);
    });

    describe('initialize', () => {
        it('stores releases and formats', () => {
            const releases = [{ id: 1, name: 'Release 1' }];
            const formats = ['VINYL', 'CD'];

            ReturnForm.initialize(releases, formats);

            expect(ReturnForm.releases).toEqual(releases);
            expect(ReturnForm.formats).toEqual(formats);
        });
    });

    describe('createLineItemHTML', () => {
        it('creates HTML with correct structure', () => {
            ReturnForm.initialize(
                [{ id: 1, name: 'Test Release' }],
                ['VINYL', 'CD']
            );

            const html = ReturnForm.createLineItemHTML(0);

            expect(html).toContain('Item <span class="item-number">1</span>');
            expect(html).toContain('lineItems0.releaseId');
            expect(html).toContain('lineItems[0].releaseId');
            expect(html).toContain('Test Release');
            expect(html).toContain('VINYL');
            expect(html).toContain('CD');
        });

        it('does not contain a price field', () => {
            ReturnForm.initialize([{ id: 1, name: 'Release' }], ['VINYL']);

            const html = ReturnForm.createLineItemHTML(0);

            expect(html).not.toContain('unitPrice');
            expect(html).not.toContain('price-input');
        });

        it('escapes HTML in release names', () => {
            ReturnForm.initialize(
                [{ id: 1, name: '<script>alert("xss")</script>' }],
                ['VINYL']
            );

            const html = ReturnForm.createLineItemHTML(0);

            expect(html).not.toContain('<script>');
            expect(html).toContain('&lt;script&gt;');
        });
    });

    describe('addLineItem', () => {
        beforeEach(() => {
            document.body.innerHTML = '<div id="lineItemsContainer"></div>';
            ReturnForm.initialize(
                [{ id: 1, name: 'Release' }],
                ['VINYL']
            );
        });

        it('adds a new line item to the container', () => {
            const container = document.getElementById('lineItemsContainer');
            expect(container.querySelectorAll('.line-item')).toHaveLength(0);

            ReturnForm.addLineItem();

            expect(container.querySelectorAll('.line-item')).toHaveLength(1);
        });

        it('creates item with correct index', () => {
            ReturnForm.addLineItem();
            ReturnForm.addLineItem();

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
            ReturnForm.initialize([{ id: 1, name: 'Release' }], ['VINYL']);
        });

        it('removes the line item', () => {
            ReturnForm.addLineItem();
            ReturnForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const removeButton = items[1].querySelector('.remove-item');

            ReturnForm.removeLineItem(removeButton);

            expect(document.querySelectorAll('.line-item')).toHaveLength(1);
        });

        it('shows alert and does not remove if only one item', () => {
            ReturnForm.addLineItem();
            const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

            const removeButton = document.querySelector('.remove-item');
            ReturnForm.removeLineItem(removeButton);

            expect(alertSpy).toHaveBeenCalledWith('At least one line item is required');
            expect(document.querySelectorAll('.line-item')).toHaveLength(1);
        });

        it('reindexes remaining items after removal', () => {
            ReturnForm.addLineItem();
            ReturnForm.addLineItem();
            ReturnForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const removeButton = items[1].querySelector('.remove-item');
            ReturnForm.removeLineItem(removeButton);

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
            ReturnForm.initialize([{ id: 1, name: 'Release' }], ['VINYL']);
        });

        it('updates item numbers correctly', () => {
            ReturnForm.addLineItem();
            ReturnForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const firstNumber = items[0].querySelector('.item-number');
            const secondNumber = items[1].querySelector('.item-number');

            expect(firstNumber.textContent).toBe('1');
            expect(secondNumber.textContent).toBe('2');
        });

        it('hides remove button on first item', () => {
            ReturnForm.addLineItem();
            ReturnForm.addLineItem();

            const items = document.querySelectorAll('.line-item');
            const firstRemoveBtn = items[0].querySelector('.remove-item');
            const secondRemoveBtn = items[1].querySelector('.remove-item');

            expect(firstRemoveBtn.style.display).toBe('none');
            expect(secondRemoveBtn.style.display).toBe('inline-block');
        });
    });

    describe('escapeHtml', () => {
        it('escapes HTML special characters', () => {
            expect(ReturnForm.escapeHtml('<script>alert("xss")</script>'))
                .toBe('&lt;script&gt;alert("xss")&lt;/script&gt;');

            expect(ReturnForm.escapeHtml('Test & Co'))
                .toBe('Test &amp; Co');
        });

        it('handles plain text', () => {
            expect(ReturnForm.escapeHtml('Normal text'))
                .toBe('Normal text');
        });
    });
});
