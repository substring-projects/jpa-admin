import DOMPurify from 'dompurify';

/** Rich-text fields store HTML from the editor or other clients, so it is always sanitized before rendering. */
export function sanitizeHtml(html: string | null | undefined): string {
  if (!html) return '';
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true }, FORBID_TAGS: ['style', 'form', 'input', 'button'], FORBID_ATTR: ['style'] });
}
