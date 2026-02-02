import { describe, it, expect } from 'vitest';
import { getArtistById, buildArtistOptions } from './artist-utils.js';

describe('getArtistById', () => {
  const artists = [
    { id: 1, artistName: 'Artist One' },
    { id: 2, artistName: 'Artist Two' },
    { id: 3, artistName: 'Artist Three' },
  ];

  it('finds artist by numeric id', () => {
    const result = getArtistById(artists, 2);
    expect(result).toEqual({ id: 2, artistName: 'Artist Two' });
  });

  it('finds artist by string id', () => {
    const result = getArtistById(artists, '2');
    expect(result).toEqual({ id: 2, artistName: 'Artist Two' });
  });

  it('returns undefined for non-existent id', () => {
    const result = getArtistById(artists, 99);
    expect(result).toBeUndefined();
  });

  it('returns undefined for empty array', () => {
    const result = getArtistById([], 1);
    expect(result).toBeUndefined();
  });

  it('handles artists with string ids', () => {
    const stringIdArtists = [{ id: 'abc', artistName: 'String ID Artist' }];
    const result = getArtistById(stringIdArtists, 'abc');
    expect(result).toEqual({ id: 'abc', artistName: 'String ID Artist' });
  });
});

describe('buildArtistOptions', () => {
  it('builds option elements from artists', () => {
    const artists = [
      { id: 1, artistName: 'Artist One' },
      { id: 2, artistName: 'Artist Two' },
    ];
    const result = buildArtistOptions(artists);
    expect(result).toBe(
      '<option value="1">Artist One</option><option value="2">Artist Two</option>'
    );
  });

  it('returns empty string for empty array', () => {
    const result = buildArtistOptions([]);
    expect(result).toBe('');
  });

  it('escapes HTML in artist names', () => {
    const artists = [{ id: 1, artistName: '<script>alert("xss")</script>' }];
    const result = buildArtistOptions(artists);
    expect(result).not.toContain('<script>');
    expect(result).toContain('&lt;script&gt;');
  });

  it('escapes ampersands in artist names', () => {
    const artists = [{ id: 1, artistName: 'Tom & Jerry' }];
    const result = buildArtistOptions(artists);
    expect(result).toContain('Tom &amp; Jerry');
  });
});
