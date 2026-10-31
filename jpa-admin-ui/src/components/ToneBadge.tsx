import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

const TONES: Record<string, string> = {
  GREEN: 'bg-emerald-500/10 text-emerald-700 ring-emerald-600/20 dark:text-emerald-400 dark:ring-emerald-400/20',
  BLUE: 'bg-sky-500/10 text-sky-700 ring-sky-600/20 dark:text-sky-400 dark:ring-sky-400/20',
  YELLOW: 'bg-amber-500/10 text-amber-700 ring-amber-600/25 dark:text-amber-400 dark:ring-amber-400/20',
  RED: 'bg-rose-500/10 text-rose-700 ring-rose-600/20 dark:text-rose-400 dark:ring-rose-400/20',
  PURPLE: 'bg-violet-500/10 text-violet-700 ring-violet-600/20 dark:text-violet-400 dark:ring-violet-400/20',
  INDIGO: 'bg-indigo-500/10 text-indigo-700 ring-indigo-600/20 dark:text-indigo-400 dark:ring-indigo-400/20',
  PINK: 'bg-pink-500/10 text-pink-700 ring-pink-600/20 dark:text-pink-400 dark:ring-pink-400/20',
  PRIMARY: 'bg-primary/10 text-primary ring-primary/25',
  GRAY: 'bg-muted text-muted-foreground ring-border',
  DEFAULT: 'bg-muted text-muted-foreground ring-border',
};

const DOTS: Record<string, string> = {
  GREEN: 'bg-emerald-500', BLUE: 'bg-sky-500', YELLOW: 'bg-amber-500', RED: 'bg-rose-500', PURPLE: 'bg-violet-500',
  INDIGO: 'bg-indigo-500', PINK: 'bg-pink-500', PRIMARY: 'bg-primary', GRAY: 'bg-muted-foreground', DEFAULT: 'bg-muted-foreground',
};

export function ToneBadge({ tone, children, className }: { tone: string; children: ReactNode; className?: string }) {
  const key = tone?.toUpperCase() in TONES ? tone.toUpperCase() : 'DEFAULT';
  return <span className={cn('inline-flex max-w-full items-center gap-1.5 whitespace-nowrap rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset', TONES[key], className)}>
    <span aria-hidden className={cn('size-1.5 shrink-0 rounded-full', DOTS[key])} />
    <span className="truncate">{children}</span>
  </span>;
}
