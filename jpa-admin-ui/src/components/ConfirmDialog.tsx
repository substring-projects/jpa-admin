import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import {
  AlertDialog, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';

export interface ConfirmRequest {
  title: string;
  description: string;
  confirmLabel: string;
  destructive?: boolean;
  onConfirm: () => Promise<unknown>;
}

/** Keeps the dialog open while the action runs so failures can be retried in place. */
export function ConfirmDialog({ request, onClose }: { request: ConfirmRequest | null; onClose: () => void }) {
  const [pending, setPending] = useState(false);
  const confirm = async () => {
    if (!request || pending) return;
    setPending(true);
    try { await request.onConfirm(); onClose(); }
    catch { /* the action reports its own error */ }
    finally { setPending(false); }
  };
  return <AlertDialog open={request !== null} onOpenChange={open => { if (!open && !pending) onClose(); }}>
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>{request?.title}</AlertDialogTitle>
        <AlertDialogDescription>{request?.description}</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel disabled={pending}>Cancel</AlertDialogCancel>
        <Button variant={request?.destructive ? 'destructive' : 'default'} disabled={pending} onClick={confirm}>
          {pending && <Loader2 className="animate-spin" />}{request?.confirmLabel}
        </Button>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>;
}
