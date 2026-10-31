import { API_BASE } from '@/lib/base-path';

export interface AdminSession {
  authenticated: boolean;
  username: string;
  title: string;
  csrfToken: string;
  csrfHeader: string;
  logoUrl?: string;
  primaryColor?: string;
  exportEnabled?: boolean;
}
let session: AdminSession | null = null;

export async function fetchSession(): Promise<AdminSession> {
  const response = await fetch(`${API_BASE}/auth/session`, { credentials: 'same-origin', cache: 'no-store' });
  if (!response.ok) throw new Error('Unable to connect to the admin server. Please try again.');
  session = await response.json();
  return session!;
}

export async function login(username: string, password: string): Promise<AdminSession> {
  const current = await fetchSession();
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST', credentials: 'same-origin',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', [current.csrfHeader]: current.csrfToken },
    body: new URLSearchParams({ username, password }),
  });
  if (!response.ok) throw new Error(response.status === 401 ? 'Invalid username or password.' : 'Unable to sign in. Please try again.');
  return fetchSession(); // Authentication rotates the session and CSRF token.
}

export async function logout() {
  const current = await fetchSession();
  const response = await fetch(`${API_BASE}/auth/logout`, {
    method: 'POST', credentials: 'same-origin', headers: { [current.csrfHeader]: current.csrfToken },
  });
  if (!response.ok) throw new Error('Unable to sign out. Please try again.');
  session = null;
}

/** CSRF header for requests the browser sends to the admin API outside adminFetch (for example XHR uploads). */
export async function csrfHeaders(): Promise<Record<string, string>> {
  const current = session || await fetchSession();
  return { [current.csrfHeader]: current.csrfToken };
}

export async function adminFetch(url: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers);
  if (init.method && !['GET', 'HEAD', 'OPTIONS'].includes(init.method.toUpperCase())) {
    const current = session || await fetchSession();
    headers.set(current.csrfHeader, current.csrfToken);
  }
  const response = await fetch(url, { ...init, headers, credentials: 'same-origin' });
  if (response.status === 401) {
    session = null;
    window.dispatchEvent(new Event('admin-session-expired'));
    throw new Error('Your session has expired. Please sign in again.');
  }
  if (response.status === 403) {
    // Do not replay mutations. Refresh the token for an explicit user retry.
    const current = await fetchSession();
    if (!current.authenticated) window.dispatchEvent(new Event('admin-session-expired'));
    throw new Error(current.authenticated ? 'Request could not be verified. Please try again.' : 'Your session has expired. Please sign in again.');
  }
  return response;
}
