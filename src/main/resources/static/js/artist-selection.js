import { getArtistById } from './artist-utils.js';
import { createTag } from './tag-factory.js';

/**
 * Creates an artist selection manager for handling artist tags and hidden inputs.
 * @param {Object} config - Configuration object
 * @param {Array} config.artists - Array of all available artists
 * @param {HTMLElement} config.selectElement - The select dropdown element
 * @param {HTMLElement} config.tagsContainer - Container for artist tags
 * @param {HTMLElement} config.inputsContainer - Container for hidden inputs
 * @param {string} config.inputName - Name attribute for hidden inputs (e.g., 'artistIds')
 * @param {function} [config.onAdd] - Optional callback when artist is added
 * @param {function} [config.onRemove] - Optional callback when artist is removed
 * @returns {Object} Artist selection manager with methods
 */
export function createArtistSelection(config) {
  const {
    artists,
    selectElement,
    tagsContainer,
    inputsContainer,
    inputName,
    onAdd,
    onRemove,
  } = config;

  const selectedIds = [];

  /**
   * Adds an artist to the selection.
   * @param {string|number} artistId - The artist ID to add
   * @returns {boolean} True if added, false if already selected or invalid
   */
  function add(artistId) {
    if (!artistId) return false;

    const id = String(artistId);
    if (selectedIds.includes(id)) return false;

    const artist = getArtistById(artists, id);
    if (!artist) return false;

    selectedIds.push(id);

    const tag = createTag(id, artist.artistName, () => remove(id));
    tagsContainer.appendChild(tag);

    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = inputName;
    input.value = id;
    inputsContainer.appendChild(input);

    if (selectElement) {
      selectElement.value = '';
    }

    if (onAdd) {
      onAdd(id, artist);
    }

    return true;
  }

  /**
   * Removes an artist from the selection.
   * @param {string|number} artistId - The artist ID to remove
   * @returns {boolean} True if removed, false if not found
   */
  function remove(artistId) {
    const id = String(artistId);
    const index = selectedIds.indexOf(id);
    if (index === -1) return false;

    selectedIds.splice(index, 1);

    const tag = tagsContainer.querySelector(`[data-artist-id="${id}"]`);
    if (tag) tag.remove();

    const input = inputsContainer.querySelector(`input[value="${id}"]`);
    if (input) input.remove();

    if (onRemove) {
      onRemove(id);
    }

    return true;
  }

  /**
   * Clears all selected artists.
   */
  function clear() {
    selectedIds.length = 0;
    tagsContainer.innerHTML = '';
    inputsContainer.innerHTML = '';
    if (selectElement) {
      selectElement.value = '';
    }
  }

  /**
   * Gets the list of currently selected artist IDs.
   * @returns {string[]} Array of selected artist IDs
   */
  function getSelectedIds() {
    return [...selectedIds];
  }

  /**
   * Checks if an artist is selected.
   * @param {string|number} artistId - The artist ID to check
   * @returns {boolean} True if selected
   */
  function isSelected(artistId) {
    return selectedIds.includes(String(artistId));
  }

  // Auto-add on select change
  if (selectElement) {
    selectElement.addEventListener('change', () => {
      add(selectElement.value);
    });
  }

  return {
    add,
    remove,
    clear,
    getSelectedIds,
    isSelected,
  };
}

/**
 * Creates a track artist selection manager (simplified version for track rows).
 * @param {Object} config - Configuration object
 * @param {Array} config.artists - Array of all available artists
 * @param {HTMLElement} config.trackRow - The track row element
 * @param {number} config.trackIndex - The track index for input naming
 * @param {function} [config.onAdd] - Optional callback when artist is added
 * @returns {Object} Track artist selection manager
 */
export function createTrackArtistSelection(config) {
  const { artists, trackRow, trackIndex, onAdd } = config;

  const tagsContainer = trackRow.querySelector('.track-artist-tags');
  const inputsContainer = trackRow.querySelector('.track-artist-inputs');
  const selectElement = trackRow.querySelector('.track-artist-select');

  return createArtistSelection({
    artists,
    selectElement,
    tagsContainer,
    inputsContainer,
    inputName: `tracks[${trackIndex}].artistIds`,
    onAdd,
  });
}
