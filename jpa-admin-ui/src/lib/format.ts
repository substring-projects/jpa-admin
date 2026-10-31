import type { EntityMetadata, FieldMetadata } from '@/types/metadata';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const ISO_DATE_TIME_RE = /^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/;

export const isUuid = (value: unknown): boolean => typeof value === 'string' && UUID_RE.test(value);

export function shortId(value: unknown): string {
  const text = String(value ?? '');
  return isUuid(text) ? text.slice(0, 8) : text;
}

const dateFormatter = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' });
const dateTimeFormatter = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });
const numberFormatter = new Intl.NumberFormat(undefined, { maximumFractionDigits: 4 });
const relativeFormatter = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });

export function parseTemporal(value: unknown, dateOnly: boolean): Date | null {
  if (typeof value !== 'string' && typeof value !== 'number') return null;
  if (typeof value === 'string' && ISO_DATE_RE.test(value)) {
    const [y, m, d] = value.split('-').map(Number);
    return new Date(y, m - 1, d);
  }
  if (typeof value === 'string' && !ISO_DATE_TIME_RE.test(value)) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return dateOnly ? new Date(date.getFullYear(), date.getMonth(), date.getDate()) : date;
}

export function formatDate(value: unknown, withTime: boolean): string {
  const date = parseTemporal(value, !withTime);
  if (!date) return String(value ?? '');
  return withTime ? dateTimeFormatter.format(date) : dateFormatter.format(date);
}

export function formatRelative(value: unknown): string {
  const date = parseTemporal(value, false);
  if (!date) return '';
  const seconds = Math.round((date.getTime() - Date.now()) / 1000);
  const units: Array<[Intl.RelativeTimeFormatUnit, number]> = [
    ['year', 31536000], ['month', 2592000], ['week', 604800], ['day', 86400], ['hour', 3600], ['minute', 60],
  ];
  for (const [unit, size] of units) {
    if (Math.abs(seconds) >= size) return relativeFormatter.format(Math.round(seconds / size), unit);
  }
  return 'just now';
}

export const formatNumber = (value: unknown) =>
  typeof value === 'number' ? numberFormatter.format(value) : String(value ?? '');

export const formatCount = (value: number) => new Intl.NumberFormat(undefined, { notation: value >= 10000 ? 'compact' : 'standard' }).format(value);

export function humanize(value: string): string {
  if (!value) return value;
  if (/^[A-Z0-9_]+$/.test(value)) {
    const words = value.toLowerCase().split('_').filter(Boolean).join(' ');
    return words.charAt(0).toUpperCase() + words.slice(1);
  }
  return value;
}

export function initials(name: string): string {
  return name.split(/[\s._@-]+/).filter(Boolean).slice(0, 2).map(part => part[0]!.toUpperCase()).join('') || '?';
}

const LABEL_CANDIDATES = ['name', 'title', 'label', 'fullName', 'displayName', 'username', 'email', 'code', 'subject'];

export function recordLabel(entity: EntityMetadata, record: Record<string, unknown> | null | undefined): string {
  if (!record) return '';
  for (const candidate of LABEL_CANDIDATES) {
    const value = record[candidate];
    if (typeof value === 'string' && value.trim()) return value;
  }
  const firstString = entity.fields.find(f => !f.primaryKey && !f.sensitive && f.fieldType === 'STRING'
    && typeof record[f.name] === 'string' && (record[f.name] as string).trim());
  if (firstString) return String(record[firstString.name]);
  return `#${shortId(record[entity.primaryKeyField])}`;
}

/** Plain-text rendering used for CSV export and tooltips. */
export function plainValue(field: FieldMetadata, value: unknown): string {
  if (value === null || value === undefined) return '';
  if (typeof value === 'object' && !Array.isArray(value)) {
    const rel = value as { label?: unknown; id?: unknown };
    return String(rel.label ?? rel.id ?? JSON.stringify(value));
  }
  if (Array.isArray(value)) return value.map(v => plainValue(field, v)).join(', ');
  if (field.fieldType === 'BOOLEAN') return value ? 'Yes' : 'No';
  return String(value);
}

const ENUM_TONES = ['BLUE', 'GREEN', 'YELLOW', 'PURPLE', 'PINK', 'INDIGO', 'GRAY'] as const;
const NEGATIVE = /(cancel|fail|error|reject|block|inactive|disabled|deleted|lost|overdue|closed|refund)/i;
const POSITIVE = /(active|success|complete|done|paid|approved|enabled|delivered|resolved|published|won|open)/i;
const WARNING = /(pending|wait|hold|draft|review|progress|processing|new|trial)/i;

/** Gives enum values a stable, meaningful color when no explicit @AdminBadge mapping exists. */
export function enumTone(value: string): string {
  if (NEGATIVE.test(value)) return 'RED';
  if (POSITIVE.test(value)) return 'GREEN';
  if (WARNING.test(value)) return 'YELLOW';
  let hash = 0;
  for (const char of value) hash = (hash * 31 + char.charCodeAt(0)) | 0;
  return ENUM_TONES[Math.abs(hash) % ENUM_TONES.length];
}

const ICON_HINTS: Array<[RegExp, string]> = [
  [/comment|reply|note/i, 'message-square'],
  [/conversation|chat|message|thread/i, 'messages-square'],
  [/user|account|member|staff|employee|admin/i, 'users'],
  [/customer|client|contact|person|people|profile/i, 'contact'],
  [/order|cart|purchase|checkout/i, 'shopping-cart'],
  [/product|item|sku|inventory|stock/i, 'package'],
  [/category|tag|label|type/i, 'tag'],
  [/ticket|issue|support|case/i, 'ticket'],
  [/invoice|payment|billing|transaction|charge/i, 'receipt'],
  [/event|outbox|log|audit|history/i, 'activity'],
  [/role|permission|policy|security/i, 'shield'],
  [/team|group|organization|company|org/i, 'building-2'],
  [/file|document|attachment|asset|media/i, 'file-text'],
  [/address|location|place|region|country|city/i, 'map-pin'],
  [/setting|config|preference/i, 'settings'],
  [/schedule|calendar|booking|appointment/i, 'calendar'],
  [/notification|alert/i, 'bell'],
  [/email|mail/i, 'mail'],
  [/project|task|job/i, 'kanban'],
  [/review|rating|feedback/i, 'star'],
];

export function entityIcon(entity: Pick<EntityMetadata, 'icon' | 'entityName'>): string {
  if (entity.icon && entity.icon !== 'database') return entity.icon;
  return ICON_HINTS.find(([pattern]) => pattern.test(entity.entityName))?.[1] ?? 'database';
}

/** Web URLs and same-site paths only; anything else (javascript:, data:) is never linked or rendered. */
export const isSafeUrl = (url: string) => /^(https?:\/\/|\/(?!\/))/i.test(url.trim());

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const units = ['KB', 'MB', 'GB'];
  let value = bytes / 1024;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) { value /= 1024; unit++; }
  return `${value >= 10 || Number.isInteger(value) ? Math.round(value) : value.toFixed(1)} ${units[unit]}`;
}

export function formatDuration(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 1) return 'less than a second';
  if (seconds < 60) return `${Math.ceil(seconds)}s`;
  return `${Math.floor(seconds / 60)}m ${Math.ceil(seconds % 60)}s`;
}

/** Last path segment of a file URL, decoded, for showing an uploaded file's name. */
export function fileNameFromUrl(url: string): string {
  try {
    const path = new URL(url, window.location.href).pathname;
    return decodeURIComponent(path.slice(path.lastIndexOf('/') + 1)) || url;
  } catch {
    return url;
  }
}
