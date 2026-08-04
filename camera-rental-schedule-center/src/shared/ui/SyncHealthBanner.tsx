import { AlertTriangle, LoaderCircle, LogIn, RefreshCw, ShieldX } from 'lucide-react';
import { usePreferences } from '../../features/preferences/PreferenceContext';
import { classifySafeError } from '../lib/safeError';
import { Button } from './Button';

export function SyncHealthBanner({
  isLoading,
  error,
  authRequired,
  accessDenied,
  onRetry,
  onLogin,
}: {
  isLoading: boolean;
  error: string | null;
  authRequired: boolean;
  accessDenied: boolean;
  onRetry: () => void;
  onLogin: () => void;
}) {
  const { t } = usePreferences();
  if (!isLoading && !error) return null;

  const category = classifySafeError(error);
  const title = isLoading
    ? t('sync.loadingTitle')
    : accessDenied
      ? t('sync.permissionTitle')
      : t('sync.errorTitle');
  const detail = isLoading ? t('sync.loadingDetail') : t(`sync.${category}`);
  const Icon = isLoading ? LoaderCircle : accessDenied ? ShieldX : AlertTriangle;

  return (
    <section
      role={isLoading ? 'status' : 'alert'}
      className={`sc-workspace-card mb-3 flex flex-col gap-3 rounded-xl px-4 py-3 sm:flex-row sm:items-center sm:justify-between ${
        isLoading
          ? 'border-[color-mix(in_srgb,var(--sc-blue)_25%,var(--sc-border))]'
          : 'border-[color-mix(in_srgb,var(--sc-amber)_25%,var(--sc-border))]'
      }`}
    >
      <div className="flex min-w-0 items-start gap-3">
        <Icon className={`mt-0.5 h-4 w-4 shrink-0 ${isLoading ? 'animate-spin text-[var(--sc-blue)]' : 'text-[var(--sc-amber)]'}`} />
        <div>
          <strong className="block text-xs text-[var(--sc-ink)]">{title}</strong>
          <p className="mt-1 text-[11px] leading-5 text-[var(--sc-ink-soft)]">{detail}</p>
        </div>
      </div>
      {!isLoading && (
        <div className="flex shrink-0 gap-2">
          {authRequired && (
            <Button onClick={onLogin} variant="secondary" size="sm" icon={<LogIn />}>
              {t('action.login')}
            </Button>
          )}
          {!accessDenied && (
            <Button onClick={onRetry} size="sm" icon={<RefreshCw />}>
              {t('action.retry')}
            </Button>
          )}
        </div>
      )}
    </section>
  );
}
