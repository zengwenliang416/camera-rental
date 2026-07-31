import { DatabaseZap, LoaderCircle, ShieldCheck, Trash2 } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';

import {
  backfillRentalLogistics,
  cleanupRentalLogistics,
  type RentalLogisticsBackfillResultVO,
  type RentalLogisticsCleanupResultVO,
} from '../../../api/rental';
import { ConfirmDialogShell } from '../../../shared/ui/ConfirmDialogShell';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { OperationResultPanel } from '../../../shared/ui/OperationResultPanel';
import type { LocalePreference } from '../../preferences/preferenceModel';
import {
  DEFAULT_BACKFILL_COMMAND,
  DEFAULT_CLEANUP_COMMAND,
  normalizeBackfillCommand,
  normalizeCleanupCommand,
  type LogisticsOperationsAccess,
} from '../operationsModel';
import {
  operationsCopy,
  operationsErrorCopy,
} from '../operationsCopy';
import { useOperationsRequest } from '../useOperationsRequest';
import {
  fieldClassName,
  OperationsPanel,
  primaryButtonClassName,
  secondaryButtonClassName,
} from './OperationsPanel';

function PermissionBoundary({
  allowed,
  locale,
  children,
}: {
  allowed: boolean;
  locale: LocalePreference;
  children: ReactNode;
}) {
  if (allowed) return <>{children}</>;
  return (
    <EmptyState
      icon={<ShieldCheck className="h-4 w-4" />}
      title={operationsCopy(locale, 'common.permissionTitle')}
      description={operationsCopy(locale, 'common.permissionDetail')}
    />
  );
}

export function BackfillPanel({
  access,
  locale,
}: {
  access: LogisticsOperationsAccess;
  locale: LocalePreference;
}) {
  const request = useOperationsRequest<RentalLogisticsBackfillResultVO>();
  const [command, setCommand] = useState({ ...DEFAULT_BACKFILL_COMMAND });
  const [confirming, setConfirming] = useState(false);

  const run = async () => {
    const normalized = normalizeBackfillCommand(command);
    if (!normalized.dryRun && !confirming) {
      setConfirming(true);
      return;
    }
    setConfirming(false);
    await request.run(() => backfillRentalLogistics(normalized));
  };

  const result = request.state.data;
  return (
    <>
      <OperationsPanel
        title={operationsCopy(locale, 'backfill.title')}
        description={operationsCopy(locale, 'backfill.description')}
      >
        <PermissionBoundary allowed={access.canBackfill} locale={locale}>
          <div className="space-y-4">
            <label className="flex min-h-11 items-center gap-3 rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold">
              <input
                type="checkbox"
                checked={command.dryRun}
                onChange={(event) =>
                  setCommand({
                    ...command,
                    dryRun: event.target.checked,
                    enqueueProviderTasks: event.target.checked
                      ? false
                      : command.enqueueProviderTasks,
                  })}
                className="h-4 w-4 accent-[var(--sc-blue)]"
              />
              {operationsCopy(locale, 'backfill.dryRun')}
            </label>
            <label className="text-[10px] font-bold text-[var(--sc-ink-muted)]">
              {operationsCopy(locale, 'backfill.limit')}
              <input
                type="number"
                min={1}
                max={100}
                value={command.limit}
                onChange={(event) =>
                  setCommand({ ...command, limit: Number(event.target.value) })}
                className={`${fieldClassName} mt-1.5`}
              />
            </label>
            <label className="flex items-start gap-3 rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] p-3 text-xs">
              <input
                type="checkbox"
                checked={command.enqueueProviderTasks}
                disabled={command.dryRun}
                onChange={(event) =>
                  setCommand({
                    ...command,
                    enqueueProviderTasks: event.target.checked,
                  })}
                className="mt-0.5 h-4 w-4 accent-[var(--sc-blue)]"
              />
              <span>
                <strong className="block">{operationsCopy(locale, 'backfill.enqueue')}</strong>
                <small className="mt-1 block leading-5 text-[var(--sc-ink-muted)]">
                  {operationsCopy(locale, 'backfill.enqueueHint')}
                </small>
              </span>
            </label>
            {request.state.status === 'error' && (
              <OperationResultPanel state="error" message={operationsErrorCopy(locale, request.state.error)} />
            )}
            {result && (
              <OperationResultPanel
                state="success"
                message={operationsCopy(locale, 'backfill.result', {
                  candidates: result.candidateCount,
                  created: result.createdOrReusedCount,
                  skipped: result.skippedCount,
                })}
              />
            )}
            {result && result.items.length > 0 && (
              <div className="max-h-48 overflow-y-auto rounded-lg border border-[var(--sc-border)]">
                {result.items.map((item) => (
                  <div key={item.shipmentId} className="flex items-center justify-between gap-3 border-b border-[var(--sc-border)] px-3 py-2 text-[10px] last:border-0">
                    <span className="sc-data">SHP-{item.shipmentId} · {item.maskedWaybillNo || '—'}</span>
                    <span>{item.status} · {item.reason}</span>
                  </div>
                ))}
              </div>
            )}
            <button type="button" onClick={() => void run()} disabled={request.state.status === 'loading'} className={primaryButtonClassName}>
              {request.state.status === 'loading' ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <DatabaseZap className="h-3.5 w-3.5" />}
              {request.state.status === 'loading' ? operationsCopy(locale, 'backfill.running') : operationsCopy(locale, 'backfill.run')}
            </button>
          </div>
        </PermissionBoundary>
      </OperationsPanel>
      {confirming && (
        <ConfirmDialogShell
          ariaLabel={operationsCopy(locale, 'backfill.confirmTitle')}
          closeLabel={operationsCopy(locale, 'common.close')}
          title={operationsCopy(locale, 'backfill.confirmTitle')}
          description={operationsCopy(locale, 'backfill.confirmDetail')}
          onClose={() => setConfirming(false)}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setConfirming(false)} className={secondaryButtonClassName}>{operationsCopy(locale, 'common.cancel')}</button>
              <button type="button" onClick={() => void run()} className={primaryButtonClassName}>{operationsCopy(locale, 'common.confirm')}</button>
            </div>
          }
        >
          <p className="text-xs text-[var(--sc-ink-soft)]">{operationsCopy(locale, 'maintenance.safeBoundary')}</p>
        </ConfirmDialogShell>
      )}
    </>
  );
}

export function CleanupPanel({
  access,
  locale,
}: {
  access: LogisticsOperationsAccess;
  locale: LocalePreference;
}) {
  const request = useOperationsRequest<RentalLogisticsCleanupResultVO>();
  const [command, setCommand] = useState({ ...DEFAULT_CLEANUP_COMMAND });
  const [confirming, setConfirming] = useState(false);

  const run = async () => {
    const normalized = normalizeCleanupCommand(command);
    if (!normalized.dryRun && !confirming) {
      setConfirming(true);
      return;
    }
    setConfirming(false);
    await request.run(() => cleanupRentalLogistics(normalized));
  };
  const result = request.state.data;
  return (
    <>
      <OperationsPanel title={operationsCopy(locale, 'cleanup.title')} description={operationsCopy(locale, 'cleanup.description')}>
        <PermissionBoundary allowed={access.canCleanup} locale={locale}>
          <div className="space-y-4">
            <label className="flex min-h-11 items-center gap-3 rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold">
              <input type="checkbox" checked={command.dryRun} onChange={(event) => setCommand({ ...command, dryRun: event.target.checked })} className="h-4 w-4 accent-[var(--sc-blue)]" />
              {operationsCopy(locale, 'cleanup.dryRun')}
            </label>
            <div className="grid grid-cols-2 gap-3">
              <label className="text-[10px] font-bold text-[var(--sc-ink-muted)]">
                {operationsCopy(locale, 'cleanup.retention')}
                <input type="number" min={30} max={3650} value={command.retentionDays} onChange={(event) => setCommand({ ...command, retentionDays: Number(event.target.value) })} className={`${fieldClassName} mt-1.5`} />
              </label>
              <label className="text-[10px] font-bold text-[var(--sc-ink-muted)]">
                {operationsCopy(locale, 'cleanup.limit')}
                <input type="number" min={1} max={1000} value={command.limit} onChange={(event) => setCommand({ ...command, limit: Number(event.target.value) })} className={`${fieldClassName} mt-1.5`} />
              </label>
            </div>
            {request.state.status === 'error' && <OperationResultPanel state="error" message={operationsErrorCopy(locale, request.state.error)} />}
            {result && <OperationResultPanel state="success" message={operationsCopy(locale, 'cleanup.result', { traces: result.traceCount, inbox: result.inboxCount, outbox: result.outboxCount })} />}
            <button type="button" onClick={() => void run()} disabled={request.state.status === 'loading'} className={primaryButtonClassName}>
              {request.state.status === 'loading' ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <Trash2 className="h-3.5 w-3.5" />}
              {request.state.status === 'loading' ? operationsCopy(locale, 'cleanup.running') : operationsCopy(locale, 'cleanup.run')}
            </button>
          </div>
        </PermissionBoundary>
      </OperationsPanel>
      {confirming && (
        <ConfirmDialogShell
          ariaLabel={operationsCopy(locale, 'cleanup.confirmTitle')}
          closeLabel={operationsCopy(locale, 'common.close')}
          title={operationsCopy(locale, 'cleanup.confirmTitle')}
          description={operationsCopy(locale, 'cleanup.confirmDetail')}
          onClose={() => setConfirming(false)}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setConfirming(false)} className={secondaryButtonClassName}>{operationsCopy(locale, 'common.cancel')}</button>
              <button type="button" onClick={() => void run()} className={`${primaryButtonClassName} bg-[var(--sc-red)]`}>{operationsCopy(locale, 'common.confirm')}</button>
            </div>
          }
        >
          <p className="text-xs text-[var(--sc-ink-soft)]">{operationsCopy(locale, 'maintenance.safeBoundary')}</p>
        </ConfirmDialogShell>
      )}
    </>
  );
}
