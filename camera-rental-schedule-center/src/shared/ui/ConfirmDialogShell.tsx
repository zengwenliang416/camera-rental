import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';

const focusableSelector =
  'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

export function overlayKeyAction(
  key: string,
  shiftKey: boolean,
  activeIndex: number,
  itemCount: number
) {
  if (key === 'Escape') return { kind: 'dismiss' as const };
  if (key !== 'Tab' || itemCount <= 0) return { kind: 'none' as const };
  if (shiftKey && activeIndex === 0) {
    return { kind: 'focus' as const, index: itemCount - 1 };
  }
  if (!shiftKey && activeIndex === itemCount - 1) {
    return { kind: 'focus' as const, index: 0 };
  }
  return { kind: 'none' as const };
}

export function ConfirmDialogShell({
  ariaLabel,
  closeLabel,
  title,
  description,
  onClose,
  children,
  footer,
}: {
  ariaLabel: string;
  closeLabel: string;
  title: ReactNode;
  description?: ReactNode;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}) {
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const dialog = dialogRef.current;
    const first = dialog?.querySelector<HTMLElement>(focusableSelector);
    (first || dialog)?.focus();

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !dialog) return;
      const focusable = Array.from(
        dialog.querySelectorAll<HTMLElement>(focusableSelector)
      ) as HTMLElement[];
      if (focusable.length === 0) {
        event.preventDefault();
        dialog.focus();
        return;
      }
      const activeIndex = focusable.findIndex((item) => item === document.activeElement);
      const action = overlayKeyAction(
        event.key,
        event.shiftKey,
        activeIndex,
        focusable.length
      );
      if (action.kind === 'focus') {
        event.preventDefault();
        focusable[action.index]?.focus();
      }
    };

    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      previousFocus?.focus();
    };
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-zinc-950/70 p-2 backdrop-blur-sm sm:p-5">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        tabIndex={-1}
        className="flex max-h-[94vh] w-full max-w-4xl flex-col overflow-hidden rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] shadow-2xl focus:outline-none"
      >
        <header className="flex items-start justify-between gap-4 border-b border-[var(--sc-border)] px-4 py-4 sm:px-6">
          <div>
            <h2 className="text-base font-black text-[var(--sc-ink)] sm:text-lg">{title}</h2>
            {description && (
              <p className="mt-1 max-w-2xl text-[11px] leading-5 text-[var(--sc-ink-muted)]">
                {description}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label={closeLabel}
            className="grid h-11 w-11 shrink-0 place-items-center rounded-lg text-[var(--sc-ink-muted)] hover:bg-[var(--sc-surface-soft)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
          >
            <X className="h-4 w-4" />
          </button>
        </header>
        <div className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-6">{children}</div>
        {footer && (
          <footer className="border-t border-[var(--sc-border)] bg-[var(--sc-surface-soft)] px-4 py-3 sm:px-6">
            {footer}
          </footer>
        )}
      </div>
    </div>
  );
}
