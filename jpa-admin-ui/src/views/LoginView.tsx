import { useState, type FormEvent } from 'react';
import { AlertCircle, Eye, EyeOff, Loader2, ShieldCheck } from 'lucide-react';
import type { Branding } from '@/context/AuthContext';
import { BrandMark } from '@/components/BrandMark';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

interface Props { branding: Branding; connectionError: string; retry: () => void; onLogin: (username: string, password: string) => Promise<void> }

export function LoginView({ branding, connectionError, retry, onLogin }: Props) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [visible, setVisible] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (pending) return;
    setPending(true); setError('');
    try { await onLogin(username, password); }
    catch (err) { setError(err instanceof Error ? err.message : 'Unable to sign in'); setPassword(''); }
    finally { setPending(false); }
  };
  const message = error || connectionError;
  return <main className="flex min-h-dvh flex-col items-center justify-center gap-6 bg-muted/40 p-6">
    <div className="flex items-center gap-2.5">
      <BrandMark logoUrl={branding.logoUrl} title={branding.title} />
      <span className="text-base font-semibold tracking-tight">{branding.title}</span>
    </div>
    <Card className="w-full max-w-sm">
      <CardHeader className="text-center">
        <CardTitle className="text-xl">Welcome back</CardTitle>
        <CardDescription>Sign in to your admin account</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={submit} className="grid gap-5" aria-busy={pending}>
          {message && <Alert variant="destructive">
            <AlertCircle />
            <AlertDescription>
              {message}
              {connectionError && !error && <Button type="button" variant="link" className="h-auto p-0" onClick={retry}>Retry connection</Button>}
            </AlertDescription>
          </Alert>}
          <div className="grid gap-2">
            <Label htmlFor="username">Username</Label>
            <Input id="username" name="username" autoComplete="username" autoCapitalize="none" spellCheck={false} required autoFocus
              value={username} onChange={event => setUsername(event.target.value)} disabled={pending} />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="password">Password</Label>
            <div className="relative">
              <Input id="password" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" required
                value={password} onChange={event => setPassword(event.target.value)} disabled={pending} className="pr-10" />
              <Button type="button" variant="ghost" size="icon-sm" className="absolute right-0.5 top-0.5 text-muted-foreground"
                aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible(!visible)}>
                {visible ? <EyeOff /> : <Eye />}
              </Button>
            </div>
          </div>
          <Button type="submit" className="w-full" disabled={pending}>
            {pending && <Loader2 className="animate-spin" />}
            {pending ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>
      </CardContent>
    </Card>
    <p className="flex items-center gap-1.5 text-xs text-muted-foreground"><ShieldCheck className="size-3.5" />Authorized administrators only</p>
  </main>;
}
