import { CircleHelp, LoaderCircle, LockKeyhole, Power, Send } from 'lucide-react';
import { usePreferences } from '../features/preferences/PreferenceContext';
import { StatusBadge, type StatusTone } from '../shared/ui/StatusBadge';
import type { IntegrationReadiness } from './integrationModel';

const stateMeta: Record<
  IntegrationReadiness,
  { tone: StatusTone; icon: typeof Send; label: 'ready' | 'readOnly' | 'disabled' | 'loading' | 'unavailable' }
> = {
  ready: { tone: 'green', icon: Send, label: 'ready' },
  'read-only': { tone: 'amber', icon: LockKeyhole, label: 'readOnly' },
  disabled: { tone: 'neutral', icon: Power, label: 'disabled' },
  loading: { tone: 'blue', icon: LoaderCircle, label: 'loading' },
  unavailable: { tone: 'red', icon: CircleHelp, label: 'unavailable' },
};

export function IntegrationReadinessBadge({ state }: { state: IntegrationReadiness }) {
  const { t } = usePreferences();
  const meta = stateMeta[state];
  const Icon = meta.icon;
  const fullLabel = t(`integration.${meta.label}`);
  const shortLabel = t(`integration.${meta.label}Short`);

  return (
    <span title={fullLabel} aria-label={fullLabel} className="shrink-0">
      <StatusBadge
        tone={meta.tone}
        icon={<Icon className={`h-3 w-3 ${state === 'loading' ? 'animate-spin' : ''}`} />}
      >
        <span className="sm:hidden">{shortLabel}</span>
        <span className="hidden sm:inline">{fullLabel}</span>
      </StatusBadge>
    </span>
  );
}
