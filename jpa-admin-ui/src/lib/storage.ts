import { useCallback, useSyncExternalStore } from 'react';

const listeners = new Set<() => void>();
const cache = new Map<string, unknown>();

function read<T>(key: string, fallback: T): T {
  if (cache.has(key)) return cache.get(key) as T;
  let value = fallback;
  try {
    const raw = localStorage.getItem(key);
    if (raw !== null) value = JSON.parse(raw) as T;
  } catch { /* storage unavailable or corrupt: use fallback */ }
  cache.set(key, value);
  return value;
}

export function writeStored<T>(key: string, value: T) {
  cache.set(key, value);
  try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* keep in-memory value */ }
  listeners.forEach(listener => listener());
}

const subscribe = (listener: () => void) => { listeners.add(listener); return () => listeners.delete(listener); };

/** Per-browser UI preference backed by localStorage, shared across components. */
export function useStored<T>(key: string, fallback: T): [T, (value: T) => void] {
  const value = useSyncExternalStore(subscribe, () => read(key, fallback));
  const set = useCallback((next: T) => writeStored(key, next), [key]);
  return [value, set];
}

export interface RecentRecord { entity: string; id: string; label: string; at: number }
const RECENT_KEY = 'jpa-admin-recent';

export function rememberRecord(entry: Omit<RecentRecord, 'at'>) {
  const existing = read<RecentRecord[]>(RECENT_KEY, []).filter(r => !(r.entity === entry.entity && r.id === entry.id));
  writeStored(RECENT_KEY, [{ ...entry, at: Date.now() }, ...existing].slice(0, 8));
}

export function forgetRecord(entity: string, id: string) {
  writeStored(RECENT_KEY, read<RecentRecord[]>(RECENT_KEY, []).filter(r => !(r.entity === entity && r.id === id)));
}

export const useRecentRecords = () => useStored<RecentRecord[]>(RECENT_KEY, [])[0];
