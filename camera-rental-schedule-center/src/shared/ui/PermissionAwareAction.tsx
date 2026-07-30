import type { ReactNode } from 'react';

export function PermissionAwareAction({
  allowed,
  label,
  deniedLabel,
  icon,
  onSelect,
  tone = 'secondary',
}: {
  allowed: boolean;
  label: string;
  deniedLabel: string;
  icon?: ReactNode;
  onSelect: () => void;
  tone?: 'primary' | 'secondary';
}) {
  return (
    <button
      type="button"
      disabled={!allowed}
      onClick={onSelect}
      className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 text-xs font-bold focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] disabled:cursor-not-allowed disabled:opacity-45 ${
        tone === 'primary'
          ? 'bg-[var(--sc-ink)] text-[var(--sc-surface)]'
          : 'border border-[var(--sc-border-strong)] text-[var(--sc-ink)]'
      }`}
    >
      {icon}
      {allowed ? label : deniedLabel}
    </button>
  );
}
