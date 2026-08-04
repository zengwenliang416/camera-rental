import type { ReactNode } from 'react';

export function FeaturePageHeader({
  eyebrow,
  title,
  description,
  meta,
  actions,
}: {
  eyebrow: string;
  title: string;
  description: string;
  meta?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className="sc-feature-hero relative overflow-hidden rounded-2xl px-4 py-5 sm:px-6 sm:py-7">
      <div className="sc-subtle-grid pointer-events-none absolute inset-x-0 top-0 h-28 opacity-40" />
      <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-3xl">
          <div className="sc-data text-[9px] font-bold tracking-[0.18em] text-[var(--sc-blue)]">{eyebrow}</div>
          <h1 className="mt-2 text-2xl font-black tracking-[-0.04em] text-[var(--sc-ink)] sm:text-[30px]">{title}</h1>
          <p className="mt-2 max-w-2xl text-xs leading-6 text-[var(--sc-ink-soft)] sm:text-sm">{description}</p>
          {meta && <div className="mt-3">{meta}</div>}
        </div>
        {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
      </div>
    </header>
  );
}
