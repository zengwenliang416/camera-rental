import { Check, ShieldAlert } from 'lucide-react';

import type { DeviceInstance, ScheduleBlock } from '../../../types';
import { checkDeviceAvailability, inclusiveDateFromExclusive } from '../../../lib/scheduleEngine';

export function AllocationCandidatePicker({
  modelId,
  devices,
  blocks,
  occupyStartDate,
  occupyEndDateExclusive,
  excludeOrderId,
  selectedIds,
  maximum,
  labels,
  onToggle,
  onClose,
}: {
  modelId: string;
  devices: DeviceInstance[];
  blocks: ScheduleBlock[];
  occupyStartDate: string;
  occupyEndDateExclusive: string;
  excludeOrderId: string;
  selectedIds: string[];
  maximum: number;
  labels: {
    candidates: string;
    available: string;
    noCandidates: string;
    close: string;
  };
  onToggle: (deviceId: string) => void;
  onClose: () => void;
}) {
  const occupiedEndDate = inclusiveDateFromExclusive(occupyEndDateExclusive) || '';
  const candidates = devices
    .filter((device) => device.modelId === modelId)
    .map((device) => ({
      device,
      availability: checkDeviceAvailability(
        device,
        blocks,
        occupyStartDate,
        occupiedEndDate,
        excludeOrderId
      ),
    }));
  const availableCount = candidates.filter((candidate) => candidate.availability.available).length;

  return (
    <section className="rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-ink)] p-3 text-white">
      <header className="flex items-center justify-between gap-3 border-b border-white/10 pb-2">
        <div>
          <strong className="text-xs">{labels.candidates}</strong>
          <span className="ml-2 text-[10px] text-zinc-400">
            {availableCount} {labels.available}
          </span>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="min-h-11 rounded-md px-3 text-[10px] font-bold text-zinc-300 hover:bg-white/10 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-300/40"
        >
          {labels.close}
        </button>
      </header>

      {availableCount === 0 ? (
        <p className="mt-3 flex items-start gap-2 rounded-md bg-white/5 p-3 text-[11px] leading-5 text-zinc-300">
          <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
          {labels.noCandidates}
        </p>
      ) : (
        <div className="mt-3 grid max-h-56 gap-2 overflow-y-auto sm:grid-cols-2 lg:grid-cols-3">
          {candidates.map(({ device, availability }) => {
            const selected = selectedIds.includes(device.id);
            const selectionFull = !selected && selectedIds.length >= maximum;
            const disabled = !availability.available || selectionFull;
            return (
              <button
                key={device.id}
                type="button"
                disabled={disabled}
                onClick={() => onToggle(device.id)}
                className={`min-h-16 rounded-lg border p-2 text-left focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-300/40 disabled:cursor-not-allowed ${
                  selected
                    ? 'border-blue-400 bg-blue-600 text-white'
                    : availability.available
                      ? 'border-white/15 bg-white/8 text-zinc-100 hover:bg-white/12'
                      : 'border-white/5 bg-white/3 text-zinc-500 opacity-70'
                }`}
              >
                <span className="flex items-center justify-between gap-2 text-xs font-black">
                  {device.unitCode}
                  {selected && <Check className="h-3.5 w-3.5" />}
                </span>
                <span className="sc-data mt-1 block truncate text-[9px]">{device.sn}</span>
                {!availability.available && (
                  <span className="mt-1 block truncate text-[9px] text-rose-300">
                    {availability.reason}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}
