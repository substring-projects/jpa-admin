import React, { createContext, useContext, useState, useEffect } from 'react';
import { EntityMetadata } from '../types/metadata';
import { fetchMetadata } from '../api/client';

export type ViewType = 'dashboard' | 'list' | 'create' | 'edit';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info';
  message: string;
}

interface AdminContextType {
  metadata: EntityMetadata[];
  loading: boolean;
  error: string | null;
  currentEntity: EntityMetadata | null;
  currentView: ViewType;
  editingId: any;
  isDark: boolean;
  toasts: ToastMessage[];
  refreshMetadata: () => Promise<void>;
  toggleTheme: () => void;
  navigateToList: (entityName: string) => void;
  navigateToCreate: (entityName: string) => void;
  navigateToEdit: (entityName: string, id: any) => void;
  navigateToDashboard: () => void;
  addToast: (message: string, type?: 'success' | 'error' | 'info') => void;
  removeToast: (id: string) => void;
}

const AdminContext = createContext<AdminContextType | null>(null);

export const AdminProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [metadata, setMetadata] = useState<EntityMetadata[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [currentEntity, setCurrentEntity] = useState<EntityMetadata | null>(null);
  const [currentView, setCurrentView] = useState<ViewType>('dashboard');
  const [editingId, setEditingId] = useState<any>(null);

  const [isDark, setIsDark] = useState(() => {
    return localStorage.getItem('jpa-admin-theme') === 'dark' ||
      (!('jpa-admin-theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches);
  });

  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('jpa-admin-theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('jpa-admin-theme', 'light');
    }
  }, [isDark]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchMetadata();
      setMetadata(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load entities');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const toggleTheme = () => setIsDark(prev => !prev);

  const addToast = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => removeToast(id), 4000);
  };

  const removeToast = (id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  const navigateToList = (entityName: string) => {
    const found = metadata.find(e => e.entityName.toLowerCase() === entityName.toLowerCase());
    if (found) {
      setCurrentEntity(found);
      setCurrentView('list');
      setEditingId(null);
    }
  };

  const navigateToCreate = (entityName: string) => {
    const found = metadata.find(e => e.entityName.toLowerCase() === entityName.toLowerCase());
    if (found) {
      setCurrentEntity(found);
      setCurrentView('create');
      setEditingId(null);
    }
  };

  const navigateToEdit = (entityName: string, id: any) => {
    const found = metadata.find(e => e.entityName.toLowerCase() === entityName.toLowerCase());
    if (found) {
      setCurrentEntity(found);
      setCurrentView('edit');
      setEditingId(id);
    }
  };

  const navigateToDashboard = () => {
    setCurrentEntity(null);
    setCurrentView('dashboard');
    setEditingId(null);
  };

  return (
    <AdminContext.Provider
      value={{
        metadata,
        loading,
        error,
        currentEntity,
        currentView,
        editingId,
        isDark,
        toasts,
        refreshMetadata: loadData,
        toggleTheme,
        navigateToList,
        navigateToCreate,
        navigateToEdit,
        navigateToDashboard,
        addToast,
        removeToast,
      }}
    >
      {children}
    </AdminContext.Provider>
  );
};

export const useAdmin = () => {
  const context = useContext(AdminContext);
  if (!context) throw new Error('useAdmin must be used within AdminProvider');
  return context;
};
