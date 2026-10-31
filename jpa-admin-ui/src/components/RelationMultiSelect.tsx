import { useEffect, useState } from 'react';
import { Check, ChevronsUpDown, Loader2, X } from 'lucide-react';
import { lookupRecords } from '@/api/client';
import { isUuid, shortId } from '@/lib/format';
import { cn } from '@/lib/utils';
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from '@/components/ui/command';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';

export type Ref = { id: string | number; label?: string };
interface Props {
  id: string;
  label: string;
  entity: string;
  value: Ref[];
  invalid?: boolean;
  disabled?: boolean;
  onChange: (value: Ref[]) => void;
}

const display = (ref: Ref) => { const text = ref.label ?? String(ref.id); return isUuid(text) ? shortId(text) : text; };

export function RelationMultiSelect({ id, label, entity, value, invalid, disabled, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState<Ref[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const selected = new Set(value.map(v => String(v.id)));

  useEffect(() => {
    if (!open) return;
    let active = true;
    setLoading(true);
    const timer = window.setTimeout(() => {
      lookupRecords(entity, query)
        .then(results => { if (active) { setOptions(results); setError(''); } })
        .catch(err => { if (active) setError(err instanceof Error ? err.message : 'Lookup failed'); })
        .finally(() => { if (active) setLoading(false); });
    }, 200);
    return () => { active = false; window.clearTimeout(timer); };
  }, [entity, query, open]);

  const toggle = (option: Ref) => onChange(selected.has(String(option.id))
    ? value.filter(v => String(v.id) !== String(option.id))
    : [...value, { id: option.id, label: option.label }]);

  return <Popover open={open} onOpenChange={setOpen}>
    <PopoverTrigger asChild>
      <button id={id} type="button" role="combobox" aria-expanded={open} aria-invalid={invalid || undefined} disabled={disabled}
        className={cn('flex min-h-9 w-full items-center gap-1.5 rounded-md border bg-transparent px-2 py-1 text-left text-sm shadow-xs outline-none',
          'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:opacity-50 dark:bg-input/30',
          'aria-invalid:border-destructive aria-invalid:ring-destructive/20')}>
        <span className="flex flex-1 flex-wrap gap-1">
          {value.length === 0 && <span className="px-1 text-muted-foreground">Select {label.toLowerCase()}…</span>}
          {value.map(ref => <span key={String(ref.id)} className="inline-flex items-center gap-1 rounded-md bg-secondary px-1.5 py-0.5 text-xs font-medium">
            {display(ref)}
            {!disabled && <span role="button" tabIndex={-1} aria-label={`Remove ${display(ref)}`}
              onPointerDown={e => { e.preventDefault(); e.stopPropagation(); toggle(ref); }}
              className="rounded-sm text-muted-foreground hover:text-foreground"><X className="size-3" /></span>}
          </span>)}
        </span>
        <ChevronsUpDown className="size-4 shrink-0 opacity-50" />
      </button>
    </PopoverTrigger>
    <PopoverContent className="w-(--radix-popover-trigger-width) min-w-64 p-0" align="start">
      <Command shouldFilter={false}>
        <CommandInput placeholder={`Search ${label.toLowerCase()}…`} value={query} onValueChange={setQuery} />
        <CommandList>
          {loading && <div className="flex items-center justify-center gap-2 py-6 text-sm text-muted-foreground"><Loader2 className="size-4 animate-spin" />Searching…</div>}
          {!loading && error && <div role="alert" className="px-3 py-6 text-center text-sm text-destructive">{error}</div>}
          {!loading && !error && <CommandEmpty>No results found.</CommandEmpty>}
          {!loading && !error && <CommandGroup>
            {options.map(option => <CommandItem key={String(option.id)} value={String(option.id)} onSelect={() => toggle(option)}>
              <span className={cn('flex size-4 items-center justify-center rounded-sm border', selected.has(String(option.id)) && 'border-primary bg-primary text-primary-foreground')}>
                {selected.has(String(option.id)) && <Check className="size-3" />}
              </span>
              <span className="truncate">{display(option)}</span>
            </CommandItem>)}
          </CommandGroup>}
        </CommandList>
        {value.length > 0 && <div className="border-t p-1">
          <button type="button" onClick={() => onChange([])} className="w-full rounded-sm px-2 py-1.5 text-center text-sm hover:bg-accent">Clear selection</button>
        </div>}
      </Command>
    </PopoverContent>
  </Popover>;
}
