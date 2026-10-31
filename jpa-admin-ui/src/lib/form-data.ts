import type { EntityMetadata, FieldMetadata, InlineMetadata } from '../types/metadata';

const NEVER_EDITABLE = ['RELATION_ONE_TO_MANY', 'EMBEDDED', 'UNKNOWN'];

export function editableFormFields(entity: EntityMetadata): FieldMetadata[] {
  return entity.fields.filter(field => field.inForm
    && !(field.primaryKey && field.generatedValue)
    && !entity.inlines.some(inline => inline.propertyName === field.name)
    && !NEVER_EDITABLE.includes(field.fieldType));
}

export function buildFormPayload(fields: FieldMetadata[], data: Record<string, unknown>, isEdit: boolean) {
  return Object.fromEntries(fields
    .filter(field => !field.readOnly && !field.computed && !(isEdit && field.primaryKey))
    .filter(field => !(isEdit && field.sensitive && !data[field.name]))
    .filter(field => data[field.name] !== undefined)
    .map(field => [field.name, data[field.name] === '' ? null : data[field.name]]));
}

/** Inline rows the parent form can edit: one-to-many collections whose rows point back at the parent. */
export const isEditableInline = (inline: InlineMetadata) => !!inline.mappedBy;

/** Child columns for an inline, without the back-reference to the parent. */
export function inlineFields(inline: InlineMetadata, child: EntityMetadata | undefined): FieldMetadata[] {
  if (!child) return [];
  return editableFormFields(child).filter(field => field.name !== inline.mappedBy && field.fieldType !== 'RELATION_MANY_TO_MANY');
}

export function buildInlinePayload(fields: FieldMetadata[], pk: string, rows: Record<string, unknown>[]) {
  return rows.map(row => {
    const payload: Record<string, unknown> = buildFormPayload(fields, row, false);
    if (row[pk] != null && row[pk] !== '') payload[pk] = row[pk];
    return payload;
  });
}
