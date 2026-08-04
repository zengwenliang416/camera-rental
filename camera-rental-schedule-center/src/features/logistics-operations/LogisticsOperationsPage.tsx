import { LockKeyhole, ShieldCheck } from 'lucide-react';

import { usePermissions } from '../permissions/PermissionContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { EmptyState } from '../../shared/ui/EmptyState';
import { FeaturePageHeader } from '../../shared/ui/FeaturePageHeader';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import { CarrierMappingPanel } from './components/CarrierMappingPanel';
import { CleanupPanel, BackfillPanel } from './components/MaintenancePanels';
import { MetricsPanel } from './components/MetricsPanel';
import { ProviderConfigPanel } from './components/ProviderConfigPanel';
import { TaskQueuePanel } from './components/TaskQueuePanel';
import {
  buildLogisticsOperationsAccess,
  hasLogisticsOperationsAccess,
} from './operationsModel';
import { operationsCopy } from './operationsCopy';

export default function LogisticsOperationsPage() {
  const { permissions } = usePermissions();
  const { locale } = usePreferences();
  const access = buildLogisticsOperationsAccess(permissions);

  if (!hasLogisticsOperationsAccess(permissions)) {
    return (
      <div className="sc-workspace-card grid min-h-[55vh] place-items-center rounded-2xl p-6">
        <EmptyState
          icon={<LockKeyhole className="h-4 w-4" />}
          title={operationsCopy(locale, 'page.noAccess')}
          description={operationsCopy(locale, 'page.noAccessDetail')}
        />
      </div>
    );
  }

  return (
    <div className="sc-page-stack min-w-0 space-y-4">
      <FeaturePageHeader
        eyebrow={operationsCopy(locale, 'page.eyebrow')}
        title={operationsCopy(locale, 'page.title')}
        description={operationsCopy(locale, 'page.description')}
        meta={
          <StatusBadge tone="blue" icon={<ShieldCheck className="h-3 w-3" />}>
            {operationsCopy(locale, 'page.safety')}
          </StatusBadge>
        }
      />

      <div className="grid min-w-0 gap-4 2xl:grid-cols-2">
        <ProviderConfigPanel access={access} locale={locale} />
        <CarrierMappingPanel access={access} locale={locale} />
      </div>
      <TaskQueuePanel access={access} locale={locale} />
      <MetricsPanel access={access} locale={locale} />
      <div className="grid min-w-0 gap-4 xl:grid-cols-2">
        <BackfillPanel access={access} locale={locale} />
        <CleanupPanel access={access} locale={locale} />
      </div>
    </div>
  );
}
