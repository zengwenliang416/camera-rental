import { ChevronDown, LogOut, UserRound } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { usePreferences } from '../features/preferences/PreferenceContext';
import { Button } from '../shared/ui/Button';
import { LocaleToggle } from './LocaleToggle';
import { ThemeToggle } from './ThemeToggle';

interface CurrentUser {
  id?: number;
  username?: string;
  nickname?: string;
  avatar?: string;
}

export function AccountAndPreferenceMenu({
  user,
  onRelogin,
  onLogout,
}: {
  user?: CurrentUser;
  onRelogin: () => void;
  onLogout: () => void;
}) {
  const [open, setOpen] = useState(false);
  const { t } = usePreferences();
  const triggerRef = useRef<HTMLButtonElement>(null);
  const firstChoiceRef = useRef<HTMLButtonElement>(null);
  const displayName = user?.nickname || user?.username || t('action.account');

  const closeMenu = (restoreFocus = false) => {
    setOpen(false);
    if (restoreFocus) requestAnimationFrame(() => triggerRef.current?.focus());
  };

  useEffect(() => {
    if (!open) return;
    requestAnimationFrame(() => firstChoiceRef.current?.focus());
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeMenu(true);
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open]);

  return (
    <div className="relative">
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        aria-label={t('action.account')}
        data-login-return-focus
        className="sc-glass-control flex min-h-11 items-center gap-2 rounded-xl p-1.5 pr-2 text-[var(--sc-ink)]"
      >
        {user?.avatar ? (
          <img src={user.avatar} alt={displayName} className="h-8 w-8 rounded-md object-cover" />
        ) : (
          <span className="grid h-8 w-8 place-items-center rounded-md bg-[var(--sc-brand)] text-[10px] font-black text-[var(--sc-surface)]">
            {displayName.slice(0, 2).toUpperCase()}
          </span>
        )}
        <span className="hidden max-w-28 truncate text-left text-[13px] font-semibold leading-5 2xl:block">{displayName}</span>
        <ChevronDown className="hidden h-3.5 w-3.5 text-[var(--sc-ink-muted)] 2xl:block" />
      </button>

      {open && (
        <>
          <button type="button" aria-label="Close menu" className="fixed inset-0 z-40 cursor-default" onClick={() => closeMenu(true)} />
          <section role="dialog" aria-label={t('action.account')} className="sc-popover absolute right-0 z-50 mt-2 w-[min(344px,calc(100vw-24px))] rounded-2xl p-4">
            <div className="mb-4 border-b border-[var(--sc-border)] pb-4">
              <strong className="block text-[15px] font-semibold leading-6 text-[var(--sc-ink)]">{displayName}</strong>
              <span className="sc-data mt-1 block text-[11px] leading-4 text-[var(--sc-ink-muted)]">ID: {user?.id ?? '-'}</span>
            </div>
            <div className="grid gap-4">
              <ThemeToggle firstButtonRef={firstChoiceRef} />
              <LocaleToggle />
            </div>
            <div className="mt-4 grid grid-cols-2 gap-2 border-t border-[var(--sc-border)] pt-4">
              <Button onClick={() => { closeMenu(); onRelogin(); }} size="sm" icon={<UserRound />}>
                {t('action.login')}
              </Button>
              <Button onClick={() => { closeMenu(); onLogout(); }} variant="danger" size="sm" icon={<LogOut />}>
                {t('action.logout')}
              </Button>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
