const HEX_RE = /^#([0-9a-f]{6})$/i;
const BRAND_VARS = ['--primary', '--ring', '--sidebar-primary', '--sidebar-ring', '--chart-1'];

function readableForeground(hex: string): string {
  const [r, g, b] = [1, 3, 5].map(i => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map(c => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  return luminance > 0.45 ? 'oklch(0.141 0.005 285.823)' : 'oklch(0.985 0 0)';
}

/** Applies a configured `#rrggbb` accent; anything else keeps the built-in theme. */
export function applyBrandColor(color: string | undefined) {
  const root = document.documentElement.style;
  if (!color || !HEX_RE.test(color.trim())) {
    [...BRAND_VARS, '--primary-foreground', '--sidebar-primary-foreground'].forEach(name => root.removeProperty(name));
    return;
  }
  const hex = color.trim();
  BRAND_VARS.forEach(name => root.setProperty(name, hex));
  const foreground = readableForeground(hex);
  root.setProperty('--primary-foreground', foreground);
  root.setProperty('--sidebar-primary-foreground', foreground);
}
