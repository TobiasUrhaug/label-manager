/**
 * Module for extracting invoice data from uploaded documents.
 */

const EXTRACTION_API_URL = '/api/costs/extract';
const EXTRACTABLE_TYPES = ['application/pdf', 'image/png', 'image/jpeg'];
const HIGHLIGHT_CLASS = 'extracted-field';
const HIGHLIGHT_DURATION_MS = 3000;

/**
 * Sets up invoice extraction for a cost form.
 * @param {Object} config - Configuration object
 * @param {HTMLInputElement} config.fileInput - The file input element
 * @param {HTMLButtonElement} config.extractButton - The extract button element
 * @param {Object} config.fieldIds - IDs of form fields to populate
 * @param {string} config.csrfToken - CSRF token for the request
 * @returns {Object} Extraction handler with methods
 */
export function setupInvoiceExtraction(config) {
  const { fileInput, extractButton, fieldIds, csrfToken } = config;

  // Initially hide the extract button
  extractButton.classList.add('d-none');

  // Show/hide extract button based on file selection
  fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    if (file && EXTRACTABLE_TYPES.includes(file.type)) {
      extractButton.classList.remove('d-none');
    } else {
      extractButton.classList.add('d-none');
    }
  });

  // Handle extract button click
  extractButton.addEventListener('click', async (e) => {
    e.preventDefault();
    await extractAndPopulate();
  });

  /**
   * Extracts data from the uploaded file and populates form fields.
   */
  async function extractAndPopulate() {
    const file = fileInput.files[0];
    if (!file) return;

    setLoading(true);

    try {
      const data = await callExtractionApi(file);
      populateFields(data);
    } catch (error) {
      console.error('Extraction failed:', error);
      // Silently fail - user can still fill manually
    } finally {
      setLoading(false);
    }
  }

  /**
   * Calls the extraction API with the uploaded file.
   * @param {File} file - The file to extract from
   * @returns {Promise<Object>} Extracted invoice data
   */
  async function callExtractionApi(file) {
    const formData = new FormData();
    formData.append('document', file);

    const response = await fetch(EXTRACTION_API_URL, {
      method: 'POST',
      headers: {
        'X-CSRF-TOKEN': csrfToken,
      },
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Extraction failed: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Populates form fields with extracted data.
   * @param {Object} data - Extracted invoice data
   */
  function populateFields(data) {
    const mappings = [
      { key: 'netAmount', fieldId: fieldIds.netAmount },
      { key: 'vatAmount', fieldId: fieldIds.vatAmount },
      { key: 'grossAmount', fieldId: fieldIds.grossAmount },
      { key: 'invoiceDate', fieldId: fieldIds.incurredOn },
      { key: 'invoiceReference', fieldId: fieldIds.documentReference },
    ];

    // Handle VAT rate specially (convert from percentage to decimal)
    if (data.vatRate != null) {
      const vatRateField = document.getElementById(fieldIds.vatRate);
      if (vatRateField) {
        const rateDecimal = (data.vatRate / 100).toFixed(2);
        const option = vatRateField.querySelector(`option[value="${rateDecimal}"]`);
        if (option) {
          vatRateField.value = rateDecimal;
          highlightField(vatRateField);
        }
      }
    }

    mappings.forEach(({ key, fieldId }) => {
      if (data[key] != null) {
        const field = document.getElementById(fieldId);
        if (field) {
          field.value = data[key];
          highlightField(field);
        }
      }
    });
  }

  /**
   * Briefly highlights a field to show it was auto-filled.
   * @param {HTMLElement} field - The field to highlight
   */
  function highlightField(field) {
    field.classList.add(HIGHLIGHT_CLASS);
    setTimeout(() => {
      field.classList.remove(HIGHLIGHT_CLASS);
    }, HIGHLIGHT_DURATION_MS);
  }

  /**
   * Sets the loading state of the extract button.
   * @param {boolean} loading - Whether extraction is in progress
   */
  function setLoading(loading) {
    if (loading) {
      extractButton.disabled = true;
      extractButton.innerHTML = `
        <span class="spinner-border spinner-border-sm me-1" role="status"></span>
        Extracting...
      `;
    } else {
      extractButton.disabled = false;
      extractButton.innerHTML = 'Extract from Document';
    }
  }

  return {
    extractAndPopulate,
  };
}
