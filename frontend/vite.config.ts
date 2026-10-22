import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

// Same paths as nginx in Docker (frontend/nginx.conf), so the app calls the backend the
// same way in development, `vite preview` and `docker compose`.
const backendProxy = {
  '/api/': 'http://localhost:8080',
  '^/actuator/health$': 'http://localhost:8080',
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: { proxy: backendProxy },
  preview: { proxy: backendProxy },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
