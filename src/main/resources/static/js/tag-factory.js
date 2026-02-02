/**
 * Creates a badge tag element for an artist with a remove button.
 * @param {number|string} artistId - The artist's ID
 * @param {string} artistName - The artist's display name
 * @param {function} onRemove - Callback function when remove button is clicked
 * @returns {HTMLSpanElement} The created tag element
 */
export function createTag(artistId, artistName, onRemove) {
  const tag = document.createElement('span');
  tag.className = 'badge bg-secondary d-flex align-items-center gap-1';
  tag.dataset.artistId = artistId;

  const nameText = document.createTextNode(artistName + ' ');
  tag.appendChild(nameText);

  const removeBtn = document.createElement('button');
  removeBtn.type = 'button';
  removeBtn.className = 'btn-close btn-close-white';
  removeBtn.style.fontSize = '0.6rem';
  removeBtn.setAttribute('aria-label', 'Remove');
  removeBtn.addEventListener('click', onRemove);

  tag.appendChild(removeBtn);

  return tag;
}
