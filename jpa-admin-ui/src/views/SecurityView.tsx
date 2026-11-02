import { useEffect, useState, type FormEvent } from 'react';
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
  return <div className="space-y-6 p-4 md:p-6">
    <PageHeader title="Users and groups" description="Control who can enter the admin and which records they can manage." />
    {error && <ErrorState title="Unable to load security settings" description={error} onRetry={load} />}
    {loading ? <p role="status">Loading security settings…</p> : !error && <Tabs defaultValue="users" onValueChange={() => setSearch('')}>
      <div className="flex flex-wrap items-center justify-between gap-3"><TabsList><TabsTrigger value="users" aria-label="Users">Users <Badge variant="secondary">{users.length}</Badge></TabsTrigger><TabsTrigger value="groups" aria-label="Groups">Groups <Badge variant="secondary">{groups.length}</Badge></TabsTrigger></TabsList>
      <Input className="w-full sm:w-72" aria-label="Search users or groups" placeholder="Search users or groups…" value={search} onChange={e => setSearch(e.target.value)} /></div>
      <TabsContent value="users" className="space-y-4">
        <Button onClick={() => { setFormError(''); setUser({ username: '', password: '', active: true, adminAccess: true, superuser: false, groupIds: [], permissions: [] }); }}>Add user</Button>
        <Table><TableHeader><TableRow><TableHead>Username</TableHead><TableHead>Status</TableHead><TableHead>Access</TableHead><TableHead>Groups</TableHead><TableHead>Actions</TableHead></TableRow></TableHeader>
          <TableBody>{users.filter(u => matches(u.username) || groups.some(g => u.groupIds.includes(g.id!) && matches(g.name))).map(u => <TableRow key={u.id}><TableCell>{u.username}</TableCell>
            <TableCell><Badge variant={!u.active || !u.adminAccess ? 'outline' : 'secondary'}>{!u.active ? 'Disabled' : !u.adminAccess ? 'No admin access' : 'Active'}</Badge></TableCell>
            <TableCell><Badge variant={u.superuser ? 'default' : 'outline'}>{u.superuser ? 'Superuser' : 'Group permissions'}</Badge></TableCell>
            <TableCell><div className="flex flex-wrap gap-1">{groups.filter(g => u.groupIds.includes(g.id!)).map(g => <Badge key={g.id} variant="secondary">{g.name}</Badge>)}{!u.groupIds.length && <span className="text-muted-foreground">No groups</span>}</div></TableCell>
            <TableCell><Button variant="ghost" aria-label={`Edit user ${u.username}`} onClick={() => { setFormError(''); setUser({ ...u, password: '' }); }}>Edit</Button>
              <Button variant="ghost" aria-label={`Delete user ${u.username}`} onClick={() => remove('users', u.id!, u.username)}>Delete</Button></TableCell>
          </TableRow>)}</TableBody></Table>
      </TabsContent>
      <TabsContent value="groups" className="space-y-4">
        <Button onClick={() => { openGroup({ name: '', permissions: [] }); }}>Add group</Button>
        {!groups.length && <div className="rounded-xl border border-dashed bg-muted/20 px-6 py-12 text-center"><h3 className="font-semibold">Give your team the right access</h3><p className="mx-auto mt-2 max-w-md text-sm text-muted-foreground">Groups bundle entity permissions. Create a group, choose what it can do, then assign users.</p><Button className="mt-5" onClick={() => openGroup({ name: '', permissions: [] })}>Create first group</Button></div>}
        <Table><TableHeader><TableRow><TableHead>Group</TableHead><TableHead>Members</TableHead><TableHead>Permissions</TableHead><TableHead>Actions</TableHead></TableRow></TableHeader>
          <TableBody>{groups.filter(g => matches(g.name)).map(g => <TableRow key={g.id}><TableCell>{g.name}</TableCell><TableCell>{users.filter(u => u.groupIds.includes(g.id!)).length}</TableCell><TableCell><Badge variant="secondary">{g.permissions.length} permissions</Badge></TableCell>
            <TableCell><Button variant="ghost" aria-label={`Edit group ${g.name}`} onClick={() => { openGroup(g); }}>Edit</Button>
              <Button variant="ghost" aria-label={`Delete group ${g.name}`} onClick={() => remove('groups', g.id!, g.name)}>Delete</Button></TableCell>
          </TableRow>)}</TableBody></Table>
      </TabsContent>
    </Tabs>}
    <Dialog open={!!user || !!group} onOpenChange={open => { if (!open && !saving) { setUser(null); setGroup(null); } }}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-4xl">
        <DialogHeader><DialogTitle>{user ? user.id ? 'Edit user' : 'Add user' : group?.id ? 'Edit group' : 'Add group'}</DialogTitle>
          <DialogDescription>{user ? 'Changes to groups and account access apply to subsequent requests. Password resets require a new sign-in.' : 'Permissions from all groups are combined. Selecting a write or action permission also enables View. Clearing View removes all access to that entity.'}</DialogDescription></DialogHeader>
        <form onSubmit={save} className="space-y-5">
          {formError && <p role="alert" className="text-sm text-destructive">{formError}</p>}
          <fieldset disabled={saving} className="space-y-5">
          {user && <>
            <div className="space-y-2"><Label htmlFor="admin-username">Username</Label><Input id="admin-username" required maxLength={100} value={user.username} onChange={e => setUser({ ...user, username: e.target.value })} /></div>
            <div className="space-y-2"><Label htmlFor="admin-password">{user.id ? 'New password (leave blank to keep)' : 'Password'}</Label>
              <Input id="admin-password" type="password" autoComplete="new-password" required={!user.id} minLength={12} value={user.password} onChange={e => setUser({ ...user, password: e.target.value })} />
              <p className="text-xs text-muted-foreground">At least 12 characters; at most 72 UTF-8 bytes.</p></div>
            {(['active', 'adminAccess', 'superuser'] as const).map(key => <Label key={key} className="flex gap-2"><Checkbox checked={user[key]} onCheckedChange={v => setUser({ ...user, [key]: !!v })} />{key === 'active' ? 'Active account' : key === 'adminAccess' ? 'Allow admin access' : 'Superuser — all entity permissions and security management'}</Label>)}
            <Card><CardHeader><CardTitle>Groups</CardTitle></CardHeader><CardContent className="space-y-3">{groups.length ? groups.map(g => <Label key={g.id} className="flex gap-2"><Checkbox checked={user.groupIds.includes(g.id!)} onCheckedChange={v => setUser({ ...user, groupIds: toggle(user.groupIds, g.id!, !!v) })} />{g.name}</Label>) : <p>Create groups first to grant entity access.</p>}</CardContent></Card>
            <details><summary className="cursor-pointer text-sm">Effective assigned permissions</summary>
              <p className="mt-2 text-xs text-muted-foreground">{user.superuser ? 'All entity permissions. Entity read-only restrictions still apply.' : [...new Set(groups.filter(g => user.groupIds.includes(g.id!)).flatMap(g => g.permissions))].join(', ') || 'No entity permissions assigned.'}</p></details>
          </>}
          {group && <>
            <div className="space-y-2"><Label htmlFor="group-name">Group name</Label><Input id="group-name" required maxLength={100} value={group.name} onChange={e => setGroup({ ...group, name: e.target.value })} /></div>
            <div className="flex flex-wrap items-center justify-between gap-3"><Input className="sm:max-w-xs" aria-label="Search entities" placeholder="Search entities…" value={permissionSearch} onChange={e => setPermissionSearch(e.target.value)} /><Badge variant="secondary">{group.permissions.length} selected</Badge></div>
            <div className="rounded-lg border"><Table><TableHeader><TableRow><TableHead>Entity</TableHead>{['View', 'Add', 'Change', 'Delete', 'All'].map(label => <TableHead key={label} className="text-center">{label}</TableHead>)}</TableRow></TableHeader>
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
          </fieldset>
          <div className="flex justify-end gap-2"><Button type="button" variant="outline" disabled={saving} onClick={() => { setUser(null); setGroup(null); }}>Cancel</Button><Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save'}</Button></div>
        </form>
      </DialogContent>
    </Dialog>
    <ConfirmDialog request={confirm} onClose={() => setConfirm(null)} />
  </div>;
}
