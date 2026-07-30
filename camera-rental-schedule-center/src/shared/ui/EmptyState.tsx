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
    <div className="grid min-h-28 place-items-center rounded-lg border border-dashed border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-4 py-5 text-center">
      <div>
        <span className="mx-auto mb-2 grid h-8 w-8 place-items-center rounded-full bg-[var(--sc-green-soft)] text-[var(--sc-green)]">
          {icon}
        </span>
        <p className="text-xs font-bold text-[var(--sc-ink)]">{title}</p>
        {description && <p className="mt-1 text-[11px] leading-5 text-[var(--sc-ink-muted)]">{description}</p>}
      </div>
    </div>
  );
}
