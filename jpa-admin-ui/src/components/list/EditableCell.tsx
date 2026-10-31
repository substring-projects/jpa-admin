import { useState, type FormEvent, type ReactNode } from 'react';
import { Loader2, Pencil } from 'lucide-react';
import { toast } from 'sonner';
import { ApiError, updateRecord } from '@/api/client';
import { FieldInput, isLockedField } from '@/components/FieldInput';
import { validateField } from '@/lib/validation';
import type { EntityMetadata, FieldMetadata } from '@/types/metadata';
import { Button } from '@/components/ui/button';
import { Popover, PopoverAnchor, PopoverContent } from '@/components/ui/popover';

const INLINE_EDITABLE = ['STRING', 'NUMBER', 'BOOLEAN', 'ENUM', 'DATE', 'DATE_TIME', 'RELATION_MANY_TO_ONE', 'RELATION_ONE_TO_ONE'];

export const canEditInTable = (entity: EntityMetadata, field: FieldMetadata) =>
  entity.updatable && !entity.readOnly && field.inForm && !isLockedField(field, true) && !field.sensitive
  && INLINE_EDITABLE.includes(field.fieldType) && !['RICH_TEXT', 'CODE_EDITOR', 'TEXTAREA', 'PASSWORD'].includes(field.widget);

interface Props {
  entity: EntityMetadata;
  field: FieldMetadata;
  record: Record<string, unknown>;
  onSaved: (record: Record<string, unknown>) => void;
  children: ReactNode;
}

/** Shows the value with a pencil on hover; editing opens a small popover so row height and layout never shift. */
export function EditableCell({ entity, field, record, onSaved, children }: Props) {
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState<unknown>(record[field.name]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const id = record[entity.primaryKeyField];
  const inputId = `cell-${field.name}-${String(id)}`;

  const begin = () => { setValue(record[field.name] ?? (field.fieldType === 'BOOLEAN' ? false : '')); setError(''); setOpen(true); };
  const save = async (event: FormEvent) => {
    event.preventDefault();
    const message = validateField(field, value, true);
    if (message) { setError(message); return; }
    setSaving(true);
    try {
      const updated = await updateRecord(entity.entityName, id, { [field.name]: value === '' ? null : value });
      onSaved(updated);
      setOpen(false);
      toast.success(`${field.label} updated`);
    } catch (err) {
      setError(err instanceof ApiError ? err.fieldErrors[field.name] ?? err.message : 'Unable to save');
    } finally {
      setSaving(false);
    }
  };

  return <Popover open={open} onOpenChange={next => { if (!saving) setOpen(next); }}>
    <PopoverAnchor asChild>
      <div className="group/edit relative flex min-w-0 items-center gap-1">
        <div className="min-w-0">{children}</div>
        <button type="button" aria-label={`Edit ${field.label}`} onClick={e => { e.stopPropagation(); begin(); }}
          className="ml-auto inline-flex size-6 shrink-0 items-center justify-center rounded text-muted-foreground opacity-0 transition hover:bg-accent hover:text-foreground focus-visible:opacity-100 group-hover/edit:opacity-100">
          <Pencil className="size-3.5" />
        </button>
      </div>
    </PopoverAnchor>
    <PopoverContent align="start" className="w-72 p-3" onClick={e => e.stopPropagation()}>
      <form onSubmit={save} noValidate className="grid gap-2">
        <label htmlFor={inputId} className="text-xs font-medium text-muted-foreground">{field.label}</label>
        <FieldInput compact autoFocus field={field} id={inputId} value={value} onChange={v => { setValue(v); setError(''); }} isEdit
          invalid={!!error} disabled={saving} describedBy={error ? `${inputId}-error` : undefined} />
        {error && <p id={`${inputId}-error`} className="text-xs text-destructive">{error}</p>}
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="ghost" size="sm" onClick={() => setOpen(false)} disabled={saving}>Cancel</Button>
          <Button type="submit" size="sm" disabled={saving}>{saving && <Loader2 className="animate-spin" />}Save</Button>
        </div>
      </form>
    </PopoverContent>
  </Popover>;
}
