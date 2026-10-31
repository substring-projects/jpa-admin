import { useState } from 'react';
import { Database } from 'lucide-react';
import { cn } from '@/lib/utils';

export function BrandMark({ logoUrl, title, className }: { logoUrl: string; title: string; className?: string }) {
  const [failed, setFailed] = useState(false);
  if (logoUrl && !failed) {
    return <img src={logoUrl} alt={`${title} logo`} onError={() => setFailed(true)}
      className={cn('size-8 shrink-0 rounded-lg object-contain', className)} />;
  }
  return <div className={cn('flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground', className)}>
    <Database className="size-4" />
  </div>;
}
