import { lazy, Suspense, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import type { FieldMetadata } from '@/types/metadata';
import { humanize } from '@/lib/format';
import { cn } from '@/lib/utils';
import { RelationSelect } from '@/components/RelationSelect';
import { RelationMultiSelect, type Ref } from '@/components/RelationMultiSelect';
import { FileUpload } from '@/components/FileUpload';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Switch } from '@/components/ui/switch';
import { Textarea } from '@/components/ui/textarea';

const RichTextEditor = lazy(() => import('@/components/editors/RichTextEditor'));
const CodeEditor = lazy(() => import('@/components/editors/CodeEditor'));
const NONE = '__none__';

export const isWideField = (field: FieldMetadata) => ['TEXTAREA', 'RICH_TEXT', 'CODE_EDITOR'].includes(field.widget);
export const isLockedField = (field: FieldMetadata, isEdit: boolean) => field.readOnly || !!field.computed || (isEdit && field.primaryKey);

interface Props {
  field: FieldMetadata;
  value: unknown;
  onChange: (value: unknown) => void;
  id: string;
  isEdit?: boolean;
  invalid?: boolean;
  disabled?: boolean;
  describedBy?: string;
  /** Compact controls for table cells; rich editors fall back to plain inputs. */
  compact?: boolean;
  autoFocus?: boolean;
  /** Entity that owns the field; required for upload fields. */
  entity?: string;
  onBusyChange?: (busy: boolean) => void;
}

export function FieldInput({ field, value, onChange, id, isEdit = false, invalid, disabled, describedBy, compact, autoFocus, entity, onBusyChange }: Props) {
  const [revealed, setRevealed] = useState(false);
  const common = { id, 'aria-invalid': invalid || undefined, 'aria-describedby': describedBy, disabled, autoFocus };
  const size = compact ? 'h-8 text-sm' : undefined;

  if (isLockedField(field, isEdit)) {
    const text = value == null ? '' : typeof value === 'object' ? String((value as { label?: unknown }).label ?? '') : String(value);
    return <Input {...common} value={text} readOnly disabled className={cn('font-mono text-xs', size)} />;
  }
  if (field.upload && entity) {
    return <FileUpload id={id} entity={entity} field={field.name} label={field.label} rules={field.upload} value={String(value ?? '')}
      onChange={onChange} onBusyChange={onBusyChange} invalid={invalid} disabled={disabled} describedBy={describedBy} />;
  }
  if (field.fieldType === 'BOOLEAN' || field.widget === 'BOOLEAN_SWITCH') {
    return <div className={cn('flex items-center gap-3', compact ? 'h-8' : 'h-9')}>
      <Switch {...common} aria-label={field.label} checked={Boolean(value)} onCheckedChange={onChange} />
      {!compact && <span className="text-sm text-muted-foreground">{value ? 'Yes' : 'No'}</span>}
    </div>;
  }
  if (field.fieldType === 'ENUM') {
    const required = field.validation?.required;
    return <Select value={value ? String(value) : NONE} onValueChange={v => onChange(v === NONE ? '' : v)} disabled={disabled}>
      <SelectTrigger {...common} aria-label={field.label} size={compact ? 'sm' : 'default'} className="w-full"><SelectValue placeholder={`Select ${field.label.toLowerCase()}…`} /></SelectTrigger>
      <SelectContent>
        {!required && <SelectItem value={NONE}><span className="text-muted-foreground">None</span></SelectItem>}
        {required && !value && <SelectItem value={NONE} disabled>Select {field.label.toLowerCase()}…</SelectItem>}
        {(field.enumValues ?? []).map(option => <SelectItem key={option} value={option}>{humanize(option)}</SelectItem>)}
      </SelectContent>
    </Select>;
  }
  if (field.fieldType === 'RELATION_MANY_TO_ONE' || field.fieldType === 'RELATION_ONE_TO_ONE') {
    return <RelationSelect id={id} label={field.label} entity={field.lookupTargetEntity || ''} invalid={invalid} disabled={disabled}
      value={value as never} onChange={onChange} />;
  }
  if (field.fieldType === 'RELATION_MANY_TO_MANY') {
    return <RelationMultiSelect id={id} label={field.label} entity={field.lookupTargetEntity || ''} invalid={invalid} disabled={disabled}
      value={Array.isArray(value) ? value as Ref[] : []} onChange={onChange} />;
  }
  if (!compact && (field.widget === 'RICH_TEXT' || field.widget === 'CODE_EDITOR')) {
    const Editor = field.widget === 'RICH_TEXT' ? RichTextEditor : CodeEditor;
    return <Suspense fallback={<Skeleton className="h-40 w-full" />}>
      <Editor id={id} value={String(value ?? '')} onChange={onChange} disabled={disabled} invalid={invalid} describedBy={describedBy} />
    </Suspense>;
  }
  if (!compact && isWideField(field)) {
    return <Textarea {...common} rows={5} value={String(value ?? '')} placeholder={field.placeholder} onChange={e => onChange(e.target.value)} />;
  }
  if (field.sensitive || field.widget === 'PASSWORD') {
    return <div className="relative">
      <Input {...common} type={revealed ? 'text' : 'password'} autoComplete="new-password" value={String(value ?? '')} className={cn('pr-10', size)}
        placeholder={isEdit ? 'Leave blank to keep the current value' : field.placeholder} onChange={e => onChange(e.target.value)} />
      <Button type="button" variant="ghost" size="icon-sm" className={cn('absolute right-0.5 text-muted-foreground', compact ? 'top-0 size-8' : 'top-0.5')}
        aria-label={revealed ? 'Hide value' : 'Show value'} onClick={() => setRevealed(!revealed)}>
        {revealed ? <EyeOff /> : <Eye />}
      </Button>
    </div>;
  }
  if (field.fieldType === 'DATE') {
    return <Input {...common} type="date" className={size} value={value ? String(value).slice(0, 10) : ''} onChange={e => onChange(e.target.value)} />;
  }
  if (field.fieldType === 'DATE_TIME') {
    return <Input {...common} type="datetime-local" step="1" className={size}
      value={value ? String(value).replace(/Z$|[+-]\d{2}:\d{2}$/, '').slice(0, 19) : ''} onChange={e => onChange(e.target.value)} />;
  }
  if (field.widget === 'COLOR_PICKER') {
    return <div className="flex gap-2">
      <Input type="color" aria-label={`${field.label} color`} value={/^#[0-9a-f]{6}$/i.test(String(value)) ? String(value) : '#000000'}
        onChange={e => onChange(e.target.value)} className={cn('w-12 p-1', size)} disabled={disabled} />
      <Input {...common} value={String(value ?? '')} onChange={e => onChange(e.target.value)} className={cn('font-mono', size)} />
    </div>;
  }
  return <Input {...common} className={size}
    type={field.fieldType === 'NUMBER' ? 'number' : field.validation?.email ? 'email' : field.widget === 'IMAGE_URL' ? 'url' : 'text'}
    step={field.fieldType === 'NUMBER' ? 'any' : undefined} value={String(value ?? '')} placeholder={field.placeholder}
    onChange={e => onChange(e.target.value)} />;
}
