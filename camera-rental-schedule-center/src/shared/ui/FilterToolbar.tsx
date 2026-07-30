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
      className="grid gap-3 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-3 lg:flex lg:items-end"
    >
      <div className="grid min-w-0 flex-1 gap-3 sm:grid-cols-2 lg:flex lg:items-end">{children}</div>
      {summary && (
        <div className="rounded-lg bg-[var(--sc-surface-soft)] px-3 py-2 text-[10px] font-semibold leading-5 text-[var(--sc-ink-muted)]">
          {summary}
        </div>
      )}
    </section>
  );
}
