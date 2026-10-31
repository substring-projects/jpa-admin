import { useEffect, useState } from 'react';
import { Check, ChevronsUpDown, Loader2, X } from 'lucide-react';
import { lookupRecords } from '@/api/client';
import { isUuid, shortId } from '@/lib/format';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from '@/components/ui/command';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';

type RelationValue = { id: string | number; label?: string } | string | number | null;
interface Props {
  id: string;
  label: string;
  entity: string;
  value: RelationValue;
  invalid?: boolean;
  disabled?: boolean;
  onChange: (value: { id: string | number; label?: string } | null) => void;
}

const display = (label: string) => (isUuid(label) ? shortId(label) : label);

export function RelationSelect({ id, label, entity, value, invalid, disabled, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState<Array<{ id: string | number; label: string }>>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const selectedId = typeof value === 'object' && value !== null ? value.id : value;
  const selectedLabel = typeof value === 'object' && value?.label ? value.label
    : options.find(o => String(o.id) === String(selectedId))?.label ?? (selectedId != null && selectedId !== '' ? `#${selectedId}` : '');

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

  const hasValue = selectedId != null && selectedId !== '';
  return <Popover open={open} onOpenChange={setOpen}>
    <div className="relative">
      <PopoverTrigger asChild>
        <Button id={id} type="button" variant="outline" role="combobox" aria-expanded={open} aria-invalid={invalid || undefined} disabled={disabled}
          className={cn('w-full justify-between px-3 font-normal', !hasValue && 'text-muted-foreground')}>
          <span className={cn('truncate', hasValue && !disabled && 'mr-5')}>{hasValue ? display(selectedLabel) : `Select ${label.toLowerCase()}…`}</span>
          <ChevronsUpDown className="opacity-50" />
        </Button>
      </PopoverTrigger>
      {hasValue && !disabled && <button type="button" aria-label={`Clear ${label}`} onClick={() => onChange(null)}
        className="absolute right-8 top-1/2 -translate-y-1/2 rounded p-0.5 text-muted-foreground hover:text-foreground"><X className="size-3.5" /></button>}
    </div>
    <PopoverContent className="w-(--radix-popover-trigger-width) min-w-64 p-0" align="start">
      <Command shouldFilter={false}>
        <CommandInput placeholder={`Search ${label.toLowerCase()}…`} value={query} onValueChange={setQuery} />
        <CommandList>
          {loading && <div className="flex items-center justify-center gap-2 py-6 text-sm text-muted-foreground"><Loader2 className="size-4 animate-spin" />Searching…</div>}
          {!loading && error && <div role="alert" className="px-3 py-6 text-center text-sm text-destructive">{error}</div>}
          {!loading && !error && <CommandEmpty>No results found.</CommandEmpty>}
          {!loading && !error && <CommandGroup>
            {options.map(option => {
              const selected = String(option.id) === String(selectedId);
              return <CommandItem key={String(option.id)} value={String(option.id)} onSelect={() => { onChange({ id: option.id, label: option.label }); setOpen(false); }}>
                <Check className={cn('size-4', selected ? 'opacity-100' : 'opacity-0')} />
                <span className="truncate">{display(option.label)}</span>
                {option.label !== String(option.id) && <span className="ml-auto font-mono text-xs text-muted-foreground">{shortId(option.id)}</span>}
              </CommandItem>;
            })}
          </CommandGroup>}
        </CommandList>
      </Command>
    </PopoverContent>
  </Popover>;
}
