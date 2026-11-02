import { Plus, Trash2 } from 'lucide-react';
import type { EntityMetadata, FieldMetadata, InlineMetadata } from '@/types/metadata';
import { FieldInput } from '@/components/FieldInput';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { isWideField } from '@/components/FieldInput';

type Row = Record<string, unknown>;
interface Props {
  inline: InlineMetadata;
  child: EntityMetadata;
  fields: FieldMetadata[];
  rows: Row[];
  errors: Record<string, string>;
  disabled?: boolean;
  onChange: (rows: Row[]) => void;
  onBusyChange?: (key: string, busy: boolean) => void;
}

export const blankRow = (fields: FieldMetadata[]): Row =>
  Object.fromEntries(fields.map(f => [f.name, f.fieldType === 'BOOLEAN' ? false : '']));

/** Editable child rows saved together with the parent record. */
export function InlineEditor({ inline, child, fields, rows, errors, disabled, onChange, onBusyChange }: Props) {
  const pk = child.primaryKeyField;
  const atLimit = inline.limit > 0 && rows.length >= inline.limit;
  const update = (index: number, name: string, value: unknown) =>
    onChange(rows.map((row, i) => (i === index ? { ...row, [name]: value } : row)));
  const tableError = errors[inline.propertyName];
  const rowKey = (row: Row, index: number) => (row[pk] == null || row[pk] === '' ? `new-${index}` : String(row[pk]));
  const removable = (row: Row) => row[pk] == null || row[pk] === '' || inline.canDelete;
  const removeButton = (row: Row, index: number, className?: string) =>
    <Button type="button" variant="ghost" size="icon-sm" disabled={disabled || !removable(row)}
      aria-label={`Remove row ${index + 1}`} title={removable(row) ? 'Remove row' : 'Rows cannot be removed'}
      className={cn('text-muted-foreground hover:text-destructive', !removable(row) && 'invisible', className)}
      onClick={() => onChange(rows.filter((_, i) => i !== index))}>
      <Trash2 />
    </Button>;
  const input = (row: Row, index: number, field: FieldMetadata, compact: boolean) => {
    const path = `${inline.propertyName}[${index}].${field.name}`;
    const id = `field-${path}`;
    return <>
      <FieldInput compact={compact} field={field} id={id} value={row[field.name]} onChange={v => update(index, field.name, v)}
        entity={child.entityName} onBusyChange={busy => onBusyChange?.(path, busy)}
        invalid={!!errors[path]} disabled={disabled} describedBy={errors[path] ? `${id}-error` : undefined} />
      {errors[path] && <p id={`${id}-error`} className="mt-1 text-xs text-destructive">{errors[path]}</p>}
    </>;
  };

  return <Card className="gap-0 py-0">
    <CardHeader className="border-b py-4 [.border-b]:pb-4">
      <CardTitle>{inline.title}</CardTitle>
      <CardDescription>{rows.length} {rows.length === 1 ? 'row' : 'rows'}{inline.limit > 0 ? ` · up to ${inline.limit}` : ''} · saved with this record</CardDescription>
      {inline.canAdd && <CardAction>
        <Button type="button" variant="outline" size="sm" disabled={disabled || atLimit} onClick={() => onChange([...rows, blankRow(fields)])}>
          <Plus />Add row
        </Button>
      </CardAction>}
    </CardHeader>
    <CardContent className="px-0">
      {tableError && <p role="alert" className="px-6 pt-3 text-sm text-destructive">{tableError}</p>}
      {rows.length === 0
        ? <p className="px-6 py-8 text-center text-sm text-muted-foreground">No {inline.title.toLowerCase()} yet.{inline.canAdd && ' Use "Add row" to create one.'}</p>
        : inline.type === 'STACKED'
        ? <div className="divide-y">
          {rows.map((row, index) => <section key={rowKey(row, index)} aria-label={`${inline.title} ${index + 1}`} className="px-6 py-4">
            <div className="mb-3 flex items-center justify-between">
              <h4 className="text-xs font-medium text-muted-foreground">#{index + 1}</h4>
              {removeButton(row, index, '-mr-2')}
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              {fields.map(field => <div key={field.name} className={cn('grid gap-1.5', (isWideField(field) || field.upload) && 'sm:col-span-2')}>
                <label htmlFor={`field-${inline.propertyName}[${index}].${field.name}`} className="text-sm font-medium">
                  {field.label}{field.validation?.required && <span className="text-destructive" aria-hidden> *</span>}
                </label>
                {input(row, index, field, false)}
              </div>)}
            </div>
          </section>)}
        </div>
        : <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead className="w-10 pl-6 text-xs text-muted-foreground">#</TableHead>
              {fields.map(f => <TableHead key={f.name} className={`${f.upload ? 'min-w-72' : 'min-w-40'} text-xs font-medium text-muted-foreground`}>
                {f.label}{f.validation?.required && <span className="text-destructive" aria-hidden> *</span>}
              </TableHead>)}
              <TableHead className="w-12 pr-6"><span className="sr-only">Remove</span></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row, index) => <TableRow key={rowKey(row, index)} className="align-top hover:bg-transparent">
              <TableCell className="pl-6 pt-4 text-xs tabular text-muted-foreground">{index + 1}</TableCell>
              {fields.map(field => <TableCell key={field.name} className="whitespace-normal">{input(row, index, field, true)}</TableCell>)}
              <TableCell className="pr-6">{removeButton(row, index)}</TableCell>
            </TableRow>)}
          </TableBody>
        </Table>}
    </CardContent>
  </Card>;
}
