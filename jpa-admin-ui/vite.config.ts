import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// Builds use relative asset URLs resolved against the <base href> the server injects; dev serves under /admin/.
export default defineConfig(({ command }) => ({
  plugins: [react(), tailwindcss()],
  base: command === 'build' ? './' : '/admin/',
  resolve: { alias: { '@': new URL('./src', import.meta.url).pathname } },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/admin/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
}));
