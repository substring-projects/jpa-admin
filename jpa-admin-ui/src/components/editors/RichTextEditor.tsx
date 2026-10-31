import { useEffect } from 'react';
import { EditorContent, useEditor, useEditorState } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { Bold, Code, Heading2, Italic, List, ListOrdered, Quote, Redo2, Strikethrough, Undo2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { sanitizeHtml } from '@/lib/sanitize';

interface Props {
  id: string;
  value: string;
  onChange: (html: string) => void;
  disabled?: boolean;
  invalid?: boolean;
  describedBy?: string;
}

export default function RichTextEditor({ id, value, onChange, disabled, invalid, describedBy }: Props) {
  const editor = useEditor({
    extensions: [StarterKit.configure({ heading: { levels: [2, 3] }, trailingNode: false })],
    content: sanitizeHtml(value),
    editable: !disabled,
    editorProps: {
      attributes: {
        id,
        role: 'textbox',
        'aria-multiline': 'true',
        ...(describedBy ? { 'aria-describedby': describedBy } : {}),
        ...(invalid ? { 'aria-invalid': 'true' } : {}),
        class: 'rich-text min-h-32 px-3 py-2 text-sm outline-none',
      },
    },
    // TipTap normalizes loaded HTML with an update of its own; only edits made while focused are user changes.
    onUpdate: ({ editor: e }) => { if (e.isFocused) onChange(e.isEmpty ? '' : e.getHTML()); },
  });

  useEffect(() => { if (editor && !editor.isDestroyed) editor.setEditable(!disabled); }, [editor, disabled]);
  useEffect(() => {
    // Sync external resets (loading a record, duplicating) without clobbering the caret while typing.
    if (editor && !editor.isDestroyed && !editor.isFocused && value !== editor.getHTML()) editor.commands.setContent(sanitizeHtml(value), { emitUpdate: false });
  }, [editor, value]);

  const state = useEditorState({
    editor,
    selector: ({ editor: e }) => e && !e.isDestroyed ? {
      bold: e.isActive('bold'), italic: e.isActive('italic'), strike: e.isActive('strike'), code: e.isActive('code'),
      h2: e.isActive('heading', { level: 2 }), bullet: e.isActive('bulletList'), ordered: e.isActive('orderedList'), quote: e.isActive('blockquote'),
      undo: e.can().undo(), redo: e.can().redo(),
    } : null,
  });

  const tools = editor && state ? [
    { label: 'Bold', icon: Bold, active: state.bold, run: () => editor.chain().focus().toggleBold().run() },
    { label: 'Italic', icon: Italic, active: state.italic, run: () => editor.chain().focus().toggleItalic().run() },
    { label: 'Strikethrough', icon: Strikethrough, active: state.strike, run: () => editor.chain().focus().toggleStrike().run() },
    { label: 'Inline code', icon: Code, active: state.code, run: () => editor.chain().focus().toggleCode().run() },
    { label: 'Heading', icon: Heading2, active: state.h2, run: () => editor.chain().focus().toggleHeading({ level: 2 }).run() },
    { label: 'Bulleted list', icon: List, active: state.bullet, run: () => editor.chain().focus().toggleBulletList().run() },
    { label: 'Numbered list', icon: ListOrdered, active: state.ordered, run: () => editor.chain().focus().toggleOrderedList().run() },
    { label: 'Quote', icon: Quote, active: state.quote, run: () => editor.chain().focus().toggleBlockquote().run() },
  ] : [];

  return <div className={cn('rounded-md border bg-transparent shadow-xs focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50 dark:bg-input/30',
    invalid && 'border-destructive ring-destructive/20', disabled && 'opacity-50')}>
    <div role="toolbar" aria-label="Formatting" className="flex flex-wrap items-center gap-0.5 border-b p-1">
      {tools.map(tool => <button key={tool.label} type="button" title={tool.label} aria-label={tool.label} aria-pressed={tool.active}
        disabled={disabled} onClick={tool.run}
        className={cn('inline-flex size-7 items-center justify-center rounded text-muted-foreground hover:bg-accent hover:text-foreground disabled:pointer-events-none',
          tool.active && 'bg-accent text-foreground')}>
        <tool.icon className="size-4" />
      </button>)}
      <span className="mx-1 h-5 w-px bg-border" />
      <button type="button" title="Undo" aria-label="Undo" disabled={disabled || !state?.undo} onClick={() => editor?.chain().focus().undo().run()}
        className="inline-flex size-7 items-center justify-center rounded text-muted-foreground hover:bg-accent hover:text-foreground disabled:opacity-40"><Undo2 className="size-4" /></button>
      <button type="button" title="Redo" aria-label="Redo" disabled={disabled || !state?.redo} onClick={() => editor?.chain().focus().redo().run()}
        className="inline-flex size-7 items-center justify-center rounded text-muted-foreground hover:bg-accent hover:text-foreground disabled:opacity-40"><Redo2 className="size-4" /></button>
    </div>
    <EditorContent editor={editor} />
  </div>;
}
