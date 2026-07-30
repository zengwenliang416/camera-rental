import { useMemo, useState } from 'react';
import { Boxes, Database, Search, ShieldCheck } from 'lucide-react';

import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { calculateModelStats } from '../../lib/scheduleEngine';
import { EmptyState } from '../../shared/ui/EmptyState';
import { FeaturePageHeader } from '../../shared/ui/FeaturePageHeader';
import { FilterToolbar } from '../../shared/ui/FilterToolbar';
import { ResponsiveDataList } from '../../shared/ui/ResponsiveDataList';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import { DeviceCard } from './components/DeviceCard';
import {
  filterDevices,
  registeredAssetSummary,
  type DeviceStatusFilter,
} from './deviceModel';

export function DevicesPage() {
  const {
    models,
    devices,
    blocks,
    selectedModelId,
    setSelectedModelId,
    openDeviceDetail,
    hasPermission,
  } = useApp();
  const { t } = usePreferences();
  const [status, setStatus] = useState<DeviceStatusFilter>('ALL');
  const [search, setSearch] = useState('');
  const currentModel = models.find((model) => model.id === selectedModelId) || models[0];
  const summary = useMemo(() => registeredAssetSummary(devices), [devices]);
  const filtered = useMemo(
    () => currentModel ? filterDevices(devices, { modelId: currentModel.id, status, search }) : [],
    [currentModel, devices, search, status]
  );
  const canOpen = hasPermission('rental:device:query');
  const statusLabels = {
    IDLE: t('devices.statusIdle'),
    RESERVED: t('devices.statusReserved'),
    RENTING: t('devices.statusRenting'),
    PENDING_RETURN: t('devices.statusPendingReturn'),
    REPAIR: t('devices.statusRepair'),
    LOCKED: t('devices.statusLocked'),
  };

  return (
    <div className="space-y-4">
      <FeaturePageHeader
        eyebrow={t('devices.eyebrow')}
        title={t('devices.title')}
        description={t('devices.description')}
        meta={
          <div className="flex flex-wrap gap-2">
            <StatusBadge tone="neutral" icon={<Database className="h-3 w-3" />}>{summary.registeredCount} {t('devices.registered')}</StatusBadge>
            <StatusBadge tone="green">{summary.availableCount} {t('devices.available')}</StatusBadge>
            <StatusBadge tone="red">{summary.maintenanceCount} {t('devices.maintenance')}</StatusBadge>
          </div>
        }
      />

      <section className="rounded-xl border border-[color-mix(in_srgb,var(--sc-blue)_22%,var(--sc-border))] bg-[var(--sc-blue-soft)] p-4">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-[var(--sc-blue)]" />
          <div>
            <strong className="text-xs text-[var(--sc-ink)]">{t('devices.boundaryTitle')}</strong>
            <p className="mt-1 text-[11px] leading-5 text-[var(--sc-ink-soft)]">{t('devices.boundaryBody')}</p>
          </div>
        </div>
      </section>

      {models.length === 0 || !currentModel ? (
        <EmptyState icon={<Boxes className="h-4 w-4" />} title={t('devices.empty')} description={t('devices.emptyDetail')} />
      ) : (
        <>
          <section aria-label={t('devices.models')} className="grid grid-cols-2 gap-2 md:grid-cols-3 xl:grid-cols-5">
            {models.map((model) => {
              const stats = calculateModelStats(model.id, devices, blocks);
              const selected = model.id === currentModel.id;
              return (
                <button key={model.id} type="button" onClick={() => setSelectedModelId(model.id)} aria-pressed={selected} className={`min-h-24 rounded-xl border p-3 text-left focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] ${selected ? 'border-[var(--sc-ink)] bg-[var(--sc-ink)] text-[var(--sc-surface)]' : 'border-[var(--sc-border)] bg-[var(--sc-surface)] text-[var(--sc-ink)]'}`}>
                  <strong className="block truncate text-xs">{model.name}</strong>
                  <span className={`mt-2 block text-[10px] ${selected ? 'text-zinc-300' : 'text-[var(--sc-ink-muted)]'}`}>{stats.totalUnits} {t('unit.device')} · {stats.idleCount} {t('devices.available')}</span>
                </button>
              );
            })}
          </section>

          <FilterToolbar label={t('devices.filters')} summary={`${filtered.length} / ${currentModel.totalUnits} ${t('unit.device')}`}>
            <label className="grid gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
              {t('devices.status')}
              <select value={status} onChange={(event) => setStatus(event.target.value as DeviceStatusFilter)} className="min-h-11 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold text-[var(--sc-ink)]">
                <option value="ALL">{t('devices.allStatuses')}</option>
                {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            <label className="grid min-w-0 gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)] lg:min-w-72">
              {t('devices.search')}
              <span className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)]" />
                <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('devices.searchPlaceholder')} className="min-h-11 w-full rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] pl-10 pr-3 text-xs font-semibold text-[var(--sc-ink)]" />
              </span>
            </label>
          </FilterToolbar>

          {filtered.length === 0 ? (
            <EmptyState icon={<Search className="h-4 w-4" />} title={t('devices.noMatches')} description={t('devices.noMatchesDetail')} />
          ) : (
            <ResponsiveDataList label={t('devices.results')}>
              {filtered.map((device) => (
                <DeviceCard
                  key={device.id}
                  device={device}
                  canOpen={canOpen}
                  labels={{
                    status: statusLabels,
                    order: t('devices.order'),
                    customer: t('devices.customer'),
                    available: t('devices.expectedAvailable'),
                    now: t('devices.availableNow'),
                    unavailable: t('devices.availabilityUnknown'),
                    period: t('devices.currentPeriod'),
                    note: t('devices.note'),
                    warehouse: t('devices.warehouse'),
                    detail: t('devices.openDetail'),
                    noAccess: t('devices.noAccess'),
                  }}
                  onOpen={() => openDeviceDetail(device.id)}
                />
              ))}
            </ResponsiveDataList>
          )}
        </>
      )}
    </div>
  );
}
