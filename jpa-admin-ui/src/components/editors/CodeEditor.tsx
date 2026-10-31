import { useEffect, useRef } from 'react';
import { basicSetup } from 'codemirror';
import { EditorView } from '@codemirror/view';
import { Compartment, EditorState } from '@codemirror/state';
import { cn } from '@/lib/utils';

interface Props {
  id: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  invalid?: boolean;
  describedBy?: string;
}

const theme = EditorView.theme({
  '&': { fontSize: '12px', backgroundColor: 'transparent', color: 'var(--foreground)' },
  '.cm-content': { fontFamily: 'var(--font-mono)', caretColor: 'var(--foreground)', minHeight: '10rem' },
  '.cm-gutters': { backgroundColor: 'transparent', color: 'var(--muted-foreground)', borderRight: '1px solid var(--border)' },
  '.cm-activeLine, .cm-activeLineGutter': { backgroundColor: 'color-mix(in oklch, var(--muted) 60%, transparent)' },
  '&.cm-focused': { outline: 'none' },
  '.cm-selectionBackground, &.cm-focused .cm-selectionBackground': { backgroundColor: 'color-mix(in oklch, var(--primary) 25%, transparent)' },
});

export default function CodeEditor({ id, value, onChange, disabled, invalid, describedBy }: Props) {
  const host = useRef<HTMLDivElement>(null);
  const view = useRef<EditorView | null>(null);
  const editable = useRef(new Compartment());
  const latestOnChange = useRef(onChange);
  latestOnChange.current = onChange;

  useEffect(() => {
    const instance = new EditorView({
      parent: host.current!,
      state: EditorState.create({
        doc: value,
        extensions: [
          basicSetup, theme, EditorView.lineWrapping,
          editable.current.of(EditorView.editable.of(!disabled)),
          EditorView.contentAttributes.of({ id, 'aria-label': 'Code', ...(describedBy ? { 'aria-describedby': describedBy } : {}) }),
          EditorView.updateListener.of(update => { if (update.docChanged) latestOnChange.current(update.state.doc.toString()); }),
        ],
      }),
    });
    view.current = instance;
    return () => instance.destroy();
  }, []);

  useEffect(() => {
    view.current?.dispatch({ effects: editable.current.reconfigure(EditorView.editable.of(!disabled)) });
  }, [disabled]);

  useEffect(() => {
    const current = view.current;
    if (current && !current.hasFocus && current.state.doc.toString() !== value) {
      current.dispatch({ changes: { from: 0, to: current.state.doc.length, insert: value } });
    }
  }, [value]);

  return <div ref={host} className={cn('overflow-hidden rounded-md border shadow-xs focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50 dark:bg-input/30',
    invalid && 'border-destructive', disabled && 'opacity-50')} />;
}
