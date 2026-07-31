import { AlertTriangle, CheckCircle2, Clock3, Package2, Truck } from 'lucide-react';

import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { usePreferences } from '../../preferences/PreferenceContext';
import { useDeliveryTracking } from '../TrackingContext';
import {
  deliveryCodeLabel,
  formatTrackingDateTime,
  logisticsRiskLabel,
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

function SummaryCard({
  summary,
  onOpen,
}: {
  summary: DeliveryOrderSummary;
  onOpen: (orderId: string) => void;
}) {
  const { locale } = usePreferences();
  const primary = summary.packages[0];
  const state = primary ? trackingStatusPresentation(primary.trackingStatus) : trackingStatusPresentation('UNKNOWN');
  const Icon = iconByName[state.icon];
  const summaryText = summary.packageCount <= 1
    ? primary
      ? primary.maskedWaybillNo ?? trackingCopy(locale, 'panel.notAvailable')
      : trackingCopy(locale, 'summary.unshipped')
    : `${summary.packageCount} ${trackingCopy(locale, 'summary.multiple')}`;

  return (
    <button
      type="button"
      onClick={() => onOpen(String(summary.rentalOrderId))}
      className="rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-4 text-left transition hover:bg-[var(--sc-surface-soft)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge tone={state.tone} icon={<Icon className="h-3 w-3" />}>
              {primary
                ? trackingStatusLabel(locale, primary.trackingStatus)
                : trackingCopy(locale, 'summary.unshipped')}
            </StatusBadge>
            {summary.risks.length > 0 && (
              <StatusBadge tone="red">
                {trackingCopy(locale, 'summary.risk')} {summary.risks.length}
              </StatusBadge>
            )}
          </div>
          <h3 className="mt-3 text-sm font-black text-[var(--sc-ink)]">
            RO-{summary.rentalOrderId}
          </h3>
          <p className="mt-1 text-xs text-[var(--sc-ink-soft)]">{summaryText}</p>
        </div>
        <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
          {summary.packageCount <= 1
            ? trackingCopy(locale, 'panel.singlePackage')
            : trackingCopy(locale, 'panel.multiPackage')}
        </span>
      </div>
      {primary && (
        <div className="mt-3 flex flex-wrap items-center gap-2 text-[11px] text-[var(--sc-ink-muted)]">
          <span>{primary.carrierName || trackingCopy(locale, 'panel.notAvailable')}</span>
          <span>{summaryText}</span>
          {primary.lastSyncedAt && (
            <span>
              {trackingCopy(locale, 'panel.updated')} {formatTrackingDateTime(locale, primary.lastSyncedAt)}
            </span>
          )}
        </div>
      )}
      {summary.risks.length > 0 && (
        <p className="mt-3 text-[11px] leading-5 text-[var(--sc-red)]">
          {logisticsRiskLabel(locale, summary.risks[0].code)}
        </p>
      )}
      {primary && (
        <div className="mt-3 flex flex-wrap gap-2">
          <StatusBadge tone="neutral">
            {trackingCopy(locale, 'state.mapping')}: {deliveryCodeLabel(locale, primary.mappingStatus)}
          </StatusBadge>
          <StatusBadge tone="neutral">
            {trackingCopy(locale, 'state.query')}: {deliveryCodeLabel(locale, primary.queryStatus)}
          </StatusBadge>
          {primary.stale && (
            <StatusBadge tone="amber">{trackingCopy(locale, 'drawer.stale')}</StatusBadge>
          )}
        </div>
      )}
    </button>
  );
}

export function DeliveryTrackingSummaryPanel({
  onOpen,
}: {
  onOpen: (orderId: string) => void;
}) {
  const {
    canReadTracking,
    visibleTrackingSummaries,
    isSummaryLoading,
    summaryError,
    lastSummarySyncAt,
    refreshSummaries,
  } = useDeliveryTracking();
  const { locale } = usePreferences();
  const packageCount = visibleTrackingSummaries.reduce((total, item) => total + item.packageCount, 0);
  const riskCount = visibleTrackingSummaries.reduce((total, item) => total + item.risks.length, 0);

  return (
    <section className="grid gap-3 xl:grid-cols-[minmax(260px,0.6fr)_minmax(0,1fr)]">
      <div className="rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-4">
        <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[var(--sc-ink-muted)]">
          {trackingCopy(locale, 'panel.eyebrow')}
        </p>
        <h2 className="mt-2 text-base font-black text-[var(--sc-ink)]">
          {trackingCopy(locale, 'panel.title')}
        </h2>
        <p className="mt-2 text-xs leading-5 text-[var(--sc-ink-soft)]">
          {trackingCopy(locale, 'panel.description')}
        </p>
        <div className="mt-4 grid gap-2 sm:grid-cols-3 xl:grid-cols-1">
          <StatusBadge tone="neutral">
            {trackingCopy(locale, 'panel.trackedOrders')} {visibleTrackingSummaries.length}
          </StatusBadge>
          <StatusBadge tone="blue">
            {trackingCopy(locale, 'panel.packages')} {packageCount}
          </StatusBadge>
          <StatusBadge tone={riskCount > 0 ? 'red' : 'green'}>
            {trackingCopy(locale, 'panel.risks')} {riskCount}
          </StatusBadge>
        </div>
        <p className="mt-4 text-[11px] text-[var(--sc-ink-muted)]">
          {trackingCopy(locale, 'panel.updated')}{' '}
          {lastSummarySyncAt
            ? formatTrackingDateTime(locale, new Date(lastSummarySyncAt).toISOString())
            : trackingCopy(locale, 'panel.notAvailable')}
        </p>
        {summaryError && (
          <button
            type="button"
            onClick={() => void refreshSummaries()}
            className="mt-3 min-h-11 rounded-lg border border-[var(--sc-border-strong)] px-3 text-xs font-bold text-[var(--sc-ink)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
          >
            {trackingCopy(locale, 'panel.retry')}
          </button>
        )}
      </div>

      <div className="rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-4">
        {!canReadTracking ? (
          <EmptyState
            icon={<AlertTriangle className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.permission')}
          />
        ) : isSummaryLoading && visibleTrackingSummaries.length === 0 ? (
          <EmptyState
            icon={<Clock3 className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.loading')}
          />
        ) : summaryError && visibleTrackingSummaries.length === 0 ? (
          <EmptyState
            icon={<AlertTriangle className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.error')}
          />
        ) : visibleTrackingSummaries.length === 0 ? (
          <EmptyState
            icon={<Package2 className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.empty')}
            description={trackingCopy(locale, 'panel.emptyDetail')}
          />
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            {visibleTrackingSummaries.map((summary) => (
              <div key={summary.rentalOrderId}>
                <SummaryCard
                  summary={summary}
                  onOpen={onOpen}
                />
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
