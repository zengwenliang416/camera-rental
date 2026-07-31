import { Wrench } from 'lucide-react';

import type { DeviceInstance, ScheduleBlock } from '../../../types';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { StatusBadge, type StatusTone } from '../../../shared/ui/StatusBadge';
import { usePreferences } from '../../preferences/PreferenceContext';
import { trackingCopy, trackingStatusLabel } from '../../tracking/trackingCopy';
import {
  trackingStatusPresentation,
  type DeliveryOrderSummary,
} from '../../tracking/trackingModel';
import type { ScheduleDay, ScheduleViewMode } from '../scheduleModel';
import { blocksForDevice } from '../scheduleModel';
import { ScheduleTimeline } from './ScheduleTimeline';

const blockClasses: Record<ScheduleBlock['type'], string> = {
  RENTAL: 'border-blue-700 bg-blue-600 text-white',
  RESERVE: 'border-amber-700 bg-amber-500 text-zinc-950',
  REPAIR: 'border-rose-700 bg-rose-600 text-white',
  LOCK: 'border-zinc-800 bg-zinc-700 text-white',
};

const blockedCellClasses: Partial<Record<DeviceInstance['status'], string>> = {
  REPAIR: 'border-rose-300 bg-rose-50 text-rose-700 dark:border-rose-900 dark:bg-rose-950/50 dark:text-rose-300',
  LOCKED: 'border-zinc-400 bg-zinc-100 text-zinc-700 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300',
};

function statusPresentation(status: DeviceInstance['status'], labels: ScheduleDeviceTableProps['labels']) {
  const values: Record<DeviceInstance['status'], { label: string; tone: StatusTone }> = {
    IDLE: { label: labels.statusIdle, tone: 'green' },
    RENTING: { label: labels.statusRenting, tone: 'blue' },
    RESERVED: { label: labels.statusReserved, tone: 'amber' },
    PENDING_RETURN: { label: labels.statusRenting, tone: 'amber' },
    REPAIR: { label: labels.statusRepair, tone: 'red' },
    LOCKED: { label: labels.statusLocked, tone: 'neutral' },
  };
  return values[status];
}

interface ScheduleDeviceTableProps {
  devices: DeviceInstance[];
  blocks: ScheduleBlock[];
  days: ScheduleDay[];
  viewMode: ScheduleViewMode;
  trackingByOrderId: Record<string, DeliveryOrderSummary>;
  labels: {
    internalScroller: string;
    noMatches: string;
    noMatchesDetail: string;
    deviceIdentity: string;
    currentStatus: string;
    relatedOrder: string;
    customer: string;
    expectedAvailable: string;
    openDetail: string;
    availableNow: string;
    free: string;
    statusIdle: string;
    statusRenting: string;
    statusReserved: string;
    statusRepair: string;
    statusLocked: string;
    blockRental: string;
    blockReserve: string;
    blockRepair: string;
    blockLock: string;
  };
  onOpenDevice: (deviceId: string) => void;
  onOpenOrder: (orderId: string) => void;
  onOpenTracking: (orderId: string) => void;
}

function firstTrackedOrderSummary(
  blocks: ScheduleBlock[],
  trackingByOrderId: Record<string, DeliveryOrderSummary>
) {
  for (const block of blocks) {
    if (!block.orderId) continue;
    const summary = trackingByOrderId[block.orderId];
    if (summary) return summary;
  }
  return null;
}

export function ScheduleDeviceTable(props: ScheduleDeviceTableProps) {
  const { locale } = usePreferences();

  if (props.devices.length === 0) {
    return (
      <EmptyState
        icon={<Wrench className="h-4 w-4" />}
        title={props.labels.noMatches}
        description={props.labels.noMatchesDetail}
      />
    );
  }

  if (props.viewMode === 'table') {
    return (
      <div className="grid gap-2">
        {props.devices.map((device) => {
          const deviceBlocks = blocksForDevice(props.blocks, device.id);
          const trackedSummary = firstTrackedOrderSummary(deviceBlocks, props.trackingByOrderId);
          const trackedPackage = trackedSummary?.packages[0];
          const trackedState = trackedPackage
            ? trackingStatusPresentation(trackedPackage.trackingStatus)
            : null;
          const status = statusPresentation(device.status, props.labels);
          return (
            <article
              key={device.id}
              className="grid gap-3 rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface)] p-4 md:grid-cols-[minmax(180px,1.2fr)_repeat(4,minmax(120px,1fr))_auto] md:items-center"
            >
              <button
                type="button"
                onClick={() => props.onOpenDevice(device.id)}
                className="min-h-11 rounded-md text-left focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
              >
                <strong className="block text-sm text-[var(--sc-ink)]">{device.unitCode}</strong>
                <span className="sc-data text-[10px] text-[var(--sc-ink-muted)]">{device.sn}</span>
              </button>
              <StatusBadge tone={status.tone}>{status.label}</StatusBadge>
              <div className="space-y-2">
                <span className="sc-data block text-[11px] text-[var(--sc-ink-soft)]">
                  {trackedSummary ? `RO-${trackedSummary.rentalOrderId}` : device.currentOrderId || '-'}
                </span>
                {trackedSummary && trackedPackage && trackedState && (
                  <button
                    type="button"
                    onClick={() => props.onOpenTracking(String(trackedSummary.rentalOrderId))}
                    className="min-h-11 rounded-lg text-left focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
                  >
                    <div className="flex flex-wrap gap-2">
                      <StatusBadge tone={trackedState.tone}>
                        {trackingStatusLabel(locale, trackedPackage.trackingStatus)}
                      </StatusBadge>
                      {trackedSummary.risks.length > 0 && (
                        <StatusBadge tone="red">
                          {trackingCopy(locale, 'summary.risk')} {trackedSummary.risks.length}
                        </StatusBadge>
                      )}
                    </div>
                  </button>
                )}
              </div>
              <span className="text-xs text-[var(--sc-ink-soft)]">{device.currentCustomer || '-'}</span>
              <span className="text-xs font-semibold text-[var(--sc-ink)]">
                {device.expectedAvailableDate
                  || (device.status === 'REPAIR' || device.status === 'LOCKED'
                    ? status.label
                    : props.labels.availableNow)}
              </span>
              <button
                type="button"
                onClick={() => props.onOpenDevice(device.id)}
                className="min-h-11 rounded-lg border border-[var(--sc-border-strong)] px-3 text-xs font-bold text-[var(--sc-ink)] hover:bg-[var(--sc-surface-soft)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
              >
                {props.labels.openDetail}
              </button>
            </article>
          );
        })}
      </div>
    );
  }

  return (
    <section
      aria-label={props.labels.internalScroller}
      tabIndex={0}
      className="overflow-x-auto rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
    >
      <table className="min-w-[1220px] border-collapse text-left">
        <thead className="bg-[var(--sc-surface-soft)] text-[10px] font-bold uppercase tracking-[0.08em] text-[var(--sc-ink-muted)]">
          <tr>
            <th
              scope="col"
              className="sticky left-0 z-20 w-56 min-w-56 border-r border-[var(--sc-border)] bg-[var(--sc-surface-soft)] px-4 py-3"
            >
              {props.labels.deviceIdentity}
            </th>
            <ScheduleTimeline days={props.days} />
          </tr>
        </thead>
        <tbody>
          {props.devices.map((device) => {
            const deviceBlocks = blocksForDevice(props.blocks, device.id);
            const trackedSummary = firstTrackedOrderSummary(deviceBlocks, props.trackingByOrderId);
            const trackedPackage = trackedSummary?.packages[0];
            const trackedState = trackedPackage
              ? trackingStatusPresentation(trackedPackage.trackingStatus)
              : null;
            const status = statusPresentation(device.status, props.labels);
            const blockedCellClass = blockedCellClasses[device.status];
            return (
              <tr key={device.id} className="border-t border-[var(--sc-border)]">
                <th
                  scope="row"
                  className="sticky left-0 z-10 w-56 min-w-56 border-r border-[var(--sc-border)] bg-[var(--sc-surface)] px-4 py-2"
                >
                  <div className="grid gap-1">
                    <button
                      type="button"
                      onClick={() => props.onOpenDevice(device.id)}
                      className="min-h-11 w-full rounded-md text-left focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
                    >
                      <span className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2">
                        <strong className="truncate text-xs text-[var(--sc-ink)]">{device.unitCode}</strong>
                        <StatusBadge tone={status.tone}>{status.label}</StatusBadge>
                      </span>
                      <span className="sc-data mt-1 block truncate text-[9px] text-[var(--sc-ink-muted)]">
                        {device.sn}
                      </span>
                    </button>
                    {trackedSummary && trackedPackage && trackedState && (
                      <span className="mt-2 flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => props.onOpenTracking(String(trackedSummary.rentalOrderId))}
                          className="rounded-md focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
                        >
                          <StatusBadge tone={trackedState.tone}>
                            {trackingStatusLabel(locale, trackedPackage.trackingStatus)}
                          </StatusBadge>
                        </button>
                        {trackedSummary.risks.length > 0 && (
                          <StatusBadge tone="red">
                            {trackingCopy(locale, 'summary.risk')} {trackedSummary.risks.length}
                          </StatusBadge>
                        )}
                      </span>
                    )}
                  </div>
                </th>
                {props.days.map((day, index) => {
                  const block = deviceBlocks.find(
                    (item) => day.dateStr >= item.startDate && day.dateStr <= item.endDate
                  );
                  if (!block) {
                    if (blockedCellClass) {
                      return (
                        <td key={day.dateStr} className="border-r border-[var(--sc-border)] p-1">
                          <button
                            type="button"
                            onClick={() => props.onOpenDevice(device.id)}
                            aria-label={`${device.unitCode} ${day.dateStr} ${status.label}`}
                            className={`h-11 w-full rounded-md border px-1 text-[9px] font-bold focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] ${blockedCellClass}`}
                          >
                            {status.label}
                          </button>
                        </td>
                      );
                    }
                    return (
                      <td
                        key={day.dateStr}
                        className={`border-r border-[var(--sc-border)] p-1 ${
                          day.isToday ? 'bg-[color-mix(in_srgb,var(--sc-blue-soft)_42%,transparent)]' : ''
                        }`}
                      >
                        <button
                          type="button"
                          onClick={() => props.onOpenDevice(device.id)}
                          className="h-11 w-full rounded-md text-[9px] font-bold text-[var(--sc-ink-muted)] hover:bg-[var(--sc-green-soft)] hover:text-[var(--sc-green)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
                        >
                          {props.labels.free}
                        </button>
                      </td>
                    );
                  }
                  const isStart = block.startDate === day.dateStr || index === 0;
                  const isEnd = block.endDate === day.dateStr || index === props.days.length - 1;
                  const blockLabel = {
                    RENTAL: props.labels.blockRental,
                    RESERVE: props.labels.blockReserve,
                    REPAIR: props.labels.blockRepair,
                    LOCK: props.labels.blockLock,
                  }[block.type];
                  return (
                    <td key={day.dateStr} className="border-r border-[var(--sc-border)] p-1">
                      <button
                        type="button"
                        title={`${block.startDate} → ${block.endDate}`}
                        aria-label={`${device.unitCode} ${day.dateStr} ${blockLabel}${
                          block.orderNumber ? ` ${block.orderNumber}` : ''
                        }`}
                        onClick={() =>
                          block.orderId ? props.onOpenOrder(block.orderId) : props.onOpenDevice(device.id)
                        }
                        className={`h-11 w-full truncate border px-1 text-[9px] font-black focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] ${
                          isStart ? 'rounded-l-md' : ''
                        } ${isEnd ? 'rounded-r-md' : ''} ${blockClasses[block.type]}`}
                      >
                        {isStart ? block.orderNumber || block.statusText || blockLabel : '•'}
                      </button>
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}
