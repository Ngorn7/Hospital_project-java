import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxies API and Swagger traffic to the Spring Boot backend, so no CORS setup is needed.
const backend = process.env.VITE_BACKEND_URL || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: 'localhost',
    port: 5173,
    proxy: {
      '/api': { target: backend, changeOrigin: true },
      '/v3': { target: backend, changeOrigin: true },
      '/swagger-ui': { target: backend, changeOrigin: true },
      '/swagger-ui.html': { target: backend, changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    // Same origin as the backend, so /api/v1 calls need no CORS configuration in live tests.
    environmentOptions: { jsdom: { url: process.env.HMS_LIVE ? 'http://localhost:8080/' : 'http://localhost:3000/' } },
    include: ['src/**/*.test.{js,jsx}'],
    setupFiles: ['src/test/setup.js'],
  },
})
