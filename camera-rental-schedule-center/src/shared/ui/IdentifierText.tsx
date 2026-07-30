import type { Key } from 'react';

export function IdentifierText({
  label,
  value,
  emphasis = false,
}: {
  key?: Key;
  label?: string;
  value: string;
  emphasis?: boolean;
}) {
  return (
    <span className="min-w-0">
      {label && (
        <span className="block text-[9px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
          {label}
        </span>
      )}
      <span
        className={`sc-data block break-all text-[var(--sc-ink)] ${
          emphasis ? 'text-sm font-black' : 'text-[11px] font-bold'
        }`}
      >
        {value}
      </span>
    </span>
  );
}
