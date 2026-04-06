import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/login': {
        target: 'http://localhost:8080',
        bypass: (req) => req.method === 'GET' ? req.url : undefined,
      },
      '/logout': {
        target: 'http://localhost:8080',
        bypass: (req) => req.method === 'GET' ? req.url : undefined,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
  },
});
