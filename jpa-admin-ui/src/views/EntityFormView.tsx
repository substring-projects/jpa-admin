import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useBlocker, useLocation, useNavigate, useParams, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { AlertCircle, Loader2 } from 'lucide-react';
import { ApiError, createRecord, getRecord, updateRecord } from '@/api/client';
import { useAdmin, useEntity } from '@/context/AdminContext';
import { EntityIcon } from '@/components/DynamicIcon';
import { FieldInput, isLockedField, isWideField } from '@/components/FieldInput';
import { InlineEditor } from '@/components/InlineEditor';
import { ErrorState, NotFound, PageHeader } from '@/components/PageStates';
import { useLayout } from '@/components/layout/AdminLayout';
import { buildFormPayload, buildInlinePayload, editableFormFields, inlineFields, isEditableInline } from '@/lib/form-data';
import { recordLabel } from '@/lib/format';
import { validateField } from '@/lib/validation';
import { cn } from '@/lib/utils';
import type { EntityMetadata, FieldMetadata, InlineMetadata } from '@/types/metadata';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';

type Row = Record<string, unknown>;
interface EditableInline { inline: InlineMetadata; child: EntityMetadata; fields: FieldMetadata[] }

export function EntityFormView() {
  const { entity: entityParam, id } = useParams();
  const entity = useEntity(entityParam);
  const [params] = useSearchParams();
  const copyFrom = id == null ? params.get('copy') : null;
  if (!entity) return <NotFound message={`No entity named "${entityParam}" is registered.`} />;
  if (id != null && (!entity.updatable || entity.readOnly)) return <NotFound message={`${entity.pluralName || entity.displayName} can't be edited.`} />;
  if (id == null && (!entity.creatable || entity.readOnly)) return <NotFound message={`New ${entity.pluralName || entity.displayName} can't be created here.`} />;
  return <EntityForm key={`${entity.entityName}:${id ?? 'new'}:${copyFrom ?? ''}`} entity={entity} id={id} copyFrom={copyFrom} />;
}

const emptyValue = (field: FieldMetadata) => (field.fieldType === 'BOOLEAN' ? false : field.fieldType === 'RELATION_MANY_TO_MANY' ? [] : '');

function initialValues(fields: FieldMetadata[]): Row {
  return Object.fromEntries(fields.map(f => [f.name, emptyValue(f)]));
}

function fromRecord(fields: FieldMetadata[], record: Row, duplicate: boolean): Row {
  const values = initialValues(fields);
  fields.forEach(field => {
    if (field.sensitive || (duplicate && field.primaryKey)) return;
    const value = record[field.name];
    if (value !== undefined && value !== null) values[field.name] = value;
  });
  if (duplicate) {
    const labelField = fields.find(f => ['name', 'title', 'label'].includes(f.name) && typeof values[f.name] === 'string' && values[f.name]);
    if (labelField) values[labelField.name] = `${values[labelField.name]} (copy)`;
  }
  return values;
}

function rowsFromRecord(editable: EditableInline[], record: Row, duplicate: boolean): Record<string, Row[]> {
  return Object.fromEntries(editable.map(({ inline, child, fields }) => {
    const source = Array.isArray(record[inline.propertyName]) ? record[inline.propertyName] as Row[] : [];
    return [inline.propertyName, source.map(item => {
      const row = fromRecord(fields, item, false);
      if (!duplicate) row[child.primaryKeyField] = item[child.primaryKeyField];
      return row;
    })];
  }));
}

function EntityForm({ entity, id, copyFrom }: { entity: EntityMetadata; id?: string; copyFrom: string | null }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { refreshCounts, getEntity } = useAdmin();
  const { setRecordLabel } = useLayout();
  const isEdit = id != null;
  const fields = editableFormFields(entity);
  const editable: EditableInline[] = entity.inlines.filter(isEditableInline).flatMap(inline => {
    const child = getEntity(inline.targetEntity);
    return child ? [{ inline, child, fields: inlineFields(inline, child) }] : [];
  });
  const readOnlyInlines = entity.inlines.filter(i => !editable.some(e => e.inline.propertyName === i.propertyName));
  const sourceId = id ?? copyFrom;

  const blankRows = () => Object.fromEntries(editable.map(e => [e.inline.propertyName, [] as Row[]]));
  const [values, setValues] = useState<Row>(() => initialValues(fields));
  const [rows, setRows] = useState<Record<string, Row[]>>(blankRows);
  const [initial, setInitial] = useState(() => JSON.stringify([initialValues(fields), blankRows()]));
  const [loading, setLoading] = useState(sourceId != null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const saved = useRef(false);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (sourceId == null) return;
    let active = true;
    getRecord(entity.entityName, sourceId)
      .then(record => {
        if (!active) return;
        const loaded = fromRecord(fields, record, !isEdit);
        const loadedRows = rowsFromRecord(editable, record, !isEdit);
        setValues(loaded);
        setRows(loadedRows);
        setInitial(JSON.stringify([loaded, loadedRows]));
        if (isEdit) setRecordLabel(recordLabel(entity, record));
      })
      .catch(err => { if (active) setLoadError(err instanceof Error ? err.message : 'Failed to load record'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [entity.entityName, sourceId]);

  const dirty = !loading && JSON.stringify([values, rows]) !== initial;
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    dirty && !saved.current && currentLocation.pathname !== nextLocation.pathname);

  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [dirty]);

  const clearErrors = (prefix: string) => setErrors(prev => {
    const keys = Object.keys(prev).filter(k => k === prefix || k.startsWith(`${prefix}[`) || k.startsWith(`${prefix}.`));
    if (keys.length === 0) return prev;
    const next = { ...prev };
    keys.forEach(k => delete next[k]);
    return next;
  });
  const setValue = (name: string, value: unknown) => { setValues(prev => ({ ...prev, [name]: value })); clearErrors(name); };
  const setInlineRows = (prop: string, next: Row[]) => { setRows(prev => ({ ...prev, [prop]: next })); clearErrors(prop); };

  const backTo = isEdit ? `/${entity.entityName}/${encodeURIComponent(id!)}` : `/${entity.entityName}`;
  const cancel = () => (location.key !== 'default' ? navigate(-1) : navigate(backTo));

  const focusFirstError = (found: Record<string, string>) => {
    const key = Object.keys(found)[0];
    if (!key) return;
    window.requestAnimationFrame(() => {
      const el = formRef.current?.querySelector<HTMLElement>(`[id="field-${CSS.escape(key)}"]`);
      if (el) { el.focus(); el.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
      else window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (saving || loading || loadError) return;
    const found: Record<string, string> = {};
    fields.forEach(field => {
      if (isLockedField(field, isEdit)) return;
      const message = validateField(field, values[field.name], isEdit);
      if (message) found[field.name] = message;
    });
    editable.forEach(({ inline, fields: childFields }) => (rows[inline.propertyName] ?? []).forEach((row, index) => childFields.forEach(field => {
      const message = validateField(field, row[field.name], false);
      if (message) found[`${inline.propertyName}[${index}].${field.name}`] = message;
    })));
    setErrors(found);
    setServerError(null);
    if (Object.keys(found).length) { focusFirstError(found); return; }

    setSaving(true);
    try {
      const payload: Row = buildFormPayload(fields, values, isEdit);
      editable.forEach(({ inline, child, fields: childFields }) => {
        payload[inline.propertyName] = buildInlinePayload(childFields, child.primaryKeyField, rows[inline.propertyName] ?? []);
      });
      const result = isEdit ? await updateRecord(entity.entityName, id, payload) : await createRecord(entity.entityName, payload);
      saved.current = true;
      refreshCounts();
      toast.success(isEdit ? `${entity.displayName} saved` : `${entity.displayName} created`);
      const savedId = result?.[entity.primaryKeyField] ?? id;
      navigate(savedId != null ? `/${entity.entityName}/${encodeURIComponent(String(savedId))}` : `/${entity.entityName}`, { replace: true });
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) {
        setErrors(err.fieldErrors);
        focusFirstError(err.fieldErrors);
      } else {
        setServerError(err instanceof Error ? err.message : 'Unable to save');
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    } finally {
      setSaving(false);
    }
  };

  if (loadError) return <ErrorState title="Couldn't load this record" description={loadError} onRetry={() => navigate(0)} />;

  const title = isEdit ? `Edit ${entity.displayName.toLowerCase()}` : copyFrom ? `Duplicate ${entity.displayName.toLowerCase()}` : `New ${entity.displayName.toLowerCase()}`;
  const errorCount = Object.keys(errors).length;
  const knownPaths = new Set([...fields.map(f => f.name), ...editable.map(e => e.inline.propertyName)]);
  const unplacedErrors = Object.entries(errors).filter(([key]) => !knownPaths.has(key.split(/[[.]/)[0]));

  return <div className="mx-auto w-full max-w-4xl p-4 pb-0 sm:p-6 sm:pb-0 lg:p-8 lg:pb-0">
    <PageHeader icon={<EntityIcon entity={entity} />} title={title.charAt(0).toUpperCase() + title.slice(1)}
      description={isEdit ? 'Update the fields below and save your changes.' : copyFrom ? 'Review the copied values before creating the new record.' : `Fill in the details to create a new ${entity.displayName.toLowerCase()}.`} />

    <form ref={formRef} noValidate onSubmit={submit} aria-busy={saving} className="mt-6 space-y-6">
      {(serverError || errorCount > 0) && <Alert variant="destructive">
        <AlertCircle />
        <AlertTitle>{serverError ? 'The record could not be saved' : `Please fix ${errorCount} ${errorCount === 1 ? 'field' : 'fields'}`}</AlertTitle>
        {(serverError || unplacedErrors.length > 0) && <AlertDescription>
          {serverError}
          {unplacedErrors.map(([key, message]) => <span key={key} className="block">{key}: {message}</span>)}
        </AlertDescription>}
      </Alert>}

      <Card>
        <CardHeader>
          <CardTitle>Details</CardTitle>
          <CardDescription>Fields marked with <span className="text-destructive">*</span> are required.</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? <div className="grid gap-6 sm:grid-cols-2">{Array.from({ length: Math.min(fields.length, 6) || 4 }, (_, i) => <div key={i} className="space-y-2"><Skeleton className="h-4 w-24" /><Skeleton className="h-9 w-full" /></div>)}</div>
            : <div className="grid gap-x-6 gap-y-5 sm:grid-cols-2">
              {fields.map(field => {
                const inputId = `field-${field.name}`;
                const error = errors[field.name];
                return <div key={field.name} className={cn('grid content-start gap-2', (isWideField(field) || field.fieldType === 'RELATION_MANY_TO_MANY') && 'sm:col-span-2')}>
                  <Label htmlFor={inputId} className={cn(error && 'text-destructive')}>
                    {field.label}{field.validation?.required && !(isEdit && field.sensitive) && <span className="text-destructive" aria-hidden>*</span>}
                  </Label>
                  <FieldInput field={field} id={inputId} value={values[field.name]} onChange={v => setValue(field.name, v)} isEdit={isEdit}
                    invalid={!!error} disabled={saving} describedBy={error ? `${inputId}-error` : field.description ? `${inputId}-hint` : undefined} />
                  {error
                    ? <p id={`${inputId}-error`} className="text-xs text-destructive">{error}</p>
                    : field.description && <p id={`${inputId}-hint`} className="text-xs text-muted-foreground">{field.description}</p>}
                </div>;
              })}
              {fields.length === 0 && <p className="text-sm text-muted-foreground">This entity has no editable fields.</p>}
            </div>}
        </CardContent>
      </Card>

      {!loading && editable.map(({ inline, child, fields: childFields }) => <InlineEditor key={inline.propertyName} inline={inline} child={child}
        fields={childFields} rows={rows[inline.propertyName] ?? []} errors={errors} disabled={saving}
        onChange={next => setInlineRows(inline.propertyName, next)} />)}

      {readOnlyInlines.length > 0 && isEdit && <p className="text-sm text-muted-foreground">
        Related {readOnlyInlines.map(i => i.title.toLowerCase()).join(', ')} are shown on the <Link to={backTo} className="font-medium text-foreground underline underline-offset-4">record page</Link>.
      </p>}

      <div className="sticky bottom-0 z-10 flex items-center justify-end gap-2 border-t bg-background/95 py-3 backdrop-blur">
        {dirty && <span className="mr-auto flex items-center gap-2 text-sm text-muted-foreground"><span className="size-2 rounded-full bg-amber-500" />Unsaved changes</span>}
        <Button type="button" variant="outline" onClick={cancel} disabled={saving}>Cancel</Button>
        <Button type="submit" disabled={saving || loading}>
          {saving && <Loader2 className="animate-spin" />}
          {saving ? 'Saving…' : isEdit ? 'Save changes' : `Create ${entity.displayName.toLowerCase()}`}
        </Button>
      </div>
    </form>

    <AlertDialog open={blocker.state === 'blocked'} onOpenChange={open => { if (!open && blocker.state === 'blocked') blocker.reset(); }}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Discard unsaved changes?</AlertDialogTitle>
          <AlertDialogDescription>You have changes that haven't been saved. If you leave now, they will be lost.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={() => blocker.reset?.()}>Keep editing</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={() => blocker.proceed?.()}>Discard changes</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>;
}
