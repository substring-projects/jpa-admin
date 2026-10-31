import { useState, type FormEvent } from 'react';
import { Bookmark, BookmarkPlus, Check, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useStored } from '@/lib/storage';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export interface SavedView { name: string; query: string }

const canonical = (query: string) => {
  const params = new URLSearchParams(query);
  params.delete('page');
  params.sort();
  return params.toString();
};

/** Named search/filter/sort combinations, kept per entity in this browser. */
export function SavedViews({ entity, query, onApply }: { entity: string; query: string; onApply: (query: string) => void }) {
  const [views, setViews] = useStored<SavedView[]>(`jpa-admin-views:${entity}`, []);
  const [naming, setNaming] = useState(false);
  const [name, setName] = useState('');
  const current = canonical(query);
  const active = views.find(v => v.query === current);

  const save = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return;
    setViews([...views.filter(v => v.name.toLowerCase() !== trimmed.toLowerCase()), { name: trimmed, query: current }]
      .sort((a, b) => a.name.localeCompare(b.name)));
    setNaming(false);
    setName('');
    toast.success(`Saved view “${trimmed}”`);
  };

  return <>
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm"><Bookmark />{active ? <span className="max-w-32 truncate">{active.name}</span> : 'Views'}</Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-60">
        <DropdownMenuLabel>Saved views</DropdownMenuLabel>
        {views.length === 0 && <p className="px-2 pb-2 text-xs text-muted-foreground">Save the current search, filters and sort to reuse them later.</p>}
        {views.map(view => <DropdownMenuItem key={view.name} onSelect={() => onApply(view.query)} className="group/view">
          <Check className={view === active ? 'opacity-100' : 'opacity-0'} />
          <span className="flex-1 truncate">{view.name}</span>
          <button type="button" aria-label={`Delete view ${view.name}`}
            onClick={e => { e.preventDefault(); e.stopPropagation(); setViews(views.filter(v => v !== view)); }}
            className="rounded p-0.5 text-muted-foreground opacity-0 hover:text-destructive group-hover/view:opacity-100 group-focus/view:opacity-100">
            <Trash2 className="size-3.5" />
          </button>
        </DropdownMenuItem>)}
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled={!current || !!active} onSelect={() => setNaming(true)}><BookmarkPlus />Save current view…</DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
    <Dialog open={naming} onOpenChange={setNaming}>
      <DialogContent className="sm:max-w-sm">
        <form onSubmit={save} className="grid gap-4">
          <DialogHeader>
            <DialogTitle>Save view</DialogTitle>
            <DialogDescription>Keeps the current search, filters and sort order on this device.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-2">
            <Label htmlFor="view-name">Name</Label>
            <Input id="view-name" autoFocus maxLength={40} value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Out of stock" />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setNaming(false)}>Cancel</Button>
            <Button type="submit" disabled={!name.trim()}>Save view</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </>;
}
