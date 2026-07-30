import { CalendarRange } from 'lucide-react';

export function DateRangeDisplay({
  label,
  startDate,
  endDate,
  hint,
  tone = 'neutral',
}: {
  label: string;
  startDate?: string;
  endDate?: string;
  hint?: string;
  tone?: 'neutral' | 'blue' | 'amber';
}) {
  const toneClass = {
    neutral: 'border-[var(--sc-border)] bg-[var(--sc-surface-soft)]',
    blue: 'border-[color-mix(in_srgb,var(--sc-blue)_24%,var(--sc-border))] bg-[var(--sc-blue-soft)]',
    amber: 'border-[color-mix(in_srgb,var(--sc-amber)_28%,var(--sc-border))] bg-[var(--sc-amber-soft)]',
  }[tone];

  return (
    <div className={`rounded-lg border px-3 py-2.5 ${toneClass}`}>
      <div className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-[0.12em] text-[var(--sc-ink-muted)]">
        <CalendarRange className="h-3.5 w-3.5" />
        <span>{label}</span>
      </div>
      <div className="sc-data mt-1 text-xs font-bold text-[var(--sc-ink)]">
        {startDate && endDate ? `${startDate} → ${endDate}` : '-'}
      </div>
      {hint && <p className="mt-1 text-[10px] leading-4 text-[var(--sc-ink-muted)]">{hint}</p>}
    </div>
  );
}
