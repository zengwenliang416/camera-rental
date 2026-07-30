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
    <>
      <div className="flex items-start justify-between gap-2">
        <span className={`grid h-8 w-8 place-items-center rounded-md ${toneStyles[metric.tone]}`}>
          {metric.icon}
        </span>
        {metric.detail && (
          <span className="sc-data text-[9px] text-[var(--sc-ink-muted)]">{metric.detail}</span>
        )}
      </div>
      <div className="mt-4">
        <div className="flex items-baseline gap-1">
          <strong className="sc-data text-2xl tracking-[-0.05em] text-[var(--sc-ink)]">
            {metric.value}
          </strong>
          <span className="text-[10px] text-[var(--sc-ink-muted)]">{metric.unit}</span>
        </div>
        <p className="mt-1 text-[11px] font-semibold text-[var(--sc-ink-soft)]">{metric.label}</p>
      </div>
    </>
  );
}

export function OperationalMetricCard({ metric }: { key?: Key; metric: OperationalMetric }) {
  const className =
    'sc-surface min-h-32 rounded-lg p-3 text-left transition hover:border-[var(--sc-border-strong)]';
  return metric.onSelect ? (
    <button type="button" onClick={metric.onSelect} className={`${className} hover:-translate-y-0.5`}>
      <MetricContent metric={metric} />
    </button>
  ) : (
    <article className={className}>
      <MetricContent metric={metric} />
    </article>
  );
}
