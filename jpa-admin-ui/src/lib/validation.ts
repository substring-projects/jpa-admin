import type { FieldMetadata } from '@/types/metadata';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Mirrors the server's bean-validation metadata so errors show next to the field before submitting. */
export function validateField(field: FieldMetadata, value: unknown, isEdit: boolean): string | null {
  const v = field.validation ?? { required: false };
  const empty = value === null || value === undefined || value === ''
    || (Array.isArray(value) && value.length === 0)
    || (typeof value === 'object' && !Array.isArray(value) && (value as { id?: unknown }).id == null);
  if (empty) {
    if (field.fieldType === 'BOOLEAN') return null;
    if (field.sensitive && isEdit) return null;
    return v.required ? `${field.label} is required` : null;
  }
  if (field.fieldType === 'NUMBER') {
    const n = Number(value);
    if (Number.isNaN(n)) return `${field.label} must be a number`;
    if (v.min != null && n < v.min) return `Must be at least ${v.min}`;
    if (v.max != null && n > v.max) return `Must be at most ${v.max}`;
  }
  if (typeof value === 'string') {
    if (v.minLength != null && value.length < v.minLength) return `Must be at least ${v.minLength} characters`;
    if (v.maxLength != null && v.maxLength > 0 && value.length > v.maxLength) return `Must be at most ${v.maxLength} characters`;
    if (v.email && !EMAIL_RE.test(value)) return 'Enter a valid email address';
    if (v.pattern) {
      try { if (!new RegExp(`^(?:${v.pattern})$`).test(value)) return `${field.label} has an invalid format`; }
      catch { /* an unsupported server-side pattern is left to the server */ }
    }
  }
  return null;
}
