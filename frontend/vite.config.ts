import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    globals: true,
    env: {
      // Testes não devem depender do .env local (gitignorado) nem cair no
      // fallback de produção do api/config.ts — precisam ser determinísticos
      // no CI, que não tem esse arquivo.
      VITE_API_URL: 'http://localhost:8080',
    },
  },
});
