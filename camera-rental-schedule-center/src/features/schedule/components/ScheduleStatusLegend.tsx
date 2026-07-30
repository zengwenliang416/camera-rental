import type { ScheduleBlock } from '../../../types';

const legendClasses: Record<ScheduleBlock['type'] | 'FREE', string> = {
  RENTAL: 'border-blue-700 bg-blue-600',
  RESERVE: 'border-amber-700 bg-amber-500',
  REPAIR: 'border-rose-700 bg-rose-600',
  LOCK: 'border-zinc-800 bg-zinc-700',
  FREE: 'border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)]',
};

export function ScheduleStatusLegend({
  label,
  labels,
}: {
  label: string;
  labels: Record<ScheduleBlock['type'] | 'FREE', string>;
}) {
  return (
    <section
      aria-label={label}
      className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] px-3 py-2.5"
    >
      <strong className="text-[10px] font-black uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
        {label}
      </strong>
      {(Object.keys(labels) as Array<ScheduleBlock['type'] | 'FREE'>).map((type) => (
        <span key={type} className="inline-flex items-center gap-1.5 text-[10px] font-semibold text-[var(--sc-ink-soft)]">
          <span
            aria-hidden="true"
            className={`h-2.5 w-5 rounded-sm border ${legendClasses[type]}`}
          />
          {labels[type]}
        </span>
      ))}
    </section>
  );
}
