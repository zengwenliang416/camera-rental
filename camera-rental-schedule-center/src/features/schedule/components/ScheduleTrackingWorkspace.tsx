import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  Link2Off,
  Package2,
  RefreshCw,
  Truck,
} from 'lucide-react';

import type { ScheduleBlock } from '../../../types';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { usePreferences } from '../../preferences/PreferenceContext';
import {
  formatTrackingDateTime,
  trackingCopy,
  trackingStatusLabel,
} from '../../tracking/trackingCopy';
import {
  trackingStatusPresentation,
  type DeliveryOrderSummary,
} from '../../tracking/trackingModel';
import {
  DeliveryTrackingSummaryBadges,
  primaryTrackingPackage,
} from '../../tracking/components/DeliveryTrackingSummaryBadges';

export type ScheduleTrackingFilter =
  | 'ALL'
  | 'ACTIVE'
  | 'DELIVERED'
  | 'RISK'
  | 'MAPPING_REQUIRED';

const activeStatuses = new Set([
  'INFO_RECEIVED',
  'PICKED_UP',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'RETURNING',
  'CUSTOMS',
]);

export function matchesTrackingFilter(
  summary: DeliveryOrderSummary | undefined,
  filter: ScheduleTrackingFilter
) {
  if (filter === 'ALL') return true;
  if (!summary) return false;
  if (filter === 'RISK') return summary.risks.length > 0;
  if (filter === 'MAPPING_REQUIRED') {
    return summary.packages.some((item) => item.mappingStatus === 'MAPPING_REQUIRED');
  }
  if (filter === 'DELIVERED') {
    return summary.packages.some(
      (item) => item.trackingStatus === 'DELIVERED' || item.trackingStatus === 'RETURNED'
    );
  }
  return summary.packages.some((item) => activeStatuses.has(item.trackingStatus));
}

export function filterDevicesByTracking<T extends { id: string }>(
  devices: T[],
  blocks: ScheduleBlock[],
  trackingByOrderId: Record<string, DeliveryOrderSummary>,
  filter: ScheduleTrackingFilter
) {
  if (filter === 'ALL') return devices;
  const matchingDeviceIds = new Set(
    blocks
      .filter((block) => (
        block.orderId
        && matchesTrackingFilter(trackingByOrderId[block.orderId], filter)
      ))
      .map((block) => block.deviceId)
  );
  return devices.filter((device) => matchingDeviceIds.has(device.id));
}

function latestTimestamp(summaries: DeliveryOrderSummary[]) {
  const values = summaries.flatMap((summary) =>
    summary.packages
      .flatMap((item) => [item.latestEventTime, item.lastSyncedAt])
      .filter((value): value is string => Boolean(value))
  );
  return values.sort((left, right) => right.localeCompare(left))[0];
}

export function ScheduleTrackingMetrics({
  summaries,
  isLoading,
}: {
  summaries: DeliveryOrderSummary[];
  isLoading: boolean;
}) {
  const { locale } = usePreferences();
  const packages = summaries.flatMap((summary) => summary.packages);
  const inTransit = packages.filter((item) => activeStatuses.has(item.trackingStatus)).length;
  const delivered = packages.filter(
    (item) => item.trackingStatus === 'DELIVERED' || item.trackingStatus === 'RETURNED'
  ).length;
  const risks = summaries.reduce((total, summary) => total + summary.risks.length, 0);
  const highRisks = summaries.reduce(
    (total, summary) =>
      total + summary.risks.filter((risk) => risk.severity === 'high').length,
    0
  );
  const mappingRequired = packages.filter(
    (item) => item.mappingStatus === 'MAPPING_REQUIRED'
  ).length;
  const latest = latestTimestamp(summaries);

  const cards = [
    {
      label: trackingCopy(locale, 'workspace.packages'),
      value: packages.length,
      detail: `${trackingCopy(locale, 'workspace.covering')} ${summaries.length} ${trackingCopy(locale, 'workspace.orders')}`,
      icon: Package2,
      tone: 'text-[var(--sc-ink)]',
    },
    {
      label: trackingCopy(locale, 'workspace.inTransit'),
      value: inTransit,
      detail: latest
        ? `${trackingCopy(locale, 'workspace.latest')} ${formatTrackingDateTime(locale, latest)}`
        : trackingCopy(locale, 'panel.notAvailable'),
      icon: Truck,
      tone: 'text-[var(--sc-blue)]',
    },
    {
      label: trackingCopy(locale, 'workspace.delivered'),
      value: delivered,
      detail: trackingCopy(locale, 'workspace.inspectionPending'),
      icon: CheckCircle2,
      tone: 'text-[var(--sc-green)]',
    },
    {
      label: trackingCopy(locale, 'workspace.risks'),
      value: risks,
      detail: `${highRisks} ${trackingCopy(locale, 'workspace.highRisk')}`,
      icon: AlertTriangle,
      tone: 'text-[var(--sc-red)]',
    },
    {
      label: trackingCopy(locale, 'workspace.mapping'),
      value: mappingRequired,
      detail: trackingCopy(locale, 'workspace.noProviderCall'),
      icon: Link2Off,
      tone: 'text-[var(--sc-amber)]',
    },
  ];

  return (
    <section
      aria-label={trackingCopy(locale, 'workspace.metrics')}
      className="sc-metric-group"
    >
      {cards.map(({ label, value, detail, icon: Icon, tone }) => (
        <article
          key={label}
          className="sc-metric-card p-4"
        >
          <div className="flex items-center justify-between gap-3">
            <span className="text-[11px] font-semibold text-[var(--sc-ink-muted)]">{label}</span>
            <Icon className={`h-4 w-4 ${tone}`} />
          </div>
          <strong className={`sc-data mt-3 block text-2xl ${tone}`}>
            {isLoading ? '…' : value}
          </strong>
          <small className="mt-2 block text-[10px] text-[var(--sc-ink-muted)]">{detail}</small>
        </article>
      ))}
    </section>
  );
}

export function CurrentWindowTracking({
  summaries,
  orderNumberByOrderId,
  lastSummarySyncAt,
  canReadTracking,
  isLoading,
  hasError,
  onOpen,
}: {
  summaries: DeliveryOrderSummary[];
  orderNumberByOrderId: Record<string, string>;
  lastSummarySyncAt: number | null;
  canReadTracking: boolean;
  isLoading: boolean;
  hasError: boolean;
  onOpen: (orderId: string) => void;
}) {
  const { locale } = usePreferences();
  const sorted = [...summaries].sort((left, right) => {
    const leftTime = latestTimestamp([left]) || '';
    const rightTime = latestTimestamp([right]) || '';
    return rightTime.localeCompare(leftTime);
  });

  return (
    <aside className="grid content-start gap-3">
      <section className="sc-workspace-card overflow-hidden rounded-2xl">
        <header className="sc-panel-header flex items-center justify-between gap-3 border-b px-4 py-3">
          <strong className="text-xs text-[var(--sc-ink)]">
            {trackingCopy(locale, 'workspace.current')}
          </strong>
          <span className="sc-data text-[9px] text-[var(--sc-ink-muted)]">
            {lastSummarySyncAt
              ? new Intl.DateTimeFormat(locale, {
                  hour: '2-digit',
                  minute: '2-digit',
                  hour12: false,
                }).format(lastSummarySyncAt)
              : '—'}
          </span>
        </header>
        {!canReadTracking ? (
          <EmptyState
            icon={<AlertTriangle className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.permission')}
          />
        ) : isLoading && sorted.length === 0 ? (
          <EmptyState
            icon={<RefreshCw className="h-4 w-4 animate-spin" />}
            title={trackingCopy(locale, 'panel.loading')}
          />
        ) : hasError && sorted.length === 0 ? (
          <EmptyState
            icon={<AlertTriangle className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.error')}
          />
        ) : sorted.length === 0 ? (
          <EmptyState
            icon={<Package2 className="h-4 w-4" />}
            title={trackingCopy(locale, 'panel.empty')}
          />
        ) : (
          <div>
            {sorted.slice(0, 6).map((summary) => {
              const primary = primaryTrackingPackage(summary);
              const state = primary
                ? trackingStatusPresentation(primary.trackingStatus)
                : null;
              const orderId = String(summary.rentalOrderId);
              return (
                <button
                  key={orderId}
                  type="button"
                  onClick={() => onOpen(orderId)}
                  className="block min-h-20 w-full border-b border-[var(--sc-border)] px-4 py-3 text-left last:border-b-0 hover:bg-[var(--sc-surface-hover)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
                >
                  <span className="flex items-start justify-between gap-2">
                    <strong className="sc-data min-w-0 truncate text-[11px] text-[var(--sc-ink)]">
                      {orderNumberByOrderId[orderId] || `RO-${orderId}`}
                    </strong>
                    <DeliveryTrackingSummaryBadges summary={summary} />
                  </span>
                  <span className="mt-1.5 block truncate text-[10px] text-[var(--sc-ink-soft)]">
                    {primary?.latestTraceText
                      || (primary ? trackingStatusLabel(locale, primary.trackingStatus) : trackingCopy(locale, 'summary.unshipped'))}
                  </span>
                  <span className="sc-data mt-1 block truncate text-[9px] text-[var(--sc-ink-muted)]">
                    {primary?.maskedWaybillNo || trackingCopy(locale, 'panel.notAvailable')}
                    {state && primary?.latestEventTime
                      ? ` · ${formatTrackingDateTime(locale, primary.latestEventTime)}`
                      : ''}
                  </span>
                </button>
              );
            })}
          </div>
        )}
      </section>

      <section className="sc-workspace-card rounded-2xl">
        <header className="sc-panel-header border-b px-4 py-3">
          <strong className="text-xs text-[var(--sc-ink)]">
            {trackingCopy(locale, 'workspace.boundary')}
          </strong>
        </header>
        <dl className="divide-y divide-[var(--sc-border)] px-4 text-[10px]">
          <div className="grid grid-cols-[100px_1fr] gap-3 py-3">
            <dt className="text-[var(--sc-ink-muted)]">{trackingCopy(locale, 'workspace.polling')}</dt>
            <dd className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'workspace.pollingValue')}</dd>
          </div>
          <div className="grid grid-cols-[100px_1fr] gap-3 py-3">
            <dt className="text-[var(--sc-ink-muted)]">{trackingCopy(locale, 'workspace.providerQuery')}</dt>
            <dd className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'workspace.providerQueryValue')}</dd>
          </div>
          <div className="grid grid-cols-[100px_1fr] gap-3 py-3">
            <dt className="text-[var(--sc-ink-muted)]">{trackingCopy(locale, 'workspace.deliveryState')}</dt>
            <dd className="font-bold text-[var(--sc-ink)]">{trackingCopy(locale, 'workspace.deliveryStateValue')}</dd>
          </div>
        </dl>
      </section>
    </aside>
  );
}
