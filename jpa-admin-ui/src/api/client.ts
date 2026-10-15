import { adminFetch } from './auth';
import { EntityMetadata, PageResult } from '../types/metadata';

// In production, served from same origin under /admin/api
const BASE_URL = '/admin/api';

export async function fetchMetadata(): Promise<EntityMetadata[]> {
  const res = await adminFetch(`${BASE_URL}/metadata`);
  if (!res.ok) throw new Error(`Failed to load metadata: ${res.statusText}`);
  return res.json();
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
  if (!res.ok) throw new Error(`Failed to load records for ${encodeURIComponent(entity)}`);
  return res.json();
}

export async function getRecord(entity: string, id: any): Promise<Record<string, any>> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/${encodeURIComponent(String(id))}`);
  if (!res.ok) throw new Error(`Failed to load ${encodeURIComponent(entity)} record #${encodeURIComponent(String(id))}`);
  return res.json();
}

export async function createRecord(entity: string, payload: Record<string, any>): Promise<Record<string, any>> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const errorJson = await res.json().catch(() => ({}));
    throw new Error(errorJson.error || `Failed to create ${encodeURIComponent(entity)}`);
  }
  return res.json();
}

export async function updateRecord(entity: string, id: any, payload: Record<string, any>): Promise<Record<string, any>> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/${encodeURIComponent(String(id))}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const errorJson = await res.json().catch(() => ({}));
    throw new Error(errorJson.error || `Failed to update ${encodeURIComponent(entity)} #${encodeURIComponent(String(id))}`);
  }
  return res.json();
}

export async function deleteRecord(entity: string, id: any): Promise<void> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/${encodeURIComponent(String(id))}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error(`Failed to delete ${encodeURIComponent(entity)} #${encodeURIComponent(String(id))}`);
}

export async function batchDeleteRecords(entity: string, ids: any[]): Promise<number> {
  const res = await adminFetch(`${BASE_URL}/${encodeURIComponent(entity)}/batch-delete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ids),
  });
  if (!res.ok) throw new Error(`Batch delete failed`);
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
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `Action failed`);
  }
  return res.json();
}
