import { describe, it, expect } from 'vitest';
import { formatDuration } from './duration.js';

describe('formatDuration', () => {
  it('formats 0 seconds as 0:00', () => {
    expect(formatDuration(0)).toBe('0:00');
  });

  it('formats seconds under a minute with leading zero', () => {
    expect(formatDuration(5)).toBe('0:05');
    expect(formatDuration(45)).toBe('0:45');
  });

  it('formats exact minutes', () => {
    expect(formatDuration(60)).toBe('1:00');
    expect(formatDuration(120)).toBe('2:00');
    expect(formatDuration(300)).toBe('5:00');
  });

  it('formats minutes and seconds', () => {
    expect(formatDuration(90)).toBe('1:30');
    expect(formatDuration(185)).toBe('3:05');
    expect(formatDuration(225)).toBe('3:45');
  });

  it('formats durations over 10 minutes', () => {
    expect(formatDuration(600)).toBe('10:00');
    expect(formatDuration(754)).toBe('12:34');
  });

  it('returns empty string for null or undefined', () => {
    expect(formatDuration(null)).toBe('');
    expect(formatDuration(undefined)).toBe('');
  });
});
