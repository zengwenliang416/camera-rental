import { Cpu, Plus, X } from 'lucide-react';

import type { DeviceInstance, OrderItemNeed } from '../../../types';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { Button } from '../../../shared/ui/Button';

export function AllocationItemCard({
  item,
  devices,
  selectedIds,
  labels,
  onRemove,
  onOpenCandidates,
  canSelect,
}: {
  item: OrderItemNeed;
  devices: DeviceInstance[];
  selectedIds: string[];
  labels: {
    need: string;
    complete: string;
    missing: string;
    select: string;
    remove: string;
    selectUnavailable: string;
  };
  canSelect: boolean;
  onRemove: (deviceId: string) => void;
  onOpenCandidates: () => void;
}) {
  const missing = Math.max(0, item.quantity - selectedIds.length);
  return (
    <section className="sc-workspace-card rounded-2xl p-4">
      <header className="flex flex-wrap items-center gap-2 border-b border-[var(--sc-glass-hairline)] pb-3">
        <Cpu className="h-4 w-4 text-[var(--sc-blue)]" />
        <strong className="mr-auto text-sm text-[var(--sc-ink)]">{item.modelName}</strong>
        <StatusBadge tone="neutral">{labels.need} {item.quantity}</StatusBadge>
        <StatusBadge tone={missing === 0 ? 'green' : 'amber'}>
          {missing === 0 ? labels.complete : `${labels.missing} ${missing}`}
        </StatusBadge>
      </header>

      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        {selectedIds.map((deviceId) => {
          const device = devices.find((itemDevice) => itemDevice.id === deviceId);
          if (!device) return null;
          return (
            <div
              key={device.id}
              className="flex min-h-14 items-center justify-between gap-3 rounded-lg border border-[color-mix(in_srgb,var(--sc-green)_24%,var(--sc-border))] bg-[var(--sc-green-soft)] px-3 py-2"
            >
              <span className="min-w-0">
                <strong className="block text-xs text-[var(--sc-ink)]">{device.unitCode}</strong>
                <span className="sc-data block truncate text-[9px] text-[var(--sc-ink-muted)]">
                  {device.sn}
                </span>
              </span>
              <button
                type="button"
                onClick={() => onRemove(device.id)}
                aria-label={`${labels.remove} ${device.unitCode}`}
                className="grid h-11 w-11 shrink-0 place-items-center rounded-md text-[var(--sc-ink-muted)] hover:bg-white/50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          );
        })}
        {missing > 0 && (
          <Button
            onClick={onOpenCandidates}
            disabled={!canSelect}
            variant="glass"
            className="min-h-14 border-dashed"
            icon={<Plus />}
          >
            {canSelect ? labels.select : labels.selectUnavailable}
          </Button>
        )}
      </div>
    </section>
  );
}
