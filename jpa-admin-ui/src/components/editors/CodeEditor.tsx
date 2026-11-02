import { useEffect, useRef } from 'react';
import { basicSetup } from 'codemirror';
import { EditorView } from '@codemirror/view';
import { Compartment, EditorState, type Extension } from '@codemirror/state';
import { cn } from '@/lib/utils';

interface Props {
  id: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  invalid?: boolean;
  describedBy?: string;
  /** Syntax from @AdminWidget(language); unknown values fall back to plain text. */
  language?: string;
}

// Each grammar loads on first use so forms without that language don't download it.
const LANGUAGES: Record<string, () => Promise<Extension>> = {
  json: () => import('@codemirror/lang-json').then(m => m.json()),
  javascript: () => import('@codemirror/lang-javascript').then(m => m.javascript()),
  typescript: () => import('@codemirror/lang-javascript').then(m => m.javascript({ typescript: true })),
  html: () => import('@codemirror/lang-html').then(m => m.html()),
  css: () => import('@codemirror/lang-css').then(m => m.css()),
  sql: () => import('@codemirror/lang-sql').then(m => m.sql()),
  xml: () => import('@codemirror/lang-xml').then(m => m.xml()),
  markdown: () => import('@codemirror/lang-markdown').then(m => m.markdown()),
  yaml: () => import('@codemirror/lang-yaml').then(m => m.yaml()),
  python: () => import('@codemirror/lang-python').then(m => m.python()),
};

const theme = EditorView.theme({
  '&': { fontSize: '12px', backgroundColor: 'transparent', color: 'var(--foreground)' },
  '.cm-content': { fontFamily: 'var(--font-mono)', caretColor: 'var(--foreground)', minHeight: '10rem' },
  '.cm-gutters': { backgroundColor: 'transparent', color: 'var(--muted-foreground)', borderRight: '1px solid var(--border)' },
  '.cm-activeLine, .cm-activeLineGutter': { backgroundColor: 'color-mix(in oklch, var(--muted) 60%, transparent)' },
  '&.cm-focused': { outline: 'none' },
  '.cm-selectionBackground, &.cm-focused .cm-selectionBackground': { backgroundColor: 'color-mix(in oklch, var(--primary) 25%, transparent)' },
});

export default function CodeEditor({ id, value, onChange, disabled, invalid, describedBy, language = 'json' }: Props) {
  const host = useRef<HTMLDivElement>(null);
  const view = useRef<EditorView | null>(null);
  const editable = useRef(new Compartment());
  const syntax = useRef(new Compartment());
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
          syntax.current.of([]),
          EditorView.contentAttributes.of({ id, 'aria-label': `Code (${language})`, ...(describedBy ? { 'aria-describedby': describedBy } : {}) }),
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
    let active = true;
    const load = LANGUAGES[language];
    void (load ? load() : Promise.resolve<Extension>([])).then(extension => {
      if (active) view.current?.dispatch({ effects: syntax.current.reconfigure(extension) });
    });
    return () => { active = false; };
  }, [language]);

  useEffect(() => {
    const current = view.current;
    if (current && !current.hasFocus && current.state.doc.toString() !== value) {
      current.dispatch({ changes: { from: 0, to: current.state.doc.length, insert: value } });
    }
  }, [value]);

  return <div ref={host} className={cn('overflow-hidden rounded-md border shadow-xs focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50 dark:bg-input/30',
    invalid && 'border-destructive', disabled && 'opacity-50')} />;
}
