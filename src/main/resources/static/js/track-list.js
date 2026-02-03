import { createTrackRow } from './track-row.js';
import { createTrackArtistSelection } from './artist-selection.js';

/**
 * Creates a track list manager for handling dynamic track rows.
 * @param {Object} config - Configuration object
 * @param {HTMLElement} config.container - Container element for track rows
 * @param {HTMLElement} config.addButton - Button to add new tracks
 * @param {Array} config.artists - Array of all available artists
 * @param {function} [config.onTrackArtistAdd] - Callback when artist added to a track
 * @param {function} [config.getSelectedReleaseArtists] - Function returning currently selected release artists
 * @returns {Object} Track list manager with methods
 */
export function createTrackList(config) {
  const {
    container,
    addButton,
    artists,
    onTrackArtistAdd,
    getSelectedReleaseArtists,
  } = config;

  let currentIndex = 0;
  const trackSelections = new Map(); // trackIndex -> artist selection manager

  /**
   * Updates visibility of remove buttons based on track count.
   */
  function updateRemoveButtons() {
    const rows = container.querySelectorAll('.track-row');
    rows.forEach(row => {
      const removeBtn = row.querySelector('.remove-track');
      if (removeBtn) {
        removeBtn.style.display = rows.length > 1 ? 'inline-block' : 'none';
      }
    });
  }

  /**
   * Reindexes all tracks after removal.
   */
  function reindexTracks() {
    const rows = container.querySelectorAll('.track-row');
    rows.forEach((row, index) => {
      row.dataset.trackIndex = index;
      row.querySelectorAll('input[name]').forEach(input => {
        const name = input.getAttribute('name');
        if (name) {
          input.setAttribute('name', name.replace(/tracks\[\d+\]/, `tracks[${index}]`));
        }
      });
    });
    currentIndex = rows.length > 0 ? rows.length - 1 : 0;
  }

  /**
   * Creates artist selection for a track row.
   * @param {HTMLElement} row - The track row element
   * @param {number} index - The track index
   * @returns {Object} Artist selection manager
   */
  function setupTrackArtistSelection(row, index) {
    const selection = createTrackArtistSelection({
      artists,
      trackRow: row,
      trackIndex: index,
      onAdd: onTrackArtistAdd,
    });
    trackSelections.set(index, selection);
    return selection;
  }

  /**
   * Adds a new track row.
   * @param {Object} [trackData] - Optional existing track data for edit mode
   * @returns {HTMLElement} The created track row
   */
  function addTrack(trackData = null) {
    currentIndex++;
    const row = createTrackRow(currentIndex, artists, trackData);
    container.appendChild(row);

    const selection = setupTrackArtistSelection(row, currentIndex);

    // Pre-select release artists for new tracks
    if (!trackData && getSelectedReleaseArtists) {
      const releaseArtists = getSelectedReleaseArtists();
      releaseArtists.forEach(artistId => selection.add(artistId));
    }

    // Pre-populate existing track artists
    if (trackData?.artists) {
      trackData.artists.forEach(artist => selection.add(artist.id));
    }

    updateRemoveButtons();
    return row;
  }

  /**
   * Removes a track row.
   * @param {HTMLElement} row - The track row to remove
   */
  function removeTrack(row) {
    const index = parseInt(row.dataset.trackIndex, 10);
    trackSelections.delete(index);
    row.remove();
    reindexTracks();
    updateRemoveButtons();
  }

  /**
   * Clears all tracks and resets to initial state.
   */
  function clear() {
    container.innerHTML = '';
    trackSelections.clear();
    currentIndex = -1;
  }

  /**
   * Initializes the track list with existing tracks or a single empty track.
   * @param {Array} [existingTracks] - Optional array of existing track data
   */
  function initialize(existingTracks = []) {
    clear();

    if (existingTracks.length > 0) {
      existingTracks.forEach((track, index) => {
        currentIndex = index - 1; // Will be incremented in addTrack
        addTrack(track);
      });
    } else {
      currentIndex = -1;
      addTrack();
    }
  }

  // Event delegation for track container
  container.addEventListener('click', (e) => {
    if (e.target.classList.contains('remove-track')) {
      const row = e.target.closest('.track-row');
      if (row) removeTrack(row);
    }
    if (e.target.classList.contains('add-track-artist-btn')) {
      const row = e.target.closest('.track-row');
      const select = row?.querySelector('.track-artist-select');
      const index = parseInt(row?.dataset.trackIndex, 10);
      const selection = trackSelections.get(index);
      if (select && selection) {
        selection.add(select.value);
      }
    }
  });

  container.addEventListener('change', (e) => {
    if (e.target.classList.contains('track-artist-select')) {
      const row = e.target.closest('.track-row');
      const index = parseInt(row?.dataset.trackIndex, 10);
      const selection = trackSelections.get(index);
      if (selection) {
        selection.add(e.target.value);
      }
    }
  });

  // Add track button
  if (addButton) {
    addButton.addEventListener('click', () => addTrack());
  }

  return {
    addTrack,
    removeTrack,
    clear,
    initialize,
    updateRemoveButtons,
    reindexTracks,
  };
}
