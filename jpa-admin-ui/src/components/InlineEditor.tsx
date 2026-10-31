import { Plus, Trash2 } from 'lucide-react';
import type { EntityMetadata, FieldMetadata, InlineMetadata } from '@/types/metadata';
import { FieldInput } from '@/components/FieldInput';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

type Row = Record<string, unknown>;
interface Props {
  inline: InlineMetadata;
  child: EntityMetadata;
  fields: FieldMetadata[];
  rows: Row[];
  errors: Record<string, string>;
  disabled?: boolean;
  onChange: (rows: Row[]) => void;
}

export const blankRow = (fields: FieldMetadata[]): Row =>
  Object.fromEntries(fields.map(f => [f.name, f.fieldType === 'BOOLEAN' ? false : '']));

/** Editable child rows saved together with the parent record. */
export function InlineEditor({ inline, child, fields, rows, errors, disabled, onChange }: Props) {
  const pk = child.primaryKeyField;
  const atLimit = inline.limit > 0 && rows.length >= inline.limit;
  const update = (index: number, name: string, value: unknown) =>
    onChange(rows.map((row, i) => (i === index ? { ...row, [name]: value } : row)));
  const tableError = errors[inline.propertyName];

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
        : <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead className="w-10 pl-6 text-xs text-muted-foreground">#</TableHead>
              {fields.map(f => <TableHead key={f.name} className="min-w-40 text-xs font-medium text-muted-foreground">
                {f.label}{f.validation?.required && <span className="text-destructive" aria-hidden> *</span>}
              </TableHead>)}
              <TableHead className="w-12 pr-6"><span className="sr-only">Remove</span></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row, index) => {
              const isNew = row[pk] == null || row[pk] === '';
              const removable = isNew || inline.canDelete;
              return <TableRow key={isNew ? `new-${index}` : String(row[pk])} className="align-top hover:bg-transparent">
                <TableCell className="pl-6 pt-4 text-xs tabular text-muted-foreground">{index + 1}</TableCell>
                {fields.map(field => {
                  const path = `${inline.propertyName}[${index}].${field.name}`;
                  const id = `field-${path}`;
                  return <TableCell key={field.name} className="whitespace-normal">
                    <FieldInput compact field={field} id={id} value={row[field.name]} onChange={v => update(index, field.name, v)}
                      invalid={!!errors[path]} disabled={disabled} describedBy={errors[path] ? `${id}-error` : undefined} />
                    {errors[path] && <p id={`${id}-error`} className="mt-1 text-xs text-destructive">{errors[path]}</p>}
                  </TableCell>;
                })}
                <TableCell className="pr-6">
                  <Button type="button" variant="ghost" size="icon-sm" disabled={disabled || !removable}
                    aria-label={`Remove row ${index + 1}`} title={removable ? 'Remove row' : 'Rows cannot be removed'}
                    className={cn('text-muted-foreground hover:text-destructive', !removable && 'invisible')}
                    onClick={() => onChange(rows.filter((_, i) => i !== index))}>
                    <Trash2 />
                  </Button>
                </TableCell>
              </TableRow>;
            })}
          </TableBody>
        </Table>}
    </CardContent>
  </Card>;
}
