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
    ? 'bg-rose-50 text-rose-600'
    : tone === 'neutral'
      ? 'bg-zinc-100 text-zinc-600'
      : 'bg-blue-50 text-blue-700';

  return (
    <header className="flex min-h-16 items-center justify-between gap-3 border-b border-[var(--sc-border)] bg-gradient-to-b from-[var(--sc-surface-soft)] to-[var(--sc-surface)] px-4 py-3">
      <div className="flex min-w-0 items-center gap-3">
        {step && (
          <span className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl font-mono text-[10px] font-black ${stepTone}`}>
            {step}
          </span>
        )}
        <div className="min-w-0">
          <p className="mb-0.5 font-mono text-[9px] font-black tracking-[0.14em] text-blue-600">
            {eyebrow}
          </p>
          <h2 className="truncate text-sm font-black text-[var(--sc-ink)]">{title}</h2>
        </div>
      </div>
      {badge && <div className="shrink-0">{badge}</div>}
    </header>
  );
}
