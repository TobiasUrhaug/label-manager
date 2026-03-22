/**
 * Formats a duration in seconds to MM:SS format.
 * @param {number} totalSeconds - Total duration in seconds
 * @returns {string} Formatted duration string (e.g., "3:45")
 */
export function formatDuration(totalSeconds) {
  if (!totalSeconds && totalSeconds !== 0) return '';
  if (totalSeconds === 0) return '0:00';

  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}
