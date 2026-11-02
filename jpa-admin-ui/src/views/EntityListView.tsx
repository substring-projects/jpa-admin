import { useEffect, useMemo, useRef, useState, type KeyboardEvent, type MouseEvent } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import {
  ArrowDown, ArrowUp, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsLeftRight, ChevronsRight, ChevronsUpDown, Copy, Download,
  Eye, ListFilter, Loader2, MoreHorizontal, Pencil, Plus, RotateCw, Search, Settings2, Trash2, X, Zap,
} from 'lucide-react';
import { fetchRecords } from '@/api/client';
import { useEntity } from '@/context/AdminContext';
import { useAuth } from '@/context/AuthContext';
import { CellValue } from '@/components/CellValue';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { EntityIcon, DynamicIcon } from '@/components/DynamicIcon';
import { ErrorState, NotFound, PageHeader } from '@/components/PageStates';
import { useEntityMutations } from '@/hooks/use-entity-mutations';
import { useHotkeys } from '@/hooks/use-hotkeys';
import { FilterChip, FilterField } from '@/components/list/Filters';
import { SavedViews } from '@/components/list/SavedViews';
import { EditableCell, canEditInTable } from '@/components/list/EditableCell';
import { downloadCsv, toCsv } from '@/lib/csv';
import { formatCount } from '@/lib/format';
import { useStored } from '@/lib/storage';
import { adminUrl } from '@/lib/base-path';
import { cn } from '@/lib/utils';
import type { EntityMetadata, FieldMetadata } from '@/types/metadata';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import {
  DropdownMenu, DropdownMenuCheckboxItem, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuRadioGroup,
  DropdownMenuRadioItem, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';
import { Input } from '@/components/ui/input';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

const PAGE_SIZES = [10, 25, 50, 100];
const EXPORT_LIMIT = 50000;

function useListParams(entity: EntityMetadata) {
  const [params, setParams] = useSearchParams();
  const defaultSort = entity.defaultSortBy ? `${entity.defaultSortDir === 'ASC' ? '' : '-'}${entity.defaultSortBy}` : '';
  const sort = params.get('sort') ?? defaultSort;
  const filters = Object.fromEntries([...params].filter(([key]) => key.startsWith('f.')).map(([key, value]) => [key.slice(2), value]));
  const update = (changes: Record<string, string | null>, resetPage = true) => setParams(prev => {
    const next = new URLSearchParams(prev);
    Object.entries(changes).forEach(([key, value]) => (value == null || value === '' ? next.delete(key) : next.set(key, value)));
    if (resetPage) next.delete('page');
    return next;
  }, { replace: true });
  return {
    page: Math.max(1, Number(params.get('page')) || 1),
    size: Number(params.get('size')) || entity.pageSize || 25,
    sortField: sort.replace(/^-/, ''),
    sortDesc: sort.startsWith('-'),
    q: params.get('q') ?? '',
    filters,
    update,
    query: params.toString(),
    replaceAll: (query: string) => setParams(new URLSearchParams(query), { replace: true }),
    clearAll: () => setParams(new URLSearchParams(), { replace: true }),
  };
}

export function EntityListView() {
  const { entity: entityParam } = useParams();
  const entity = useEntity(entityParam);
  if (!entity) return <NotFound message={`No entity named "${entityParam}" is registered.`} />;
  return <EntityList key={entity.entityName} entity={entity} />;
}

function EntityList({ entity }: { entity: EntityMetadata }) {
  const navigate = useNavigate();
  const { exportEnabled } = useAuth();
  const { page, size, sortField, sortDesc, q, filters, update, query, replaceAll, clearAll } = useListParams(entity);
  const filtersKey = JSON.stringify(filters);

  const [rows, setRows] = useState<Record<string, unknown>[] | null>(null);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [exporting, setExporting] = useState(false);
  const [hidden, setHidden] = useStored<string[]>(`jpa-admin-hidden-columns:${entity.entityName}`, []);
  const [density, setDensity] = useStored<'comfortable' | 'compact'>('jpa-admin-density', 'comfortable');
  const reload = () => setReloadKey(k => k + 1);
  const { confirm, closeConfirm, requestDelete, requestAction } = useEntityMutations(entity, reload);

  const searchRef = useRef<HTMLInputElement>(null);
  const [search, setSearch] = useState(q);
  const pushedSearch = useRef(q);
  useEffect(() => {
    if (search === pushedSearch.current) return;
    const timer = window.setTimeout(() => { pushedSearch.current = search; update({ q: search }); }, 300);
    return () => window.clearTimeout(timer);
  }, [search]);
  useEffect(() => { if (q !== pushedSearch.current) { pushedSearch.current = q; setSearch(q); } }, [q]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    const sort = sortField ? `${sortField},${sortDesc ? 'desc' : 'asc'}` : undefined;
    fetchRecords(entity.entityName, page - 1, size, sort, q, filters)
      .then(res => {
        if (!active) return;
        const lastPage = Math.max(1, Math.ceil(res.totalElements / size));
        if (page > lastPage) { update({ page: String(lastPage) }, false); return; }
        setRows(res.content ?? []);
        setTotal(res.totalElements ?? 0);
        setSelected(new Set());
        setError(null);
      })
      .catch(err => { if (active) setError(err instanceof Error ? err.message : 'Failed to load records'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [entity.entityName, page, size, sortField, sortDesc, q, filtersKey, reloadKey]);

  const listFields = entity.fields.filter(f => f.inList);
  const visibleFields = listFields.filter(f => !hidden.includes(f.name));
  const filterableFields = entity.fields.filter(f => f.filterable);
  const activeFilters = Object.entries(filters).filter(([, value]) => value !== '');
  const hasQuery = q !== '' || activeFilters.length > 0;
  const pk = entity.primaryKeyField;
  const canEdit = entity.updatable && !entity.readOnly;
  const canDelete = entity.deletable && !entity.readOnly;
  const canCreate = entity.creatable && !entity.readOnly;
  const rowActions = entity.actions.filter(a => a.scope === 'SINGLE_ROW' || a.scope === 'BOTH');
  const bulkActions = entity.actions.filter(a => a.scope === 'BULK_SELECTION' || a.scope === 'BOTH');
  const selectable = canDelete || bulkActions.length > 0;
  const totalPages = Math.max(1, Math.ceil(total / size));
  const rowIds = useMemo(() => (rows ?? []).map(r => String(r[pk])), [rows, pk]);
  const selectedRaw = (rows ?? []).filter(r => selected.has(String(r[pk]))).map(r => r[pk]);
  const allSelected = rowIds.length > 0 && rowIds.every(id => selected.has(id));
  const someSelected = !allSelected && rowIds.some(id => selected.has(id));
  const cellPadding = density === 'compact' ? 'py-1.5' : 'py-3';

  const toggleSort = (field: FieldMetadata) => {
    if (!field.sortable) return;
    update({ sort: sortField === field.name && !sortDesc ? `-${field.name}` : field.name });
  };

  const recordPath = (id: unknown) => `/${entity.entityName}/${encodeURIComponent(String(id))}`;
  const openRow = (event: MouseEvent | KeyboardEvent, id: unknown) => {
    if ((event.target as HTMLElement).closest('button, a, input, [role="checkbox"], [role="menuitem"]')) return;
    if (event.metaKey || event.ctrlKey) { window.open(adminUrl(recordPath(id)), '_blank'); return; }
    navigate(recordPath(id));
  };

  const exportCsv = async () => {
    setExporting(true);
    try {
      const sort = sortField ? `${sortField},${sortDesc ? 'desc' : 'asc'}` : undefined;
      const all: Record<string, unknown>[] = [];
      for (let p = 0; all.length < Math.min(total || EXPORT_LIMIT, EXPORT_LIMIT); p++) {
        const res = await fetchRecords(entity.entityName, p, 500, sort, q, filters);
        all.push(...res.content);
        if (res.content.length < 500) break;
      }
      const stamp = new Date().toISOString().slice(0, 10);
      downloadCsv(`${entity.pluralName || entity.entityName}-${stamp}.csv`.replace(/\s+/g, '-').toLowerCase(), toCsv(visibleFields, all));
      toast.success(`Exported ${formatCount(all.length)} ${all.length === 1 ? 'row' : 'rows'}`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Export failed');
    } finally {
      setExporting(false);
    }
  };

  const title = entity.pluralName || entity.displayName;

  const focusedRow = () => (document.activeElement as HTMLElement | null)?.closest<HTMLElement>('tr[data-row-id]');
  const moveFocus = (step: number) => {
    const all = [...document.querySelectorAll<HTMLElement>('tr[data-row-id]')];
    if (all.length === 0) return;
    const index = all.indexOf(focusedRow() as HTMLElement);
    const next = all[index === -1 ? (step > 0 ? 0 : all.length - 1) : Math.min(all.length - 1, Math.max(0, index + step))];
    next.focus();
    next.scrollIntoView({ block: 'nearest' });
  };
  const focusedId = () => {
    const key = focusedRow()?.dataset.rowId;
    return key == null ? undefined : rows?.find(r => String(r[pk]) === key)?.[pk];
  };
  useHotkeys({
    '/': () => searchRef.current?.focus(),
    n: () => { if (canCreate) navigate(`/${entity.entityName}/new`); },
    j: () => moveFocus(1),
    k: () => moveFocus(-1),
    e: () => { const id = focusedId(); if (id != null && canEdit) navigate(`${recordPath(id)}/edit`); },
    x: () => {
      const key = focusedRow()?.dataset.rowId;
      if (key == null || !selectable) return;
      setSelected(prev => { const next = new Set(prev); if (next.has(key)) next.delete(key); else next.add(key); return next; });
    },
  });

  return <div className="mx-auto w-full max-w-7xl space-y-4 p-4 sm:p-6 lg:p-8">
    <PageHeader
      icon={<EntityIcon entity={entity} />}
      title={title}
      description={rows === null ? <Skeleton className="mt-1 h-4 w-28" /> : `${formatCount(total)} ${total === 1 ? 'record' : 'records'}${hasQuery ? ' match your filters' : ''}`}
      actions={<>
        {exportEnabled && entity.exportable !== false && <Button variant="outline" size="sm" onClick={exportCsv} disabled={exporting || total === 0}>
          {exporting ? <Loader2 className="animate-spin" /> : <Download />}Export
        </Button>}
        {canCreate && <Button size="sm" asChild><Link to={`/${entity.entityName}/new`}><Plus />New {entity.displayName}</Link></Button>}
      </>}
    />

    <div className="flex flex-wrap items-center gap-2">
      <div className="relative w-full sm:w-72">
        <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input ref={searchRef} aria-label={`Search ${title}`} placeholder={`Search ${title.toLowerCase()}…`} value={search}
          onChange={e => setSearch(e.target.value)} className="h-8 pl-8 pr-8" />
        {search && <button type="button" aria-label="Clear search" onClick={() => setSearch('')}
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded text-muted-foreground hover:text-foreground"><X className="size-4" /></button>}
      </div>

      {filterableFields.length > 0 && <Popover>
        <PopoverTrigger asChild>
          <Button variant="outline" size="sm" className="border-dashed">
            <ListFilter />Filter
            {activeFilters.length > 0 && <Badge variant="secondary" className="ml-0.5 rounded-sm px-1.5 font-normal">{activeFilters.length}</Badge>}
          </Button>
        </PopoverTrigger>
        <PopoverContent align="start" className="max-h-[70vh] w-80 overflow-y-auto">
          <div className="grid gap-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium">Filters</p>
              {activeFilters.length > 0 && <Button variant="ghost" size="xs" onClick={() => update(Object.fromEntries(activeFilters.map(([k]) => [`f.${k}`, null])))}>Reset</Button>}
            </div>
            {filterableFields.map(field => <FilterField key={field.name} field={field} filters={filters} update={update} />)}
          </div>
        </PopoverContent>
      </Popover>}

      {activeFilters.map(([key, value]) => <FilterChip key={key} fields={entity.fields} filterKey={key} value={value} onRemove={() => update({ [`f.${key}`]: null })} />)}
      {hasQuery && <Button variant="ghost" size="sm" onClick={() => { setSearch(''); clearAll(); }}>Clear all<X /></Button>}

      <div className="ml-auto flex items-center gap-2">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="outline" size="icon-sm" aria-label="Refresh" onClick={reload} disabled={loading}>
              <RotateCw className={cn(loading && rows !== null && 'animate-spin')} />
            </Button>
          </TooltipTrigger>
          <TooltipContent>Refresh</TooltipContent>
        </Tooltip>
        <SavedViews entity={entity.entityName} query={query} onApply={view => { setSearch(new URLSearchParams(view).get('q') ?? ''); replaceAll(view); }} />
        <DropdownMenu>
          <DropdownMenuTrigger asChild><Button variant="outline" size="sm"><Settings2 />Display</Button></DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-52">
            <DropdownMenuLabel>Density</DropdownMenuLabel>
            <DropdownMenuRadioGroup value={density} onValueChange={v => setDensity(v as typeof density)}>
              <DropdownMenuRadioItem value="comfortable">Comfortable</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="compact">Compact</DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
            <DropdownMenuSeparator />
            <DropdownMenuLabel>Columns</DropdownMenuLabel>
            {listFields.map(field => <DropdownMenuCheckboxItem key={field.name} checked={!hidden.includes(field.name)}
              disabled={!hidden.includes(field.name) && visibleFields.length === 1}
              onSelect={e => e.preventDefault()}
              onCheckedChange={checked => setHidden(checked ? hidden.filter(n => n !== field.name) : [...hidden, field.name])}>
              {field.label}
            </DropdownMenuCheckboxItem>)}
            {hidden.length > 0 && <><DropdownMenuSeparator /><DropdownMenuItem onSelect={() => setHidden([])}>Show all columns</DropdownMenuItem></>}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>

    {error && rows === null ? <div className="rounded-xl border"><ErrorState title="Couldn't load records" description={error} onRetry={reload} /></div> : <div className="overflow-hidden rounded-xl border bg-card">
      <Table containerClassName={cn('max-h-[calc(100dvh-17rem)] overflow-auto', rows?.length !== 0 && 'min-h-48')} aria-busy={loading}
        className="border-separate border-spacing-0 [&_td]:border-b [&_th]:border-b [&_tbody_tr:last-child_td]:border-b-0">
        <TableHeader className="sticky top-0 z-20 bg-card">
          <TableRow className="hover:bg-transparent">
            {selectable && <TableHead className="sticky left-0 z-10 w-10 min-w-10 bg-card pl-4">
              <Checkbox aria-label="Select all on this page" disabled={!rows?.length}
                checked={allSelected ? true : someSelected ? 'indeterminate' : false}
                onCheckedChange={checked => setSelected(checked ? new Set(rowIds) : new Set())} />
            </TableHead>}
            {visibleFields.map((field, index) => {
              const active = sortField === field.name;
              const numeric = field.fieldType === 'NUMBER';
              return <TableHead key={field.name} aria-sort={active ? (sortDesc ? 'descending' : 'ascending') : undefined}
                className={cn('h-10 whitespace-nowrap text-xs font-medium text-muted-foreground', numeric && 'text-right',
                  index === 0 && cn('sticky z-10 bg-card', selectable ? 'left-10' : 'left-0 pl-4'))}>
                {field.sortable
                  ? <button type="button" onClick={() => toggleSort(field)} className={cn('-mx-2 inline-flex items-center gap-1 rounded-md px-2 py-1 hover:bg-accent hover:text-foreground', active && 'text-foreground', numeric && 'flex-row-reverse')}>
                      {field.label}
                      {active ? (sortDesc ? <ArrowDown className="size-3.5" /> : <ArrowUp className="size-3.5" />) : <ChevronsUpDown className="size-3.5 opacity-40" />}
                    </button>
                  : field.label}
              </TableHead>;
            })}
            <TableHead className="sticky right-0 z-10 w-12 bg-card"><span className="sr-only">Actions</span></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody className={cn(loading && rows !== null && 'opacity-60 transition-opacity')}>
          {rows === null && Array.from({ length: 6 }, (_, i) => <TableRow key={i} className="hover:bg-transparent">
            {selectable && <TableCell className="w-10 min-w-10 pl-4"><Skeleton className="size-4" /></TableCell>}
            {visibleFields.map(f => <TableCell key={f.name} className={cellPadding}><Skeleton className="h-4 w-full max-w-32" /></TableCell>)}
            <TableCell />
          </TableRow>)}

          {rows?.map(record => {
            const id = record[pk];
            const key = String(id);
            const isSelected = selected.has(key);
            return <TableRow key={key} data-state={isSelected ? 'selected' : undefined} tabIndex={0} data-row-id={key}
              onClick={e => openRow(e, id)} onKeyDown={e => { if (e.key === 'Enter' && e.target === e.currentTarget) openRow(e, id); }}
              className="group/row cursor-pointer hover:bg-muted data-[state=selected]:bg-muted focus-visible:bg-muted focus-visible:outline-none">
              {selectable && <TableCell className="sticky left-0 z-[1] w-10 min-w-10 bg-card pl-4 group-hover/row:bg-muted group-data-[state=selected]/row:bg-muted group-focus-visible/row:bg-muted">
                <Checkbox aria-label={`Select ${key}`} checked={isSelected}
                  onCheckedChange={checked => setSelected(prev => { const next = new Set(prev); if (checked) next.add(key); else next.delete(key); return next; })} />
              </TableCell>}
              {visibleFields.map((field, index) => <TableCell key={field.name}
                className={cn(cellPadding, 'max-w-80', field.fieldType === 'NUMBER' && 'text-right',
                  index === 0 && cn('sticky z-[1] bg-card font-medium group-hover/row:bg-muted group-data-[state=selected]/row:bg-muted group-focus-visible/row:bg-muted', selectable ? 'left-10' : 'left-0 pl-4'))}>
                {canEditInTable(entity, field)
                  ? <EditableCell entity={entity} field={field} record={record} onSaved={updated => setRows(prev => prev?.map(r => (r[pk] === id ? { ...r, ...updated } : r)) ?? prev)}>
                      <CellValue field={field} value={record[field.name]} />
                    </EditableCell>
                  : <CellValue field={field} value={record[field.name]} />}
              </TableCell>)}
              <TableCell className={cn(cellPadding, 'sticky right-0 z-[1] bg-card pr-3 text-right group-hover/row:bg-muted group-data-[state=selected]/row:bg-muted group-focus-visible/row:bg-muted')}>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon-sm" className="text-muted-foreground data-[state=open]:bg-accent" aria-label={`Actions for ${key}`}><MoreHorizontal /></Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-48">
                    <DropdownMenuItem onSelect={() => navigate(recordPath(id))}><Eye />View details</DropdownMenuItem>
                    {canEdit && <DropdownMenuItem onSelect={() => navigate(`${recordPath(id)}/edit`)}><Pencil />Edit</DropdownMenuItem>}
                    {canCreate && <DropdownMenuItem onSelect={() => navigate(`/${entity.entityName}/new?copy=${encodeURIComponent(key)}`)}><Copy />Duplicate</DropdownMenuItem>}
                    <DropdownMenuItem onSelect={() => { void navigator.clipboard?.writeText(key); toast.success('ID copied'); }}><ChevronsLeftRight />Copy ID</DropdownMenuItem>
                    {rowActions.length > 0 && <DropdownMenuSeparator />}
                    {rowActions.map(action => <DropdownMenuItem key={action.id} onSelect={() => requestAction(action, [id])}><DynamicIcon name={action.icon || 'zap'} fallback={Zap} />{action.name}</DropdownMenuItem>)}
                    {canDelete && <><DropdownMenuSeparator /><DropdownMenuItem variant="destructive" onSelect={() => requestDelete([id])}><Trash2 />Delete</DropdownMenuItem></>}
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
            </TableRow>;
          })}
        </TableBody>
      </Table>

      {rows?.length === 0 && <Empty className="border-t">
        <EmptyHeader>
          <EmptyMedia variant="icon">{hasQuery ? <Search /> : <EntityIcon entity={entity} />}</EmptyMedia>
          <EmptyTitle>{hasQuery ? 'No matching records' : `No ${title.toLowerCase()} yet`}</EmptyTitle>
          <EmptyDescription>{hasQuery ? 'Try a different search term or remove some filters.' : canCreate ? `Create the first ${entity.displayName.toLowerCase()} to get started.` : 'Records will appear here once they exist.'}</EmptyDescription>
        </EmptyHeader>
        <EmptyContent>
          {hasQuery ? <Button variant="outline" onClick={() => { setSearch(''); clearAll(); }}>Clear search and filters</Button>
            : canCreate && <Button asChild><Link to={`/${entity.entityName}/new`}><Plus />New {entity.displayName}</Link></Button>}
        </EmptyContent>
      </Empty>}

      <div className="flex flex-col gap-3 border-t px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
        <p className="tabular text-muted-foreground">
          {total === 0 ? 'No records' : `Showing ${formatCount((page - 1) * size + 1)}–${formatCount(Math.min(page * size, total))} of ${formatCount(total)}`}
        </p>
        <div className="flex items-center gap-4 sm:gap-6">
          <div className="flex items-center gap-2">
            <span className="hidden text-muted-foreground sm:inline">Rows per page</span>
            <Select value={String(size)} onValueChange={v => update({ size: v })}>
              <SelectTrigger size="sm" className="w-[4.5rem]" aria-label="Rows per page"><SelectValue /></SelectTrigger>
              <SelectContent side="top">
                {Array.from(new Set([...PAGE_SIZES, size])).sort((a, b) => a - b).map(n => <SelectItem key={n} value={String(n)}>{n}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
          <span className="tabular whitespace-nowrap font-medium">Page {page} of {totalPages}</span>
          <div className="flex items-center gap-1">
            <Button variant="outline" size="icon-sm" className="hidden sm:inline-flex" aria-label="First page" disabled={page <= 1 || loading} onClick={() => update({ page: null }, false)}><ChevronsLeft /></Button>
            <Button variant="outline" size="icon-sm" aria-label="Previous page" disabled={page <= 1 || loading} onClick={() => update({ page: page - 1 > 1 ? String(page - 1) : null }, false)}><ChevronLeft /></Button>
            <Button variant="outline" size="icon-sm" aria-label="Next page" disabled={page >= totalPages || loading} onClick={() => update({ page: String(page + 1) }, false)}><ChevronRight /></Button>
            <Button variant="outline" size="icon-sm" className="hidden sm:inline-flex" aria-label="Last page" disabled={page >= totalPages || loading} onClick={() => update({ page: String(totalPages) }, false)}><ChevronsRight /></Button>
          </div>
        </div>
      </div>
    </div>}

    {selected.size > 0 && <div role="toolbar" aria-label="Bulk actions"
      className="fixed inset-x-4 bottom-4 z-30 mx-auto flex max-w-fit flex-wrap items-center gap-2 rounded-xl border bg-popover p-2 pl-4 text-popover-foreground shadow-lg animate-in fade-in slide-in-from-bottom-2">
      <span className="tabular text-sm font-medium">{selected.size} selected</span>
      <span className="mx-1 h-5 w-px bg-border" />
      {bulkActions.map(action => <Button key={action.id} variant="ghost" size="sm" onClick={() => requestAction(action, selectedRaw)}><DynamicIcon name={action.icon || 'zap'} fallback={Zap} />{action.name}</Button>)}
      {canDelete && <Button variant="ghost" size="sm" className="text-destructive hover:bg-destructive/10 hover:text-destructive" onClick={() => requestDelete(selectedRaw)}><Trash2 />Delete</Button>}
      <Button variant="ghost" size="icon-sm" aria-label="Clear selection" onClick={() => setSelected(new Set())}><X /></Button>
    </div>}

    <ConfirmDialog request={confirm} onClose={closeConfirm} />
  </div>;
}
