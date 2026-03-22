import { describe, it, expect, vi } from 'vitest';
import { createTag } from './tag-factory.js';

describe('createTag', () => {
  it('creates a span element', () => {
    const tag = createTag(1, 'Artist Name', () => {});
    expect(tag.tagName).toBe('SPAN');
  });

  it('has correct CSS classes', () => {
    const tag = createTag(1, 'Artist Name', () => {});
    expect(tag.className).toBe('badge bg-secondary d-flex align-items-center gap-1');
  });

  it('stores artist ID in dataset', () => {
    const tag = createTag(42, 'Artist Name', () => {});
    expect(tag.dataset.artistId).toBe('42');
  });

  it('displays artist name', () => {
    const tag = createTag(1, 'Test Artist', () => {});
    expect(tag.textContent).toContain('Test Artist');
  });

  it('contains a remove button', () => {
    const tag = createTag(1, 'Artist Name', () => {});
    const button = tag.querySelector('button');
    expect(button).not.toBeNull();
    expect(button.type).toBe('button');
    expect(button.getAttribute('aria-label')).toBe('Remove');
  });

  it('calls onRemove callback when button is clicked', () => {
    const onRemove = vi.fn();
    const tag = createTag(1, 'Artist Name', onRemove);
    const button = tag.querySelector('button');

    button.click();

    expect(onRemove).toHaveBeenCalledTimes(1);
  });

  it('button has correct styling classes', () => {
    const tag = createTag(1, 'Artist Name', () => {});
    const button = tag.querySelector('button');
    expect(button.className).toBe('btn-close btn-close-white');
    expect(button.style.fontSize).toBe('0.6rem');
  });
});
