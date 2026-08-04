import {
  Activity,
  AlertTriangle,
  Clock3,
  PackageCheck,
  RefreshCw,
  RotateCcw,
} from 'lucide-react';
import { useCallback, useEffect } from 'react';

import {
  fetchRentalLogisticsMetrics,
  type RentalLogisticsMetricsVO,
} from '../../../api/rental';
import { Button } from '../../../shared/ui/Button';
import { OperationalMetricGrid } from '../../../shared/ui/OperationalMetricGrid';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import type { LocalePreference } from '../../preferences/preferenceModel';
import {
  metricsAreEmpty,
  operationsStatusTone,
  type LogisticsOperationsAccess,
} from '../operationsModel';
import {
  formatOperationsDateTime,
  operationsCodeLabel,
  operationsCopy,
} from '../operationsCopy';
import { useOperationsRequest } from '../useOperationsRequest';
import {
  OperationsPanel,
  PanelQueryBoundary,
} from './OperationsPanel';

function StatusDistribution({
  locale,
  title,
  counts,
}: {
  locale: LocalePreference;
  title: string;
  counts: Record<string, number>;
}) {
  return (
    <div className="sc-workspace-card rounded-xl p-3">
      <p className="text-[10px] font-black text-[var(--sc-ink-soft)]">{title}</p>
      <div className="mt-2 flex flex-wrap gap-2">
        {Object.entries(counts).length === 0 ? (
          <span className="text-[10px] text-[var(--sc-ink-muted)]">—</span>
        ) : (
          Object.entries(counts).map(([status, count]) => (
            <span key={status}>
              <StatusBadge tone={operationsStatusTone(status)}>
                {operationsCodeLabel(locale, status)} · {count}
              </StatusBadge>
            </span>
          ))
        )}
      </div>
    </div>
  );
}

export function MetricsPanel({
  access,
  locale,
}: {
  access: LogisticsOperationsAccess;
  locale: LocalePreference;
}) {
  const query = useOperationsRequest<RentalLogisticsMetricsVO>();
  const load = useCallback(() => {
    if (!access.canQueryMetrics) return Promise.resolve(null);
    return query.run(
      () => fetchRentalLogisticsMetrics(),
      metricsAreEmpty
    );
  }, [access.canQueryMetrics, query.run]);

  useEffect(() => {
    if (access.canQueryMetrics) void load();
    else query.reset();
  }, [access.canQueryMetrics, load, query.reset]);

  const metrics = query.state.data;
  return (
    <OperationsPanel
      title={operationsCopy(locale, 'metrics.title')}
      description={operationsCopy(locale, 'metrics.description')}
      actions={
        access.canQueryMetrics ? (
          <Button
            variant="outline"
            size="md"
            type="button"
            onClick={() => void load()}
            disabled={query.state.status === 'loading'}
            icon={<RefreshCw className={`h-3.5 w-3.5 ${query.state.status === 'loading' ? 'animate-spin' : ''}`} />}
          >
            {operationsCopy(locale, 'common.refresh')}
          </Button>
        ) : undefined
      }
    >
      <PanelQueryBoundary
        allowed={access.canQueryMetrics}
        state={query.state}
        locale={locale}
        onRetry={() => void load()}
        isEmpty={query.state.status === 'empty'}
        emptyTitle={operationsCopy(locale, 'metrics.empty')}
        emptyDetail={operationsCopy(locale, 'metrics.emptyDetail')}
      >
        {metrics && (
          <div className="space-y-4">
            <OperationalMetricGrid
              metrics={[
                {
                  id: 'deliveries',
                  label: operationsCopy(locale, 'metrics.deliveries'),
                  value: metrics.deliveryCount,
                  unit: '',
                  icon: <PackageCheck className="h-4 w-4" />,
                  tone: 'blue',
                },
                {
                  id: 'stale',
                  label: operationsCopy(locale, 'metrics.stale'),
                  value: metrics.staleDeliveryCount,
                  unit: '',
                  icon: <Clock3 className="h-4 w-4" />,
                  tone: metrics.staleDeliveryCount ? 'amber' : 'green',
                },
                {
                  id: 'failed',
                  label: operationsCopy(locale, 'metrics.failed'),
                  value: metrics.failedInboxCount + metrics.failedOutboxCount,
                  unit: '',
                  detail: `${metrics.failedInboxCount} / ${metrics.failedOutboxCount}`,
                  icon: <AlertTriangle className="h-4 w-4" />,
                  tone: metrics.failedInboxCount + metrics.failedOutboxCount ? 'red' : 'green',
                },
                {
                  id: 'retried',
                  label: operationsCopy(locale, 'metrics.retried'),
                  value: metrics.retriedInboxCount + metrics.retriedOutboxCount,
                  unit: '',
                  icon: <RotateCcw className="h-4 w-4" />,
                  tone: 'neutral',
                },
                {
                  id: 'delay',
                  label: operationsCopy(locale, 'metrics.delay'),
                  value: metrics.averageOutboxDelaySeconds,
                  unit: operationsCopy(locale, 'metrics.seconds'),
                  icon: <Activity className="h-4 w-4" />,
                  tone: 'neutral',
                },
              ]}
            />
            <div className="grid gap-3 lg:grid-cols-3">
              <StatusDistribution locale={locale} title={`Delivery · ${operationsCopy(locale, 'metrics.distribution')}`} counts={metrics.deliveryStatusCounts} />
              <StatusDistribution locale={locale} title={`Outbox · ${operationsCopy(locale, 'metrics.distribution')}`} counts={metrics.outboxStatusCounts} />
              <StatusDistribution locale={locale} title={`Inbox · ${operationsCopy(locale, 'metrics.distribution')}`} counts={metrics.inboxStatusCounts} />
            </div>
            <div className="grid gap-2 text-[10px] text-[var(--sc-ink-muted)] sm:grid-cols-2">
              <div className="sc-glass-control rounded-xl px-3 py-2">{operationsCopy(locale, 'metrics.lastOutbox')}: {formatOperationsDateTime(locale, metrics.lastOutboxSuccessAt) || '—'}</div>
              <div className="sc-glass-control rounded-xl px-3 py-2">{operationsCopy(locale, 'metrics.lastInbox')}: {formatOperationsDateTime(locale, metrics.lastInboxSuccessAt) || '—'}</div>
            </div>
          </div>
        )}
      </PanelQueryBoundary>
    </OperationsPanel>
  );
}
