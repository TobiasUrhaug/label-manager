import { buildArtistOptions } from './artist-utils.js';

/**
 * Builds the HTML for a track row.
 * @param {number} index - The track index (for form field names)
 * @param {Array} artists - Array of artist objects for the select dropdown
 * @param {Object} [trackData] - Optional existing track data for edit mode
 * @param {string} [trackData.name] - Track name
 * @param {Object} [trackData.duration] - Track duration object
 * @param {number} [trackData.duration.totalSeconds] - Duration in seconds
 * @returns {string} HTML string for the track row
 */
export function buildTrackRowHtml(index, artists, trackData = null) {
  const nameValue = trackData?.name || '';
  const durationValue = trackData?.duration
    ? formatDurationForInput(trackData.duration.totalSeconds)
    : '';

  return `
    <div class="row g-2 align-items-end">
      <div class="col-md-3">
        <label class="form-label">Artist</label>
        <div class="input-group">
          <select class="form-select track-artist-select" data-testid="track-artist-select">
            <option value="">Select...</option>
            ${buildArtistOptions(artists)}
          </select>
          <button class="btn btn-outline-secondary btn-sm add-track-artist-btn" type="button">+</button>
        </div>
      </div>
      <div class="col-md-3">
        <label class="form-label">Remixer (optional)</label>
        <div class="input-group">
          <select class="form-select track-remixer-select" data-testid="track-remixer-select">
            <option value="">Select...</option>
            ${buildArtistOptions(artists)}
          </select>
          <button class="btn btn-outline-secondary btn-sm add-track-remixer-btn" type="button">+</button>
        </div>
      </div>
      <div class="col-md-3">
        <label class="form-label">Name</label>
        <input type="text" class="form-control" name="tracks[${index}].name" value="${escapeAttr(nameValue)}" data-testid="track-name-input" required>
      </div>
      <div class="col-md-2">
        <label class="form-label">Duration</label>
        <input type="text" class="form-control" name="tracks[${index}].duration" placeholder="MM:SS" pattern="[0-9]+:[0-5][0-9]" value="${durationValue}" data-testid="track-duration-input" required>
      </div>
      <div class="col-md-1 text-end">
        <button type="button" class="btn btn-outline-danger btn-sm remove-track">X</button>
      </div>
    </div>
    <div class="track-artist-tags d-flex flex-wrap gap-1 mt-2"></div>
    <div class="track-artist-inputs"></div>
    <div class="track-remixer-tags d-flex flex-wrap gap-1 mt-2"></div>
    <div class="track-remixer-inputs"></div>
  `;
}

/**
 * Creates a track row DOM element.
 * @param {number} index - The track index
 * @param {Array} artists - Array of artist objects
 * @param {Object} [trackData] - Optional existing track data
 * @returns {HTMLDivElement} The track row element
 */
export function createTrackRow(index, artists, trackData = null) {
  const row = document.createElement('div');
  row.className = 'track-row mb-3 p-2 border rounded';
  row.dataset.trackIndex = index;
  row.innerHTML = buildTrackRowHtml(index, artists, trackData);
  return row;
}

/**
 * Formats seconds to MM:SS for input fields.
 * @param {number} totalSeconds - Duration in seconds
 * @returns {string} Formatted duration
 */
function formatDurationForInput(totalSeconds) {
  if (!totalSeconds && totalSeconds !== 0) return '';
  if (totalSeconds === 0) return '0:00';
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

/**
 * Escapes a string for use in HTML attributes.
 * @param {string} str - String to escape
 * @returns {string} Escaped string
 */
function escapeAttr(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}
