import { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Package2,
  RefreshCw,
  Truck,
} from 'lucide-react';

import { DetailDrawerShell } from '../../../shared/ui/DetailDrawerShell';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { IdentifierText } from '../../../shared/ui/IdentifierText';
import { PermissionAwareAction } from '../../../shared/ui/PermissionAwareAction';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { usePreferences } from '../../preferences/PreferenceContext';
import { useDeliveryTracking } from '../TrackingContext';
import {
  deliveryCodeLabel,
  deliveryDirectionLabel,
  formatTrackingDateTime,
  logisticsRiskLabel,
  severityLabel,
  trackingCopy,
  trackingStatusLabel,
} from '../trackingCopy';
import { trackingStatusPresentation, type DeliveryOrderSummary } from '../trackingModel';

const iconByName = {
  alert: AlertTriangle,
  check: CheckCircle2,
  clock: Clock3,
  package: Package2,
  truck: Truck,
};

function PackageSelector({
  summary,
  selectedDeliveryId,
  onSelect,
}: {
  summary: DeliveryOrderSummary;
  selectedDeliveryId: number | null;
  onSelect: (deliveryId: number) => void;
}) {
  const { locale } = usePreferences();
  return (
    <section className="space-y-3">
      <h3 className="text-sm font-black text-[var(--sc-ink)]">
        {trackingCopy(locale, 'drawer.packages')}
      </h3>
      <div className="grid gap-2 sm:grid-cols-2">
        {summary.packages.map((item) => {
          const state = trackingStatusPresentation(item.trackingStatus);
          const Icon = iconByName[state.icon];
          return (
            <button
              key={item.deliveryId}
              type="button"
              onClick={() => onSelect(item.deliveryId)}
              className={`sc-button rounded-xl border p-3 text-left ${
                selectedDeliveryId === item.deliveryId
                  ? 'border-[var(--sc-blue)] bg-[var(--sc-blue-soft)]'
                  : 'border-[var(--sc-glass-hairline)] bg-[var(--sc-glass-soft)]'
              }`}
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <strong className="text-xs text-[var(--sc-ink)]">
                    #{item.packageSeq} · {deliveryDirectionLabel(locale, item.direction)}
                  </strong>
                  <p className="mt-1 text-[11px] text-[var(--sc-ink-soft)]">
                    {item.carrierName || trackingCopy(locale, 'panel.notAvailable')} · {item.maskedWaybillNo ?? trackingCopy(locale, 'panel.notAvailable')}
                  </p>
                </div>
                <StatusBadge tone={state.tone} icon={<Icon className="h-3 w-3" />}>
                  {trackingStatusLabel(locale, item.trackingStatus)}
                </StatusBadge>
              </div>
              <div className="mt-2 flex flex-wrap gap-2">
                <StatusBadge tone="neutral">
                  {trackingCopy(locale, 'state.query')}: {deliveryCodeLabel(locale, item.queryStatus)}
                </StatusBadge>
                {item.stale && (
                  <StatusBadge tone="amber">{trackingCopy(locale, 'drawer.stale')}</StatusBadge>
                )}
              </div>
            </button>
          );
        })}
      </div>
    </section>
  );
}

export function DeliveryTrackingDrawer({
  orderId,
  orderNumber,
  customerName,
  onClose,
}: {
  orderId: string;
  orderNumber?: string;
  customerName?: string;
  onClose: () => void;
}) {
  const { locale } = usePreferences();
  const {
    canReadTracking,
    trackingByOrderId,
    getDetailState,
    loadDetail,
    refreshDelivery,
  } = useDeliveryTracking();
  const summary = trackingByOrderId[orderId];
  const [selectedDeliveryId, setSelectedDeliveryId] = useState<number | null>(
    summary?.packages[0]?.deliveryId ?? null
  );

  useEffect(() => {
    setSelectedDeliveryId(summary?.packages[0]?.deliveryId ?? null);
  }, [summary?.rentalOrderId]);

  useEffect(() => {
    if (!selectedDeliveryId) return;
    void loadDetail(selectedDeliveryId);
  }, [loadDetail, selectedDeliveryId]);

  const detailState = selectedDeliveryId ? getDetailState(selectedDeliveryId) : null;
  const detail = detailState?.detail || null;
  const detailRisks = detail?.risks || summary?.risks || [];
  const refreshAllowed = canReadTracking && Boolean(selectedDeliveryId);
  const refreshLabel = detailState?.isRefreshPending
    ? trackingCopy(locale, 'drawer.refreshBusy')
    : trackingCopy(locale, 'drawer.refresh');

  const currentStatus = useMemo(() => {
    if (!detail) return null;
    const state = trackingStatusPresentation(detail.trackingStatus);
    const Icon = iconByName[state.icon];
    return {
      ...state,
      Icon,
    };
  }, [detail]);
  const linkedDevices = detail?.devices.map((device) => device.deviceNo).join(' · ');
  const drawerContext = [customerName, linkedDevices].filter(Boolean).join(' · ');

  if (!summary) return null;

  return (
    <DetailDrawerShell
      title={
        <span className="grid gap-1">
          <span className="sc-data text-[9px] uppercase tracking-[0.12em] text-[var(--sc-blue)]">
            {trackingCopy(locale, 'drawer.eyebrow')}
          </span>
          <span>{orderNumber || `RO-${summary.rentalOrderId}`}</span>
        </span>
      }
      description={drawerContext || trackingCopy(locale, 'drawer.description')}
      closeLabel={trackingCopy(locale, 'drawer.close')}
      onClose={onClose}
    >
      <div className="space-y-5">
        <PackageSelector
          summary={summary}
          selectedDeliveryId={selectedDeliveryId}
          onSelect={setSelectedDeliveryId}
        />

        {!selectedDeliveryId ? (
          <EmptyState
            icon={<Package2 className="h-4 w-4" />}
            title={trackingCopy(locale, 'drawer.detailEmpty')}
            description={trackingCopy(locale, 'drawer.detailEmptyDetail')}
          />
        ) : detailState?.isLoading && !detail ? (
          <EmptyState
            icon={<Clock3 className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.loading')}
          />
        ) : detailState?.error && !detail ? (
          <EmptyState
            icon={<AlertTriangle className="h-4 w-4" />}
            title={trackingCopy(locale, 'drawer.detailError')}
          />
        ) : !detail ? (
          <EmptyState
            icon={<Package2 className="h-4 w-4" />}
            title={trackingCopy(locale, 'drawer.detailEmpty')}
            description={trackingCopy(locale, 'drawer.detailEmptyDetail')}
          />
        ) : (
          <>
            <section className="sc-workspace-card rounded-2xl p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    {currentStatus && (
                      <StatusBadge tone={currentStatus.tone} icon={<currentStatus.Icon className="h-3 w-3" />}>
                        {trackingStatusLabel(locale, detail.trackingStatus)}
                      </StatusBadge>
                    )}
                    {detail.stale && (
                      <StatusBadge tone="amber">{trackingCopy(locale, 'drawer.stale')}</StatusBadge>
                    )}
                  </div>
                  <div className="mt-3 grid gap-2 text-[11px] text-[var(--sc-ink-soft)] sm:grid-cols-2">
                    <div>
                      <span className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'drawer.direction')}</span>
                      <p>{deliveryDirectionLabel(locale, detail.direction)}</p>
                    </div>
                    <div>
                      <span className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'drawer.carrier')}</span>
                      <p>{detail.carrierName || trackingCopy(locale, 'panel.notAvailable')}</p>
                    </div>
                    <div>
                      <span className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'drawer.waybill')}</span>
                      <p className="sc-data">{detail.maskedWaybillNo ?? trackingCopy(locale, 'panel.notAvailable')}</p>
                    </div>
                    <div>
                      <span className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'drawer.lastSynced')}</span>
                      <p>{formatTrackingDateTime(locale, detail.lastSyncedAt) || trackingCopy(locale, 'panel.notAvailable')}</p>
                    </div>
                    <div>
                      <span className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'drawer.latestEvent')}</span>
                      <p>{formatTrackingDateTime(locale, detail.latestEventTime) || trackingCopy(locale, 'panel.notAvailable')}</p>
                    </div>
                    <div>
                      <span className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'drawer.eta')}</span>
                      <p>{formatTrackingDateTime(locale, detail.estimatedDeliveryAt) || trackingCopy(locale, 'panel.notAvailable')}</p>
                    </div>
                  </div>
                </div>
                <PermissionAwareAction
                  allowed={refreshAllowed && !detailState?.isRefreshPending}
                  label={refreshLabel}
                  deniedLabel={
                    detailState?.isRefreshPending
                      ? refreshLabel
                      : trackingCopy(locale, 'drawer.refreshDenied')
                  }
                  icon={<RefreshCw className={`h-4 w-4 ${detailState?.isRefreshPending ? 'animate-spin' : ''}`} />}
                  onSelect={() => {
                    if (!selectedDeliveryId) return;
                    void refreshDelivery(selectedDeliveryId);
                  }}
                />
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <StatusBadge tone="neutral">
                  {trackingCopy(locale, 'state.mapping')}: {deliveryCodeLabel(locale, detail.mappingStatus)}
                </StatusBadge>
                <StatusBadge tone="neutral">
                  {trackingCopy(locale, 'state.subscribe')}: {deliveryCodeLabel(locale, detail.subscribeStatus)}
                </StatusBadge>
                <StatusBadge tone="neutral">
                  {trackingCopy(locale, 'state.query')}: {deliveryCodeLabel(locale, detail.queryStatus)}
                </StatusBadge>
              </div>
              {detailState?.refreshResult && (
                <div className="sc-soft-panel mt-4 rounded-xl p-3 text-[11px] text-[var(--sc-ink-soft)]">
                  <strong className="block text-[var(--sc-ink)]">{trackingCopy(locale, 'drawer.refreshResult')}</strong>
                  <p className="mt-1">
                    {deliveryCodeLabel(locale, detailState.refreshResult.reason)}
                  </p>
                  {detailState.refreshResult.nextAllowedAt && (
                    <p className="mt-1">
                      {trackingCopy(locale, 'state.nextAllowed')}{' '}
                      {formatTrackingDateTime(locale, detailState.refreshResult.nextAllowedAt)}
                    </p>
                  )}
                </div>
              )}
            </section>

            <section className="sc-workspace-card rounded-2xl p-4">
              <h3 className="text-sm font-black text-[var(--sc-ink)]">
                {trackingCopy(locale, 'drawer.devices')}
              </h3>
              <div className="mt-3 flex flex-wrap gap-2">
                {detail.devices.map((device) => (
                  <span key={`${detail.deliveryId}-${device.deviceNo}`}>
                    <StatusBadge tone="blue">
                      <IdentifierText value={device.deviceNo} />
                    </StatusBadge>
                  </span>
                ))}
              </div>
            </section>

            <section className="sc-workspace-card rounded-2xl p-4">
              <h3 className="text-sm font-black text-[var(--sc-ink)]">
                {trackingCopy(locale, 'drawer.risks')}
              </h3>
              {detailRisks.length === 0 ? (
                <p className="mt-2 text-[11px] text-[var(--sc-ink-muted)]">
                  {trackingCopy(locale, 'drawer.noRisks')}
                </p>
              ) : (
                <div className="mt-3 space-y-3">
                  {detailRisks.map((risk) => (
                    <article
                      key={`${detail.deliveryId}-${risk.code}`}
                      className="sc-soft-panel rounded-xl p-3"
                    >
                      <div className="flex flex-wrap items-center gap-2">
                        <StatusBadge tone={risk.severity === 'high' ? 'red' : risk.severity === 'medium' ? 'amber' : 'neutral'}>
                          {severityLabel(locale, risk.severity)}
                        </StatusBadge>
                        <strong className="text-xs text-[var(--sc-ink)]">
                          {logisticsRiskLabel(locale, risk.code)}
                        </strong>
                      </div>
                      <p className="mt-2 text-[11px] leading-5 text-[var(--sc-ink-soft)]">
                        {risk.safeMessage}
                      </p>
                      {risk.nextAction && (
                        <p className="mt-2 text-[11px] text-[var(--sc-ink-muted)]">
                          {trackingCopy(locale, 'exception.nextAction')}: {risk.nextAction}
                        </p>
                      )}
                    </article>
                  ))}
                </div>
              )}
            </section>

            <section className="sc-workspace-card rounded-2xl p-4">
              <h3 className="text-sm font-black text-[var(--sc-ink)]">
                {trackingCopy(locale, 'drawer.timeline')}
              </h3>
              {detail.traces.length === 0 ? (
                <EmptyState
                  icon={<Clock3 className="h-4 w-4" />}
                  title={trackingCopy(locale, 'drawer.timelineEmpty')}
                  description={trackingCopy(locale, 'drawer.timelineEmptyDetail')}
                />
              ) : (
                <div className="relative ml-2 mt-4 space-y-5 border-l border-[var(--sc-border-strong)] pl-6">
                  {detail.traces.map((trace) => {
                    const state = trackingStatusPresentation(trace.trackingStatus);
                    const Icon = iconByName[state.icon];
                    return (
                      <article
                        key={`${detail.deliveryId}-${trace.eventSeq || trace.businessTime || trace.traceText}`}
                        className="relative"
                      >
                        <span className="absolute -left-[31px] top-1.5 h-3 w-3 rounded-full border-2 border-[var(--sc-surface)] bg-[var(--sc-blue)] ring-1 ring-[var(--sc-blue)]" />
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <StatusBadge tone={state.tone} icon={<Icon className="h-3 w-3" />}>
                            {trackingStatusLabel(locale, trace.trackingStatus)}
                          </StatusBadge>
                          <span className="text-[10px] text-[var(--sc-ink-muted)]">
                            {formatTrackingDateTime(locale, trace.businessTime) || trackingCopy(locale, 'panel.notAvailable')}
                          </span>
                        </div>
                        {trace.traceText && (
                          <p className="mt-2 text-[11px] leading-5 text-[var(--sc-ink-soft)]">
                            {trace.traceText}
                          </p>
                        )}
                        {trace.location && (
                          <p className="mt-2 text-[11px] text-[var(--sc-ink-muted)]">
                            {trace.location}
                          </p>
                        )}
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </DetailDrawerShell>
  );
}
