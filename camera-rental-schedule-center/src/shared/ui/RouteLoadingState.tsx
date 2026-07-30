import { LoaderCircle } from 'lucide-react';
import { usePreferences } from '../../features/preferences/PreferenceContext';

export function RouteLoadingState() {
  const { t } = usePreferences();
  return (
    <div className="sc-surface grid min-h-72 place-items-center rounded-xl">
      <div className="text-center">
        <LoaderCircle className="mx-auto h-6 w-6 animate-spin text-[var(--sc-blue)]" />
        <p className="mt-3 text-xs font-bold text-[var(--sc-ink-soft)]">{t('state.loadingRoute')}</p>
      </div>
    </div>
  );
}
