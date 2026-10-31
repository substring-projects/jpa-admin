import { adminFetch } from './auth';
import { EntityMetadata, PageResult } from '../types/metadata';
import { API_BASE as BASE_URL } from '@/lib/base-path';

/** A failed request; `fieldErrors` maps field paths such as `price` or `items[0].quantity` to messages. */
export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly fieldErrors: Record<string, string> = {}) {
    super(message);
  }
}

export async function failure(res: Response, fallback: string): Promise<ApiError> {
  const body = await res.json().catch(() => ({}));
  return new ApiError(body.error || fallback, res.status, body.fieldErrors || {});
}

export interface Timeline { days: string[]; series: Record<string, number[]> }

export async function fetchTimeline(days = 30): Promise<Timeline> {
  const res = await adminFetch(`${BASE_URL}/stats/timeline?days=${days}`);
  if (!res.ok) throw await failure(res, 'Failed to load activity');
  return res.json();
}

export async function fetchMetadata(): Promise<EntityMetadata[]> {
  const res = await adminFetch(`${BASE_URL}/metadata`);
  if (!res.ok) throw new Error(`Failed to load metadata: ${res.statusText}`);
  return res.json();
}

export async function fetchStats(): Promise<Record<string, number>> {
  const res = await adminFetch(`${BASE_URL}/stats`);
  if (!res.ok) throw new Error('Failed to load record counts');
  const data = await res.json();
  return data.counts || {};
}

export async function fetchRecords(
  entity: string,
  page = 0,
  size = 25,
  sort?: string,
  q?: string,
  filters: Record<string, string> = {}
): Promise<PageResult<Record<string, any>>> {
  const params = new URLSearchParams();
  params.set('page', page.toString());
  params.set('size', size.toString());
  if (sort) params.set('sort', sort);
  if (q && q.trim()) params.set('q', q.trim());

  Object.entries(filters).forEach(([key, val]) => {
    if (val !== undefined && val !== null && val !== '') {
      params.set(key, val);
    }
  });

  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}?${params.toString()}`);
  if (!res.ok) throw await failure(res, `Failed to load records for ${entity}`);
  return res.json();
}

export async function getRecord(entity: string, id: any): Promise<Record<string, any>> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/${encodeURIComponent(String(id))}`);
  if (!res.ok) throw await failure(res, `Failed to load ${entity} #${id}`);
  return res.json();
}

export async function createRecord(entity: string, payload: Record<string, any>): Promise<Record<string, any>> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw await failure(res, `Failed to create ${entity}`);
  return res.json();
}

export async function updateRecord(entity: string, id: any, payload: Record<string, any>): Promise<Record<string, any>> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/${encodeURIComponent(String(id))}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw await failure(res, `Failed to update ${entity} #${id}`);
  return res.json();
}

export async function deleteRecord(entity: string, id: any): Promise<void> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/${encodeURIComponent(String(id))}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw await failure(res, `Failed to delete ${entity} #${id}`);
}

export async function batchDeleteRecords(entity: string, ids: any[]): Promise<number> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/batch-delete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ids),
  });
  if (!res.ok) throw await failure(res, 'Batch delete failed');
  const data = await res.json();
  return data.deleted || 0;
}

export async function lookupRecords(entity: string, q = ''): Promise<Array<{ id: any; label: string }>> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/lookup?q=${encodeURIComponent(q)}&limit=25`);
  if (!res.ok) throw new Error(`Failed to load ${encodeURIComponent(entity)} options`);
  return res.json();
}

export async function executeAction(entity: string, actionId: string, ids: any[]): Promise<any> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/actions/${encodeURIComponent(actionId)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ids),
  });
  if (!res.ok) throw await failure(res, 'Action failed');
  return res.json();
}
