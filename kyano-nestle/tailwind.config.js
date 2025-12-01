/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{cljs,cljc}",
    "./public/**/*.html"
  ],
  theme: {
    extend: {
      colors: {
        // Dashboard color palette (matches React app)
        slate: {
          850: '#1e293b',
          950: '#0f172a'
        }
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      }
    },
  },
  plugins: [],
}
