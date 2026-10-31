import { useEffect, useRef, useState, type DragEvent, type KeyboardEvent } from 'react';
import { AlertCircle, CheckCircle2, ExternalLink, FileText, ImageUp, Loader2, RefreshCw, Trash2, Upload, X } from 'lucide-react';
import { accepts, fileType, UploadAborted, uploadFile, type UploadPhase } from '@/api/uploads';
import { fileNameFromUrl, formatBytes, formatDuration, isSafeUrl } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { UploadMetadata } from '@/types/metadata';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';

interface Props {
  id: string;
  entity: string;
  field: string;
  label: string;
  rules: UploadMetadata;
  value: string;
  onChange: (url: string) => void;
  onBusyChange?: (busy: boolean) => void;
  invalid?: boolean;
  disabled?: boolean;
  describedBy?: string;
}

type State =
  | { status: 'idle' }
  | { status: 'active'; file: File; phase: UploadPhase; loaded: number; total: number; rate: number }
  | { status: 'error'; file: File | null; message: string }
  | { status: 'done' };

const LABELS: Record<string, string> = {
  'image/png': 'PNG', 'image/jpeg': 'JPEG', 'image/webp': 'WebP', 'image/gif': 'GIF', 'image/avif': 'AVIF', 'image/svg+xml': 'SVG',
  'application/pdf': 'PDF', 'text/csv': 'CSV', 'image/*': 'any image',
};
const describeAccept = (rules: UploadMetadata) => {
  const names = rules.accept.map(type => LABELS[type.toLowerCase()] ?? type.split('/').pop()?.toUpperCase() ?? type);
  return names.length > 1 ? `${names.slice(0, -1).join(', ')} or ${names[names.length - 1]}` : names[0];
};
const isImageUrl = (url: string) => /\.(png|jpe?g|gif|webp|avif|svg)(\?|$)/i.test(url) || /ik\.imagekit\.io/.test(url);

export function FileUpload({ id, entity, field, label, rules, value, onChange, onBusyChange, invalid, disabled, describedBy }: Props) {
  const [state, setState] = useState<State>({ status: 'idle' });
  const [dragging, setDragging] = useState(false);
  const [localPreview, setLocalPreview] = useState<string | null>(null);
  const [previewBroken, setPreviewBroken] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  const abort = useRef<AbortController | null>(null);
  const samples = useRef<Array<{ t: number; loaded: number }>>([]);
  const busy = state.status === 'active';

  useEffect(() => { onBusyChange?.(busy); }, [busy]);
  useEffect(() => () => { abort.current?.abort(); }, []);
  useEffect(() => () => { if (localPreview) URL.revokeObjectURL(localPreview); }, [localPreview]);
  useEffect(() => { setPreviewBroken(false); }, [value]);
  useEffect(() => {
    if (state.status !== 'done') return;
    const timer = window.setTimeout(() => setState({ status: 'idle' }), 2500);
    return () => window.clearTimeout(timer);
  }, [state.status]);

  const start = async (file: File) => {
    const type = fileType(file);
    if (!accepts(rules, type)) {
      setState({ status: 'error', file: null, message: `${file.name} isn't an allowed file type. Use ${describeAccept(rules)}.` });
      return;
    }
    if (file.size > rules.maxSize) {
      setState({ status: 'error', file: null, message: `${file.name} is ${formatBytes(file.size)}; the limit is ${formatBytes(rules.maxSize)}.` });
      return;
    }
    if (file.size === 0) { setState({ status: 'error', file: null, message: `${file.name} is empty.` }); return; }

    abort.current?.abort();
    const controller = new AbortController();
    abort.current = controller;
    samples.current = [];
    setLocalPreview(type.startsWith('image/') ? URL.createObjectURL(file) : null);
    setState({ status: 'active', file, phase: 'preparing', loaded: 0, total: file.size, rate: 0 });
    try {
      const url = await uploadFile(entity, field, file, {
        signal: controller.signal,
        onPhase: phase => setState(prev => (prev.status === 'active' ? { ...prev, phase } : prev)),
        onProgress: (loaded, total) => {
          const now = performance.now();
          samples.current = [...samples.current.filter(s => now - s.t < 3000), { t: now, loaded }];
          const first = samples.current[0];
          const rate = now - first.t > 250 ? ((loaded - first.loaded) / (now - first.t)) * 1000 : 0;
          setState(prev => (prev.status === 'active' ? { ...prev, loaded, total, rate } : prev));
        },
      });
      if (controller.signal.aborted) return;
      onChange(url);
      setState({ status: 'done' });
    } catch (err) {
      if (err instanceof UploadAborted || controller.signal.aborted) { setState({ status: 'idle' }); return; }
      setState({ status: 'error', file, message: err instanceof Error ? err.message : 'Upload failed' });
    } finally {
      if (abort.current === controller) abort.current = null;
    }
  };

  const choose = () => { if (!disabled && !busy) input.current?.click(); };
  const onDrop = (event: DragEvent) => {
    event.preventDefault();
    setDragging(false);
    if (disabled || busy) return;
    const file = event.dataTransfer.files?.[0];
    if (file) void start(file);
  };
  const onKey = (event: KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); choose(); }
  };

  const acceptAttr = rules.accept.join(',');
  const hint = `${describeAccept(rules)} · up to ${formatBytes(rules.maxSize)}`;
  const fileInput = <input ref={input} type="file" accept={acceptAttr} className="sr-only" tabIndex={-1} aria-hidden
    onChange={e => { const file = e.target.files?.[0]; e.target.value = ''; if (file) void start(file); }} />;

  if (state.status === 'active') {
    const percent = state.total ? Math.round((state.loaded / state.total) * 100) : 0;
    const remaining = state.rate > 0 ? (state.total - state.loaded) / state.rate : NaN;
    const status = state.phase === 'preparing' ? 'Preparing upload…'
      : state.phase === 'verifying' ? 'Checking the file…'
      : `${formatBytes(state.loaded)} of ${formatBytes(state.total)}${state.rate > 0 ? ` · ${formatBytes(state.rate)}/s · ${formatDuration(remaining)} left` : ''}`;
    return <div className="flex items-center gap-3 rounded-lg border bg-card p-3" aria-busy>
      {localPreview ? <img src={localPreview} alt="" className="size-14 shrink-0 rounded-md border object-cover" />
        : <div className="flex size-14 shrink-0 items-center justify-center rounded-md border bg-muted text-muted-foreground"><FileText className="size-6" /></div>}
      <div className="min-w-0 flex-1 space-y-1.5">
        <div className="flex items-baseline justify-between gap-2">
          <p className="truncate text-sm font-medium">{state.file.name}</p>
          <span className="tabular shrink-0 text-sm font-medium">{state.phase === 'uploading' ? `${percent}%` : <Loader2 className="size-4 animate-spin text-muted-foreground" />}</span>
        </div>
        <Progress value={state.phase === 'verifying' ? 100 : percent} aria-label={`Uploading ${state.file.name}`} className={cn('h-1.5', state.phase === 'preparing' && 'animate-pulse')} />
        <p role="status" aria-live="polite" className="tabular truncate text-xs text-muted-foreground">{status}</p>
      </div>
      <Button type="button" variant="ghost" size="icon-sm" aria-label="Cancel upload" onClick={() => abort.current?.abort()} disabled={state.phase === 'verifying'}><X /></Button>
    </div>;
  }

  const error = state.status === 'error' ? state : null;
  const errorBox = error && <div role="alert" className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-2.5 text-sm text-destructive">
    <AlertCircle className="mt-0.5 size-4 shrink-0" />
    <span className="flex-1">{error.message}</span>
    {error.file && <Button type="button" variant="ghost" size="xs" onClick={() => void start(error.file!)}><RefreshCw />Retry</Button>}
    <button type="button" aria-label="Dismiss" onClick={() => setState({ status: 'idle' })} className="rounded p-0.5 hover:bg-destructive/10"><X className="size-3.5" /></button>
  </div>;

  if (value) {
    const safe = isSafeUrl(value);
    const showImage = safe && (rules.image || isImageUrl(value)) && !previewBroken;
    return <div className="space-y-2">
      <div className={cn('flex items-center gap-3 rounded-lg border bg-card p-3', invalid && 'border-destructive')}>
        {showImage
          ? <a href={value} target="_blank" rel="noopener noreferrer" className="shrink-0" aria-label="Open full size">
              <img src={value} alt={`${label} preview`} onError={() => setPreviewBroken(true)} className="size-14 rounded-md border object-cover" />
            </a>
          : <div className="flex size-14 shrink-0 items-center justify-center rounded-md border bg-muted text-muted-foreground"><FileText className="size-6" /></div>}
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium" title={value}>{fileNameFromUrl(value)}</p>
          <p className="flex items-center gap-1 text-xs text-muted-foreground">
            {state.status === 'done' ? <><CheckCircle2 className="size-3.5 text-emerald-600" />Uploaded</> : previewBroken ? 'Preview unavailable' : hint}
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-1">
          {safe && <Button asChild variant="ghost" size="icon-sm" aria-label="Open file"><a href={value} target="_blank" rel="noopener noreferrer"><ExternalLink /></a></Button>}
          <Button id={id} type="button" variant="outline" size="sm" onClick={choose} disabled={disabled} aria-describedby={describedBy}><Upload />Replace</Button>
          <Button type="button" variant="ghost" size="icon-sm" aria-label={`Remove ${label}`} onClick={() => onChange('')} disabled={disabled}
            className="text-muted-foreground hover:text-destructive"><Trash2 /></Button>
        </div>
      </div>
      {errorBox}
      {fileInput}
    </div>;
  }

  return <div className="space-y-2">
    <div id={id} role="button" tabIndex={disabled ? -1 : 0} aria-disabled={disabled || undefined} aria-invalid={invalid || undefined}
      aria-describedby={describedBy} aria-label={`Upload ${label}. ${hint}`}
      onClick={choose} onKeyDown={onKey}
      onDragOver={e => { e.preventDefault(); if (!disabled) setDragging(true); }} onDragLeave={() => setDragging(false)} onDrop={onDrop}
      className={cn('flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-dashed px-4 py-6 text-center transition-colors outline-none',
        'hover:border-primary/60 hover:bg-primary/5 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50',
        dragging && 'border-primary bg-primary/5', invalid && 'border-destructive', disabled && 'pointer-events-none opacity-50')}>
      <div className={cn('flex size-10 items-center justify-center rounded-full bg-muted text-muted-foreground', dragging && 'bg-primary/10 text-primary')}>
        {rules.image ? <ImageUp className="size-5" /> : <Upload className="size-5" />}
      </div>
      <p className="text-sm"><span className="font-medium text-primary">{dragging ? 'Drop to upload' : 'Click to upload'}</span>{!dragging && ' or drag and drop'}</p>
      <p className="text-xs text-muted-foreground">{hint}</p>
    </div>
    {errorBox}
    {fileInput}
  </div>;
}
