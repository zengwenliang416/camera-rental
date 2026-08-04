import type { ScheduleBlock } from '../../../types';

const legendClasses: Record<ScheduleBlock['type'] | 'FREE', string> = {
  RENTAL: 'border-[var(--sc-blue)] bg-[var(--sc-blue)]',
  RESERVE: 'border-[var(--sc-amber)] bg-[var(--sc-amber)]',
  REPAIR: 'border-[var(--sc-red)] bg-[var(--sc-red)]',
  LOCK: 'border-[var(--sc-ink)] bg-[var(--sc-ink)]',
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
      className="sc-soft-panel flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl px-3 py-2.5"
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
