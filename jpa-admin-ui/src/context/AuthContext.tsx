import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { fetchSession, login, logout, type AdminSession } from '@/api/auth';
import { applyBrandColor } from '@/lib/branding';
import { LoginView } from '@/views/LoginView';
import { Loader2 } from 'lucide-react';

export interface Branding { title: string; logoUrl: string }
interface AuthContextValue extends Branding { username: string; manageSecurity: boolean; exportEnabled: boolean; signOut: () => Promise<void> }
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
    void loadSession();
    const expired = () => { setSession(null); setError('Your session has expired. Please sign in again.'); };
    window.addEventListener('admin-session-expired', expired);
    return () => window.removeEventListener('admin-session-expired', expired);
  }, []);
  useEffect(() => { if (session?.title) document.title = session.title; }, [session?.title]);
  useEffect(() => { applyBrandColor(session?.primaryColor); }, [session?.primaryColor]);

  const branding: Branding = { title: session?.title || 'JPA Admin', logoUrl: session?.logoUrl || '' };

  if (loading) return <main role="status" className="flex min-h-dvh items-center justify-center bg-background text-muted-foreground">
    <Loader2 className="size-5 animate-spin" /><span className="sr-only">Checking session…</span>
  </main>;
  if (!session?.authenticated) return <LoginView branding={branding} connectionError={error}
    retry={loadSession} onLogin={async (username, password) => {
      const next = await login(username, password);
      if (!next.authenticated) throw new Error('Sign in was not completed. Please try again.');
      setError(''); setSession(next);
    }} />;
  return <AuthContext.Provider value={{
    ...branding, username: session.username, manageSecurity: session.manageSecurity === true, exportEnabled: session.exportEnabled !== false,
    signOut: async () => { await logout(); setSession({ ...session, authenticated: false, username: '' }); setError(''); },
  }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const auth = useContext(AuthContext);
  if (!auth) throw new Error('useAuth requires an authenticated session');
  return auth;
}
