import { useState, type MouseEvent } from 'react';
import { Link } from 'react-router';
import { Check, Copy, FileText, Minus, X } from 'lucide-react';
import type { FieldMetadata } from '@/types/metadata';
import { enumTone, fileNameFromUrl, formatDate, formatNumber, formatRelative, humanize, isSafeUrl, isUuid, shortId } from '@/lib/format';
import { ToneBadge } from '@/components/ToneBadge';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { sanitizeHtml } from '@/lib/sanitize';

export function CopyButton({ value, label = 'Copy' }: { value: string; label?: string }) {
  const [copied, setCopied] = useState(false);
  const copy = (event: MouseEvent) => {
    event.stopPropagation();
    void navigator.clipboard?.writeText(value).then(() => {
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1200);
    });
  };
  return <button type="button" onClick={copy} aria-label={copied ? 'Copied' : label}
    className="inline-flex size-6 shrink-0 items-center justify-center rounded text-muted-foreground opacity-0 transition hover:bg-accent hover:text-foreground focus-visible:opacity-100 group-hover/cell:opacity-100">
    {copied ? <Check className="size-3.5 text-primary" /> : <Copy className="size-3.5" />}
  </button>;
}

const Empty = () => <Minus aria-label="Empty" className="size-3.5 text-muted-foreground/50" />;

interface Props {
  field: FieldMetadata;
  value: unknown;
  /** Detail views show full text instead of truncated cells. */
  expanded?: boolean;
}

export function CellValue({ field, value, expanded = false }: Props) {
  if (value === null || value === undefined || value === '') return <Empty />;

  if (field.sensitive) return <span className="font-mono text-xs tracking-widest text-muted-foreground">••••••••</span>;

  const mapped = field.badges?.find(b => String(b.value).toLowerCase() === String(value).toLowerCase());
  if (mapped) return <ToneBadge tone={mapped.color}>{mapped.label}</ToneBadge>;

  switch (typeof value === 'boolean' ? 'BOOLEAN' : field.fieldType) {
    case 'ENUM':
      return <ToneBadge tone={enumTone(String(value))}>{humanize(String(value))}</ToneBadge>;
    case 'BOOLEAN':
      return value
        ? <span className="inline-flex items-center gap-1 text-sm text-emerald-700 dark:text-emerald-400"><Check className="size-3.5" />Yes</span>
        : <span className="inline-flex items-center gap-1 text-sm text-muted-foreground"><X className="size-3.5" />No</span>;
    case 'DATE':
    case 'DATE_TIME': {
      const withTime = field.fieldType === 'DATE_TIME';
      const formatted = formatDate(value, withTime);
      return <Tooltip>
        <TooltipTrigger asChild><time dateTime={String(value)} className="tabular whitespace-nowrap">{formatted}</time></TooltipTrigger>
        <TooltipContent><span className="font-mono">{String(value)}</span>{withTime && <span className="ml-2 opacity-70">{formatRelative(value)}</span>}</TooltipContent>
      </Tooltip>;
    }
    case 'NUMBER':
      return <span className="tabular">{formatNumber(value)}</span>;
    case 'RELATION_MANY_TO_ONE':
    case 'RELATION_ONE_TO_ONE': {
      if (typeof value !== 'object') return <span>{String(value)}</span>;
      const rel = value as { id?: unknown; label?: unknown };
      const label = String(rel.label ?? rel.id ?? '');
      const display = isUuid(label) ? shortId(label) : label;
      if (!field.lookupTargetEntity || rel.id == null) return <span>{display}</span>;
      return <Link to={`/${field.lookupTargetEntity}/${encodeURIComponent(String(rel.id))}`} onClick={e => e.stopPropagation()}
        className={cn('font-medium text-foreground underline decoration-border underline-offset-4 hover:decoration-primary', !expanded && 'inline-block max-w-56 truncate align-bottom')}>
        {display}
      </Link>;
    }
  }

  if (field.fieldType === 'RELATION_MANY_TO_MANY' && Array.isArray(value)) {
    if (value.length === 0) return <Empty />;
    const refs = value as Array<{ id?: unknown; label?: unknown }>;
    const shown = expanded ? refs : refs.slice(0, 3);
    return <span className={cn('inline-flex gap-1', expanded ? 'flex-wrap' : 'max-w-72 overflow-hidden')}>
      {shown.map(ref => {
        const label = String(ref.label ?? ref.id);
        const text = isUuid(label) ? shortId(label) : label;
        return field.lookupTargetEntity && ref.id != null
          ? <Link key={String(ref.id)} to={`/${field.lookupTargetEntity}/${encodeURIComponent(String(ref.id))}`} onClick={e => e.stopPropagation()}
              className="whitespace-nowrap rounded-md bg-secondary px-1.5 py-0.5 text-xs font-medium hover:bg-accent">{text}</Link>
          : <span key={String(ref.id)} className="whitespace-nowrap rounded-md bg-secondary px-1.5 py-0.5 text-xs font-medium">{text}</span>;
      })}
      {refs.length > shown.length && <span className="text-xs text-muted-foreground">+{refs.length - shown.length}</span>}
    </span>;
  }

  if (field.widget === 'RICH_TEXT' && typeof value === 'string') {
    if (expanded) return <div className="rich-text text-sm" dangerouslySetInnerHTML={{ __html: sanitizeHtml(value) }} />;
    const plain = new DOMParser().parseFromString(sanitizeHtml(value), 'text/html').body.textContent ?? '';
    return <span title={plain.length > 40 ? plain : undefined} className="block max-w-72 truncate">{plain}</span>;
  }

  if (field.widget === 'CODE_EDITOR' && typeof value === 'string' && expanded) {
    return <pre className="max-h-96 overflow-auto rounded-md border bg-muted/50 p-3 font-mono text-xs">{value}</pre>;
  }

  if (Array.isArray(value) || typeof value === 'object') {
    return <code className="font-mono text-xs text-muted-foreground">{JSON.stringify(value)}</code>;
  }

  const text = String(value);

  if (field.widget === 'COLOR_PICKER' && /^#[0-9a-f]{3,8}$/i.test(text)) {
    return <span className="inline-flex items-center gap-2"><span className="size-4 rounded border" style={{ backgroundColor: text }} /><span className="font-mono text-xs">{text}</span></span>;
  }

  // Only web URLs and same-origin paths; never javascript: or data: values.
  if ((field.widget === 'IMAGE_URL' || field.upload) && isSafeUrl(text)) {
    const image = field.widget === 'IMAGE_URL' || field.upload?.image || /\.(png|jpe?g|gif|webp|avif|svg)(\?|$)/i.test(text);
    if (image) {
      return <a href={text} target="_blank" rel="noopener noreferrer" onClick={e => e.stopPropagation()} aria-label="Open image" className="block w-fit">
        {/* An explicit height keeps SVGs without intrinsic dimensions from collapsing. */}
        <img src={text} alt="" loading="lazy" className={cn('rounded border bg-muted', expanded ? 'h-40 w-auto max-w-full object-contain' : 'size-9 object-cover')} />
      </a>;
    }
    return <a href={text} target="_blank" rel="noopener noreferrer" onClick={e => e.stopPropagation()}
      className="inline-flex max-w-64 items-center gap-1.5 rounded-md border bg-muted/40 px-2 py-1 text-xs font-medium hover:bg-accent">
      <FileText className="size-3.5 shrink-0 text-muted-foreground" /><span className="truncate">{fileNameFromUrl(text)}</span>
    </a>;
  }

  if (isUuid(text) || field.primaryKey) {
    return <span className="group/cell inline-flex items-center gap-1">
      <Tooltip>
        <TooltipTrigger asChild><span className="font-mono text-xs text-muted-foreground">{expanded ? text : shortId(text)}</span></TooltipTrigger>
        {!expanded && isUuid(text) && <TooltipContent><span className="font-mono">{text}</span></TooltipContent>}
      </Tooltip>
      <CopyButton value={text} label="Copy ID" />
    </span>;
  }

  if (field.validation?.email || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(text)) {
    return <a href={`mailto:${text}`} onClick={e => e.stopPropagation()} className={cn('hover:underline', !expanded && 'block max-w-64 truncate')}>{text}</a>;
  }

  if (expanded) return <span className="whitespace-pre-wrap break-words">{text}</span>;
  return <span title={text.length > 40 ? text : undefined} className="block max-w-72 truncate">{text}</span>;
}
