import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': '/src',
      '@/api': '/src/api'
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://cloud.kritikal.org:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})