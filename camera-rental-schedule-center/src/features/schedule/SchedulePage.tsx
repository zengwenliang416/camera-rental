import { useMemo, useState } from 'react';
import { CalendarRange, Database, Wrench } from 'lucide-react';

import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { calculateModelStats } from '../../lib/scheduleEngine';
import { FeaturePageHeader } from '../../shared/ui/FeaturePageHeader';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { BillableOccupiedRangeLegend } from '../../shared/ui/BillableOccupiedRangeLegend';
import {
  buildScheduleWindow,
  filterScheduleDevices,
  type ScheduleStatusFilter,
  type ScheduleViewMode,
} from './scheduleModel';
import { ScheduleFilters } from './components/ScheduleFilters';
import { ScheduleDeviceTable } from './components/ScheduleDeviceTable';
import { ScheduleStatusLegend } from './components/ScheduleStatusLegend';

export function SchedulePage() {
  const {
    models,
    devices,
    blocks,
    selectedModelId,
    setSelectedModelId,
    openAllocationModal,
    openDeviceDetail,
  } = useApp();
  const { locale, t } = usePreferences();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<ScheduleStatusFilter>('ALL');
  const [viewMode, setViewMode] = useState<ScheduleViewMode>('gantt');

  const currentModel = models.find((model) => model.id === selectedModelId) || models[0];
  const days = useMemo(() => buildScheduleWindow(new Date(), 14, locale), [locale]);
  const modelDevices = useMemo(
    () => devices.filter((device) => device.modelId === currentModel?.id),
    [currentModel?.id, devices]
  );
  const filteredDevices = useMemo(
    () => filterScheduleDevices(modelDevices, { search, status }),
    [modelDevices, search, status]
  );
  const stats = useMemo(
    () =>
      currentModel
        ? calculateModelStats(currentModel.id, devices, blocks)
        : null,
    [blocks, currentModel, devices]
  );

  const statusLabels = {
    statusIdle: t('schedule.statusIdle'),
    statusRenting: t('schedule.statusRenting'),
    statusReserved: t('schedule.statusReserved'),
    statusRepair: t('schedule.statusRepair'),
    statusLocked: t('schedule.statusLocked'),
  };

  return (
    <div className="space-y-4">
      <FeaturePageHeader
        eyebrow={t('schedule.eyebrow')}
        title={t('schedule.title')}
        description={t('schedule.description')}
        meta={
          <StatusBadge tone="neutral" icon={<Database className="h-3 w-3" />}>
            {t('schedule.registeredBoundary')}
          </StatusBadge>
        }
      />

      {!currentModel ? (
        <EmptyState
          icon={<CalendarRange className="h-4 w-4" />}
          title={t('schedule.noModels')}
          description={t('schedule.noModelsDetail')}
        />
      ) : (
        <>
          <ScheduleFilters
            models={models}
            selectedModelId={currentModel.id}
            search={search}
            status={status}
            viewMode={viewMode}
            labels={{
              model: t('schedule.model'),
              search: t('schedule.search'),
              statusAll: t('schedule.statusAll'),
              ...statusLabels,
              viewGantt: t('schedule.viewGantt'),
              viewTable: t('schedule.viewTable'),
            }}
            onModelChange={setSelectedModelId}
            onSearchChange={setSearch}
            onStatusChange={setStatus}
            onViewModeChange={setViewMode}
          />

          <section className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_minmax(320px,0.62fr)]">
            <div className="flex flex-wrap items-center gap-2 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-3">
              <strong className="mr-auto text-sm text-[var(--sc-ink)]">{currentModel.name}</strong>
              <StatusBadge tone="neutral">{stats?.totalUnits || 0} {t('unit.device')}</StatusBadge>
              <StatusBadge tone="green">{t('schedule.statusIdle')} {stats?.idleCount || 0}</StatusBadge>
              <StatusBadge tone="blue">{t('schedule.statusRenting')} {stats?.rentingCount || 0}</StatusBadge>
              <StatusBadge tone="red" icon={<Wrench className="h-3 w-3" />}>
                {t('schedule.statusRepair')} {stats?.repairCount || 0}
              </StatusBadge>
            </div>
            <BillableOccupiedRangeLegend
              billableLabel={t('schedule.billable')}
              occupiedLabel={t('schedule.occupied')}
              billableHint={t('schedule.billableHint')}
              occupiedHint={t('schedule.occupiedHint')}
            />
          </section>

          <ScheduleStatusLegend
            label={t('schedule.legend')}
            labels={{
              RENTAL: t('schedule.legendRental'),
              RESERVE: t('schedule.legendReserve'),
              REPAIR: t('schedule.legendRepair'),
              LOCK: t('schedule.legendLock'),
              FREE: t('schedule.legendFree'),
            }}
          />

          <ScheduleDeviceTable
            devices={filteredDevices}
            blocks={blocks}
            days={days}
            viewMode={viewMode}
            labels={{
              internalScroller: t('schedule.internalScroller'),
              noMatches: t('schedule.noMatches'),
              noMatchesDetail: t('schedule.noMatchesDetail'),
              deviceIdentity: t('schedule.deviceIdentity'),
              currentStatus: t('schedule.currentStatus'),
              relatedOrder: t('schedule.relatedOrder'),
              customer: t('schedule.customer'),
              expectedAvailable: t('schedule.expectedAvailable'),
              openDetail: t('schedule.openDetail'),
              availableNow: t('schedule.availableNow'),
              free: t('schedule.free'),
              blockRental: t('schedule.legendRental'),
              blockReserve: t('schedule.legendReserve'),
              blockRepair: t('schedule.legendRepair'),
              blockLock: t('schedule.legendLock'),
              ...statusLabels,
            }}
            onOpenDevice={(deviceId) => openDeviceDetail(deviceId)}
            onOpenOrder={(orderId) => openAllocationModal(orderId)}
          />
        </>
      )}
    </div>
  );
}
