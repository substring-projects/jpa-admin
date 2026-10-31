import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { AlertTriangle, FileQuestion, RotateCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';

export function PageHeader({ title, description, icon, actions }: { title: ReactNode; description?: ReactNode; icon?: ReactNode; actions?: ReactNode }) {
  return <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
    <div className="flex min-w-0 items-start gap-3">
      {icon && <div className="flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-lg border bg-card text-muted-foreground shadow-xs [&_svg]:size-5">{icon}</div>}
      <div className="min-w-0">
        <h1 className="truncate text-xl font-semibold tracking-tight sm:text-2xl">{title}</h1>
        {description && <div className="mt-0.5 text-sm text-muted-foreground">{description}</div>}
      </div>
    </div>
    {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
  </div>;
}

export function ErrorState({ title, description, onRetry }: { title: string; description?: string; onRetry?: () => void }) {
  return <Empty className="min-h-[60vh]">
    <EmptyHeader>
      <EmptyMedia variant="icon" className="bg-destructive/10 text-destructive"><AlertTriangle /></EmptyMedia>
      <EmptyTitle>{title}</EmptyTitle>
      {description && <EmptyDescription className="break-words">{description}</EmptyDescription>}
    </EmptyHeader>
    {onRetry && <EmptyContent><Button variant="outline" onClick={onRetry}><RotateCw />Try again</Button></EmptyContent>}
  </Empty>;
}

export function NotFound({ message = "The page you're looking for doesn't exist or was moved." }: { message?: string }) {
  return <Empty className="min-h-[60vh]">
    <EmptyHeader>
      <EmptyMedia variant="icon"><FileQuestion /></EmptyMedia>
      <EmptyTitle>Not found</EmptyTitle>
      <EmptyDescription>{message}</EmptyDescription>
    </EmptyHeader>
    <EmptyContent><Button asChild variant="outline"><Link to="/">Back to dashboard</Link></Button></EmptyContent>
  </Empty>;
}
