import { useState, type FormEvent } from 'react';
import { Database, ArrowRight, Loader2, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/card';

interface Props { title: string; connectionError: string; retry: () => void; onLogin: (username: string, password: string) => Promise<void> }
export function LoginView({ title, connectionError, retry, onLogin }: Props) {
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
  return <main className="min-h-dvh flex items-center justify-center bg-muted/30 px-4 py-10">
    <div className="w-full max-w-md space-y-6">
      <div className="flex items-center justify-center gap-3"><div className="rounded-xl bg-primary p-3 text-primary-foreground"><Database className="h-6 w-6" /></div><span className="text-lg font-semibold tracking-tight break-words">{title}</span></div>
      <Card className="shadow-lg">
        <CardHeader className="space-y-2 pb-6"><CardTitle className="text-2xl">Welcome back</CardTitle><CardDescription>Sign in to manage your application.</CardDescription></CardHeader>
        <CardContent>
          <form onSubmit={submit} className="space-y-5" aria-busy={pending}>
            {(error || connectionError) && <div role="alert" className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error || connectionError}{connectionError && <Button type="button" variant="link" className="h-auto p-0 ml-2" onClick={retry}>Retry connection</Button>}</div>}
            <div className="space-y-2"><label htmlFor="username" className="text-sm font-medium">Username</label><Input id="username" name="username" autoComplete="username" autoCapitalize="none" spellCheck={false} required value={username} onChange={event => setUsername(event.target.value)} disabled={pending} placeholder="Enter your username" /></div>
            <div className="space-y-2"><label htmlFor="password" className="text-sm font-medium">Password</label><div className="relative"><Input id="password" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} disabled={pending} placeholder="Enter your password" className="pr-12" /><Button type="button" variant="ghost" size="icon" className="absolute right-1 top-0.5" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible(!visible)}>{visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</Button></div></div>
            <Button type="submit" className="w-full gap-2" disabled={pending}>{pending ? <Loader2 className="h-4 w-4 animate-spin" /> : <ArrowRight className="h-4 w-4" />}{pending ? 'Signing in…' : 'Sign in'}</Button>
          </form>
        </CardContent>
      </Card>
      <p className="flex justify-center items-center gap-2 text-xs text-muted-foreground"><ShieldCheck className="h-4 w-4" />Authorized administrators only</p>
    </div>
  </main>;
}
