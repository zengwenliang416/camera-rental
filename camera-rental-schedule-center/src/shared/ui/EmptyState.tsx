import type { ReactNode } from 'react';

export function EmptyState({
  icon,
  title,
  description,
}: {
  icon: ReactNode;
  title: string;
  description?: string;
}) {
  return (
    <div className="sc-empty-state grid min-h-32 place-items-center rounded-xl border border-dashed px-5 py-7 text-center">
      <div>
        <span className="mx-auto mb-3 grid h-10 w-10 place-items-center rounded-xl bg-[var(--sc-green-soft)] text-[var(--sc-green)]">
          {icon}
        </span>
        <p className="text-sm font-bold text-[var(--sc-ink)]">{title}</p>
        {description && <p className="mx-auto mt-1 max-w-md text-[11px] leading-5 text-[var(--sc-ink-muted)]">{description}</p>}
      </div>
    </div>
  );
}
