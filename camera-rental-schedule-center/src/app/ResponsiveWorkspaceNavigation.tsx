import { Menu, X } from 'lucide-react';
import { forwardRef, useEffect, useRef, useState } from 'react';
import type { Key } from 'react';
import { usePreferences } from '../features/preferences/PreferenceContext';
import type { WorkspaceNavItem, WorkspaceTab } from './navigation';

const NavButton = forwardRef<HTMLButtonElement, {
  key?: Key;
  item: WorkspaceNavItem;
  active: boolean;
  onSelect: () => void;
  mobile?: boolean;
}>(function NavButton({
  item,
  active,
  onSelect,
  mobile = false,
}, ref) {
  const Icon = item.icon;
  return (
    <button
      ref={ref}
      type="button"
      onClick={onSelect}
      aria-current={active ? 'page' : undefined}
      className={`relative flex items-center gap-2 rounded-lg text-xs font-bold transition ${
        mobile ? 'min-h-12 rounded-md border px-3 text-left' : 'min-h-10 px-3'
      } ${
        active
          ? mobile
            ? 'border-[var(--sc-blue)] bg-[var(--sc-blue-soft)] text-[var(--sc-blue)]'
            : 'sc-nav-active'
          : mobile
            ? 'border-[var(--sc-border)] bg-[var(--sc-surface)] text-[var(--sc-ink-soft)]'
            : 'text-[var(--sc-ink-muted)] hover:text-[var(--sc-ink)]'
      }`}
    >
      <Icon className="h-3.5 w-3.5 shrink-0" />
      <span className="min-w-0 flex-1 truncate">{item.label}</span>
      {item.badge !== undefined && (
        <span className={`sc-data rounded-full px-1.5 py-0.5 text-[9px] ${item.danger ? 'bg-[var(--sc-red-soft)] text-[var(--sc-red)]' : 'bg-[var(--sc-blue-soft)] text-[var(--sc-blue)]'}`}>
          {item.badge}
        </span>
      )}
    </button>
  );
});

export function ResponsiveWorkspaceNavigation({
  items,
  activeTab,
  onSelect,
}: {
  items: WorkspaceNavItem[];
  activeTab: WorkspaceTab;
  onSelect: (tab: WorkspaceTab) => void;
}) {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const firstItemRef = useRef<HTMLButtonElement>(null);
  const { t } = usePreferences();

  const close = (restoreFocus = false) => {
    setOpen(false);
    if (restoreFocus) requestAnimationFrame(() => triggerRef.current?.focus());
  };

  const select = (tab: WorkspaceTab) => {
    onSelect(tab);
    close(true);
  };

  useEffect(() => {
    if (!open) return;
    requestAnimationFrame(() => firstItemRef.current?.focus());
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        close(true);
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open]);

  return (
    <>
      <nav className="sc-desktop-nav hidden min-w-0 flex-1 items-center justify-center xl:flex" aria-label="Workspace">
        {items.map((item) => (
          <NavButton key={item.id} item={item} active={activeTab === item.id} onSelect={() => select(item.id)} />
        ))}
      </nav>
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        aria-label={open ? t('nav.close') : t('nav.open')}
        className="sc-glass-control grid h-11 w-11 place-items-center rounded-xl text-[var(--sc-ink)] xl:hidden"
      >
        {open ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
      </button>
      {open && (
        <div className="sc-mobile-nav-panel sc-glass-strong absolute inset-x-0 top-full border-b border-[var(--sc-glass-border)] p-3 shadow-[var(--sc-shadow)] xl:hidden">
          <nav className="mx-auto grid max-w-[1600px] grid-cols-2 gap-2 sm:grid-cols-3" aria-label="Workspace mobile">
            {items.map((item, index) => (
              <NavButton ref={index === 0 ? firstItemRef : undefined} key={item.id} item={item} active={activeTab === item.id} onSelect={() => select(item.id)} mobile />
            ))}
          </nav>
        </div>
      )}
    </>
  );
}
