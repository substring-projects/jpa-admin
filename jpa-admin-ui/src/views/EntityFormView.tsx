import React, { useState, useEffect } from 'react';
import { FormSkeleton } from '../components/LoadingSkeletons';
import { useAdmin } from '../context/AdminContext';
import { getRecord, createRecord, updateRecord } from '../api/client';
import { buildFormPayload, editableFormFields } from '../lib/form-data';
import { RelationSelect } from '../components/RelationSelect';
import { FieldSelect } from '../components/ui/field-select';
import { FieldMetadata } from '../types/metadata';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Switch } from '../components/ui/switch';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/card';
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from '../components/ui/table';
import { ArrowLeft, Loader2, Check, Eye, EyeOff } from 'lucide-react';
import { cn } from '../lib/utils';

export const EntityFormView: React.FC = () => {
  const { currentEntity, currentView, editingId, navigateToList, addToast } = useAdmin();

  if (!currentEntity) return null;

  const isEdit = currentView === 'edit';
  const formFields = editableFormFields(currentEntity);

  const [formData, setFormData] = useState<Record<string, any>>({});
  const [inlineData, setInlineData] = useState<Record<string, any[]>>({});
  const [loading, setLoading] = useState(isEdit);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [showPassword, setShowPassword] = useState<Record<string, boolean>>({});

  useEffect(() => {
    let active = true;
    // Initial fetch if edit mode
    if (isEdit && editingId != null) {
      setLoading(true);
      getRecord(currentEntity.entityName, editingId)
        .then(data => {
          if (!active) return;
          setFormData(Object.fromEntries(Object.entries(data || {}).filter(([name]) => !currentEntity.fields.some(f => f.name === name && f.sensitive))));
          // Populate inlines
          const inlines: Record<string, any[]> = {};
          currentEntity.inlines.forEach(inline => {
            if (Array.isArray(data[inline.propertyName])) {
              inlines[inline.propertyName] = data[inline.propertyName];
            } else {
              inlines[inline.propertyName] = [];
            }
          });
          setInlineData(inlines);
        })
        .catch(err => { if (!active) return; setLoadError(err.message); addToast(err.message, 'error'); })
        .finally(() => { if (active) setLoading(false); });
    } else {
      // Create mode - initialize default values
      const initial: Record<string, any> = {};
      formFields.forEach(f => {
        if (f.fieldType === 'BOOLEAN') initial[f.name] = false;
        else if (f.fieldType === 'ENUM' && f.enumValues?.length) initial[f.name] = f.enumValues[0];
        else initial[f.name] = '';
      });
      setFormData(initial);

      const inlines: Record<string, any[]> = {};
      currentEntity.inlines.forEach(inline => {
        inlines[inline.propertyName] = [];
      });
      setInlineData(inlines);
    }

    return () => { active = false; };
  }, [currentEntity.entityName, editingId, isEdit]);

  const handleChange = (name: string, value: any) => {
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (saving || loading || loadError) return;
    setSaving(true);
    try {
      const payload = buildFormPayload(formFields, formData, isEdit);

      if (isEdit) {
        await updateRecord(currentEntity.entityName, editingId, payload);
        addToast(`${currentEntity.displayName} updated successfully`);
      } else {
        await createRecord(currentEntity.entityName, payload);
        addToast(`New ${currentEntity.displayName} created successfully`);
      }
      navigateToList(currentEntity.entityName);
    } catch (err: any) {
      addToast(err.message || 'Error saving record', 'error');
    } finally {
      setSaving(false);
    }
  };

  const renderFieldInput = (field: FieldMetadata) => {
    const val = formData[field.name] ?? '';
    if (field.readOnly || (isEdit && field.primaryKey)) return <Input id={field.name} value={String(val)} readOnly />;
    const isRequired = field.validation?.required;

    if (field.fieldType === 'BOOLEAN' || field.widget === 'BOOLEAN_SWITCH') {
      return (
        <div className="flex items-center space-x-3 pt-1">
          <Switch
            id={field.name}
            checked={Boolean(val)}
            onCheckedChange={checked => handleChange(field.name, checked)}
          />
          <span className="text-sm font-medium text-foreground">
            {val ? 'Enabled / Active' : 'Disabled / Inactive'}
          </span>
        </div>
      );
    }

    if (field.fieldType === 'ENUM' && field.enumValues) {
      return (
        <FieldSelect id={field.name} label={field.label} value={String(val ?? '')}
          onChange={value => handleChange(field.name, value)} required={isRequired}
          placeholder={`Select ${field.label}…`}
          options={field.enumValues.map(value => ({ value, label: value }))} />
      );
    }

    if (field.fieldType === 'RELATION_MANY_TO_ONE' || field.fieldType === 'RELATION_ONE_TO_ONE') {
      return <RelationSelect id={field.name} label={field.label} entity={field.lookupTargetEntity || ''}
        value={val} required={isRequired} onChange={value => handleChange(field.name, value)} />;
    }

    if (field.widget === 'TEXTAREA' || field.widget === 'RICH_TEXT') {
      return (
        <Textarea
          id={field.name}
          minLength={field.validation?.minLength}
          maxLength={field.validation?.maxLength}
          rows={5}
          value={val}
          onChange={e => handleChange(field.name, e.target.value)}
          placeholder={field.placeholder || `Enter ${field.label}...`}
          required={isRequired}
          className="font-sans"
        />
      );
    }

    if (field.sensitive || field.widget === 'PASSWORD') {
      const isVisible = Boolean(showPassword[field.name]);
      return (
        <div className="relative">
          <Input
            id={field.name}
            type={isVisible ? 'text' : 'password'}
            value={val}
            onChange={e => handleChange(field.name, e.target.value)}
            placeholder={isEdit ? 'Leave blank to keep unchanged' : field.placeholder}
            required={!isEdit && isRequired}
            className="pr-10"
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={isVisible ? "Hide password" : "Show password"}
            onClick={() => setShowPassword(prev => ({ ...prev, [field.name]: !prev[field.name] }))}
            className="absolute right-1 top-1 h-8 w-8 text-muted-foreground hover:text-foreground"
          >
            {isVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </Button>
        </div>
      );
    }

    if (field.fieldType === 'DATE') {
      return (
        <Input
            id={field.name}
          type="date"
          value={val ? val.substring(0, 10) : ''}
          onChange={e => handleChange(field.name, e.target.value)}
          required={isRequired}
        />
      );
    }

    if (field.fieldType === 'DATE_TIME') {
      return (
        <Input
            id={field.name}
          type="datetime-local"
          value={val ? val.replace('Z', '').substring(0, 16) : ''}
          onChange={e => handleChange(field.name, e.target.value)}
          required={isRequired}
        />
      );
    }

    return (
      <Input
            id={field.name}
        type={field.fieldType === 'NUMBER' ? 'number' : field.validation?.email ? 'email' : 'text'}
        min={field.validation?.min}
        max={field.validation?.max}
        minLength={field.validation?.minLength}
        maxLength={field.validation?.maxLength}
        value={val}
        onChange={e => handleChange(field.name, e.target.value)}
        placeholder={field.placeholder || `Enter ${field.label}...`}
        required={isRequired}
        step={field.fieldType === 'NUMBER' ? 'any' : undefined}
      />
    );
  };

  return (
    <div className="p-4 sm:p-8 max-w-5xl mx-auto space-y-6 animate-fade-in">
      {/* Top Header */}
      <div className="flex flex-wrap gap-4 items-center justify-between">
        <div className="flex min-w-0 flex-1 basis-64 items-center gap-3">
          <Button
            aria-label="Back to records"
            variant="outline"
            size="icon"
            onClick={() => navigateToList(currentEntity.entityName)}
            className="rounded-xl shrink-0"
          >
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <div className="min-w-0">
            <h1 className="text-xl sm:text-2xl break-words font-extrabold tracking-tight">
              {isEdit ? `Edit ${currentEntity.displayName} #${editingId}` : `Create New ${currentEntity.displayName}`}
            </h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              Fill in the attributes below. All validations will be verified by the server.
            </p>
          </div>
        </div>

        <div className="flex w-full sm:w-auto items-center gap-3 [&>button]:flex-1 sm:[&>button]:flex-none">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigateToList(currentEntity.entityName)}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            form="entity-form"
            disabled={saving || loading || !!loadError}
            className="gap-2 shadow-sm"
          >
            {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
            <span>{saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Record'}</span>
          </Button>
        </div>
      </div>

      {loading && <FormSkeleton />}
      {loadError && <p role="alert" className="text-destructive">{loadError}</p>}
      {/* Main Form Fields */}
      <form hidden={loading} aria-busy={saving} id="entity-form" onSubmit={handleSubmit} className="space-y-8">
        <Card>
          <CardHeader className="pb-4">
            <CardTitle className="text-base font-bold">General Attributes</CardTitle>
            <CardDescription className="text-xs">Standard scalar attributes and relations</CardDescription>
          </CardHeader>

          <CardContent>
            <fieldset disabled={loading || saving || !!loadError}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {formFields.map(field => (
                <div
                  key={field.name}
                  className={cn(
                    'space-y-2',
                    (field.widget === 'TEXTAREA' || field.widget === 'RICH_TEXT') && 'md:col-span-2'
                  )}
                >
                  <label htmlFor={field.name} className="block text-xs font-semibold text-foreground">
                    {field.label}
                    {field.validation?.required && <span className="text-destructive ml-1">*</span>}
                  </label>

                  {renderFieldInput(field)}

                  {field.description && (
                    <p className="text-[11px] text-muted-foreground">{field.description}</p>
                  )}
                </div>
              ))}
            </div>
            </fieldset>
          </CardContent>
        </Card>

        {/* Inlines / Child Tables (@AdminInline) */}
        {currentEntity.inlines.map(inline => {
          const rows = inlineData[inline.propertyName] || [];

          return (
            <Card key={inline.propertyName}>
              <CardHeader className="flex flex-row items-center justify-between pb-3">
                <div>
                  <CardTitle className="text-base font-bold">{inline.title}</CardTitle>
                  <CardDescription className="text-xs">
                    Related records ({rows.length}) — manage these from their entity list
                  </CardDescription>
                </div>

              </CardHeader>

              <CardContent className="pt-0">
                {rows.length > 0 ? (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-16">#</TableHead>
                        <TableHead>Details / Data</TableHead>
                        
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rows.map((row, idx) => (
                        <TableRow key={idx}>
                          <TableCell className="font-semibold text-muted-foreground">#{idx + 1}</TableCell>
                          <TableCell>
                            <span className="font-mono text-xs text-foreground break-all">
                              {JSON.stringify(row)}
                            </span>
                          </TableCell>
                          
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                ) : (
                  <div className="text-center py-6 text-xs text-muted-foreground italic">
                    No {inline.title} added yet.
                  </div>
                )}
              </CardContent>
            </Card>
          );
        })}
      </form>
    </div>
  );
};
