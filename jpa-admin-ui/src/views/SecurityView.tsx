import { useEffect, useState, type FormEvent } from 'react';
import { Users, ShieldCheck, UsersRound, Plus, Search, Pencil, Trash2, KeyRound } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/context/AuthContext';
import { adminFetch } from '@/api/auth';
import { API_BASE } from '@/lib/base-path';
import { ErrorState, PageHeader } from '@/components/PageStates';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { ConfirmDialog, type ConfirmRequest } from '@/components/ConfirmDialog';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

interface User { id?: string; username: string; active: boolean; adminAccess: boolean; superuser: boolean; groupIds: string[]; permissions: string[]; password?: string }
interface Group { id?: string; name: string; permissions: string[] }
interface Permission { key: string; entity: string; operation: string }
async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const response = await adminFetch(`${API_BASE}/security/${path}`, { method,
    ...(body ? { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) } : {}) });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.error || 'Unable to update security settings');
  }
  return response.status === 204 || method === 'DELETE' ? undefined as T : response.json();
}
const message = (error: unknown) => error instanceof Error ? error.message : 'Unable to load security settings';
const toggle = (values: string[], value: string, checked: boolean) => checked ? [...new Set([...values, value])] : values.filter(v => v !== value);

export function SecurityView() {
  const { manageSecurity } = useAuth();
  if (!manageSecurity) return <ErrorState title="Access denied" description="Only superusers can manage admin users and groups." />;
  return <SecurityManager />;
}

function SecurityManager() {
  const [users, setUsers] = useState<User[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [catalog, setCatalog] = useState<Permission[]>([]);
  const [tab, setTab] = useState('users');
  const [search, setSearch] = useState('');
  const [permissionSearch, setPermissionSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [user, setUser] = useState<User | null>(null);
  const [group, setGroup] = useState<Group | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [confirm, setConfirm] = useState<ConfirmRequest | null>(null);
  const load = async () => {
    setLoading(true); setError('');
    try {
      const [u, g, p] = await Promise.all([request<User[]>('users'), request<Group[]>('groups'), request<Permission[]>('permissions')]);
      setUsers(u); setGroups(g); setCatalog(p);
    } catch (err) { setError(message(err)); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);
  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (saving) return;
    setSaving(true); setFormError('');
    try {
      if (user) await request(`users${user.id ? `/${user.id}` : ''}`, user.id ? 'PUT' : 'POST', user);
      if (group) await request(`groups${group.id ? `/${group.id}` : ''}`, group.id ? 'PUT' : 'POST', group);
      setUser(null); setGroup(null); toast.success('Security settings saved'); await load();
    } catch (err) { setFormError(message(err)); }
    finally { setSaving(false); }
  };
  const remove = (kind: 'users' | 'groups', id: string, name: string) => setConfirm({
    title: `Delete ${name}?`, description: kind === 'users' ? 'This removes the admin account and its access.' : 'Members will lose the permissions granted by this group.',
    confirmLabel: 'Delete', destructive: true,
    onConfirm: async () => {
      try { await request(`${kind}/${id}`, 'DELETE'); toast.success('Deleted'); await load(); }
      catch (err) { toast.error(message(err)); throw err; }
    },
  });
  const resources = [...new Set(catalog.map(p => p.key.slice(0, p.key.indexOf(':'))))];
  const matches = (value: string) => value.toLowerCase().includes(search.trim().toLowerCase());
  const openGroup = (value: Group) => { setFormError(''); setPermissionSearch(''); setGroup(value); };
  const changePermission = (permission: Permission, checked: boolean) => {
    if (!group) return;
    const resource = permission.key.slice(0, permission.key.indexOf(':'));
    let next = toggle(group.permissions, permission.key, checked);
    if (checked && permission.operation !== 'view') next = toggle(next, `${resource}:view`, true);
    if (!checked && permission.operation === 'view') next = next.filter(key => !key.startsWith(`${resource}:`));
    setGroup({ ...group, permissions: next });
  };
  const visibleUsers = users.filter(u => matches(u.username) || groups.some(g => u.groupIds.includes(g.id!) && matches(g.name)));
  const visibleGroups = groups.filter(g => matches(g.name));
  const addUser = () => { setFormError(''); setUser({ username: '', password: '', active: true, adminAccess: true, superuser: false, groupIds: [], permissions: [] }); };
  return <div className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-8">
    <PageHeader title="Users and groups" description="Control who can enter the admin and which records they can manage." />
    {error && <ErrorState title="Unable to load security settings" description={error} onRetry={load} />}
    {loading ? <div role="status" className="rounded-xl border p-8 text-sm text-muted-foreground">Loading security settings…</div> : !error && <>
      <div className="grid gap-3 sm:grid-cols-3">
        {[{ label: 'Admin users', value: users.length, detail: `${users.filter(u => u.active && u.adminAccess).length} with active access`, icon: Users },
          { label: 'Permission groups', value: groups.length, detail: 'Reusable access for your team', icon: UsersRound },
          { label: 'Superusers', value: users.filter(u => u.superuser).length, detail: 'Full administration privileges', icon: ShieldCheck }].map(stat => <div key={stat.label} className="flex items-start justify-between rounded-xl border bg-card p-5 shadow-xs"><div><p className="text-sm font-medium text-muted-foreground">{stat.label}</p><p className="mt-2 text-3xl font-semibold tracking-tight tabular-nums">{stat.value}</p><p className="mt-1 text-xs text-muted-foreground">{stat.detail}</p></div><div className="rounded-lg bg-primary/10 p-2.5 text-primary"><stat.icon className="size-5" aria-hidden="true" /></div></div>)}
      </div>
      <Tabs value={tab} onValueChange={value => { setTab(value); setSearch(''); }} className="gap-0 overflow-hidden rounded-xl border bg-card shadow-xs">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b p-4 md:px-6">
          <TabsList><TabsTrigger value="users" aria-label="Users"><Users className="size-4" />Users <span className="ml-1 text-xs text-muted-foreground">{users.length}</span></TabsTrigger><TabsTrigger value="groups" aria-label="Groups"><UsersRound className="size-4" />Groups <span className="ml-1 text-xs text-muted-foreground">{groups.length}</span></TabsTrigger></TabsList>
          <Button onClick={tab === 'users' ? addUser : () => openGroup({ name: '', permissions: [] })}><Plus className="size-4" />{tab === 'users' ? 'Add user' : 'Add group'}</Button>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-4 md:px-6"><div className="relative w-full sm:w-80"><Search aria-hidden="true" className="pointer-events-none absolute left-3 top-2.5 size-4 text-muted-foreground" /><Input className="pl-9" aria-label="Search users or groups" placeholder={tab === 'users' ? 'Search by username or group…' : 'Search groups…'} value={search} onChange={e => setSearch(e.target.value)} /></div><p className="text-xs text-muted-foreground">{tab === 'users' ? 'Dedicated accounts for admin access' : 'Permissions combine across assigned groups'}</p></div>
        <TabsContent value="users" className="mt-0">
          <Table><TableHeader className="bg-muted/40"><TableRow><TableHead className="pl-6">User</TableHead><TableHead>Status</TableHead><TableHead>Access level</TableHead><TableHead>Groups</TableHead><TableHead className="pr-6 text-right">Actions</TableHead></TableRow></TableHeader>
            <TableBody>{visibleUsers.map(u => <TableRow key={u.id}><TableCell className="py-4 pl-6"><div className="flex items-center gap-3"><div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary" aria-hidden="true">{u.username.slice(0, 2).toUpperCase()}</div><span className="font-medium">{u.username}</span></div></TableCell>
              <TableCell><Badge variant="outline" className={u.active && u.adminAccess ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400' : 'text-muted-foreground'}><span className="size-1.5 rounded-full bg-current" />{!u.active ? 'Disabled' : !u.adminAccess ? 'No admin access' : 'Active'}</Badge></TableCell>
              <TableCell><span className="inline-flex items-center gap-1.5 text-sm">{u.superuser && <ShieldCheck className="size-4 text-primary" />}{u.superuser ? 'Superuser' : 'Group permissions'}</span></TableCell>
              <TableCell><div className="flex max-w-72 flex-wrap gap-1">{groups.filter(g => u.groupIds.includes(g.id!)).map(g => <Badge key={g.id} variant="secondary">{g.name}</Badge>)}{!u.groupIds.length && <span className="text-sm text-muted-foreground">No groups assigned</span>}</div></TableCell>
              <TableCell className="pr-6 text-right"><Button variant="ghost" size="icon" title={`Edit user ${u.username}`} aria-label={`Edit user ${u.username}`} onClick={() => { setFormError(''); setUser({ ...u, password: '' }); }}><Pencil className="size-4" /></Button><Button variant="ghost" size="icon" className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive" title={`Delete user ${u.username}`} aria-label={`Delete user ${u.username}`} onClick={() => remove('users', u.id!, u.username)}><Trash2 className="size-4" /></Button></TableCell>
            </TableRow>)}{!visibleUsers.length && <TableRow><TableCell colSpan={5} className="py-12 text-center text-muted-foreground">No users match your search.</TableCell></TableRow>}</TableBody></Table>
          <div className="border-t px-6 py-3 text-xs text-muted-foreground">Showing {visibleUsers.length} of {users.length} users</div>
        </TabsContent>
        <TabsContent value="groups" className="mt-0">
          {!groups.length ? <div className="px-6 py-14 text-center"><div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary"><UsersRound className="size-6" /></div><h3 className="font-semibold">Give your team the right access</h3><p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-muted-foreground">Create a group, choose its permissions, and assign users to manage access in one place.</p><Button className="mt-5" onClick={() => openGroup({ name: '', permissions: [] })}><Plus className="size-4" />Create first group</Button></div> : <>
            <Table><TableHeader className="bg-muted/40"><TableRow><TableHead className="pl-6">Group</TableHead><TableHead>Members</TableHead><TableHead>Permissions</TableHead><TableHead className="pr-6 text-right">Actions</TableHead></TableRow></TableHeader>
              <TableBody>{visibleGroups.map(g => <TableRow key={g.id}><TableCell className="py-4 pl-6"><div className="flex items-center gap-3"><div className="rounded-lg bg-muted p-2"><UsersRound className="size-4 text-muted-foreground" /></div><span className="font-medium">{g.name}</span></div></TableCell><TableCell className="tabular-nums">{users.filter(u => u.groupIds.includes(g.id!)).length}</TableCell><TableCell><Badge variant="secondary">{g.permissions.length} permissions</Badge></TableCell>
                <TableCell className="pr-6 text-right"><Button variant="ghost" size="icon" title={`Edit group ${g.name}`} aria-label={`Edit group ${g.name}`} onClick={() => openGroup(g)}><Pencil className="size-4" /></Button><Button variant="ghost" size="icon" className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive" title={`Delete group ${g.name}`} aria-label={`Delete group ${g.name}`} onClick={() => remove('groups', g.id!, g.name)}><Trash2 className="size-4" /></Button></TableCell>
              </TableRow>)}{!visibleGroups.length && <TableRow><TableCell colSpan={4} className="py-12 text-center text-muted-foreground">No groups match your search.</TableCell></TableRow>}</TableBody></Table><div className="border-t px-6 py-3 text-xs text-muted-foreground">Showing {visibleGroups.length} of {groups.length} groups</div>
          </>}
        </TabsContent>
      </Tabs>
    </>}
    <Dialog open={!!user || !!group} onOpenChange={open => { if (!open && !saving) { setUser(null); setGroup(null); } }}>
      <DialogContent className={`flex max-h-[90dvh] flex-col gap-0 overflow-hidden p-0 ${user ? 'sm:max-w-2xl' : 'sm:max-w-4xl'}`}>
        <DialogHeader className="shrink-0 border-b px-6 py-5 pr-12"><div className="mb-2 flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary"><KeyRound className="size-5" /></div><DialogTitle>{user ? user.id ? 'Edit user' : 'Add user' : group?.id ? 'Edit group' : 'Add group'}</DialogTitle>
          <DialogDescription>{user ? 'Changes to groups and account access apply to subsequent requests. Password resets require a new sign-in.' : 'Permissions from all groups are combined. Selecting a write or action permission also enables View. Clearing View removes all access to that entity.'}</DialogDescription></DialogHeader>
        <form onSubmit={save} className="flex min-h-0 flex-1 flex-col">
          {formError && <p role="alert" className="mx-6 mt-4 rounded-lg border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive">{formError}</p>}
          <div className="min-h-0 flex-1 overflow-y-auto p-6"><fieldset disabled={saving} className="min-w-0 space-y-5">
          {user && <>
            <div className="space-y-2"><Label htmlFor="admin-username">Username</Label><Input id="admin-username" required maxLength={100} value={user.username} onChange={e => setUser({ ...user, username: e.target.value })} /></div>
            <div className="space-y-2"><Label htmlFor="admin-password">{user.id ? 'New password (leave blank to keep)' : 'Password'}</Label>
              <Input id="admin-password" type="password" autoComplete="new-password" required={!user.id} minLength={12} value={user.password} onChange={e => setUser({ ...user, password: e.target.value })} />
              <p className="text-xs text-muted-foreground">At least 12 characters; at most 72 UTF-8 bytes.</p></div>
            {(['active', 'adminAccess', 'superuser'] as const).map(key => <Label key={key} className="flex items-start gap-3 rounded-lg border p-4 leading-relaxed"><Checkbox className="mt-0.5" checked={user[key]} onCheckedChange={v => setUser({ ...user, [key]: !!v })} />{key === 'active' ? 'Active account' : key === 'adminAccess' ? 'Allow admin access' : 'Superuser — all entity permissions and security management'}</Label>)}
            <Card><CardHeader><CardTitle>Groups</CardTitle></CardHeader><CardContent className="space-y-3">{groups.length ? groups.map(g => <Label key={g.id} className="flex items-center gap-3 rounded-lg border p-3"><Checkbox checked={user.groupIds.includes(g.id!)} onCheckedChange={v => setUser({ ...user, groupIds: toggle(user.groupIds, g.id!, !!v) })} />{g.name}</Label>) : <p>Create groups first to grant entity access.</p>}</CardContent></Card>
            <details className="rounded-lg border bg-muted/20 p-4"><summary className="cursor-pointer text-sm">Effective assigned permissions</summary>
              <p className="mt-2 break-words text-xs leading-relaxed text-muted-foreground">{user.superuser ? 'All entity permissions. Entity read-only restrictions still apply.' : [...new Set(groups.filter(g => user.groupIds.includes(g.id!)).flatMap(g => g.permissions))].join(', ') || 'No entity permissions assigned.'}</p></details>
          </>}
          {group && <>
            <div className="space-y-2"><Label htmlFor="group-name">Group name</Label><Input id="group-name" required maxLength={100} value={group.name} onChange={e => setGroup({ ...group, name: e.target.value })} /></div>
            <div className="flex flex-wrap items-center justify-between gap-3"><Input className="sm:max-w-xs" aria-label="Search entities" placeholder="Search entities…" value={permissionSearch} onChange={e => setPermissionSearch(e.target.value)} /><Badge variant="secondary">{group.permissions.length} selected</Badge></div>
            <div className="overflow-hidden rounded-lg border"><Table><TableHeader className="bg-muted/50"><TableRow><TableHead>Entity</TableHead>{['View', 'Add', 'Change', 'Delete', 'All'].map(label => <TableHead key={label} className="text-center">{label}</TableHead>)}</TableRow></TableHeader>
              <TableBody>{resources.filter(resource => catalog.some(p => p.key.startsWith(`${resource}:`) && `${p.entity} ${resource}`.toLowerCase().includes(permissionSearch.trim().toLowerCase()))).map(resource => {
                const permissions = catalog.filter(p => p.key.startsWith(`${resource}:`));
                const selected = permissions.filter(p => group.permissions.includes(p.key)).length;
                const actions = permissions.filter(p => p.operation.startsWith('action:'));
                return <TableRow key={resource}><TableCell className="min-w-48 whitespace-normal"><span className="font-medium">{permissions[0].entity}</span>
                  {actions.length > 0 && <div className="mt-3 space-y-2"><span className="text-xs text-muted-foreground">Custom actions</span>{actions.map(p => <Label key={p.key} className="flex items-center gap-2 text-xs"><Checkbox aria-label={`${p.entity} ${p.operation}`} checked={group.permissions.includes(p.key)} onCheckedChange={v => changePermission(p, v === true)} />{p.operation.slice(7)}</Label>)}</div>}</TableCell>
                  {['view', 'add', 'change', 'delete'].map(operation => {
                    const permission = permissions.find(p => p.operation === operation);
                    return <TableCell key={operation} className="text-center">{permission ? <Checkbox aria-label={`${permission.entity} ${operation}`} checked={group.permissions.includes(permission.key)} onCheckedChange={v => changePermission(permission, v === true)} /> : <span className="text-muted-foreground" aria-label="Not available">—</span>}</TableCell>;
                  })}
                  <TableCell className="text-center"><Checkbox aria-label={`Select all ${permissions[0].entity} permissions`} checked={selected === permissions.length ? true : selected ? 'indeterminate' : false} onCheckedChange={v => setGroup({ ...group, permissions: v === true ? [...new Set([...group.permissions, ...permissions.map(p => p.key)])] : group.permissions.filter(key => !key.startsWith(`${resource}:`)) })} /></TableCell>
                </TableRow>;
              })}</TableBody></Table></div>
            {!resources.some(resource => catalog.some(p => p.key.startsWith(`${resource}:`) && `${p.entity} ${resource}`.toLowerCase().includes(permissionSearch.trim().toLowerCase()))) && <p role="status" className="py-4 text-center text-sm text-muted-foreground">No entities found.</p>}
          </>}
          </fieldset></div>
          <div className="flex shrink-0 justify-end gap-2 border-t bg-muted/20 px-6 py-4"><Button type="button" variant="outline" disabled={saving} onClick={() => { setUser(null); setGroup(null); }}>Cancel</Button><Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save'}</Button></div>
        </form>
      </DialogContent>
    </Dialog>
    <ConfirmDialog request={confirm} onClose={() => setConfirm(null)} />
  </div>;
}
