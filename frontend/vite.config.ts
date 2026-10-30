import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import type { Plugin } from 'vite';

// Identifica o build: vai embutido no JS (__BUILD_ID__) e no version.json. Aba
// aberta com o JS antigo vê os dois diferentes (ver hooks/useNovaVersao.ts).
const buildId = Date.now().toString();

function versionJson(): Plugin {
  return {
    name: 'version-json',
    apply: 'build',
    generateBundle() {
      this.emitFile({
        type: 'asset',
        fileName: 'version.json',
        source: JSON.stringify({ buildId }),
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), versionJson()],
  define: {
    __BUILD_ID__: JSON.stringify(buildId),
  },
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
