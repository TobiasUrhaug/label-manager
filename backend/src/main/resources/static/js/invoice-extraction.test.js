import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { setupInvoiceExtraction } from './invoice-extraction.js';

describe('invoice-extraction', () => {
  let fileInput;
  let extractButton;
  let netAmountField;
  let vatAmountField;
  let grossAmountField;
  let incurredOnField;
  let documentReferenceField;
  let vatRateField;

  beforeEach(() => {
    // Create DOM elements
    fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.id = 'document';

    extractButton = document.createElement('button');
    extractButton.id = 'extractInvoiceBtn';

    netAmountField = document.createElement('input');
    netAmountField.id = 'netAmount';

    vatAmountField = document.createElement('input');
    vatAmountField.id = 'vatAmount';

    grossAmountField = document.createElement('input');
    grossAmountField.id = 'grossAmount';

    incurredOnField = document.createElement('input');
    incurredOnField.id = 'incurredOn';

    documentReferenceField = document.createElement('input');
    documentReferenceField.id = 'documentReference';

    vatRateField = document.createElement('select');
    vatRateField.id = 'vatRate';
    vatRateField.innerHTML = `
      <option value="0.00">0%</option>
      <option value="0.09">9%</option>
      <option value="0.21">21%</option>
      <option value="0.25">25%</option>
    `;

    // Add to document
    document.body.appendChild(fileInput);
    document.body.appendChild(extractButton);
    document.body.appendChild(netAmountField);
    document.body.appendChild(vatAmountField);
    document.body.appendChild(grossAmountField);
    document.body.appendChild(incurredOnField);
    document.body.appendChild(documentReferenceField);
    document.body.appendChild(vatRateField);
  });

  afterEach(() => {
    document.body.innerHTML = '';
    vi.restoreAllMocks();
  });

  function createConfig() {
    return {
      fileInput,
      extractButton,
      fieldIds: {
        netAmount: 'netAmount',
        vatAmount: 'vatAmount',
        grossAmount: 'grossAmount',
        vatRate: 'vatRate',
        incurredOn: 'incurredOn',
        documentReference: 'documentReference',
      },
      csrfToken: 'test-csrf-token',
    };
  }

  describe('extract button visibility', () => {
    it('hides extract button initially', () => {
      setupInvoiceExtraction(createConfig());

      expect(extractButton.classList.contains('d-none')).toBe(true);
    });

    it('shows extract button when PDF file is selected', () => {
      setupInvoiceExtraction(createConfig());

      const file = new File(['test'], 'invoice.pdf', { type: 'application/pdf' });
      Object.defineProperty(fileInput, 'files', { value: [file] });
      fileInput.dispatchEvent(new Event('change'));

      expect(extractButton.classList.contains('d-none')).toBe(false);
    });

    it('shows extract button when PNG image is selected', () => {
      setupInvoiceExtraction(createConfig());

      const file = new File(['test'], 'invoice.png', { type: 'image/png' });
      Object.defineProperty(fileInput, 'files', { value: [file] });
      fileInput.dispatchEvent(new Event('change'));

      expect(extractButton.classList.contains('d-none')).toBe(false);
    });

    it('hides extract button for unsupported file types', () => {
      setupInvoiceExtraction(createConfig());

      const file = new File(['test'], 'invoice.doc', { type: 'application/msword' });
      Object.defineProperty(fileInput, 'files', { value: [file] });
      fileInput.dispatchEvent(new Event('change'));

      expect(extractButton.classList.contains('d-none')).toBe(true);
    });
  });

  describe('extraction API call', () => {
    it('calls API and populates fields on successful extraction', async () => {
      const mockResponse = {
        netAmount: 100.0,
        vatAmount: 21.0,
        vatRate: 21,
        grossAmount: 121.0,
        invoiceDate: '2024-01-15',
        invoiceReference: 'INV-2024-001',
        currency: 'EUR',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockResponse),
      });

      const { extractAndPopulate } = setupInvoiceExtraction(createConfig());

      const file = new File(['test'], 'invoice.pdf', { type: 'application/pdf' });
      Object.defineProperty(fileInput, 'files', { value: [file] });

      await extractAndPopulate();

      expect(netAmountField.value).toBe('100');
      expect(vatAmountField.value).toBe('21');
      expect(grossAmountField.value).toBe('121');
      expect(incurredOnField.value).toBe('2024-01-15');
      expect(documentReferenceField.value).toBe('INV-2024-001');
      expect(vatRateField.value).toBe('0.21');
    });

    it('handles partial extraction', async () => {
      const mockResponse = {
        netAmount: 50.0,
        vatAmount: null,
        grossAmount: null,
        invoiceDate: null,
        invoiceReference: null,
        currency: 'EUR',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockResponse),
      });

      const { extractAndPopulate } = setupInvoiceExtraction(createConfig());

      const file = new File(['test'], 'invoice.pdf', { type: 'application/pdf' });
      Object.defineProperty(fileInput, 'files', { value: [file] });

      await extractAndPopulate();

      expect(netAmountField.value).toBe('50');
      expect(vatAmountField.value).toBe('');
      expect(grossAmountField.value).toBe('');
    });

    it('handles API error gracefully', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
      });

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const { extractAndPopulate } = setupInvoiceExtraction(createConfig());

      const file = new File(['test'], 'invoice.pdf', { type: 'application/pdf' });
      Object.defineProperty(fileInput, 'files', { value: [file] });

      await extractAndPopulate();

      expect(consoleSpy).toHaveBeenCalled();
      // Fields should remain empty
      expect(netAmountField.value).toBe('');
    });
  });

  describe('loading state', () => {
    it('shows spinner while extracting', async () => {
      let resolvePromise;
      const pendingPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      global.fetch = vi.fn().mockReturnValue(pendingPromise);

      const { extractAndPopulate } = setupInvoiceExtraction(createConfig());

      const file = new File(['test'], 'invoice.pdf', { type: 'application/pdf' });
      Object.defineProperty(fileInput, 'files', { value: [file] });

      const extractPromise = extractAndPopulate();

      // Check loading state
      expect(extractButton.disabled).toBe(true);
      expect(extractButton.innerHTML).toContain('spinner-border');
      expect(extractButton.innerHTML).toContain('Extracting...');

      // Resolve and wait
      resolvePromise({
        ok: true,
        json: () => Promise.resolve({ netAmount: 100 }),
      });
      await extractPromise;

      // Check loading state cleared
      expect(extractButton.disabled).toBe(false);
      expect(extractButton.innerHTML).toContain('Extract from Document');
    });
  });
});
