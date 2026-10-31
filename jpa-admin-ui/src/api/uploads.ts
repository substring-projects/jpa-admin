import { adminFetch, csrfHeaders } from './auth';
import { ApiError, failure } from './client';
import { API_BASE } from '@/lib/base-path';
import type { UploadMetadata } from '@/types/metadata';

interface Ticket {
  token: string;
  method: 'PUT' | 'POST';
  url: string;
  headers: Record<string, string>;
  fields: Record<string, string>;
  fileField: string | null;
}

export type UploadPhase = 'preparing' | 'uploading' | 'verifying';

interface Options {
  onPhase: (phase: UploadPhase) => void;
  onProgress: (loaded: number, total: number) => void;
  signal: AbortSignal;
}

const EXTENSION_TYPES: Record<string, string> = {
  png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', gif: 'image/gif', webp: 'image/webp', avif: 'image/avif', svg: 'image/svg+xml',
  pdf: 'application/pdf', csv: 'text/csv', txt: 'text/plain', json: 'application/json', zip: 'application/zip', mp4: 'video/mp4',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
};

/** Browsers leave `type` empty for some files (often CSV on Windows), so fall back to the extension. */
export function fileType(file: File): string {
  if (file.type) return file.type.toLowerCase();
  const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
  return EXTENSION_TYPES[ext] ?? 'application/octet-stream';
}

/** Mirrors the server rule: exact types and `type/*` wildcards, with SVG only when listed explicitly. */
export function accepts(rules: UploadMetadata, type: string): boolean {
  return rules.accept.some(rule => {
    const r = rule.toLowerCase();
    if (r === type) return true;
    return r.endsWith('/*') && type.startsWith(r.slice(0, -1)) && type !== 'image/svg+xml';
  });
}

export class UploadAborted extends Error {
  constructor() { super('Upload cancelled'); }
}

/** Sends the file with XMLHttpRequest, because fetch can't report upload progress. */
function send(ticket: Ticket, file: File, onProgress: Options['onProgress'], signal: AbortSignal, extraHeaders: Record<string, string>): Promise<string> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(ticket.method, ticket.url);
    Object.entries({ ...ticket.headers, ...extraHeaders }).forEach(([name, value]) => xhr.setRequestHeader(name, value));
    xhr.upload.onprogress = event => { if (event.lengthComputable) onProgress(event.loaded, event.total); };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) { resolve(xhr.responseText); return; }
      let message = `The storage rejected the upload (HTTP ${xhr.status})`;
      try { const body = JSON.parse(xhr.responseText); message = body.error || body.message || message; } catch { /* non-JSON error body */ }
      reject(new ApiError(message, xhr.status));
    };
    xhr.onerror = () => reject(new ApiError(sameOrigin(ticket.url)
      ? 'Network error. Check your connection and try again.'
      : "Couldn't reach the file storage. Check your connection, or ask an administrator to allow uploads from this site (CORS).", 0));
    xhr.onabort = () => reject(new UploadAborted());
    signal.addEventListener('abort', () => xhr.abort(), { once: true });

    if (ticket.method === 'PUT') {
      xhr.send(file);
    } else {
      const form = new FormData();
      Object.entries(ticket.fields).forEach(([name, value]) => form.append(name, value));
      form.append(ticket.fileField || 'file', file, file.name);
      xhr.send(form);
    }
  });
}

const sameOrigin = (url: string) => new URL(url, window.location.href).origin === window.location.origin;

/** Prepare → upload with progress → server verification. Resolves with the URL to save on the record. */
export async function uploadFile(entity: string, field: string, file: File, { onPhase, onProgress, signal }: Options): Promise<string> {
  onPhase('preparing');
  const prepared = await adminFetch(`${API_BASE}/uploads/prepare`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ entity, field, fileName: file.name, contentType: fileType(file), size: file.size }),
    signal,
  });
  if (!prepared.ok) throw await failure(prepared, 'Could not start the upload');
  const ticket: Ticket = await prepared.json();

  onPhase('uploading');
  onProgress(0, file.size);
  const response = await send(ticket, file, onProgress, signal, sameOrigin(ticket.url) ? await csrfHeaders() : {});

  onPhase('verifying');
  let providerData: Record<string, string> = {};
  try {
    const body = JSON.parse(response);
    if (body && typeof body.fileId === 'string') providerData = { fileId: body.fileId };
  } catch { /* PUT uploads return an empty body */ }
  const completed = await adminFetch(`${API_BASE}/uploads/complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token: ticket.token, providerData }),
    signal,
  });
  if (!completed.ok) throw await failure(completed, 'The uploaded file could not be verified');
  return (await completed.json()).url;
}
