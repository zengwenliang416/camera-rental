import type { ReactNode } from 'react';

export function FilterToolbar({
  label,
  summary,
  children,
}: {
  label: string;
  summary?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section
      aria-label={label}
      className="sc-filter-panel grid gap-3 rounded-2xl p-3 lg:flex lg:items-end"
    >
      <div className="grid min-w-0 flex-1 gap-3 sm:grid-cols-2 lg:flex lg:items-end">{children}</div>
      {summary && (
        <div className="sc-soft-panel rounded-xl px-3 py-2 text-[10px] font-semibold leading-5 text-[var(--sc-ink-muted)]">
          {summary}
        </div>
      )}
    </section>
  );
}
