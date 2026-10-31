/**
 * The server writes `<base href="{spring.jpa.admin.path}/">` into index.html, so the same build works under any prefix.
 * Example: "/admin".
 */
export const ADMIN_BASE = new URL(document.baseURI).pathname.replace(/\/+$/, '');
export const API_BASE = `${ADMIN_BASE}/api`;
export const adminUrl = (path: string) => `${ADMIN_BASE}${path.startsWith('/') ? path : `/${path}`}`;
