import { DateRangeDisplay } from './DateRangeDisplay';

export function BillableOccupiedRangeLegend({
  billableLabel,
  occupiedLabel,
  billableHint,
  occupiedHint,
  billable,
  occupied,
}: {
  billableLabel: string;
  occupiedLabel: string;
  billableHint: string;
  occupiedHint: string;
  billable?: { startDate?: string; endDate?: string };
  occupied?: { startDate?: string; endDate?: string };
}) {
  return (
    <div className="grid gap-2 sm:grid-cols-2" aria-label={`${billableLabel} / ${occupiedLabel}`}>
      <DateRangeDisplay
        label={billableLabel}
        startDate={billable?.startDate}
        endDate={billable?.endDate}
        hint={billableHint}
        tone="blue"
      />
      <DateRangeDisplay
        label={occupiedLabel}
        startDate={occupied?.startDate}
        endDate={occupied?.endDate}
        hint={occupiedHint}
        tone="amber"
      />
    </div>
  );
}
