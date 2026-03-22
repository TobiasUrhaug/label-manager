/**
 * Finds an artist by ID in an array of artists.
 * @param {Array<{id: number|string, artistName: string}>} artists - Array of artist objects
 * @param {number|string} id - The artist ID to find
 * @returns {Object|undefined} The found artist or undefined
 */
export function getArtistById(artists, id) {
  return artists.find(a => String(a.id) === String(id));
}

/**
 * Builds HTML option elements from an array of artists.
 * @param {Array<{id: number|string, artistName: string}>} artists - Array of artist objects
 * @returns {string} HTML string of option elements
 */
export function buildArtistOptions(artists) {
  return artists
    .map(artist => `<option value="${artist.id}">${escapeHtml(artist.artistName)}</option>`)
    .join('');
}

/**
 * Escapes HTML special characters to prevent XSS.
 * @param {string} text - The text to escape
 * @returns {string} Escaped text
 */
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
