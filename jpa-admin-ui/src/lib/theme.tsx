import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

export type Theme = 'light' | 'dark' | 'system';
interface ThemeContextValue { theme: Theme; resolvedTheme: 'light' | 'dark'; setTheme: (theme: string) => void }

const STORAGE_KEY = 'jpa-admin-theme';
const ThemeContext = createContext<ThemeContextValue | null>(null);
const media = () => window.matchMedia('(prefers-color-scheme: dark)');

function storedTheme(): Theme {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value === 'light' || value === 'dark' ? value : 'system';
  } catch { return 'system'; }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(storedTheme);
  const [systemDark, setSystemDark] = useState(() => media().matches);
  const resolvedTheme = theme === 'system' ? (systemDark ? 'dark' : 'light') : theme;

  useEffect(() => {
    const query = media();
    const onChange = () => setSystemDark(query.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    root.classList.toggle('dark', resolvedTheme === 'dark');
    root.style.colorScheme = resolvedTheme;
  }, [resolvedTheme]);

  const setTheme = (next: string) => {
    const value: Theme = next === 'light' || next === 'dark' ? next : 'system';
    setThemeState(value);
    try { if (value === 'system') localStorage.removeItem(STORAGE_KEY); else localStorage.setItem(STORAGE_KEY, value); } catch { /* preference stays in memory */ }
  };

  return <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
}
