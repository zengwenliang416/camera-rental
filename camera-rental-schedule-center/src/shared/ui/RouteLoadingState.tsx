import { usePreferences } from '../../features/preferences/PreferenceContext';
import { Skeleton } from './Skeleton';

export function RouteLoadingState() {
  const { t } = usePreferences();
  return (
    <div role="status" className="sc-feature-hero min-h-72 rounded-2xl p-5 sm:p-7">
      <span className="sr-only">{t('state.loadingRoute')}</span>
      <div className="flex items-center justify-between gap-4">
        <div className="w-full max-w-xl">
          <Skeleton className="h-3 w-40" />
          <Skeleton className="mt-4 h-8 w-3/4" />
          <Skeleton className="mt-3 h-4 w-full" />
        </div>
        <Skeleton className="hidden h-11 w-32 sm:block" />
      </div>
      <div className="mt-8 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {[0, 1, 2, 3].map((item) => (
          <span key={item}>
            <Skeleton className="h-28" />
          </span>
        ))}
      </div>
    </div>
  );
}
