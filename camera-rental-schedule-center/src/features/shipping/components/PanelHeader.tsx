import type { ReactNode } from 'react';

interface PanelHeaderProps {
  step?: string;
  eyebrow: string;
  title: string;
  badge?: ReactNode;
  tone?: 'blue' | 'red' | 'neutral';
}

export function PanelHeader({
  step,
  eyebrow,
  title,
  badge,
  tone = 'blue',
}: PanelHeaderProps) {
  const stepTone = tone === 'red'
    ? 'border-[color-mix(in_srgb,var(--sc-red)_24%,var(--sc-border))] bg-[var(--sc-red-soft)] text-[var(--sc-red)]'
    : tone === 'neutral'
      ? 'border-[var(--sc-border)] bg-[var(--sc-surface-soft)] text-[var(--sc-ink-soft)]'
      : 'border-[color-mix(in_srgb,var(--sc-blue)_24%,var(--sc-border))] bg-[var(--sc-blue-soft)] text-[var(--sc-blue)]';

  return (
    <header className="sc-panel-header flex min-h-16 flex-wrap items-center justify-between gap-3 border-b px-4 py-3">
      <div className="flex min-w-0 flex-1 items-center gap-3">
        {step && (
          <span className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl border font-mono text-[10px] font-black ${stepTone}`}>
            {step}
          </span>
        )}
        <div className="min-w-0 flex-1">
          <p className="mb-0.5 font-mono text-[9px] font-black tracking-[0.14em] text-[var(--sc-brand)]">
            {eyebrow}
          </p>
          <h2 className="break-words text-sm font-black leading-5 text-[var(--sc-ink)]">{title}</h2>
        </div>
      </div>
      {badge && <div className="shrink-0">{badge}</div>}
    </header>
  );
}
