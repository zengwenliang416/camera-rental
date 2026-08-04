import type { OperationalMetric } from './OperationalMetricGrid';
import type { StatusTone } from './StatusBadge';
import type { Key } from 'react';

const toneStyles: Record<StatusTone, string> = {
  neutral: 'text-[var(--sc-ink)] bg-[var(--sc-surface-soft)]',
  blue: 'text-[var(--sc-blue)] bg-[var(--sc-blue-soft)]',
  green: 'text-[var(--sc-green)] bg-[var(--sc-green-soft)]',
  amber: 'text-[var(--sc-amber)] bg-[var(--sc-amber-soft)]',
  red: 'text-[var(--sc-red)] bg-[var(--sc-red-soft)]',
};

function MetricContent({ metric }: { metric: OperationalMetric }) {
  return (
    <div className="flex h-full min-w-0 flex-col justify-between">
      <div className="flex items-start justify-between gap-2">
        <span className={`grid size-9 place-items-center rounded-xl ${toneStyles[metric.tone]}`}>
          {metric.icon}
        </span>
        {metric.detail && (
          <span className="sc-data rounded-full border border-[var(--sc-glass-hairline)] bg-[var(--sc-glass-soft)] px-2 py-1 text-[9px] text-[var(--sc-ink-muted)]">
            {metric.detail}
          </span>
        )}
      </div>
      <div className="mt-4">
        <div className="flex items-baseline gap-1">
          <strong className="sc-data text-[1.7rem] leading-none tracking-[-0.055em] text-[var(--sc-ink)]">
            {metric.value}
          </strong>
          <span className="text-[10px] text-[var(--sc-ink-muted)]">{metric.unit}</span>
        </div>
        <p className="mt-1 text-[11px] font-semibold text-[var(--sc-ink-soft)]">{metric.label}</p>
      </div>
    </div>
  );
}

export function OperationalMetricCard({ metric }: { key?: Key; metric: OperationalMetric }) {
  const className = 'sc-metric-card min-h-36 p-4 text-left';
  return metric.onSelect ? (
    <button type="button" onClick={metric.onSelect} className={`${className} w-full`}>
      <MetricContent metric={metric} />
    </button>
  ) : (
    <article className={className}>
      <MetricContent metric={metric} />
    </article>
  );
}
