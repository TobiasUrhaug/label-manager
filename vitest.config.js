import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['src/main/resources/static/js/**/*.test.js'],
  },
});
