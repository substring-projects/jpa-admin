import type { FieldMetadata } from '@/types/metadata';
import { plainValue } from './format';

function escapeCell(value: string): string {
  // Leading formula characters are neutralised so spreadsheets don't execute exported data.
  const safe = /^[=+\-@\t\r]/.test(value) ? `'${value}` : value;
  return /[",\n\r]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
}

export function toCsv(fields: FieldMetadata[], rows: Record<string, unknown>[]): string {
  const header = fields.map(f => escapeCell(f.label)).join(',');
  const body = rows.map(row => fields.map(f => escapeCell(f.sensitive ? '' : plainValue(f, row[f.name]))).join(','));
  return [header, ...body].join('\r\n');
}

export function downloadCsv(filename: string, csv: string) {
  const url = URL.createObjectURL(new Blob(['﻿', csv], { type: 'text/csv;charset=utf-8' }));
  const link = Object.assign(document.createElement('a'), { href: url, download: filename });
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
