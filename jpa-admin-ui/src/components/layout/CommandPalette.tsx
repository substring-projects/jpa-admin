import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { History, Keyboard, LayoutDashboard, Monitor, Moon, Plus, Sun } from 'lucide-react';
import { openShortcuts } from './ShortcutsDialog';
import { useTheme } from '@/lib/theme';
import { useAdmin } from '@/context/AdminContext';
import { EntityIcon } from '@/components/DynamicIcon';
import { formatCount } from '@/lib/format';
import { useRecentRecords } from '@/lib/storage';
import {
  CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator, CommandShortcut,
} from '@/components/ui/command';

const OPEN_EVENT = 'jpa-admin-command-palette';
export const openCommandPalette = () => window.dispatchEvent(new Event(OPEN_EVENT));

export function CommandPalette() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const { metadata, counts, getEntity } = useAdmin();
  const recent = useRecentRecords();
  const { setTheme } = useTheme();

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey)) {
        event.preventDefault();
        setOpen(prev => !prev);
      }
    };
    const onOpen = () => setOpen(true);
    document.addEventListener('keydown', onKey);
    window.addEventListener(OPEN_EVENT, onOpen);
    return () => { document.removeEventListener('keydown', onKey); window.removeEventListener(OPEN_EVENT, onOpen); };
  }, []);

  const go = (path: string) => { setOpen(false); navigate(path); };
  const creatable = metadata.filter(e => e.creatable && !e.readOnly);
  const recentEntries = recent.filter(r => getEntity(r.entity)).slice(0, 5);

  return <CommandDialog open={open} onOpenChange={setOpen} title="Command palette" description="Jump to a page, record or action">
    <CommandInput placeholder="Search entities, records and actions…" />
    <CommandList>
      <CommandEmpty>No results found.</CommandEmpty>
      {recentEntries.length > 0 && <>
        <CommandGroup heading="Recently viewed">
          {recentEntries.map(r => {
            const entity = getEntity(r.entity)!;
            return <CommandItem key={`${r.entity}:${r.id}`} value={`recent ${entity.displayName} ${r.label} ${r.id}`} onSelect={() => go(`/${entity.entityName}/${encodeURIComponent(r.id)}`)}>
              <History />
              <span className="truncate">{r.label}</span>
              <span className="text-xs text-muted-foreground">{entity.displayName}</span>
            </CommandItem>;
          })}
        </CommandGroup>
        <CommandSeparator />
      </>}
      <CommandGroup heading="Navigate">
        <CommandItem value="dashboard overview home" onSelect={() => go('/')}><LayoutDashboard />Dashboard</CommandItem>
        {metadata.map(entity => <CommandItem key={entity.entityName} value={`go ${entity.pluralName} ${entity.displayName} ${entity.group}`} onSelect={() => go(`/${entity.entityName}`)}>
          <EntityIcon entity={entity} />
          <span>{entity.pluralName || entity.displayName}</span>
          {counts?.[entity.entityName] != null && <CommandShortcut className="tabular">{formatCount(counts[entity.entityName])}</CommandShortcut>}
        </CommandItem>)}
      </CommandGroup>
      {creatable.length > 0 && <>
        <CommandSeparator />
        <CommandGroup heading="Create">
          {creatable.map(entity => <CommandItem key={entity.entityName} value={`new create add ${entity.displayName}`} onSelect={() => go(`/${entity.entityName}/new`)}>
            <Plus />New {entity.displayName}
          </CommandItem>)}
        </CommandGroup>
      </>}
      <CommandSeparator />
      <CommandGroup heading="Help">
        <CommandItem value="keyboard shortcuts help" onSelect={() => { setOpen(false); openShortcuts(); }}><Keyboard />Keyboard shortcuts<CommandShortcut>?</CommandShortcut></CommandItem>
      </CommandGroup>
      <CommandSeparator />
      <CommandGroup heading="Theme">
        <CommandItem value="theme light" onSelect={() => { setTheme('light'); setOpen(false); }}><Sun />Light theme</CommandItem>
        <CommandItem value="theme dark" onSelect={() => { setTheme('dark'); setOpen(false); }}><Moon />Dark theme</CommandItem>
        <CommandItem value="theme system" onSelect={() => { setTheme('system'); setOpen(false); }}><Monitor />System theme</CommandItem>
      </CommandGroup>
    </CommandList>
  </CommandDialog>;
}
