import { useState } from 'react';
import { Link } from 'react-router';
import { Download, ExternalLink, ImageOff, Maximize2, Upload } from 'lucide-react';
import type { FieldMetadata } from '@/types/metadata';
import { fileNameFromUrl, isSafeUrl } from '@/lib/format';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

/** Fields that hold an image: image uploads and IMAGE_URL widgets. */
export const isMediaField = (field: FieldMetadata) => !!field.upload?.image || field.widget === 'IMAGE_URL';

export const imageValue = (value: unknown) => (typeof value === 'string' && value.trim() && isSafeUrl(value) ? value.trim() : null);

/** Soft checkerboard behind images so transparent PNGs and SVGs read clearly in both themes. */
const CHECKER = '[--checker:color-mix(in_oklch,var(--muted)_70%,transparent)] dark:[--checker:color-mix(in_oklch,var(--muted)_35%,transparent)] bg-[length:16px_16px] bg-[position:0_0,8px_8px] [background-image:linear-gradient(45deg,var(--checker)_25%,transparent_25%,transparent_75%,var(--checker)_75%),linear-gradient(45deg,var(--checker)_25%,transparent_25%,transparent_75%,var(--checker)_75%)]';

function Lightbox({ url, title, open, onOpenChange }: { url: string; title: string; open: boolean; onOpenChange: (open: boolean) => void }) {
  return <Dialog open={open} onOpenChange={onOpenChange}>
    <DialogContent className="max-w-[min(96vw,1200px)] gap-0 overflow-hidden p-0 sm:max-w-[min(96vw,1200px)]">
      <div className={cn('flex max-h-[80vh] min-h-64 items-center justify-center bg-background p-4 sm:p-8', CHECKER)}>
        <img src={url} alt={title} className="max-h-[calc(80vh-4rem)] max-w-full rounded-md object-contain shadow-sm" />
      </div>
      <div className="flex items-center gap-3 border-t px-4 py-3">
        <div className="min-w-0 flex-1">
          <DialogTitle className="truncate text-sm">{title}</DialogTitle>
          <DialogDescription className="truncate text-xs">{fileNameFromUrl(url)}</DialogDescription>
        </div>
        <Button asChild variant="outline" size="sm"><a href={url} download target="_blank" rel="noopener noreferrer"><Download />Download</a></Button>
        <Button asChild variant="outline" size="sm"><a href={url} target="_blank" rel="noopener noreferrer"><ExternalLink />Open original</a></Button>
      </div>
    </DialogContent>
  </Dialog>;
}

interface MediaCardProps {
  field: FieldMetadata;
  value: unknown;
  /** Edit page link, shown as an upload shortcut when the record has no image yet. */
  editPath?: string;
  recordTitle: string;
}

export function MediaCard({ field, value, editPath, recordTitle }: MediaCardProps) {
  const url = imageValue(value);
  const [broken, setBroken] = useState(false);
  const [open, setOpen] = useState(false);
  const title = `${recordTitle} · ${field.label}`;

  return <Card className="gap-0 overflow-hidden py-0">
    <div className="flex items-center justify-between gap-2 border-b px-4 py-3">
      <p className="text-sm font-medium">{field.label}</p>
      {url && !broken && <div className="flex items-center gap-0.5">
        <Tooltip><TooltipTrigger asChild>
          <Button variant="ghost" size="icon-sm" aria-label="View full size" onClick={() => setOpen(true)}><Maximize2 /></Button>
        </TooltipTrigger><TooltipContent>View full size</TooltipContent></Tooltip>
        <Tooltip><TooltipTrigger asChild>
          <Button asChild variant="ghost" size="icon-sm"><a href={url} download target="_blank" rel="noopener noreferrer" aria-label="Download"><Download /></a></Button>
        </TooltipTrigger><TooltipContent>Download</TooltipContent></Tooltip>
      </div>}
    </div>

    {url && !broken
      ? <button type="button" onClick={() => setOpen(true)} aria-label={`View ${field.label.toLowerCase()} full size`}
          className={cn('group relative flex aspect-square w-full items-center justify-center bg-background p-6 outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50', CHECKER)}>
          <img src={url} alt={title} onError={() => setBroken(true)}
            className="max-h-full max-w-full rounded-md object-contain shadow-sm ring-1 ring-black/5 transition-transform duration-300 group-hover:scale-[1.02] dark:ring-white/10" />
          <span className="pointer-events-none absolute bottom-3 right-3 inline-flex items-center gap-1 rounded-md bg-background/90 px-2 py-1 text-xs font-medium opacity-0 shadow-sm ring-1 ring-border backdrop-blur transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100">
            <Maximize2 className="size-3" />Click to enlarge
          </span>
        </button>
      : <div className="flex aspect-square w-full flex-col items-center justify-center gap-3 bg-muted/30 p-6 text-center">
          <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground"><ImageOff className="size-5" /></div>
          <div>
            <p className="text-sm font-medium">{broken ? "The image couldn't be loaded" : 'No image yet'}</p>
            <p className="mt-0.5 text-xs text-muted-foreground">{broken ? 'The link may be broken or the file was removed.' : field.description || 'Add one to make this record easier to recognize.'}</p>
          </div>
          {editPath && <Button asChild variant="outline" size="sm"><Link to={editPath}><Upload />{broken ? 'Replace image' : 'Upload image'}</Link></Button>}
        </div>}

    {url && <div className="flex items-center gap-2 border-t px-4 py-2.5 text-xs text-muted-foreground">
      <span className="min-w-0 flex-1 truncate" title={url}>{fileNameFromUrl(url)}</span>
      <a href={url} target="_blank" rel="noopener noreferrer" className="inline-flex shrink-0 items-center gap-1 font-medium text-foreground hover:underline">
        Open<ExternalLink className="size-3" />
      </a>
    </div>}
    {url && <Lightbox url={url} title={title} open={open} onOpenChange={setOpen} />}
  </Card>;
}

/** Record thumbnail for page headers; falls back to the given icon when there is no usable image. */
export function RecordThumbnail({ url, fallback }: { url: string | null; fallback: React.ReactNode }) {
  const [broken, setBroken] = useState(false);
  if (!url || broken) return <>{fallback}</>;
  return <img src={url} alt="" onError={() => setBroken(true)} className="size-full object-cover" />;
}
