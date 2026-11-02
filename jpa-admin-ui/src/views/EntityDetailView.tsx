import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { toast } from 'sonner';
import { ChevronsLeftRight, Copy, MoreHorizontal, Pencil, Trash2, Zap } from 'lucide-react';
import { ApiError, getRecord } from '@/api/client';
import { useEntity } from '@/context/AdminContext';
import { CellValue, CopyButton } from '@/components/CellValue';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { EntityIcon, DynamicIcon } from '@/components/DynamicIcon';
import { ErrorState, NotFound, PageHeader } from '@/components/PageStates';
import { useLayout } from '@/components/layout/AdminLayout';
import { useEntityMutations } from '@/hooks/use-entity-mutations';
import { useHotkeys } from '@/hooks/use-hotkeys';
import { MediaCard, RecordThumbnail, imageValue, isMediaField } from '@/components/RecordMedia';
import { cn } from '@/lib/utils';
import { recordLabel, shortId } from '@/lib/format';
import { rememberRecord } from '@/lib/storage';
import type { EntityMetadata, InlineMetadata } from '@/types/metadata';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

export function EntityDetailView() {
  const { entity: entityParam, id } = useParams();
  const entity = useEntity(entityParam);
  if (!entity || id == null) return <NotFound message={`No entity named "${entityParam}" is registered.`} />;
  return <EntityDetail key={`${entity.entityName}:${id}`} entity={entity} id={id} />;
}

function InlineTable({ inline, rows }: { inline: InlineMetadata; rows: Record<string, unknown>[] }) {
  const target = useEntity(inline.targetEntity);
  const fields = (target?.fields ?? []).filter(f => f.inList !== false && f.name !== inline.mappedBy
    && !['RELATION_ONE_TO_MANY', 'RELATION_MANY_TO_MANY'].includes(f.fieldType)).slice(0, 8);
  const pk = target?.primaryKeyField ?? 'id';
  if (rows.length === 0) {
    return <Empty className="py-10"><EmptyHeader><EmptyTitle className="text-sm">No {inline.title.toLowerCase()}</EmptyTitle>
      <EmptyDescription>Related records will be listed here.</EmptyDescription></EmptyHeader></Empty>;
  }
  const open = (row: Record<string, unknown>) => target && row[pk] != null
    && <Button asChild variant="ghost" size="xs"><Link to={`/${target.entityName}/${encodeURIComponent(String(row[pk]))}`}>Open</Link></Button>;
  if (inline.type === 'STACKED') {
    return <div className="divide-y">{rows.map((row, index) => <section key={String(row[pk] ?? index)} aria-label={`${inline.title} ${index + 1}`} className="px-6 py-4">
      <div className="mb-2 flex items-center justify-between"><h4 className="text-xs font-medium text-muted-foreground">#{index + 1}</h4>{open(row)}</div>
      <dl className="grid gap-x-6 gap-y-3 sm:grid-cols-2">
        {fields.map(f => <div key={f.name} className="min-w-0"><dt className="text-xs text-muted-foreground">{f.label}</dt>
          <dd className="mt-1 text-sm"><CellValue field={f} value={row[f.name]} expanded /></dd></div>)}
      </dl>
    </section>)}</div>;
  }
  return <Table>
    <TableHeader><TableRow className="hover:bg-transparent">
      {fields.map((f, i) => <TableHead key={f.name} className={`h-10 text-xs font-medium text-muted-foreground ${i === 0 ? 'pl-6' : ''} ${f.fieldType === 'NUMBER' ? 'text-right' : ''}`}>{f.label}</TableHead>)}
      {target && <TableHead className="w-20 pr-6" />}
    </TableRow></TableHeader>
    <TableBody>{rows.map((row, index) => <TableRow key={String(row[pk] ?? index)}>
      {fields.map((f, i) => <TableCell key={f.name} className={`max-w-72 ${i === 0 ? 'pl-6' : ''} ${f.fieldType === 'NUMBER' ? 'text-right' : ''}`}><CellValue field={f} value={row[f.name]} /></TableCell>)}
      {target && <TableCell className="pr-6 text-right">{open(row)}</TableCell>}
    </TableRow>)}</TableBody>
  </Table>;
}

function EntityDetail({ entity, id }: { entity: EntityMetadata; id: string }) {
  const navigate = useNavigate();
  const { setRecordLabel } = useLayout();
  const [record, setRecord] = useState<Record<string, unknown> | null>(null);
  const [error, setError] = useState<{ message: string; notFound: boolean } | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const { confirm, closeConfirm, requestDelete, requestAction } = useEntityMutations(entity, change => {
    if (change === 'delete') navigate(`/${entity.entityName}`); else setReloadKey(k => k + 1);
  });

  useEffect(() => {
    let active = true;
    getRecord(entity.entityName, id)
      .then(data => {
        if (!active) return;
        setRecord(data);
        setError(null);
        const label = recordLabel(entity, data);
        setRecordLabel(label);
        rememberRecord({ entity: entity.entityName, id, label });
      })
      .catch(err => { if (active) setError({ message: err instanceof Error ? err.message : 'Failed to load record', notFound: err instanceof ApiError && err.status === 404 }); });
    return () => { active = false; };
  }, [entity.entityName, id, reloadKey]);

  useHotkeys({ e: () => { if (entity.updatable && !entity.readOnly && record) navigate('edit'); } });

  if (error && !record) {
    return error.notFound ? <NotFound message={`This ${entity.displayName.toLowerCase()} doesn't exist or was deleted.`} />
      : <ErrorState title="Couldn't load this record" description={error.message} onRetry={() => setReloadKey(k => k + 1)} />;
  }

  const canEdit = entity.updatable && !entity.readOnly;
  const canDelete = entity.deletable && !entity.readOnly;
  const canCreate = entity.creatable && !entity.readOnly;
  const rowActions = entity.actions.filter(a => a.scope === 'SINGLE_ROW' || a.scope === 'BOTH');
  const inlineNames = new Set(entity.inlines.map(i => i.propertyName));
  const detailFields = entity.fields.filter(f => f.inDetail !== false && !inlineNames.has(f.name)
    && f.fieldType !== 'RELATION_ONE_TO_MANY');
  const mediaFields = detailFields.filter(isMediaField);
  const infoFields = detailFields.filter(f => !isMediaField(f));
  const primaryImage = record ? mediaFields.map(f => imageValue(record[f.name])).find(Boolean) ?? null : null;
  const title = record ? recordLabel(entity, record) : '';
  const longField = (type: string, widget: string) => widget === 'TEXTAREA' || widget === 'RICH_TEXT' || widget === 'CODE_EDITOR' || type === 'EMBEDDED' || type === 'RELATION_MANY_TO_MANY';
  const rawId = record?.[entity.primaryKeyField] ?? id;

  return <div className={cn('mx-auto w-full space-y-6 p-4 sm:p-6 lg:p-8', mediaFields.length ? 'max-w-6xl' : 'max-w-5xl')}>
    <PageHeader
      icon={<RecordThumbnail url={primaryImage} fallback={<EntityIcon entity={entity} />} />}
      title={record ? title : <Skeleton className="h-7 w-56" />}
      description={<span className="inline-flex items-center gap-2">
        <Badge variant="outline" className="font-normal">{entity.displayName}</Badge>
        <span className="group/cell inline-flex items-center gap-1 font-mono text-xs">{shortId(id)}<CopyButton value={id} label="Copy ID" /></span>
      </span>}
      actions={record && <>
        {canEdit && <Button size="sm" asChild><Link to="edit"><Pencil />Edit</Link></Button>}
        <DropdownMenu>
          <DropdownMenuTrigger asChild><Button variant="outline" size="icon-sm" aria-label="More actions"><MoreHorizontal /></Button></DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            {canCreate && <DropdownMenuItem onSelect={() => navigate(`/${entity.entityName}/new?copy=${encodeURIComponent(id)}`)}><Copy />Duplicate</DropdownMenuItem>}
            <DropdownMenuItem onSelect={() => { void navigator.clipboard?.writeText(id); toast.success('ID copied'); }}><ChevronsLeftRight />Copy ID</DropdownMenuItem>
            {rowActions.length > 0 && <DropdownMenuSeparator />}
            {rowActions.map(action => <DropdownMenuItem key={action.id} onSelect={() => requestAction(action, [rawId])}><DynamicIcon name={action.icon || 'zap'} fallback={Zap} />{action.name}</DropdownMenuItem>)}
            {canDelete && <><DropdownMenuSeparator /><DropdownMenuItem variant="destructive" onSelect={() => requestDelete([rawId])}><Trash2 />Delete</DropdownMenuItem></>}
          </DropdownMenuContent>
        </DropdownMenu>
      </>}
    />

    <Tabs defaultValue="details">
      <TabsList>
        <TabsTrigger value="details">Details</TabsTrigger>
        {entity.inlines.map(inline => {
          const rows = record?.[inline.propertyName];
          return <TabsTrigger key={inline.propertyName} value={inline.propertyName}>
            {inline.title}
            {Array.isArray(rows) && <Badge variant="secondary" className="tabular ml-1 h-5 rounded-sm px-1.5 font-normal">{rows.length}</Badge>}
          </TabsTrigger>;
        })}
      </TabsList>

      <TabsContent value="details">
        <div className={cn(mediaFields.length && 'grid items-start gap-6 lg:grid-cols-[minmax(0,340px)_minmax(0,1fr)]')}>
          {mediaFields.length > 0 && <div className="space-y-4 lg:sticky lg:top-20">
            {record ? mediaFields.map(field => <MediaCard key={field.name} field={field} value={record[field.name]} recordTitle={title}
                editPath={canEdit && !field.readOnly ? `/${entity.entityName}/${encodeURIComponent(id)}/edit` : undefined} />)
              : <Skeleton className="aspect-square w-full rounded-xl" />}
          </div>}
          <Card className="py-0">
            <CardContent className="px-0">
              <dl className="grid gap-x-8 gap-y-6 p-6 sm:grid-cols-2">
                {infoFields.map(field => <div key={field.name} className={longField(field.fieldType, field.widget) || field.upload ? 'sm:col-span-2' : undefined}>
                  <dt className="text-xs font-medium text-muted-foreground" title={field.description || undefined}>{field.label}</dt>
                  <dd className="mt-1.5 min-h-5 text-sm">{record ? <CellValue field={field} value={record[field.name]} expanded /> : <Skeleton className="h-5 w-40" />}</dd>
                </div>)}
              </dl>
            </CardContent>
          </Card>
        </div>
      </TabsContent>

      {entity.inlines.map(inline => <TabsContent key={inline.propertyName} value={inline.propertyName}>
        <Card className="overflow-hidden py-0">
          {record ? <InlineTable inline={inline} rows={(record[inline.propertyName] as Record<string, unknown>[]) ?? []} />
            : <CardContent className="space-y-3 py-6">{Array.from({ length: 3 }, (_, i) => <Skeleton key={i} className="h-8 w-full" />)}</CardContent>}
        </Card>
      </TabsContent>)}
    </Tabs>

    <ConfirmDialog request={confirm} onClose={closeConfirm} />
  </div>;
}
