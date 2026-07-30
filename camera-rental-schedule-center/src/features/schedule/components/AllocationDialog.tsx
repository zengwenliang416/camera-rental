import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Sparkles } from 'lucide-react';

import { useApp } from '../../../context/AppContext';
import { usePreferences } from '../../preferences/PreferenceContext';
import { recommendDevicesForOrder } from '../../../lib/scheduleEngine';
import { BillableOccupiedRangeLegend } from '../../../shared/ui/BillableOccupiedRangeLegend';
import { ConfirmDialogShell } from '../../../shared/ui/ConfirmDialogShell';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import {
  buildAllocationProgress,
  deriveOrderRanges,
  evaluateAllocationSubmit,
  type AllocationMap,
} from '../scheduleModel';
import { AllocationCandidatePicker } from './AllocationCandidatePicker';
import { AllocationItemCard } from './AllocationItemCard';

export function AllocationDialog() {
  const {
    orders,
    devices,
    blocks,
    selectedOrderIdForAllocation,
    openAllocationModal,
    assignDevicesToOrder,
    hasPermission,
  } = useApp();
  const { t } = usePreferences();
  const order = orders.find((item) => item.id === selectedOrderIdForAllocation);
  const [allocationMap, setAllocationMap] = useState<AllocationMap>({});
  const [candidateModelId, setCandidateModelId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!order) return;
    setAllocationMap(
      Object.fromEntries(order.items.map((item) => [item.modelId, [...item.assignedDeviceIds]]))
    );
    setCandidateModelId(null);
  }, [order]);

  const close = useCallback(() => openAllocationModal(null), [openAllocationModal]);
  const ranges = useMemo(() => (order ? deriveOrderRanges(order) : null), [order]);
  const progress = useMemo(
    () => (order ? buildAllocationProgress(order, allocationMap) : null),
    [allocationMap, order]
  );
  const submitState = order
    ? evaluateAllocationSubmit({
        order,
        allocationMap,
        hasPermission: hasPermission('rental:device:assign'),
        isSubmitting,
      })
    : { ready: false as const, reason: 'details' as const };

  if (!order || !selectedOrderIdForAllocation || !ranges || !progress) return null;
  const hasCompleteOccupiedRange = Boolean(
    order.rentalPeriodReady && order.occupyStartDate && order.occupyEndDateExclusive
  );

  const recommend = () => {
    const recommended = recommendDevicesForOrder(order, devices, blocks);
    setAllocationMap(
      Object.fromEntries(
        order.items.map((item) => [
          item.modelId,
          (recommended[item.modelId] || []).map((device) => device.id),
        ])
      )
    );
  };

  const toggleCandidate = (modelId: string, deviceId: string, maximum: number) => {
    setAllocationMap((current) => {
      const selected = current[modelId] || [];
      const next = selected.includes(deviceId)
        ? selected.filter((id) => id !== deviceId)
        : selected.length < maximum
          ? [...selected, deviceId]
          : selected;
      return { ...current, [modelId]: next };
    });
  };

  const submit = async () => {
    if (!submitState.ready) return;
    setIsSubmitting(true);
    try {
      await assignDevicesToOrder(order.id, allocationMap);
    } finally {
      setIsSubmitting(false);
    }
  };

  const reasonLabel = {
    submitting: t('allocation.submitting'),
    permission: t('allocation.reasonPermission'),
    period: t('allocation.reasonPeriod'),
    details: t('allocation.reasonDetails'),
    incomplete: t('allocation.reasonIncomplete'),
    ready: t('allocation.submit'),
  }[submitState.reason];

  return (
    <ConfirmDialogShell
      ariaLabel={`${t('allocation.title')} ${order.orderNumber}`}
      closeLabel={t('allocation.close')}
      title={
        <span className="flex flex-wrap items-center gap-2">
          {t('allocation.title')}
          <span className="sc-data text-xs text-[var(--sc-blue)]">{order.orderNumber}</span>
        </span>
      }
      description={t('allocation.description')}
      onClose={close}
      footer={
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="button"
            onClick={close}
            className="min-h-11 rounded-lg px-4 text-xs font-bold text-[var(--sc-ink-soft)] hover:bg-[var(--sc-surface)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
          >
            {t('allocation.cancel')}
          </button>
          <div className="flex flex-col items-stretch gap-1 sm:items-end">
            <button
              type="button"
              onClick={() => void submit()}
              disabled={!submitState.ready}
              className="min-h-11 rounded-lg bg-[var(--sc-ink)] px-5 text-xs font-black text-[var(--sc-surface)] hover:opacity-90 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] disabled:cursor-not-allowed disabled:opacity-45"
            >
              {reasonLabel}
            </button>
            <span className="text-[9px] text-[var(--sc-ink-muted)]">
              {t('allocation.serverAuthority')}
            </span>
          </div>
        </div>
      }
    >
      <div className="space-y-4">
        <section className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
          <BillableOccupiedRangeLegend
            billableLabel={t('schedule.billable')}
            occupiedLabel={t('schedule.occupied')}
            billableHint={t('schedule.billableHint')}
            occupiedHint={t('schedule.occupiedHint')}
            billable={ranges.billable}
            occupied={ranges.occupied}
          />
          <button
            type="button"
            onClick={recommend}
            disabled={!hasCompleteOccupiedRange}
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-blue-soft)] px-4 text-xs font-black text-[var(--sc-blue)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] disabled:cursor-not-allowed disabled:opacity-45"
          >
            <Sparkles className="h-4 w-4" />
            {t('allocation.recommend')}
          </button>
        </section>

        <section className="rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] p-4">
          <div className="flex items-center justify-between gap-3 text-xs font-bold text-[var(--sc-ink)]">
            <span>{t('allocation.progress')}</span>
            <span className="sc-data">{progress.totalAssigned} / {progress.totalRequired}</span>
          </div>
          <div
            role="progressbar"
            aria-label={t('allocation.progress')}
            aria-valuemin={0}
            aria-valuemax={progress.totalRequired}
            aria-valuenow={progress.totalAssigned}
            className="mt-2 h-2 overflow-hidden rounded-full bg-[var(--sc-border)]"
          >
            <div
              className={`h-full ${progress.complete ? 'bg-[var(--sc-green)]' : 'bg-[var(--sc-amber)]'}`}
              style={{ width: `${progress.percent}%` }}
            />
          </div>
        </section>

        {!order.rentalPeriodReady && (
          <p className="flex items-start gap-2 rounded-lg border border-[color-mix(in_srgb,var(--sc-amber)_28%,var(--sc-border))] bg-[var(--sc-amber-soft)] p-3 text-[11px] leading-5 text-[var(--sc-ink-soft)]">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-[var(--sc-amber)]" />
            {t('allocation.periodPending')}
          </p>
        )}

        <div className="flex items-center gap-2">
          <h3 className="text-xs font-black uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
            {t('allocation.requirements')}
          </h3>
          <StatusBadge tone="amber">{t('allocation.provisional')}</StatusBadge>
        </div>

        {order.items.map((item) => (
          <div key={item.modelId} className="space-y-2">
            <AllocationItemCard
              item={item}
              devices={devices}
              selectedIds={allocationMap[item.modelId] || []}
              canSelect={hasCompleteOccupiedRange}
              labels={{
                need: t('allocation.need'),
                complete: t('allocation.complete'),
                missing: t('allocation.missing'),
                select: t('allocation.select'),
                remove: t('allocation.remove'),
                selectUnavailable: t('allocation.selectUnavailable'),
              }}
              onRemove={(deviceId) => toggleCandidate(item.modelId, deviceId, item.quantity)}
              onOpenCandidates={() => setCandidateModelId(item.modelId)}
            />
            {hasCompleteOccupiedRange && candidateModelId === item.modelId && (
              <AllocationCandidatePicker
                modelId={item.modelId}
                devices={devices}
                blocks={blocks}
                occupyStartDate={order.occupyStartDate}
                occupyEndDateExclusive={order.occupyEndDateExclusive}
                excludeOrderId={order.id}
                selectedIds={allocationMap[item.modelId] || []}
                maximum={item.quantity}
                labels={{
                  candidates: t('allocation.candidates'),
                  available: t('allocation.available'),
                  noCandidates: t('allocation.noCandidates'),
                  close: t('allocation.closeCandidates'),
                }}
                onToggle={(deviceId) => toggleCandidate(item.modelId, deviceId, item.quantity)}
                onClose={() => setCandidateModelId(null)}
              />
            )}
          </div>
        ))}
      </div>
    </ConfirmDialogShell>
  );
}
