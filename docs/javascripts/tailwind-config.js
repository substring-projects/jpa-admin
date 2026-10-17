window.tailwind = window.tailwind || {};
window.tailwind.config = {
  darkMode: ['class', '[data-md-color-scheme="slate"]'],
  corePlugins: {
    preflight: false, // Keep MkDocs typography and baseline intact
  },
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef2ff',
          100: '#e0e7ff',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          900: '#1e1b4b',
        },
        slate: {
          850: '#151f32',
          900: '#0f172a',
          950: '#020617',
        }
      },
      borderRadius: {
        'xl': '0.75rem',
        '2xl': '1rem',
      }
    }
  }
};
