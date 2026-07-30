import { useMemo, useState } from 'react';
import { CheckCircle2, LoaderCircle, Search, ShieldAlert, Wrench, Zap } from 'lucide-react';

import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { EmptyState } from '../../shared/ui/EmptyState';
import { FeaturePageHeader } from '../../shared/ui/FeaturePageHeader';
import { FilterToolbar } from '../../shared/ui/FilterToolbar';
import { PermissionAwareAction } from '../../shared/ui/PermissionAwareAction';
import { ResponsiveDataList } from '../../shared/ui/ResponsiveDataList';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import {
  exceptionActions,
  exceptionSeverityTone,
  filterExceptions,
  type ExceptionFilter,
} from './exceptionModel';

export function ExceptionsPage() {
  const { exceptions, resolveException, openAllocationModal, openDeviceDetail, hasPermission } = useApp();
  const { t } = usePreferences();
  const [filter, setFilter] = useState<ExceptionFilter>('OPEN');
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const visible = useMemo(() => filterExceptions(exceptions, filter), [exceptions, filter]);
  const permissions = {
    canResolve: hasPermission('rental:review:update'),
    canAssign: hasPermission('rental:device:assign'),
    canViewDevice: hasPermission('rental:device:query'),
  };
  const severityLabel = {
    high: t('exceptions.severityHigh'),
    medium: t('exceptions.severityMedium'),
    low: t('exceptions.severityLow'),
  };

  const resolve = async (id: string) => {
    if (resolvingId) return;
    setResolvingId(id);
    try {
      await resolveException(id);
    } finally {
      setResolvingId(null);
    }
  };

  return (
    <div className="space-y-4">
      <FeaturePageHeader
        eyebrow={t('exceptions.eyebrow')}
        title={t('exceptions.title')}
        description={t('exceptions.description')}
        meta={<StatusBadge tone="red" icon={<ShieldAlert className="h-3 w-3" />}>{exceptions.filter((item) => !item.resolved).length} {t('exceptions.open')}</StatusBadge>}
      />
      <FilterToolbar label={t('exceptions.filters')} summary={`${visible.length} / ${exceptions.length} ${t('unit.item')}`}>
        <label className="grid gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
          {t('exceptions.state')}
          <select value={filter} onChange={(event) => setFilter(event.target.value as ExceptionFilter)} className="min-h-11 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold text-[var(--sc-ink)]">
            <option value="OPEN">{t('exceptions.filterOpen')}</option>
            <option value="RESOLVED">{t('exceptions.filterResolved')}</option>
            <option value="ALL">{t('exceptions.filterAll')}</option>
          </select>
        </label>
      </FilterToolbar>
      {visible.length === 0 ? (
        <EmptyState
          icon={filter === 'OPEN' ? <CheckCircle2 className="h-4 w-4" /> : <Search className="h-4 w-4" />}
          title={filter === 'OPEN' ? t('exceptions.emptyOpen') : t('exceptions.empty')}
          description={t('exceptions.emptyDetail')}
        />
      ) : (
        <ResponsiveDataList label={t('exceptions.results')}>
          {visible.map((item) => {
            const actions = exceptionActions(item, permissions);
            return (
              <article key={item.id} className="rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-4">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusBadge tone={exceptionSeverityTone(item.severity)}>{severityLabel[item.severity]}</StatusBadge>
                      <StatusBadge tone={item.resolved ? 'green' : 'amber'}>{item.resolved ? t('exceptions.resolved') : t('exceptions.pending')}</StatusBadge>
                    </div>
                    <h2 className="mt-3 text-sm font-black text-[var(--sc-ink)]">{item.title}</h2>
                    <p className="mt-1 text-xs leading-5 text-[var(--sc-ink-soft)]">{item.description}</p>
                    <span className="sc-data mt-2 block text-[9px] text-[var(--sc-ink-muted)]">{item.createdTime}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {item.relatedOrderId && <PermissionAwareAction allowed={actions.canAssign} label={t('exceptions.assign')} deniedLabel={t('exceptions.noAssign')} icon={<Zap className="h-4 w-4" />} onSelect={() => openAllocationModal(item.relatedOrderId!)} />}
                    {item.relatedDeviceId && <PermissionAwareAction allowed={actions.canViewDevice} label={t('exceptions.device')} deniedLabel={t('exceptions.noDevice')} icon={<Wrench className="h-4 w-4" />} onSelect={() => openDeviceDetail(item.relatedDeviceId!)} />}
                    {!item.resolved && (
                      <PermissionAwareAction
                        allowed={actions.canResolve && !resolvingId}
                        label={resolvingId === item.id ? t('exceptions.resolving') : t('exceptions.resolve')}
                        deniedLabel={resolvingId ? t('exceptions.busy') : t('exceptions.noResolve')}
                        icon={resolvingId === item.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                        tone="primary"
                        onSelect={() => void resolve(item.id)}
                      />
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </ResponsiveDataList>
      )}
    </div>
  );
}
