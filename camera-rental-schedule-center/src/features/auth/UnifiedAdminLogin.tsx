import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { ArrowRight, Building2, Eye, EyeOff, KeyRound, Layers, QrCode, ShieldCheck, Smartphone, User, X } from 'lucide-react';

import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
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
    const accountTrigger = Array.from(
      document.querySelectorAll<HTMLElement>('button[aria-expanded]')
    ).find((element) => element.querySelector('img'));
    const returnTarget = previous && previous !== document.body ? previous : accountTrigger;
    usernameRef.current?.focus();
    if (!isModal) return;

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

  return (
    <main
      ref={surfaceRef}
      role={isModal ? 'dialog' : undefined}
      aria-modal={isModal ? true : undefined}
      aria-labelledby="admin-login-title"
      className={`relative grid min-h-screen place-items-center overflow-y-auto bg-[var(--sc-canvas)] p-3 sm:p-6 ${isModal ? 'fixed inset-0 z-50 bg-zinc-950/75 backdrop-blur-sm' : ''}`}
    >
      <div className="sc-subtle-grid pointer-events-none absolute inset-0 opacity-60" />
      {isModal && (
        <button type="button" onClick={() => setIsLoginPageVisible(false)} aria-label={t('auth.close')} className="absolute right-4 top-4 z-20 grid h-11 w-11 place-items-center rounded-lg bg-[var(--sc-surface)] text-[var(--sc-ink)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]">
          <X className="h-4 w-4" />
        </button>
      )}
      <section className="relative z-10 grid w-full max-w-4xl overflow-hidden rounded-2xl border border-[var(--sc-border)] bg-[var(--sc-surface)] shadow-2xl lg:grid-cols-[0.85fr_1.15fr]">
        <aside className="bg-[var(--sc-ink)] p-6 text-white sm:p-8">
          <div className="flex items-center gap-3">
            <span className="grid h-11 w-11 place-items-center rounded-xl bg-blue-600"><Layers className="h-5 w-5" /></span>
            <div><strong className="block text-lg">{t('app.title')}</strong><span className="text-xs text-zinc-400">{t('auth.shared')}</span></div>
          </div>
          <div className="mt-10 space-y-4">
            {[t('auth.featureSchedule'), t('auth.featureSn'), t('auth.featureShipping')].map((item) => (
              <p key={item} className="rounded-lg border border-white/10 bg-white/5 p-3 text-xs leading-5 text-zinc-300">{item}</p>
            ))}
          </div>
          <div className="mt-10 flex items-center gap-2 text-[11px] text-zinc-400"><ShieldCheck className="h-4 w-4 text-emerald-400" />{t('auth.serverAuthority')}</div>
        </aside>
        <div className="p-5 sm:p-8">
          <h1 id="admin-login-title" className="text-2xl font-black text-[var(--sc-ink)]">{t('auth.title')}</h1>
          <p className="mt-2 text-xs leading-5 text-[var(--sc-ink-soft)]">{t('auth.description')}</p>
          <div className="mt-5 grid grid-cols-3 gap-2">
            <button type="button" aria-pressed="true" className="min-h-11 rounded-lg bg-[var(--sc-ink)] px-2 text-[10px] font-bold text-[var(--sc-surface)]"><KeyRound className="mx-auto mb-1 h-3.5 w-3.5" />{t('auth.passwordMode')}</button>
            <button type="button" disabled className="min-h-11 rounded-lg border border-[var(--sc-border)] px-2 text-[10px] font-bold text-[var(--sc-ink-muted)] opacity-60"><Smartphone className="mx-auto mb-1 h-3.5 w-3.5" />{t('auth.smsDisabled')}</button>
            <button type="button" disabled className="min-h-11 rounded-lg border border-[var(--sc-border)] px-2 text-[10px] font-bold text-[var(--sc-ink-muted)] opacity-60"><QrCode className="mx-auto mb-1 h-3.5 w-3.5" />{t('auth.qrDisabled')}</button>
          </div>
          <form onSubmit={submit} className="mt-5 space-y-4">
            {tenantEnabled && <Field label={t('auth.tenant')} icon={<Building2 />} value={tenantName} onChange={setTenantName} />}
            <Field inputRef={usernameRef} label={t('auth.username')} icon={<User />} value={username} onChange={setUsername} />
            <label className="grid gap-1 text-xs font-bold text-[var(--sc-ink-soft)]">
              {t('auth.password')}
              <span className="relative">
                <input type={showPassword ? 'text' : 'password'} value={password} onChange={(event) => setPassword(event.target.value)} className="min-h-11 w-full rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 pr-12 text-xs text-[var(--sc-ink)]" />
                <button type="button" onClick={() => setShowPassword((value) => !value)} aria-label={t('auth.togglePassword')} className="absolute right-0.5 top-1/2 grid h-11 w-11 -translate-y-1/2 place-items-center text-[var(--sc-ink-muted)]">{showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}</button>
              </span>
            </label>
            <label className="flex min-h-11 items-center gap-2 text-xs text-[var(--sc-ink-soft)]"><input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />{t('auth.remember')}</label>
            {error && <OperationResultPanel state="error" message={error} />}
            <button type="submit" disabled={isLoading} className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-lg bg-[var(--sc-blue)] text-xs font-black text-white focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] disabled:opacity-60">
              {isLoading ? t('auth.submitting') : t('auth.submit')}<ArrowRight className="h-4 w-4" />
            </button>
          </form>
        </div>
      </section>
    </main>
  );
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
  return <label className="grid gap-1 text-xs font-bold text-[var(--sc-ink-soft)]">{label}<span className="relative"><span className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)] [&>svg]:h-4 [&>svg]:w-4">{icon}</span><input ref={inputRef} value={value} onChange={(event) => onChange(event.target.value)} className="min-h-11 w-full rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] pl-10 pr-3 text-xs text-[var(--sc-ink)]" /></span></label>;
}
