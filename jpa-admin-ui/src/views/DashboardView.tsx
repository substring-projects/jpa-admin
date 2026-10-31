import { Link } from 'react-router';
import { ArrowUpRight, Boxes, Clock, Database, Layers, Plus } from 'lucide-react';
import { useAdmin } from '@/context/AdminContext';
import { useAuth } from '@/context/AuthContext';
import { EntityIcon } from '@/components/DynamicIcon';
import { PageHeader } from '@/components/PageStates';
import { ActivityChart } from '@/components/ActivityChart';
import { formatCount, formatRelative } from '@/lib/format';
import { useRecentRecords } from '@/lib/storage';
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

function StatCard({ label, value, icon, hint }: { label: string; value: string | null; icon: React.ReactNode; hint: string }) {
  return <Card className="gap-2 py-5">
    <CardHeader className="px-5">
      <CardDescription>{label}</CardDescription>
      <CardAction className="text-muted-foreground [&_svg]:size-4">{icon}</CardAction>
      <CardTitle className="tabular text-3xl font-semibold tracking-tight">
        {value ?? <Skeleton className="h-9 w-20" />}
      </CardTitle>
    </CardHeader>
    <CardContent className="px-5 text-xs text-muted-foreground">{hint}</CardContent>
  </Card>;
}

export function DashboardView() {
  const { metadata, loading, counts, getEntity } = useAdmin();
  const { username } = useAuth();
  const recent = useRecentRecords().filter(r => getEntity(r.entity));

  const groups = Array.from(new Set(metadata.map(e => e.group || 'General')));
  const total = counts ? Object.values(counts).reduce((sum, n) => sum + n, 0) : null;
  const largest = counts && metadata.length
    ? [...metadata].sort((a, b) => (counts[b.entityName] ?? 0) - (counts[a.entityName] ?? 0))[0] : undefined;
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';

  return <div className="mx-auto w-full max-w-7xl space-y-8 p-4 sm:p-6 lg:p-8">
    <PageHeader title={`${greeting}, ${username}`} description="Here's an overview of the data managed in this application." />

    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <StatCard label="Total records" value={total == null ? null : formatCount(total)} icon={<Database />} hint={`Across ${metadata.length} entities`} />
      <StatCard label="Entities" value={loading ? null : String(metadata.length)} icon={<Boxes />} hint="Discovered from your JPA model" />
      <StatCard label="Domain groups" value={loading ? null : String(groups.length)} icon={<Layers />} hint={groups.slice(0, 3).join(', ') || '—'} />
      <StatCard label="Largest table" value={largest ? (largest.pluralName || largest.displayName) : loading || !counts ? null : '—'} icon={<ArrowUpRight />}
        hint={largest && counts ? `${formatCount(counts[largest.entityName] ?? 0)} records` : 'No data yet'} />
    </div>

    <ActivityChart />

    <div className="grid gap-6 lg:grid-cols-3">
      <div className="space-y-6 lg:col-span-2">
        {loading && <Card><CardContent className="space-y-3">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-12 w-full" />)}</CardContent></Card>}
        {groups.map(group => <Card key={group} className="gap-0 py-0">
          <CardHeader className="border-b px-5 py-4 [.border-b]:pb-4">
            <CardTitle className="text-sm font-semibold">{group}</CardTitle>
            <CardDescription>{(n => `${n} ${n === 1 ? 'entity' : 'entities'}`)(metadata.filter(e => (e.group || 'General') === group).length)}</CardDescription>
          </CardHeader>
          <ul className="divide-y">
            {metadata.filter(e => (e.group || 'General') === group).map(entity => {
              const count = counts?.[entity.entityName];
              return <li key={entity.entityName} className="group relative flex items-center gap-4 px-5 py-3 transition-colors hover:bg-muted/50">
                <div className="flex size-9 shrink-0 items-center justify-center rounded-lg border bg-background text-muted-foreground group-hover:text-primary">
                  <EntityIcon entity={entity} className="size-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <Link to={`/${entity.entityName}`} className="font-medium after:absolute after:inset-0">{entity.pluralName || entity.displayName}</Link>
                  <p className="truncate text-xs text-muted-foreground">
                    {entity.readOnly ? 'Read-only' : [entity.creatable && 'Create', entity.updatable && 'Edit', entity.deletable && 'Delete'].filter(Boolean).join(' · ')}
                    {entity.actions.length > 0 && ` · ${entity.actions.length} action${entity.actions.length > 1 ? 's' : ''}`}
                  </p>
                </div>
                <div className="text-right">
                  {count == null ? <Skeleton className="h-5 w-10" /> : <span className="tabular text-sm font-semibold">{formatCount(count)}</span>}
                  <p className="text-xs text-muted-foreground">records</p>
                </div>
                {entity.creatable && !entity.readOnly && <Tooltip>
                  <TooltipTrigger asChild>
                    <Button asChild variant="ghost" size="icon-sm" className="relative z-10 text-muted-foreground">
                      <Link to={`/${entity.entityName}/new`} aria-label={`New ${entity.displayName}`}><Plus /></Link>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>New {entity.displayName}</TooltipContent>
                </Tooltip>}
              </li>;
            })}
          </ul>
        </Card>)}
      </div>

      <Card className="h-fit gap-0 py-0">
        <CardHeader className="border-b px-5 py-4 [.border-b]:pb-4">
          <CardTitle className="text-sm font-semibold">Recently viewed</CardTitle>
          <CardDescription>Records you opened on this device</CardDescription>
        </CardHeader>
        {recent.length === 0
          ? <div className="flex flex-col items-center gap-2 px-5 py-10 text-center text-sm text-muted-foreground">
              <Clock className="size-5" />Records you open will appear here.
            </div>
          : <ul className="divide-y">{recent.map(r => {
              const entity = getEntity(r.entity)!;
              return <li key={`${r.entity}:${r.id}`} className="relative flex items-center gap-3 px-5 py-3 hover:bg-muted/50">
                <EntityIcon entity={entity} className="size-4 shrink-0 text-muted-foreground" />
                <div className="min-w-0 flex-1">
                  <Link to={`/${entity.entityName}/${encodeURIComponent(r.id)}`} className="block truncate text-sm font-medium after:absolute after:inset-0">{r.label}</Link>
                  <p className="text-xs text-muted-foreground">{entity.displayName} · {formatRelative(new Date(r.at).toISOString())}</p>
                </div>
              </li>;
            })}</ul>}
      </Card>
    </div>
  </div>;
}
