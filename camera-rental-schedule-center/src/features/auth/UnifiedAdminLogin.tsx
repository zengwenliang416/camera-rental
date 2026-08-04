import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { createPortal } from 'react-dom';
import { ArrowRight, Building2, Eye, EyeOff, KeyRound, Layers, QrCode, ShieldCheck, Smartphone, User, X } from 'lucide-react';

import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { Button } from '../../shared/ui/Button';
import { OperationResultPanel } from '../../shared/ui/OperationResultPanel';
import { classifySafeError } from '../../shared/lib/safeError';
import { loginErrorPresentation, validateLoginCredentials } from './loginModel';

const focusableSelector =
  'button:not([disabled]), input:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

export function UnifiedAdminLogin({ isModal }: { isModal: boolean }) {
  const { login, setIsLoginPageVisible, isLoading } = useApp();
  const { t } = usePreferences();
  const surfaceRef = useRef<HTMLElement>(null);
  const usernameRef = useRef<HTMLInputElement>(null);
  const tenantEnabled = import.meta.env.VITE_APP_TENANT_ENABLE !== 'false';
  const [tenantName, setTenantName] = useState(
    tenantEnabled ? import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '捷租达' : ''
  );
  const [username, setUsername] = useState(import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '');
  const [password, setPassword] = useState(import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '');
  const [rememberMe, setRememberMe] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const accountTrigger = document.querySelector<HTMLElement>(
      '[data-login-return-focus]'
    );
    const returnTarget = previous && previous !== document.body ? previous : accountTrigger;
    usernameRef.current?.focus({ preventScroll: true });
    if (!isModal) return;
    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        setIsLoginPageVisible(false);
        return;
      }
      const surface = surfaceRef.current;
      if (event.key !== 'Tab' || !surface) return;
      const items = Array.from(
        surface.querySelectorAll<HTMLElement>(focusableSelector)
      ) as HTMLElement[];
      if (!items.length) return;
      const first = items[0]!;
      const last = items[items.length - 1]!;
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousBodyOverflow;
      if (returnTarget?.isConnected) returnTarget.focus();
    };
  }, [isModal, setIsLoginPageVisible]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const validation = validateLoginCredentials(username, password);
    if (validation !== 'ready') {
      setError(t(`auth.${validation}Required`));
      return;
    }
    setError('');
    try {
      await login({
        tenantName: tenantName.trim() || undefined,
        username: username.trim(),
        password,
        rememberMe,
      });
    } catch (caught) {
      const rawMessage = caught instanceof Error ? caught.message : '';
      const category = loginErrorPresentation(classifySafeError(rawMessage));
      setError(t(`auth.error${category === 'network' ? 'Network' : category === 'authentication' ? 'Authentication' : category === 'permission' ? 'Permission' : category === 'timeout' ? 'Timeout' : 'Unknown'}`));
    }
  };

  const content = (
    <main
      ref={surfaceRef}
      role={isModal ? 'dialog' : undefined}
      aria-modal={isModal ? true : undefined}
      aria-labelledby="admin-login-title"
      className={`flex flex-col items-center overflow-y-auto bg-[var(--sc-canvas)] p-3 sm:p-6 ${isModal ? 'sc-overlay-scrim fixed inset-0 z-50 h-dvh min-h-0' : 'relative min-h-screen'}`}
    >
      <div className="sc-subtle-grid pointer-events-none absolute inset-0 opacity-60" />
      {isModal && (
        <Button
          variant="glass"
          size="icon"
          onClick={() => setIsLoginPageVisible(false)}
          aria-label={t('auth.close')}
          className="sticky top-4 z-20 mb-[-44px] ml-auto shrink-0 text-[var(--sc-ink)]"
          icon={<X className="h-4 w-4" />}
        />
      )}
      <section className={`relative z-10 my-auto grid w-full max-w-4xl shrink-0 overflow-hidden rounded-2xl lg:grid-cols-[0.85fr_1.15fr] ${isModal ? 'sc-overlay-surface' : 'sc-workspace-card'}`}>
        <aside className="bg-[var(--sc-inverse)] p-6 text-[var(--sc-inverse-ink)] sm:p-8">
          <div className="flex items-center gap-3">
            <span className="sc-brand-mark grid h-11 w-11 place-items-center rounded-xl"><Layers className="h-5 w-5" /></span>
            <div><strong className="block text-lg">{t('app.title')}</strong><span className="text-xs text-[var(--sc-inverse-muted)]">{t('auth.shared')}</span></div>
          </div>
          <div className="mt-10 space-y-4">
            {[t('auth.featureSchedule'), t('auth.featureSn'), t('auth.featureShipping')].map((item) => (
              <p key={item} className="rounded-xl border border-white/10 bg-white/5 p-3 text-xs leading-5 text-[var(--sc-inverse-muted)]">{item}</p>
            ))}
          </div>
          <div className="mt-10 flex items-center gap-2 text-[11px] text-[var(--sc-inverse-muted)]"><ShieldCheck className="h-4 w-4 text-[var(--sc-green)]" />{t('auth.serverAuthority')}</div>
        </aside>
        <div className="p-5 sm:p-8">
          <h1 id="admin-login-title" className="text-2xl font-black text-[var(--sc-ink)]">{t('auth.title')}</h1>
          <p className="mt-2 text-xs leading-5 text-[var(--sc-ink-soft)]">{t('auth.description')}</p>
          <div className="sc-segmented mt-5 grid grid-cols-2 gap-2 rounded-2xl p-1.5 sm:grid-cols-3">
            <Button
              variant="secondary"
              size="md"
              aria-pressed="true"
              className="col-span-2 w-full flex-col gap-1 whitespace-normal px-2 text-[10px] leading-4 sm:col-span-1"
              icon={<KeyRound className="h-3.5 w-3.5" />}
            >
              {t('auth.passwordMode')}
            </Button>
            <Button
              variant="outline"
              size="md"
              disabled
              className="min-w-0 w-full flex-col gap-1 whitespace-normal px-2 text-[10px] leading-4 text-[var(--sc-ink-muted)]"
              icon={<Smartphone className="h-3.5 w-3.5" />}
            >
              {t('auth.smsDisabled')}
            </Button>
            <Button
              variant="outline"
              size="md"
              disabled
              className="min-w-0 w-full flex-col gap-1 whitespace-normal px-2 text-[10px] leading-4 text-[var(--sc-ink-muted)]"
              icon={<QrCode className="h-3.5 w-3.5" />}
            >
              {t('auth.qrDisabled')}
            </Button>
          </div>
          <form onSubmit={submit} className="mt-5 space-y-4">
            {tenantEnabled && <Field label={t('auth.tenant')} icon={<Building2 />} value={tenantName} onChange={setTenantName} />}
            <Field inputRef={usernameRef} label={t('auth.username')} icon={<User />} value={username} onChange={setUsername} />
            <label className="sc-field-label">
              {t('auth.password')}
              <span className="relative">
                <input type={showPassword ? 'text' : 'password'} value={password} onChange={(event) => setPassword(event.target.value)} className="sc-form-control min-h-11 w-full rounded-xl border px-3 pr-12 text-xs text-[var(--sc-ink)]" />
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={t('auth.togglePassword')}
                  className="absolute right-0.5 top-1/2 -translate-y-1/2 text-[var(--sc-ink-muted)]"
                  icon={showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                />
              </span>
            </label>
            <label className="sc-glass-control flex min-h-11 items-center gap-2 rounded-xl px-3 text-xs text-[var(--sc-ink-soft)]"><input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} className="h-4 w-4 accent-[var(--sc-blue)]" />{t('auth.remember')}</label>
            {error && <OperationResultPanel state="error" message={error} />}
            <Button type="submit" variant="primary" size="lg" disabled={isLoading} className="w-full text-xs font-black">
              {isLoading ? t('auth.submitting') : t('auth.submit')}<ArrowRight className="h-4 w-4" />
            </Button>
          </form>
        </div>
      </section>
    </main>
  );

  return isModal ? createPortal(content, document.body) : content;
}

function Field({
  label,
  icon,
  value,
  onChange,
  inputRef,
}: {
  label: string;
  icon: ReactNode;
  value: string;
  onChange: (value: string) => void;
  inputRef?: RefObject<HTMLInputElement | null>;
}) {
  return <label className="sc-field-label">{label}<span className="relative"><span className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)] [&>svg]:h-4 [&>svg]:w-4">{icon}</span><input ref={inputRef} value={value} onChange={(event) => onChange(event.target.value)} className="sc-form-control min-h-11 w-full rounded-xl border pl-10 pr-3 text-xs text-[var(--sc-ink)]" /></span></label>;
}
