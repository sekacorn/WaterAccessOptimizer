import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite configuration for React application with optimizations
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    host: true,
    proxy: {
      // Proxy API requests to backend services
      '/api': {
        target: process.env.VITE_API_GATEWAY_URL || 'http://localhost:8087',
        changeOrigin: true,
      }
    }
  },

  build: {
    outDir: 'dist',
    sourcemap: false,

    // Optimize bundle size
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },

    // Increase chunk size warning limit (default is 500kb)
    chunkSizeWarningLimit: 600,

    // Rollup optimizations
    rollupOptions: {
      output: {
        // Manual code splitting for better caching
        manualChunks: {
          // React core
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],

          // State management
          'state': ['zustand'],

          // HTTP client
          'http': ['axios'],

          // Maps
          'map-vendor': ['leaflet', 'react-leaflet'],

          // Charts
          'chart-vendor': ['chart.js', 'react-chartjs-2'],

          // Icons
          'icons': ['lucide-react'],

        },

        // Optimize asset file names
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split('.')
          const ext = info[info.length - 1]
          if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(ext)) {
            return 'assets/images/[name]-[hash][extname]'
          }
          if (/woff|woff2|eot|ttf|otf/i.test(ext)) {
            return 'assets/fonts/[name]-[hash][extname]'
          }
          return 'assets/[name]-[hash][extname]'
        },

        // Optimize chunk file names
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
      },
    },
  },

  // Optimize dependencies
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-router-dom',
      'zustand',
      'axios',
      'leaflet',
      'react-leaflet',
      'chart.js',
      'react-chartjs-2',
      'lucide-react',
      'date-fns',
    ],
  },
})
