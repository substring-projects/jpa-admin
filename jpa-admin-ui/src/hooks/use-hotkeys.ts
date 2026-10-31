import { useEffect, useRef } from 'react';

type Handler = (event: KeyboardEvent) => void;

const isTyping = (target: EventTarget | null) => {
  const el = target as HTMLElement | null;
  if (!el) return false;
  return el.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName) || !!el.closest('[role="combobox"],[role="listbox"],[role="menu"],.cm-editor');
};

/**
 * Single-key shortcuts (e.g. "n", "/", "?") that stay out of the way while typing or when a dialog or menu is open.
 * Handlers always see the latest render, so callers don't need to memoize them.
 */
export function useHotkeys(bindings: Record<string, Handler>, enabled = true) {
  const latest = useRef(bindings);
  latest.current = bindings;
  useEffect(() => {
    if (!enabled) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.metaKey || event.ctrlKey || event.altKey || event.defaultPrevented) return;
      if (isTyping(event.target)) return;
      if (document.querySelector('[role="dialog"][data-state="open"],[role="alertdialog"][data-state="open"],[role="menu"][data-state="open"]')) return;
      const handler = latest.current[event.key];
      if (handler) { event.preventDefault(); handler(event); }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [enabled]);
}
