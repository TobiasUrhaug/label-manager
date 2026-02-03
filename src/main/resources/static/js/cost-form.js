import { validateCostForm } from './cost-validation.js';

/**
 * Sets up cost form validation with error display.
 * @param {Object} config - Configuration object
 * @param {HTMLFormElement} config.form - The form element
 * @param {HTMLElement} config.errorContainer - Container for error messages
 * @param {Object} config.fields - Field selectors
 * @param {string} config.fields.netAmount - Selector for net amount input
 * @param {string} config.fields.vatRate - Selector for VAT rate select
 * @param {string} config.fields.vatAmount - Selector for VAT amount input
 * @param {string} config.fields.grossAmount - Selector for gross amount input
 * @returns {Object} Form handler with methods
 */
export function setupCostFormValidation(config) {
  const { form, errorContainer, fields } = config;

  function getFieldValue(selector) {
    const el = form.querySelector(selector);
    return parseFloat(el?.value) || 0;
  }

  function showErrors(errors) {
    errorContainer.innerHTML = errors.join('<br>');
    errorContainer.classList.remove('d-none');
  }

  function hideErrors() {
    errorContainer.classList.add('d-none');
  }

  function validate() {
    const netAmount = getFieldValue(fields.netAmount);
    const vatRate = getFieldValue(fields.vatRate);
    const vatAmount = getFieldValue(fields.vatAmount);
    const grossAmount = getFieldValue(fields.grossAmount);

    return validateCostForm(netAmount, vatRate, vatAmount, grossAmount);
  }

  form.addEventListener('submit', (e) => {
    const errors = validate();
    if (errors.length > 0) {
      e.preventDefault();
      showErrors(errors);
    } else {
      hideErrors();
    }
  });

  return {
    validate,
    showErrors,
    hideErrors,
  };
}

/**
 * Sets up an edit cost modal with form population and validation.
 * @param {Object} config - Configuration object
 * @param {HTMLElement} config.modal - The modal element
 * @param {HTMLFormElement} config.form - The form element
 * @param {HTMLElement} config.errorContainer - Container for error messages
 * @param {string} config.actionUrlTemplate - URL template with {costId} placeholder
 * @param {Object} config.fieldIds - IDs for form fields
 * @returns {Object} Modal handler with methods
 */
export function setupEditCostModal(config) {
  const { modal, form, errorContainer, actionUrlTemplate, fieldIds } = config;

  const fieldMapping = {
    costType: fieldIds.costType,
    description: fieldIds.description,
    incurredOn: fieldIds.incurredOn,
    netAmount: fieldIds.netAmount,
    vatAmount: fieldIds.vatAmount,
    vatRate: fieldIds.vatRate,
    grossAmount: fieldIds.grossAmount,
    documentReference: fieldIds.documentReference,
  };

  // Set up validation
  const validation = setupCostFormValidation({
    form,
    errorContainer,
    fields: {
      netAmount: `#${fieldIds.netAmount}`,
      vatRate: `#${fieldIds.vatRate}`,
      vatAmount: `#${fieldIds.vatAmount}`,
      grossAmount: `#${fieldIds.grossAmount}`,
    },
  });

  /**
   * Populates the form with cost data and shows the modal.
   * @param {Object} costData - Cost data from button data attributes
   */
  function open(costData) {
    form.action = actionUrlTemplate.replace('{costId}', costData.costId);

    Object.entries(fieldMapping).forEach(([dataKey, fieldId]) => {
      const field = document.getElementById(fieldId);
      if (field) {
        field.value = costData[dataKey] || '';
      }
    });

    new bootstrap.Modal(modal).show();
  }

  // Clear errors when modal is hidden
  modal.addEventListener('hidden.bs.modal', () => {
    validation.hideErrors();
  });

  return {
    open,
    validation,
  };
}

/**
 * Sets up click handlers for edit cost buttons.
 * @param {string} buttonSelector - CSS selector for edit buttons
 * @param {Object} modalHandler - Modal handler from setupEditCostModal
 */
export function setupEditCostButtons(buttonSelector, modalHandler) {
  document.querySelectorAll(buttonSelector).forEach(btn => {
    btn.addEventListener('click', function() {
      const costData = {
        costId: this.dataset.costId,
        costType: this.dataset.costType,
        description: this.dataset.description,
        incurredOn: this.dataset.incurredOn,
        netAmount: this.dataset.netAmount,
        vatAmount: this.dataset.vatAmount,
        vatRate: this.dataset.vatRate,
        grossAmount: this.dataset.grossAmount,
        documentReference: this.dataset.documentReference,
      };
      modalHandler.open(costData);
    });
  });
}
