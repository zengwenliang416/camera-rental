import { ArrowUpRight, CheckCircle2 } from 'lucide-react';
import type { ReactNode } from 'react';
import { EmptyState } from '../../shared/ui/EmptyState';

export interface QueueRow {
  id: string;
  primary: string;
  secondary: string;
  meta: string;
  state: ReactNode;
  onSelect: () => void;
  disabled?: boolean;
}

export function DashboardQueueCard({
  step,
  title,
  count,
  emptyText,
  actionLabel,
  onViewAll,
  rows,
}: {
  step: string;
  title: string;
  count: number;
  emptyText: string;
  actionLabel: string;
  onViewAll: () => void;
  rows: QueueRow[];
}) {
  return (
    <section className="sc-surface min-w-0 overflow-hidden rounded-lg">
      <header className="flex min-h-14 items-center gap-3 border-b border-[var(--sc-border)] bg-[var(--sc-surface-soft)] px-3">
        <span className="sc-data text-xs font-bold text-[var(--sc-blue)]">{step}</span>
        <h3 className="min-w-0 flex-1 truncate text-xs font-black text-[var(--sc-ink)]">{title}</h3>
        <span className="sc-data grid h-7 min-w-7 place-items-center rounded-full bg-[var(--sc-brand-soft)] px-2 text-[10px] text-[var(--sc-brand)]">{count}</span>
      </header>
      <div className="p-2">
        {rows.length === 0 ? (
          <EmptyState icon={<CheckCircle2 className="h-4 w-4" />} title={emptyText} />
        ) : (
          <div className="space-y-1">
            {rows.slice(0, 4).map((row) => (
              <button
                key={row.id}
                type="button"
                onClick={row.onSelect}
                disabled={row.disabled}
                className="grid min-h-[66px] w-full grid-cols-[minmax(0,1fr)_auto] gap-3 rounded-md px-2.5 py-2 text-left transition hover:bg-[var(--sc-surface-hover)] disabled:cursor-not-allowed disabled:opacity-55"
              >
                <span className="min-w-0">
                  <strong className="sc-data block truncate text-[10px] text-[var(--sc-ink)]">{row.primary}</strong>
                  <span className="mt-1 block truncate text-[11px] font-semibold text-[var(--sc-ink-soft)]">{row.secondary}</span>
                  <small className="mt-1 block truncate text-[9px] text-[var(--sc-ink-muted)]">{row.meta}</small>
                </span>
                <span className="self-center">{row.state}</span>
              </button>
            ))}
          </div>
        )}
      </div>
      <button type="button" onClick={onViewAll} className="flex min-h-11 w-full items-center justify-center gap-2 border-t border-[var(--sc-border)] text-[10px] font-bold text-[var(--sc-blue)]">
        {actionLabel}
        <ArrowUpRight className="h-3.5 w-3.5" />
      </button>
    </section>
  );
}
