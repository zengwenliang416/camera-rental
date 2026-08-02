import { useMemo, useState } from 'react';
import { CalendarRange, ChevronRight, RefreshCw } from 'lucide-react';

import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { FeaturePageHeader } from '../../shared/ui/FeaturePageHeader';
import { EmptyState } from '../../shared/ui/EmptyState';
import {
  buildScheduleWindow,
  filterScheduleDevices,
  type ScheduleStatusFilter,
  type ScheduleViewMode,
} from './scheduleModel';
import { ScheduleFilters } from './components/ScheduleFilters';
import { ScheduleDeviceTable } from './components/ScheduleDeviceTable';
import { useDeliveryTracking } from '../tracking/TrackingContext';
import { DeliveryTrackingDrawer } from '../tracking/components/DeliveryTrackingDrawer';
import {
  CurrentWindowTracking,
  filterDevicesByTracking,
  matchesTrackingFilter,
  ScheduleTrackingMetrics,
  type ScheduleTrackingFilter,
} from './components/ScheduleTrackingWorkspace';
import { trackingCopy } from '../tracking/trackingCopy';

export function SchedulePage() {
  const {
    models,
    devices,
    orders,
    blocks,
    setSelectedModelId,
    openAllocationModal,
    openDeviceDetail,
  } = useApp();
  const { locale, t } = usePreferences();
  const {
    canReadTracking,
    trackingByOrderId,
    visibleTrackingSummaries,
    isSummaryLoading,
    summaryError,
    lastSummarySyncAt,
    refreshSummaries,
  } = useDeliveryTracking();
  const [modelFilter, setModelFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<ScheduleStatusFilter>('ALL');
  const [trackingStatus, setTrackingStatus] = useState<ScheduleTrackingFilter>('ALL');
  const [viewMode, setViewMode] = useState<ScheduleViewMode>('gantt');
  const [selectedTrackingOrderId, setSelectedTrackingOrderId] = useState<string | null>(null);

  const days = useMemo(() => buildScheduleWindow(new Date(), 14, locale), [locale]);
  const modelDevices = useMemo(
    () => modelFilter === 'ALL'
      ? devices
      : devices.filter((device) => device.modelId === modelFilter),
    [devices, modelFilter]
  );
  const statusFilteredDevices = useMemo(
    () => filterScheduleDevices(modelDevices, { search, status }),
    [modelDevices, search, status]
  );
  const filteredDevices = useMemo(
    () => filterDevicesByTracking(
      statusFilteredDevices,
      blocks,
      trackingByOrderId,
      trackingStatus
    ),
    [blocks, statusFilteredDevices, trackingByOrderId, trackingStatus]
  );
  const orderNumberByOrderId = useMemo(
    () => Object.fromEntries(orders.flatMap((order) => {
      const entries: Array<[string, string]> = [[String(order.id), order.orderNumber]];
      if (order.rentalOrderId) {
        entries.push([String(order.rentalOrderId), order.orderNumber]);
      }
      return entries;
    })),
    [orders]
  );

  const statusLabels = {
    statusIdle: t('schedule.statusIdle'),
    statusRenting: t('schedule.statusRenting'),
    statusReserved: t('schedule.statusReserved'),
    statusRepair: t('schedule.statusRepair'),
    statusLocked: t('schedule.statusLocked'),
  };
  const selectedTrackingOrder = selectedTrackingOrderId
    ? orders.find((order) =>
        String(order.rentalOrderId ?? '') === selectedTrackingOrderId
        || String(order.id) === selectedTrackingOrderId
      )
    : undefined;
  const selectedTrackingBlock = selectedTrackingOrderId
    ? blocks.find((block) => block.orderId === selectedTrackingOrderId)
    : undefined;

  return (
    <div className="space-y-4">
      <nav
        aria-label={t('schedule.flow')}
        className="flex min-h-9 items-center gap-2 overflow-x-auto rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] px-3 text-[9px] font-bold text-[var(--sc-ink-muted)]"
      >
        {[
          t('schedule.flowShipment'),
          'Delivery',
          'Outbox',
          t('schedule.flowProvider'),
          'Inbox / Trace',
          t('schedule.flowSchedule'),
        ].map((item, index, items) => (
          <span key={item} className="flex shrink-0 items-center gap-2">
            <span>{item}</span>
            {index < items.length - 1 && <ChevronRight className="h-3 w-3 text-[var(--sc-blue)]" />}
          </span>
        ))}
      </nav>

      <FeaturePageHeader
        eyebrow={t('schedule.eyebrow')}
        title={t('schedule.title')}
        description={t('schedule.description')}
        actions={
          <button
            type="button"
            onClick={() => void refreshSummaries()}
            disabled={isSummaryLoading || !canReadTracking}
            className="inline-flex min-h-11 items-center gap-2 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface)] px-4 text-xs font-black text-[var(--sc-ink)] disabled:cursor-not-allowed disabled:opacity-45"
          >
            <RefreshCw className={`h-4 w-4 ${isSummaryLoading ? 'animate-spin' : ''}`} />
            {trackingCopy(locale, 'workspace.refresh')}
          </button>
        }
      />

      {models.length === 0 ? (
        <EmptyState
          icon={<CalendarRange className="h-4 w-4" />}
          title={t('schedule.noModels')}
          description={t('schedule.noModelsDetail')}
        />
      ) : (
        <>
          <ScheduleTrackingMetrics
            summaries={visibleTrackingSummaries}
            isLoading={isSummaryLoading}
          />

          <ScheduleFilters
            models={models}
            selectedModelId={modelFilter}
            search={search}
            status={status}
            trackingStatus={trackingStatus}
            viewMode={viewMode}
            labels={{
              model: t('schedule.model'),
              modelAll: t('schedule.modelAll'),
              search: t('schedule.search'),
              statusAll: t('schedule.statusAll'),
              ...statusLabels,
              trackingAll: t('schedule.trackingAll'),
              trackingActive: t('schedule.trackingActive'),
              trackingDelivered: t('schedule.trackingDelivered'),
              trackingRisk: t('schedule.trackingRisk'),
              trackingMapping: t('schedule.trackingMapping'),
              window: t('schedule.window'),
              viewGantt: t('schedule.viewGantt'),
              viewTable: t('schedule.viewTable'),
            }}
            onModelChange={(value) => {
              setModelFilter(value);
              if (value !== 'ALL') setSelectedModelId(value);
            }}
            onSearchChange={setSearch}
            onStatusChange={setStatus}
            onTrackingStatusChange={setTrackingStatus}
            onViewModeChange={setViewMode}
          />

          <section className="grid min-w-0 gap-3 2xl:grid-cols-[minmax(0,1fr)_308px]">
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
                occupiedInRange: t('schedule.occupiedInRange'),
                ...statusLabels,
              }}
              onOpenDevice={(deviceId) => openDeviceDetail(deviceId)}
              onOpenOrder={(orderId) => openAllocationModal(orderId)}
              onOpenTracking={setSelectedTrackingOrderId}
              trackingByOrderId={trackingByOrderId}
              orderNumberByOrderId={orderNumberByOrderId}
            />
            <CurrentWindowTracking
              summaries={visibleTrackingSummaries.filter((summary) =>
                matchesTrackingFilter(summary, trackingStatus)
              )}
              orderNumberByOrderId={orderNumberByOrderId}
              lastSummarySyncAt={lastSummarySyncAt}
              canReadTracking={canReadTracking}
              isLoading={isSummaryLoading}
              hasError={Boolean(summaryError)}
              onOpen={setSelectedTrackingOrderId}
            />
          </section>
          {selectedTrackingOrderId && (
            <DeliveryTrackingDrawer
              orderId={selectedTrackingOrderId}
              orderNumber={selectedTrackingOrder?.orderNumber || selectedTrackingBlock?.orderNumber}
              customerName={
                selectedTrackingOrder?.receiverName
                || selectedTrackingOrder?.customerName
                || selectedTrackingBlock?.customerName
              }
              onClose={() => setSelectedTrackingOrderId(null)}
            />
          )}
        </>
      )}
    </div>
  );
}
