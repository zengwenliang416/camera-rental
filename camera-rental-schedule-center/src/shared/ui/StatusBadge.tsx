import type { ReactNode } from 'react';

export type StatusTone = 'neutral' | 'blue' | 'green' | 'amber' | 'red';

const toneClasses: Record<StatusTone, string> = {
  neutral: 'border-[var(--sc-border)] bg-[var(--sc-surface-soft)] text-[var(--sc-ink-soft)]',
  blue: 'border-[color-mix(in_srgb,var(--sc-blue)_25%,var(--sc-border))] bg-[var(--sc-blue-soft)] text-[var(--sc-blue)]',
  green: 'border-[color-mix(in_srgb,var(--sc-green)_25%,var(--sc-border))] bg-[var(--sc-green-soft)] text-[var(--sc-green)]',
  amber: 'border-[color-mix(in_srgb,var(--sc-amber)_25%,var(--sc-border))] bg-[var(--sc-amber-soft)] text-[var(--sc-amber)]',
  red: 'border-[color-mix(in_srgb,var(--sc-red)_25%,var(--sc-border))] bg-[var(--sc-red-soft)] text-[var(--sc-red)]',
};

export function StatusBadge({
  tone = 'neutral',
  icon,
  children,
}: {
  tone?: StatusTone;
  icon?: ReactNode;
  children: ReactNode;
}) {
  return (
    <span className={`inline-flex min-h-6 shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 text-[10px] font-bold ${toneClasses[tone]}`}>
      {icon}
      {children}
    </span>
  );
}
