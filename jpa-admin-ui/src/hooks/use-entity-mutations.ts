import { useState } from 'react';
import { toast } from 'sonner';
import { batchDeleteRecords, deleteRecord, executeAction } from '@/api/client';
import type { ConfirmRequest } from '@/components/ConfirmDialog';
import { useAdmin } from '@/context/AdminContext';
import { forgetRecord } from '@/lib/storage';
import type { ActionMetadata, EntityMetadata } from '@/types/metadata';

const message = (err: unknown, fallback: string) => (err instanceof Error && err.message) || fallback;

export function useEntityMutations(entity: EntityMetadata, onChanged: (change: 'delete' | 'action') => void) {
  const { refreshCounts } = useAdmin();
  const [confirm, setConfirm] = useState<ConfirmRequest | null>(null);

  const remove = async (ids: unknown[]) => {
    try {
      const deleted = ids.length === 1 ? (await deleteRecord(entity.entityName, ids[0]), 1) : await batchDeleteRecords(entity.entityName, ids);
      ids.forEach(id => forgetRecord(entity.entityName, String(id)));
      toast.success(deleted === 1 ? `${entity.displayName} deleted` : `${deleted} ${entity.pluralName || 'records'} deleted`);
      refreshCounts();
      onChanged('delete');
    } catch (err) {
      toast.error(message(err, 'Delete failed'));
      throw err;
    }
  };

  const run = async (action: ActionMetadata, ids: unknown[]) => {
    try {
      const res = await executeAction(entity.entityName, action.id, ids);
      toast.success(typeof res?.result === 'string' ? res.result : `${action.name} completed`);
      refreshCounts();
      onChanged('action');
    } catch (err) {
      toast.error(message(err, `${action.name} failed`));
      throw err;
    }
  };

  const requestDelete = (ids: unknown[]) => setConfirm({
    title: ids.length === 1 ? `Delete this ${entity.displayName.toLowerCase()}?` : `Delete ${ids.length} ${(entity.pluralName || 'records').toLowerCase()}?`,
    description: 'This permanently removes the data and cannot be undone.',
    confirmLabel: ids.length === 1 ? 'Delete' : `Delete ${ids.length}`,
    destructive: true,
    onConfirm: () => remove(ids),
  });

  const requestAction = (action: ActionMetadata, ids: unknown[]) => {
    if (!action.confirmation) { void run(action, ids).catch(() => undefined); return; }
    setConfirm({
      title: action.name,
      description: action.confirmation,
      confirmLabel: ids.length > 1 ? `Run on ${ids.length} records` : 'Run action',
      onConfirm: () => run(action, ids),
    });
  };

  return { confirm, closeConfirm: () => setConfirm(null), requestDelete, requestAction };
}
