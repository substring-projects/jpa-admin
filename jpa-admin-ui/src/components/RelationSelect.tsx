import { useEffect, useState } from 'react';
import { lookupRecords } from '../api/client';
import { Loader2 } from 'lucide-react';
import { Input } from './ui/input';
import { FieldSelect } from './ui/field-select';

interface Props {
  id: string;
  label: string;
  entity: string;
  value: { id: string | number; label?: string } | string | number | null;
  required?: boolean;
  onChange: (value: { id: string } | null) => void;
}

export function RelationSelect({ id, label, entity, value, required, onChange }: Props) {
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState<Array<{ id: string | number; label: string }>>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const selectedId = typeof value === 'object' && value !== null ? value.id : value;
  useEffect(() => {
    let active = true;
    setLoading(true);
    const timer = window.setTimeout(async () => {
      setLoading(true);
      setError('');
      try {
        const results = await lookupRecords(entity, query);
        if (active) setOptions(results);
      } catch (err) {
        if (active) setError(err instanceof Error ? err.message : 'Lookup failed');
      } finally {
        if (active) setLoading(false);
      }
    }, 200);
    return () => { active = false; window.clearTimeout(timer); };
  }, [entity, query]);
  const choices = options.map(item => ({ value: String(item.id), label: item.label }));
  if (selectedId != null && selectedId !== '' && !choices.some(item => item.value === String(selectedId))) {
    choices.unshift({ value: String(selectedId), label: typeof value === 'object' && value?.label ? value.label : `#${selectedId}` });
  }
  return <div className="space-y-2">
    <Input aria-label={`Search ${label}`} placeholder={`Search ${label}…`} value={query} onChange={event => setQuery(event.target.value)} />
    <FieldSelect id={id} label={label} value={String(selectedId ?? '')} required={required}
      onChange={next => onChange(next === '' ? null : { id: next })} options={choices} placeholder={`Choose ${label}…`} />
    {loading && <p role="status" className="flex items-center gap-2 text-xs text-muted-foreground"><Loader2 className="h-3 w-3 animate-spin" />Searching…</p>}
    {error && <p role="alert" className="text-xs text-destructive">{error}</p>}
  </div>;
}
