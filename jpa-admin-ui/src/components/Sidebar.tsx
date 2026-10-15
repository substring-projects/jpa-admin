import { useState } from 'react';
import { useAdmin } from '../context/AdminContext';
import { useAuth } from '../context/AuthContext';
import { DynamicIcon } from './DynamicIcon';
import { LayoutDashboard, Search, Database, PanelLeftClose, PanelLeftOpen, LogOut, Loader2, UserRound } from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Separator } from './ui/separator';
import { Skeleton } from './ui/skeleton';
import { cn } from '../lib/utils';

export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { metadata, loading, currentEntity, currentView, navigateToList, navigateToDashboard } = useAdmin();
  const { username, title, signOut } = useAuth();
  const [query, setQuery] = useState('');
  const [collapsed, setCollapsed] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const [error, setError] = useState('');
  const compact = collapsed && !onNavigate;
  const needle = query.trim().toLowerCase();
  const entities = metadata.filter(entity => [entity.displayName, entity.pluralName, entity.entityName, entity.group || 'General'].some(value => value?.toLowerCase().includes(needle)))
    .sort((a, b) => a.order - b.order || a.displayName.localeCompare(b.displayName));
  const groups = Array.from(new Set(entities.map(entity => entity.group || 'General')));
  const navigate = (action: () => void) => { action(); onNavigate?.(); };
  const navigationClass = (active: boolean) => cn('h-10 w-full min-w-0 justify-start gap-3 rounded-lg px-3 text-sm font-medium',
    compact && 'justify-center px-0', active ? 'bg-primary/10 text-primary hover:bg-primary/15 hover:text-primary' : 'text-muted-foreground hover:bg-accent hover:text-foreground');
  return <aside aria-label="Admin sidebar" className={cn('flex h-full min-h-0 shrink-0 flex-col border-r bg-card text-card-foreground transition-[width] duration-200', onNavigate ? 'w-full' : compact ? 'w-[72px]' : 'w-64')}>
    <div className={cn('flex h-16 shrink-0 items-center gap-3 px-4', onNavigate && 'pr-12', compact && 'px-3 justify-center')}>
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground"><Database className="h-5 w-5" /></div>
      {!compact && <div className="min-w-0"><p title={title} className="truncate text-sm font-semibold tracking-tight">{title}</p><p className="text-[10px] uppercase tracking-widest text-muted-foreground">Administration</p></div>}
    </div>
    <Separator />
    {!compact && <div className="px-3 py-4"><div className="relative"><Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" /><Input aria-label="Search entities" value={query} onChange={event => setQuery(event.target.value)} placeholder="Search entities…" className="h-10 pl-9 bg-muted/40" /></div></div>}
    <nav aria-label="Main navigation" className={cn('min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-3 pb-4 space-y-5', compact && 'pt-4 px-2')}>
      <Button variant="ghost" title="Dashboard" aria-label="Dashboard" aria-current={currentView === 'dashboard' ? 'page' : undefined} className={navigationClass(currentView === 'dashboard')} onClick={() => navigate(navigateToDashboard)}><LayoutDashboard className="h-4 w-4 shrink-0" />{!compact && <span>Dashboard</span>}</Button>
      {loading ? <div className="space-y-3" role="status" aria-label="Loading navigation">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-9 w-full" />)}</div> : groups.map(group => <section key={group} aria-label={group} className="space-y-2">
        {compact ? <Separator /> : <h2 className="px-3 text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">{group}</h2>}
        <div className="space-y-1">{entities.filter(entity => (entity.group || 'General') === group).map(entity => {
          const active = currentEntity?.entityName === entity.entityName && currentView !== 'dashboard';
          const label = entity.pluralName || entity.displayName;
          return <Button key={entity.entityName} variant="ghost" aria-label={label} title={label} aria-current={active ? 'page' : undefined} onClick={() => navigate(() => navigateToList(entity.entityName))} className={navigationClass(active)}><DynamicIcon name={entity.icon || 'database'} className="h-4 w-4 shrink-0" />{!compact && <span className="min-w-0 truncate text-left">{label}</span>}</Button>;
        })}</div>
      </section>)}
      {!loading && entities.length === 0 && !compact && <p className="px-3 py-4 text-sm text-muted-foreground break-words">No matching entities.</p>}
    </nav>
    <div className="shrink-0 border-t p-3 space-y-2">
      {!compact && <div className="flex min-w-0 items-center gap-3 px-2 py-1"><div className="rounded-full bg-muted p-2"><UserRound className="h-4 w-4" /></div><div className="min-w-0"><p className="truncate text-sm font-medium">{username}</p><p className="text-xs text-muted-foreground">Administrator</p></div></div>}
      {error && <p role="alert" className="text-xs text-destructive break-words">{error}</p>}
      <div className={cn('flex gap-1', compact && 'flex-col')}>
        <Button variant="ghost" disabled={signingOut} aria-label="Sign out" title="Sign out" className={cn('min-w-0 flex-1 justify-start gap-3 text-muted-foreground', compact && 'px-0 justify-center')} onClick={async () => {
          setSigningOut(true); setError('');
          try { await signOut(); } catch (err) { setError(err instanceof Error ? err.message : 'Unable to sign out'); } finally { setSigningOut(false); }
        }}>{signingOut ? <Loader2 className="h-4 w-4 animate-spin" /> : <LogOut className="h-4 w-4 shrink-0" />}{!compact && (signingOut ? 'Signing out…' : 'Sign out')}</Button>
        {!onNavigate && <Button variant="ghost" size="icon" className="shrink-0 text-muted-foreground" aria-label={compact ? 'Expand sidebar' : 'Collapse sidebar'} title={compact ? 'Expand sidebar' : 'Collapse sidebar'} onClick={() => { setCollapsed(!collapsed); setQuery(''); }}>{compact ? <PanelLeftOpen className="h-4 w-4" /> : <PanelLeftClose className="h-4 w-4" />}</Button>}
      </div>
    </div>
  </aside>;
}
