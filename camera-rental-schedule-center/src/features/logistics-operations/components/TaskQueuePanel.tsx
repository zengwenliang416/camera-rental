import {
  LoaderCircle,
  RefreshCw,
  RotateCcw,
  ShieldAlert,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';

import {
  fetchRentalLogisticsFailedTasks,
  reconcileRentalLogistics,
  retryRentalLogisticsFailedTask,
  type RentalLogisticsFailedTaskVO,
  type RentalLogisticsReconcileResultVO,
} from '../../../api/rental';
import { Button } from '../../../shared/ui/Button';
import { OperationResultPanel } from '../../../shared/ui/OperationResultPanel';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import type { LocalePreference } from '../../preferences/preferenceModel';
import {
  boundedInteger,
  operationsStatusTone,
  type LogisticsOperationsAccess,
} from '../operationsModel';
import {
  formatOperationsDateTime,
  operationsCodeLabel,
  operationsCopy,
  operationsErrorCopy,
} from '../operationsCopy';
import { useOperationsRequest } from '../useOperationsRequest';
import {
  fieldClassName,
  OperationsPanel,
  PanelQueryBoundary,
} from './OperationsPanel';

export function TaskQueuePanel({
  access,
  locale,
}: {
  access: LogisticsOperationsAccess;
  locale: LocalePreference;
}) {
  const query = useOperationsRequest<RentalLogisticsFailedTaskVO[]>();
  const retry = useOperationsRequest<{ accepted: boolean; reason: string }>();
  const reconcile = useOperationsRequest<RentalLogisticsReconcileResultVO>();
  const [taskType, setTaskType] = useState<'ALL' | 'INBOX' | 'OUTBOX'>('ALL');
  const [limit, setLimit] = useState(50);
  const [reconcileLimit, setReconcileLimit] = useState(20);
  const [retryingKey, setRetryingKey] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!access.canQueryTasks) return Promise.resolve(null);
    return query.run(
      () =>
        fetchRentalLogisticsFailedTasks(
          taskType,
          boundedInteger(limit, 1, 100, 50)
        ),
      (items) => items.length === 0
    );
  }, [access.canQueryTasks, limit, query.run, taskType]);

  useEffect(() => {
    if (access.canQueryTasks) void load();
    else query.reset();
  }, [access.canQueryTasks, load, query.reset]);

  const safeRetry = async (task: RentalLogisticsFailedTaskVO) => {
    if (!access.canRetryTasks) return;
    const key = `${task.taskType}:${task.id}`;
    setRetryingKey(key);
    setSuccessMessage(null);
    const result = await retry.run(() =>
      retryRentalLogisticsFailedTask(task.taskType, task.id)
    );
    setRetryingKey(null);
    if (result?.accepted) {
      setSuccessMessage(operationsCopy(locale, 'tasks.retried'));
      await load();
    }
  };

  const runReconcile = async () => {
    if (!access.canReconcile) return;
    setSuccessMessage(null);
    const result = await reconcile.run(() =>
      reconcileRentalLogistics(
        boundedInteger(reconcileLimit, 1, 100, 20)
      )
    );
    if (result) {
      setSuccessMessage(
        operationsCopy(locale, 'reconcile.result', {
          requested: result.requestedLimit,
          count: result.enqueuedCount,
        })
      );
    }
  };

  const tasks = query.state.data || [];

  return (
    <OperationsPanel
      title={operationsCopy(locale, 'tasks.title')}
      description={operationsCopy(locale, 'tasks.description')}
      actions={
        access.canQueryTasks ? (
          <Button
            variant="outline"
            size="md"
            type="button"
            onClick={() => void load()}
            disabled={query.state.status === 'loading'}
            icon={<RefreshCw className={`h-3.5 w-3.5 ${query.state.status === 'loading' ? 'animate-spin' : ''}`} />}
          >
            {operationsCopy(locale, 'common.refresh')}
          </Button>
        ) : undefined
      }
    >
      <PanelQueryBoundary
        allowed={access.canQueryTasks}
        state={query.state}
        locale={locale}
        onRetry={() => void load()}
        isEmpty={query.state.status === 'empty'}
        emptyTitle={operationsCopy(locale, 'tasks.empty')}
        emptyDetail={operationsCopy(locale, 'tasks.emptyDetail')}
      >
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-[minmax(0,12rem)_minmax(0,10rem)_auto]">
            <label className="sc-field-label">
              {operationsCopy(locale, 'tasks.filter')}
              <select
                value={taskType}
                onChange={(event) =>
                  setTaskType(event.target.value as typeof taskType)}
                className={`${fieldClassName} mt-1.5`}
              >
                <option value="ALL">{operationsCopy(locale, 'tasks.all')}</option>
                <option value="OUTBOX">OUTBOX</option>
                <option value="INBOX">INBOX</option>
              </select>
            </label>
            <label className="sc-field-label">
              {operationsCopy(locale, 'tasks.limit')}
              <input
                type="number"
                min={1}
                max={100}
                value={limit}
                onChange={(event) => setLimit(Number(event.target.value))}
                className={`${fieldClassName} mt-1.5`}
              />
            </label>
            <Button
              variant="outline"
              size="md"
              type="button"
              onClick={() => void load()}
              className="self-end"
              icon={<RefreshCw className="h-3.5 w-3.5" />}
            >
              {operationsCopy(locale, 'common.refresh')}
            </Button>
          </div>

          {retry.state.status === 'error' && (
            <OperationResultPanel
              state="error"
              message={operationsErrorCopy(locale, retry.state.error)}
            />
          )}
          {successMessage && (
            <OperationResultPanel state="success" message={successMessage} />
          )}

          <div className="sc-workspace-card overflow-x-auto rounded-xl">
            <table className="w-full min-w-[900px] border-collapse text-left">
              <thead>
                <tr className="sc-panel-header border-y text-[9px] uppercase tracking-[0.12em] text-[var(--sc-ink-muted)]">
                  <th className="px-3 py-3">{operationsCopy(locale, 'tasks.type')}</th>
                  <th className="px-3 py-3">{operationsCopy(locale, 'tasks.delivery')}</th>
                  <th className="px-3 py-3">{operationsCopy(locale, 'tasks.event')}</th>
                  <th className="px-3 py-3">{operationsCopy(locale, 'tasks.status')}</th>
                  <th className="px-3 py-3">{operationsCopy(locale, 'tasks.retries')}</th>
                  <th className="px-3 py-3">{operationsCopy(locale, 'tasks.error')}</th>
                  <th className="px-3 py-3">{operationsCopy(locale, 'tasks.occurred')}</th>
                  <th className="px-3 py-3 text-right">{operationsCopy(locale, 'tasks.action')}</th>
                </tr>
              </thead>
              <tbody>
                {tasks.map((task) => {
                  const key = `${task.taskType}:${task.id}`;
                  const isRetrying = retryingKey === key;
                  return (
                    <tr key={key} className="border-b border-[var(--sc-border)]">
                      <td className="sc-data px-3 py-3 text-[10px]">{task.taskType}-{task.id}</td>
                      <td className="sc-data px-3 py-3 text-[10px]">{task.deliveryId ? `DLV-${task.deliveryId}` : '—'}</td>
                      <td className="px-3 py-3 text-[10px]">{task.eventType || task.providerCode || 'CALLBACK'}</td>
                      <td className="px-3 py-3">
                        <StatusBadge tone={operationsStatusTone(task.processingStatus)}>
                          {operationsCodeLabel(locale, task.processingStatus)}
                        </StatusBadge>
                      </td>
                      <td className="sc-data px-3 py-3 text-[10px]">{task.retryCount}</td>
                      <td className="max-w-48 px-3 py-3 text-[10px] text-[var(--sc-ink-soft)]">
                        {operationsCodeLabel(locale, task.safeErrorMessage || task.errorCode)}
                      </td>
                      <td className="px-3 py-3 text-[10px] text-[var(--sc-ink-muted)]">
                        {formatOperationsDateTime(locale, task.occurredAt) || '—'}
                      </td>
                      <td className="px-3 py-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          type="button"
                          onClick={() => void safeRetry(task)}
                          disabled={!access.canRetryTasks || Boolean(retryingKey)}
                          title={!access.canRetryTasks ? operationsCopy(locale, 'common.noPermission') : undefined}
                          icon={isRetrying
                            ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
                            : <RotateCcw className="h-3.5 w-3.5" />}
                        >
                          {isRetrying
                            ? operationsCopy(locale, 'tasks.retrying')
                            : operationsCopy(locale, 'tasks.retry')}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="sc-workspace-card grid gap-4 rounded-2xl p-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,10rem)_auto] lg:items-end">
            <div>
              <div className="flex items-center gap-2">
                <ShieldAlert className="h-4 w-4 text-[var(--sc-blue)]" />
                <h3 className="text-xs font-black text-[var(--sc-ink)]">
                  {operationsCopy(locale, 'reconcile.title')}
                </h3>
              </div>
              <p className="mt-1 text-[10px] leading-5 text-[var(--sc-ink-muted)]">
                {operationsCopy(locale, 'reconcile.description')}
              </p>
            </div>
            <label className="sc-field-label">
              {operationsCopy(locale, 'reconcile.limit')}
              <input
                type="number"
                min={1}
                max={100}
                value={reconcileLimit}
                disabled={!access.canReconcile}
                onChange={(event) => setReconcileLimit(Number(event.target.value))}
                className={`${fieldClassName} mt-1.5`}
              />
            </label>
            <Button
              variant="primary"
              size="md"
              type="button"
              onClick={() => void runReconcile()}
              disabled={!access.canReconcile || reconcile.state.status === 'loading'}
              title={!access.canReconcile ? operationsCopy(locale, 'common.noPermission') : undefined}
              icon={reconcile.state.status === 'loading'
                ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" />
                : <RotateCcw className="h-3.5 w-3.5" />}
            >
              {reconcile.state.status === 'loading'
                ? operationsCopy(locale, 'reconcile.running')
                : operationsCopy(locale, 'reconcile.run')}
            </Button>
          </div>
          {reconcile.state.status === 'error' && (
            <OperationResultPanel
              state="error"
              message={operationsErrorCopy(locale, reconcile.state.error)}
            />
          )}
        </div>
      </PanelQueryBoundary>
    </OperationsPanel>
  );
}
