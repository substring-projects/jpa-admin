import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { fetchSession, login, logout, type AdminSession } from '../api/auth';
import { LoginView } from '../views/LoginView';
import { Skeleton } from '../components/ui/skeleton';

interface AuthContextValue { username: string; title: string; signOut: () => Promise<void> }
const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AdminSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const loadSession = async () => {
    setLoading(true);
    setError('');
    try { setSession(await fetchSession()); }
    catch (err) { setError(err instanceof Error ? err.message : 'Unable to connect'); }
    finally { setLoading(false); }
  };
  useEffect(() => {
    const savedTheme = localStorage.getItem('jpa-admin-theme');
    document.documentElement.classList.toggle('dark', savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches));
    void loadSession();
    const expired = () => { setSession(null); setError('Your session has expired. Please sign in again.'); };
    window.addEventListener('admin-session-expired', expired);
    return () => window.removeEventListener('admin-session-expired', expired);
  }, []);
  useEffect(() => { if (session?.title) document.title = session.title; }, [session?.title]);

  if (loading) return <main role="status" aria-label="Checking session" className="min-h-dvh bg-background flex items-center justify-center p-6"><div className="w-full max-w-sm space-y-5"><Skeleton className="h-12 w-12 mx-auto" /><Skeleton className="h-8 w-3/4 mx-auto" /><Skeleton className="h-11 w-full" /><Skeleton className="h-11 w-full" /><Skeleton className="h-11 w-full" /><span className="sr-only">Checking session…</span></div></main>;
  if (!session?.authenticated) return <LoginView title={session?.title || 'JPA Admin'} connectionError={error}
    retry={loadSession} onLogin={async (username, password) => {
      const next = await login(username, password);
      if (!next.authenticated) throw new Error('Sign in was not completed. Please try again.');
      setError(''); setSession(next);
    }} />;
  return <AuthContext.Provider value={{ username: session.username, title: session.title, signOut: async () => {
    await logout(); setSession({ ...session, authenticated: false, username: '' }); setError('');
  } }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const auth = useContext(AuthContext);
  if (!auth) throw new Error('useAuth requires an authenticated session');
  return auth;
}
