import { useEffect, useId, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';
import { overlayKeyAction } from './ConfirmDialogShell';

const selector =
  'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

export function DetailDrawerShell({
  title,
  description,
  closeLabel,
  onClose,
  children,
}: {
  title: ReactNode;
  description?: ReactNode;
  closeLabel: string;
  onClose: () => void;
  children: ReactNode;
}) {
  const drawerRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const drawer = drawerRef.current;
    drawer?.querySelector<HTMLElement>(selector)?.focus();
    const keydown = (event: KeyboardEvent) => {
      if (!drawer) return;
      const items = Array.from(drawer.querySelectorAll<HTMLElement>(selector)) as HTMLElement[];
      const activeIndex = items.findIndex((item) => item === document.activeElement);
      const action = overlayKeyAction(event.key, event.shiftKey, activeIndex, items.length);
      if (action.kind === 'dismiss') {
        event.preventDefault();
        onClose();
      } else if (action.kind === 'focus') {
        event.preventDefault();
        items[action.index]?.focus();
      }
    };
    document.addEventListener('keydown', keydown);
    return () => {
      document.removeEventListener('keydown', keydown);
      previous?.focus();
    };
  }, [onClose]);

  return (
    <div className="sc-overlay-scrim fixed inset-0 z-50 flex justify-end p-0 sm:p-4">
      <div
        ref={drawerRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        tabIndex={-1}
        className="sc-overlay-surface flex h-full w-full max-w-xl flex-col overflow-hidden focus:outline-none sm:rounded-2xl"
      >
        <header className="sc-panel-header flex items-start justify-between gap-4 border-b px-4 py-4 sm:px-5">
          <div>
            <h2 id={titleId} className="text-base font-black text-[var(--sc-ink)]">{title}</h2>
            {description && <p id={descriptionId} className="mt-1 text-[11px] text-[var(--sc-ink-muted)]">{description}</p>}
          </div>
          <button type="button" onClick={onClose} aria-label={closeLabel} className="sc-button sc-button-ghost grid h-11 w-11 shrink-0 place-items-center rounded-xl">
            <X className="h-4 w-4" />
          </button>
        </header>
        <div className="min-h-0 flex-1 overflow-y-auto p-4 sm:p-5">{children}</div>
      </div>
    </div>
  );
}
