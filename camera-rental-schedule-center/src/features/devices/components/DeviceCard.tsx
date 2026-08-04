import { CalendarClock, MapPin, Wrench } from 'lucide-react';
import type { Key } from 'react';

import type { DeviceInstance } from '../../../types';
import { IdentifierText } from '../../../shared/ui/IdentifierText';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { deviceCardPresentation, deviceStatusTone } from '../deviceModel';
import { Button } from '../../../shared/ui/Button';

export function DeviceCard({
  device,
  labels,
  canOpen,
  onOpen,
}: {
  key?: Key;
  device: DeviceInstance;
  labels: {
    status: Record<DeviceInstance['status'], string>;
    order: string;
    customer: string;
    available: string;
    now: string;
    unavailable: string;
    period: string;
    note: string;
    warehouse: string;
    detail: string;
    noAccess: string;
  };
  canOpen: boolean;
  onOpen: () => void;
}) {
  const presentation = deviceCardPresentation(device);
  const availability = presentation.availability.kind === 'now'
    ? labels.now
    : presentation.availability.kind === 'date'
      ? presentation.availability.value
      : labels.unavailable;

  return (
    <article className="sc-workspace-card grid gap-4 rounded-2xl p-4 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)_auto] lg:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge tone={deviceStatusTone(device.status)}>{labels.status[device.status]}</StatusBadge>
          <span className="text-xs font-bold text-[var(--sc-ink-soft)]">{device.modelName}</span>
        </div>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          <IdentifierText value={device.unitCode} emphasis />
          <IdentifierText value={device.sn} />
        </div>
      </div>

      <div className="grid gap-2 text-[11px] text-[var(--sc-ink-soft)] sm:grid-cols-2">
        <span><strong className="text-[var(--sc-ink)]">{labels.order}</strong> <span className="sc-data">{device.currentOrderId || '-'}</span></span>
        <span><strong className="text-[var(--sc-ink)]">{labels.customer}</strong> {device.currentCustomer || '-'}</span>
        <span className="inline-flex items-center gap-1.5"><CalendarClock className="h-3.5 w-3.5 text-[var(--sc-blue)]" />{labels.available} {availability}</span>
        <span className="inline-flex items-center gap-1.5"><MapPin className="h-3.5 w-3.5 text-[var(--sc-green)]" />{labels.period} {device.currentPeriod ? `${device.currentPeriod.startDate} → ${device.currentPeriod.endDate}` : '-'}</span>
        {presentation.note && <span className="sm:col-span-2 inline-flex items-start gap-1.5"><Wrench className="mt-0.5 h-3.5 w-3.5 shrink-0 text-[var(--sc-amber)]" />{presentation.note.kind === 'warehouse' ? labels.warehouse : labels.note} {presentation.note.value}</span>}
      </div>

      <Button
        onClick={onOpen}
        disabled={!canOpen}
        variant="glass"
      >
        {canOpen ? labels.detail : labels.noAccess}
      </Button>
    </article>
  );
}
