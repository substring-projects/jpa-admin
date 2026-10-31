import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { EntityMetadata } from '@/types/metadata';
import { fetchMetadata, fetchStats } from '@/api/client';

interface AdminContextType {
  metadata: EntityMetadata[];
  loading: boolean;
  error: string | null;
  counts: Record<string, number> | null;
  refreshMetadata: () => Promise<void>;
  refreshCounts: () => void;
  getEntity: (name: string | undefined) => EntityMetadata | undefined;
}

const AdminContext = createContext<AdminContextType | null>(null);

export function AdminProvider({ children }: { children: ReactNode }) {
  const [metadata, setMetadata] = useState<EntityMetadata[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [counts, setCounts] = useState<Record<string, number> | null>(null);

  const refreshMetadata = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchMetadata();
      setMetadata([...data].sort((a, b) => a.order - b.order || a.displayName.localeCompare(b.displayName)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load entities');
    } finally {
      setLoading(false);
    }
  }, []);

  const refreshCounts = useCallback(() => {
    fetchStats().then(setCounts).catch(() => setCounts(prev => prev ?? {}));
  }, []);

  useEffect(() => { void refreshMetadata(); refreshCounts(); }, [refreshMetadata, refreshCounts]);

  const getEntity = useCallback((name: string | undefined) => {
    if (!name) return undefined;
    const needle = name.toLowerCase();
    return metadata.find(e => e.entityName.toLowerCase() === needle);
  }, [metadata]);

  const value = useMemo(() => ({ metadata, loading, error, counts, refreshMetadata, refreshCounts, getEntity }),
    [metadata, loading, error, counts, refreshMetadata, refreshCounts, getEntity]);
  return <AdminContext.Provider value={value}>{children}</AdminContext.Provider>;
}

export function useAdmin() {
  const context = useContext(AdminContext);
  if (!context) throw new Error('useAdmin must be used within AdminProvider');
  return context;
}

export function useEntity(name: string | undefined) {
  return useAdmin().getEntity(name);
}
