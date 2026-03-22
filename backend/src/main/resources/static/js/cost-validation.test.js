import { describe, it, expect } from 'vitest';
import { validateCostForm } from './cost-validation.js';

describe('validateCostForm', () => {
  it('returns empty array for valid calculations', () => {
    // 100 net + 21% VAT = 21 VAT, 121 gross
    const errors = validateCostForm(100, 0.21, 21, 121);
    expect(errors).toEqual([]);
  });

  it('returns empty array for 0% VAT', () => {
    const errors = validateCostForm(100, 0, 0, 100);
    expect(errors).toEqual([]);
  });

  it('returns error for incorrect VAT amount', () => {
    // 100 net + 21% VAT should be 21, not 25
    const errors = validateCostForm(100, 0.21, 25, 125);
    expect(errors).toHaveLength(1);
    expect(errors[0]).toContain('VAT amount');
    expect(errors[0]).toContain('21.00');
  });

  it('returns error for incorrect gross amount', () => {
    // 100 net + 21 VAT should be 121, not 130
    const errors = validateCostForm(100, 0.21, 21, 130);
    expect(errors).toHaveLength(1);
    expect(errors[0]).toContain('Gross amount');
    expect(errors[0]).toContain('121.00');
  });

  it('returns both errors when both calculations are wrong', () => {
    const errors = validateCostForm(100, 0.21, 30, 150);
    expect(errors).toHaveLength(2);
    expect(errors[0]).toContain('VAT amount');
    expect(errors[1]).toContain('Gross amount');
  });

  it('allows small rounding differences (within 0.01)', () => {
    // Small rounding difference should be acceptable
    const errors = validateCostForm(100, 0.21, 21.005, 121.005);
    expect(errors).toEqual([]);
  });

  it('handles 25% VAT rate', () => {
    // 100 net + 25% VAT = 25 VAT, 125 gross
    const errors = validateCostForm(100, 0.25, 25, 125);
    expect(errors).toEqual([]);
  });

  it('handles 9% VAT rate', () => {
    // 100 net + 9% VAT = 9 VAT, 109 gross
    const errors = validateCostForm(100, 0.09, 9, 109);
    expect(errors).toEqual([]);
  });

  it('handles decimal net amounts', () => {
    // 99.50 net + 21% VAT = 20.895 (rounds to 20.90), gross = 120.40
    const errors = validateCostForm(99.5, 0.21, 20.9, 120.4);
    expect(errors).toEqual([]);
  });
});
