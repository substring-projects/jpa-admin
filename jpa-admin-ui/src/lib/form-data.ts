import type { EntityMetadata, FieldMetadata } from '../types/metadata';

export function editableFormFields(entity: EntityMetadata): FieldMetadata[] {
  return entity.fields.filter(field => field.inForm
    && !(field.primaryKey && field.generatedValue)
    && !entity.inlines.some(inline => inline.propertyName === field.name)
    && !['RELATION_ONE_TO_MANY', 'RELATION_MANY_TO_MANY', 'EMBEDDED'].includes(field.fieldType));
}

export function buildFormPayload(fields: FieldMetadata[], data: Record<string, unknown>, isEdit: boolean) {
  return Object.fromEntries(fields
    .filter(field => !field.readOnly && !field.computed && !(isEdit && field.primaryKey))
    .filter(field => !(isEdit && field.sensitive && !data[field.name]))
    .filter(field => data[field.name] !== undefined)
    .map(field => [field.name, data[field.name] === '' ? null : data[field.name]]));
}
