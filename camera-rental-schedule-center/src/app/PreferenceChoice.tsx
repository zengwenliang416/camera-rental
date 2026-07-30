import { Check } from 'lucide-react';
import { forwardRef, type ReactNode } from 'react';

export const PreferenceChoice = forwardRef<
  HTMLButtonElement,
  {
    active: boolean;
    icon: ReactNode;
    label: string;
    onSelect: () => void;
  }
>(function PreferenceChoice({ active, icon, label, onSelect }, ref) {
  return (
    <button
      ref={ref}
      type="button"
      onClick={onSelect}
      aria-pressed={active}
      className={`flex min-h-11 items-center gap-2.5 rounded-lg border px-3 text-[13px] font-semibold leading-5 ${
        active
          ? 'border-[var(--sc-blue)] bg-[var(--sc-blue-soft)] text-[var(--sc-blue)]'
          : 'border-[var(--sc-border)] bg-[var(--sc-surface)] text-[var(--sc-ink-soft)]'
      }`}
    >
      {icon}
      <span className="flex-1 text-left">{label}</span>
      {active && <Check className="h-3.5 w-3.5" />}
    </button>
  );
});
