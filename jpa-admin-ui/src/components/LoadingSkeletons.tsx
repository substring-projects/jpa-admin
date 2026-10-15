import { Card, CardContent, CardHeader } from './ui/card';
import { Skeleton } from './ui/skeleton';
import { TableCell, TableRow } from './ui/table';

export function DashboardSkeleton() {
  return <div role="status" aria-label="Loading dashboard" className="p-4 sm:p-8 max-w-7xl mx-auto w-full space-y-6">
    <span className="sr-only">Loading dashboard…</span>
    <Skeleton className="h-52 w-full rounded-2xl" />
    <div className="grid gap-4 sm:grid-cols-3">{Array.from({ length: 3 }, (_, index) => <Card key={index} className="p-6 space-y-3"><Skeleton className="h-8 w-16" /><Skeleton className="h-4 w-28" /></Card>)}</div>
    <Skeleton className="h-6 w-48" />
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{Array.from({ length: 6 }, (_, index) => <Card key={index} className="p-6 space-y-5"><Skeleton className="h-10 w-10" /><Skeleton className="h-5 w-32" /><Skeleton className="h-4 w-4/5" /><Skeleton className="h-9 w-full" /></Card>)}</div>
  </div>;
}

export function FormSkeleton() {
  return <Card role="status" aria-label="Loading record">
    <span className="sr-only">Loading record…</span>
    <CardHeader className="space-y-3"><Skeleton className="h-5 w-40" /><Skeleton className="h-4 w-3/4 max-w-72" /></CardHeader>
    <CardContent className="grid gap-6 sm:grid-cols-2">{Array.from({ length: 6 }, (_, index) => <div key={index} className="space-y-2"><Skeleton className="h-4 w-24" /><Skeleton className="h-10 w-full" /></div>)}</CardContent>
  </Card>;
}

export function TableSkeleton({ columns }: { columns: number }) {
  return <>{Array.from({ length: 5 }, (_, row) => <TableRow key={row} aria-hidden="true">{Array.from({ length: columns }, (_, column) => <TableCell key={column}><Skeleton className={column === 0 ? 'h-5 w-5' : 'h-5 w-full min-w-20'} /></TableCell>)}</TableRow>)}</>;
}
