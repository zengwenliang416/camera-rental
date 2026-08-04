import {
  CalendarClock,
  Cpu,
  MapPin,
  Phone,
  Send,
  ShieldAlert,
  Sparkles,
  Truck,
  UserRound,
} from 'lucide-react';
import type { Key } from 'react';

import type { DeviceInstance, RentalOrder } from '../../../types';
import { BillableOccupiedRangeLegend } from '../../../shared/ui/BillableOccupiedRangeLegend';
import { IdentifierText } from '../../../shared/ui/IdentifierText';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import {
  getOrderActionAvailability,
  orderChannelTone,
  orderDisplayRanges,
  orderStatusTone,
} from '../orderModel';
import type { DeliveryOrderSummary } from '../../tracking/trackingModel';
import { DeliveryTrackingSummaryBadges } from '../../tracking/components/DeliveryTrackingSummaryBadges';
import { Button } from '../../../shared/ui/Button';

export function OrderCard({
  order,
  devices,
  permissions,
  labels,
  trackingSummary,
  onAssign,
  onShip,
  onOpenDevice,
  onOpenTracking,
}: {
  key?: Key;
  order: RentalOrder;
  devices: DeviceInstance[];
  permissions: { canAssign: boolean; canShip: boolean; canViewDevice: boolean };
  trackingSummary?: DeliveryOrderSummary;
  labels: {
    channel: Record<RentalOrder['channel'], string>;
    status: Record<RentalOrder['status'], string>;
    customer: string;
    phone: string;
    address: string;
    periodPending: string;
    billable: string;
    occupied: string;
    billableHint: string;
    occupiedHint: string;
    requirements: string;
    assigned: string;
    unassigned: string;
    created: string;
    assign: string;
    ship: string;
    openDevice: string;
    openTracking: string;
    returnOperational: string;
    noAction: string;
  };
  onAssign: () => void;
  onShip: () => void;
  onOpenDevice: (deviceId: string) => void;
  onOpenTracking: () => void;
}) {
  const actions = getOrderActionAvailability(order, permissions);
  const ranges = orderDisplayRanges(order);

  return (
    <article className="sc-workspace-card overflow-hidden rounded-2xl p-4 sm:p-5">
      <header className="flex flex-col gap-3 border-b border-[var(--sc-glass-hairline)] pb-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge tone={orderChannelTone(order.channel)}>{labels.channel[order.channel]}</StatusBadge>
            <StatusBadge tone={orderStatusTone(order.status)}>{labels.status[order.status]}</StatusBadge>
          </div>
          <IdentifierText value={order.orderNumber} emphasis />
          <p className="text-[10px] text-[var(--sc-ink-muted)]">{labels.created} {order.createdTime}</p>
        </div>
        <div className="sc-soft-panel grid max-w-xl gap-2 rounded-xl p-3 text-left lg:min-w-80">
          <div className="flex items-start gap-2">
            <UserRound className="mt-0.5 h-3.5 w-3.5 shrink-0 text-[var(--sc-blue)]" />
            <div className="min-w-0">
              <span className="block text-[9px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
                {labels.customer}
              </span>
              <strong className="block break-words text-xs text-[var(--sc-ink)]">
                {order.receiverName || '-'}
              </strong>
            </div>
          </div>
          <div className="flex items-start gap-2">
            <Phone className="mt-0.5 h-3.5 w-3.5 shrink-0 text-[var(--sc-ink-muted)]" />
            <div className="min-w-0">
              <span className="block text-[9px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
                {labels.phone}
              </span>
              <span className="sc-data break-all text-[10px] text-[var(--sc-ink)]">
                {order.receiverPhone || '-'}
              </span>
            </div>
          </div>
          <div className="flex items-start gap-2">
            <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0 text-[var(--sc-ink-muted)]" />
            <div className="min-w-0">
              <span className="block text-[9px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
                {labels.address}
              </span>
              <span className="break-words text-[10px] leading-5 text-[var(--sc-ink-soft)]">
                {order.receiverAddress || '-'}
              </span>
            </div>
          </div>
        </div>
      </header>

      <div className="mt-4 grid gap-4 xl:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)]">
        <BillableOccupiedRangeLegend
          billableLabel={labels.billable}
          occupiedLabel={labels.occupied}
          billableHint={order.rentalPeriodReady ? labels.billableHint : labels.periodPending}
          occupiedHint={labels.occupiedHint}
          billable={ranges.billable}
          occupied={ranges.occupied}
        />

        <section className="sc-soft-panel rounded-xl p-3">
          <h3 className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
            <Cpu className="h-3.5 w-3.5" />
            {labels.requirements}
          </h3>
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            {order.items.map((item) => {
              const assigned = item.assignedDeviceIds
                .map((deviceId) => devices.find((device) => device.id === deviceId))
                .filter((device): device is DeviceInstance => Boolean(device));
              return (
                <div key={`${item.modelId}-${item.rentalOrderItemId || 'pending'}`} className="rounded-xl border border-[var(--sc-glass-hairline)] bg-[var(--sc-glass-soft)] p-3">
                  <div className="flex items-start justify-between gap-2">
                    <strong className="text-xs text-[var(--sc-ink)]">{item.modelName}</strong>
                    <StatusBadge tone={assigned.length >= item.quantity ? 'green' : 'amber'}>
                      {assigned.length}/{item.quantity}
                    </StatusBadge>
                  </div>
                  <div className="mt-2 space-y-1">
                    {assigned.length === 0 ? (
                      <span className="text-[10px] font-semibold text-[var(--sc-ink-muted)]">{labels.unassigned}</span>
                    ) : (
                      assigned.map((device) => (
                        <IdentifierText
                          key={device.id}
                          label={labels.assigned}
                          value={`${device.unitCode} · ${device.sn}`}
                        />
                      ))
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      </div>

      <footer className="mt-4 flex flex-col gap-2 border-t border-[var(--sc-glass-hairline)] pt-4 sm:flex-row sm:flex-wrap sm:items-center">
        {trackingSummary && (
          <Button
            onClick={onOpenTracking}
            icon={<Truck />}
          >
            <span>{labels.openTracking}</span>
            <DeliveryTrackingSummaryBadges summary={trackingSummary} />
          </Button>
        )}
        {actions.canShip && (
          <Button onClick={onShip} variant="primary" icon={<Send />}>
            {labels.ship}
          </Button>
        )}
        {actions.canAssign && (
          <Button onClick={onAssign} variant="outline" icon={<Sparkles />}>
            {labels.assign}
          </Button>
        )}
        {actions.detailDeviceId && (
          <Button onClick={() => onOpenDevice(actions.detailDeviceId!)} variant="glass" icon={<CalendarClock />}>
            {labels.openDevice}
          </Button>
        )}
        {actions.returnRequiresOperationalFlow && (
          <span className="inline-flex min-h-11 items-center gap-2 rounded-lg bg-[var(--sc-amber-soft)] px-3 text-[10px] font-semibold text-[var(--sc-ink-soft)]">
            <ShieldAlert className="h-4 w-4 shrink-0 text-[var(--sc-amber)]" />
            {labels.returnOperational}
          </span>
        )}
        {!trackingSummary && !actions.canAssign && !actions.canShip && !actions.detailDeviceId && !actions.returnRequiresOperationalFlow && (
          <span className="text-[10px] font-semibold text-[var(--sc-ink-muted)]">{labels.noAction}</span>
        )}
      </footer>
    </article>
  );
}
