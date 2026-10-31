import { useEffect, useState } from 'react';
import { useHotkeys } from '@/hooks/use-hotkeys';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Kbd, KbdGroup } from '@/components/ui/kbd';

const OPEN_EVENT = 'jpa-admin-shortcuts';
export const openShortcuts = () => window.dispatchEvent(new Event(OPEN_EVENT));

const SECTIONS: Array<{ title: string; items: Array<[string[], string]> }> = [
  { title: 'Anywhere', items: [[['⌘', 'K'], 'Open the command palette'], [['?'], 'Show keyboard shortcuts'], [['⌘', 'B'], 'Toggle the sidebar']] },
  { title: 'Record lists', items: [
    [['/'], 'Focus search'], [['n'], 'New record'], [['j'], 'Next row'], [['k'], 'Previous row'],
    [['Enter'], 'Open the focused row'], [['e'], 'Edit the focused row'], [['x'], 'Select the focused row'],
  ] },
  { title: 'Record pages', items: [[['e'], 'Edit this record'], [['Esc'], 'Close menus and dialogs']] },
];

export function ShortcutsDialog() {
  const [open, setOpen] = useState(false);
  useHotkeys({ '?': () => setOpen(true) });
  useEffect(() => {
    const show = () => setOpen(true);
    window.addEventListener(OPEN_EVENT, show);
    return () => window.removeEventListener(OPEN_EVENT, show);
  }, []);
  return <Dialog open={open} onOpenChange={setOpen}>
    <DialogContent className="sm:max-w-md">
      <DialogHeader>
        <DialogTitle>Keyboard shortcuts</DialogTitle>
        <DialogDescription>Shortcuts are ignored while you type in a field.</DialogDescription>
      </DialogHeader>
      <div className="space-y-5">
        {SECTIONS.map(section => <section key={section.title}>
          <h3 className="mb-2 text-xs font-medium text-muted-foreground">{section.title}</h3>
          <dl className="space-y-1.5">
            {section.items.map(([keys, label]) => <div key={label} className="flex items-center justify-between gap-4 text-sm">
              <dt>{label}</dt>
              <dd><KbdGroup>{keys.map(k => <Kbd key={k}>{k}</Kbd>)}</KbdGroup></dd>
            </div>)}
          </dl>
        </section>)}
      </div>
    </DialogContent>
  </Dialog>;
}
