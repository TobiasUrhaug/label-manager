/**
 * Validates cost form values for consistency.
 * @param {number} netAmount - Net amount before VAT
 * @param {number} vatRate - VAT rate as decimal (e.g., 0.21 for 21%)
 * @param {number} vatAmount - VAT amount
 * @param {number} grossAmount - Gross amount (net + VAT)
 * @returns {string[]} Array of error messages (empty if valid)
 */
export function validateCostForm(netAmount, vatRate, vatAmount, grossAmount) {
  const errors = [];

  const expectedVat = Math.round(netAmount * vatRate * 100) / 100;
  const expectedGross = Math.round((netAmount + vatAmount) * 100) / 100;

  if (Math.abs(vatAmount - expectedVat) > 0.01) {
    errors.push(
      `VAT amount (${vatAmount.toFixed(2)}) should be ${expectedVat.toFixed(2)} (net × rate)`
    );
  }

  if (Math.abs(grossAmount - expectedGross) > 0.01) {
    errors.push(
      `Gross amount (${grossAmount.toFixed(2)}) should be ${expectedGross.toFixed(2)} (net + VAT)`
    );
  }

  return errors;
}
