import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import { getRecord } from '@/api/client';
import { useAdmin } from '@/context/AdminContext';
import { formatDate, formatNumber, humanize, recordLabel } from '@/lib/format';
import type { FieldMetadata } from '@/types/metadata';
import { RelationSelect } from '@/components/RelationSelect';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

const ANY = '__any__';
export const RANGE_TYPES = ['NUMBER', 'DATE', 'DATE_TIME'];
const isRelation = (f: FieldMetadata) => f.fieldType === 'RELATION_MANY_TO_ONE' || f.fieldType === 'RELATION_ONE_TO_ONE';

/** Commits after the user pauses typing or leaves the field, so each keystroke doesn't refetch the table. */
function DebouncedInput({ value, onCommit, ...props }: { value: string; onCommit: (value: string) => void } & Omit<React.ComponentProps<typeof Input>, 'value' | 'onChange'>) {
  const [draft, setDraft] = useState(value);
  const committed = useRef(value);
  useEffect(() => { if (value !== committed.current) { committed.current = value; setDraft(value); } }, [value]);
  useEffect(() => {
    if (draft === committed.current) return;
    const timer = window.setTimeout(() => { committed.current = draft; onCommit(draft); }, 450);
    return () => window.clearTimeout(timer);
  }, [draft]);
  return <Input {...props} value={draft} onChange={e => setDraft(e.target.value)} />;
}

interface FieldProps { field: FieldMetadata; filters: Record<string, string>; update: (changes: Record<string, string | null>) => void }

export function FilterField({ field, filters, update }: FieldProps) {
  const id = `filter-${field.name}`;
  const set = (key: string, value: string) => update({ [`f.${key}`]: value || null });
  const value = filters[field.name] ?? '';
  const relationLabel = useRelationLabel(field, value);

  if (RANGE_TYPES.includes(field.fieldType)) {
    const type = field.fieldType === 'NUMBER' ? 'number' : 'date';
    const [low, high] = field.fieldType === 'NUMBER' ? ['Min', 'Max'] : ['From', 'To'];
    return <fieldset className="grid gap-1.5">
      <legend className="mb-1.5 text-xs font-medium text-muted-foreground">{field.label}</legend>
      <div className="grid grid-cols-2 gap-2">
        <DebouncedInput id={id} aria-label={`${field.label} ${low.toLowerCase()}`} type={type} className="h-8" placeholder={low}
          value={filters[`${field.name}__gte`] ?? ''} onCommit={v => set(`${field.name}__gte`, v)} />
        <DebouncedInput aria-label={`${field.label} ${high.toLowerCase()}`} type={type} className="h-8" placeholder={high}
          value={filters[`${field.name}__lte`] ?? ''} onCommit={v => set(`${field.name}__lte`, v)} />
      </div>
    </fieldset>;
  }

  let control;
  if (isRelation(field)) {
    control = <RelationSelect id={id} label={field.label} entity={field.lookupTargetEntity || ''} value={value ? { id: value, label: relationLabel ?? undefined } : null}
      onChange={next => set(field.name, next == null ? '' : String(next.id))} />;
  } else if (field.fieldType === 'ENUM' || field.fieldType === 'BOOLEAN') {
    const options = field.fieldType === 'ENUM' ? (field.enumValues ?? []).map(v => ({ value: v, label: humanize(v) }))
      : [{ value: 'true', label: 'Yes' }, { value: 'false', label: 'No' }];
    control = <Select value={value || ANY} onValueChange={v => set(field.name, v === ANY ? '' : v)}>
      <SelectTrigger id={id} size="sm" className="w-full"><SelectValue /></SelectTrigger>
      <SelectContent>
        <SelectItem value={ANY}>Any</SelectItem>
        {options.map(o => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}
      </SelectContent>
    </Select>;
  } else {
    control = <DebouncedInput id={id} className="h-8" value={value} placeholder="Contains…" onCommit={v => set(field.name, v)} />;
  }
  return <div className="grid gap-1.5">
    <Label htmlFor={id} className="text-xs text-muted-foreground">{field.label}</Label>
    {control}
  </div>;
}

function useRelationLabel(field: FieldMetadata | undefined, id: string) {
  const { getEntity } = useAdmin();
  const [label, setLabel] = useState<string | null>(null);
  useEffect(() => {
    if (!field || !isRelation(field) || !id) { setLabel(null); return; }
    const target = getEntity(field.lookupTargetEntity);
    if (!target) return;
    let active = true;
    getRecord(target.entityName, id).then(r => { if (active) setLabel(recordLabel(target, r)); }).catch(() => undefined);
    return () => { active = false; };
  }, [field?.name, id]);
  return label;
}

function describe(field: FieldMetadata | undefined, op: string, value: string) {
  if (!field) return value;
  const shown = field.fieldType === 'BOOLEAN' ? (value === 'true' ? 'Yes' : 'No')
    : field.fieldType === 'ENUM' ? humanize(value)
    : field.fieldType === 'NUMBER' ? formatNumber(Number(value))
    : field.fieldType === 'DATE' || field.fieldType === 'DATE_TIME' ? formatDate(value, false)
    : value;
  if (op === 'gte') return field.fieldType === 'NUMBER' ? `≥ ${shown}` : `from ${shown}`;
  if (op === 'lte') return field.fieldType === 'NUMBER' ? `≤ ${shown}` : `until ${shown}`;
  if (field.fieldType === 'STRING') return `contains “${shown}”`;
  return shown;
}

export function FilterChip({ fields, filterKey, value, onRemove }: { fields: FieldMetadata[]; filterKey: string; value: string; onRemove: () => void }) {
  const op = filterKey.endsWith('__gte') ? 'gte' : filterKey.endsWith('__lte') ? 'lte' : 'eq';
  const name = op === 'eq' ? filterKey : filterKey.slice(0, -5);
  const field = fields.find(f => f.name === name);
  const relationLabel = useRelationLabel(field, value);
  return <Badge variant="secondary" className="h-8 gap-1 rounded-md pl-2.5 pr-1 font-normal">
    <span className="text-muted-foreground">{field?.label ?? name}:</span>
    <span className="max-w-48 truncate font-medium">{relationLabel ?? describe(field, op, value)}</span>
    <button type="button" aria-label={`Remove ${field?.label ?? name} filter`} onClick={onRemove}
      className="ml-0.5 rounded-sm p-0.5 text-muted-foreground hover:bg-background hover:text-foreground"><X className="size-3.5" /></button>
  </Badge>;
}
