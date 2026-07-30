import {
  AlertTriangle,
  Boxes,
  CalendarClock,
  CalendarDays,
  CheckCircle2,
  CircleGauge,
  ClipboardCheck,
  Cpu,
  Send,
  ShieldCheck,
  Wrench,
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { FeaturePageHeader } from '../../shared/ui/FeaturePageHeader';
import { OperationalMetricGrid, type OperationalMetric } from '../../shared/ui/OperationalMetricGrid';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import { DashboardQueueCard, type QueueRow } from './DashboardQueueCard';
import { buildDashboardReadModel, formatSyncSummary } from './dashboardModel';
import { canAccessTab } from '../../app/accessModel';

function orderItemsLabel(items: Array<{ modelName: string; quantity: number }>) {
  return items.length > 0
    ? items.map((item) => `${item.modelName || '-'} × ${item.quantity}`).join(' · ')
    : '-';
}

export function DashboardPage() {
  const {
    orders,
    devices,
    exceptions,
    isLoading,
    loadError,
    lastSyncAt,
    lastSyncDeviceCount,
    lastSyncOrderCount,
    setActiveTab,
    openAllocationModal,
    openQuickBinding,
    hasPermission,
    permissions,
  } = useApp();
  const { locale, t } = usePreferences();
  const model = buildDashboardReadModel(orders, devices, exceptions);
  const lastSyncSummary = lastSyncAt
    ? formatSyncSummary({
        locale,
        syncedLabel: t('sync.healthy'),
        deviceUnit: t('unit.device'),
        orderUnit: t('unit.order'),
        syncedAt: lastSyncAt,
        deviceCount: lastSyncDeviceCount,
        orderCount: lastSyncOrderCount,
      })
    : t('sync.waiting');

  const canOpen = (tab: Parameters<typeof canAccessTab>[1]) => canAccessTab(permissions, tab);
  const metrics: OperationalMetric[] = [
    {
      id: 'registered',
      label: t('dashboard.registered'),
      value: model.registeredDevices,
      unit: t('unit.device'),
      detail: `${model.utilizationPercent}%`,
      icon: <Boxes className="h-4 w-4" />,
      tone: 'neutral',
      onSelect: canOpen('devices') ? () => setActiveTab('devices') : undefined,
    },
    {
      id: 'available',
      label: t('dashboard.available'),
      value: model.availableDevices,
      unit: t('unit.device'),
      icon: <ShieldCheck className="h-4 w-4" />,
      tone: 'green',
      onSelect: canOpen('schedule') ? () => setActiveTab('schedule') : undefined,
    },
    {
      id: 'unassigned',
      label: t('dashboard.unassigned'),
      value: model.unassignedOrders.length,
      unit: t('unit.order'),
      icon: <CalendarClock className="h-4 w-4" />,
      tone: model.unassignedOrders.length > 0 ? 'amber' : 'neutral',
      onSelect: canOpen('orders') ? () => setActiveTab('orders') : undefined,
    },
    {
      id: 'shipping',
      label: t('dashboard.pendingShip'),
      value: model.pendingShipOrders.length,
      unit: t('unit.order'),
      icon: <Send className="h-4 w-4" />,
      tone: 'blue',
      onSelect: canOpen('binding') ? () => setActiveTab('binding') : undefined,
    },
    {
      id: 'rental',
      label: t('dashboard.activeRental'),
      value: model.activeRentalOrders.length,
      unit: t('unit.order'),
      icon: <CircleGauge className="h-4 w-4" />,
      tone: 'green',
      onSelect: canOpen('orders') ? () => setActiveTab('orders') : undefined,
    },
    {
      id: 'attention',
      label: t('dashboard.openReviews'),
      value: model.openReviews.length + model.maintenanceDevices.length,
      unit: t('unit.item'),
      icon: <AlertTriangle className="h-4 w-4" />,
      tone: model.openReviews.length + model.maintenanceDevices.length > 0 ? 'red' : 'neutral',
      onSelect: canOpen(model.openReviews.length > 0 ? 'exceptions' : 'devices')
        ? () => setActiveTab(model.openReviews.length > 0 ? 'exceptions' : 'devices')
        : undefined,
    },
  ];

  const assignmentRows: QueueRow[] = model.unassignedOrders.map((order) => ({
    id: order.id,
    primary: order.orderNumber,
    secondary: orderItemsLabel(order.items),
    meta: order.rentalPeriodLabel,
    state: (
      <StatusBadge tone={order.rentalPeriodReady ? 'amber' : 'red'}>
        {order.rentalPeriodReady ? t('dashboard.periodReady') : t('dashboard.periodPending')}
      </StatusBadge>
    ),
    disabled:
      !canOpen('orders')
      || !hasPermission('rental:device:assign')
      || !order.rentalPeriodReady,
    onSelect: () => openAllocationModal(order.id),
  }));
  const shippingRows: QueueRow[] = model.pendingShipOrders.map((order) => ({
    id: order.id,
    primary: order.orderNumber,
    secondary: orderItemsLabel(order.items),
    meta: order.rentalPeriodLabel,
    state: <StatusBadge tone={order.canShip ? 'blue' : 'amber'}>{order.canShip ? t('dashboard.shipReady') : t('dashboard.shipBlocked')}</StatusBadge>,
    disabled: !canOpen('binding') || !hasPermission('rental:xianyu:ship') || !order.canShip,
    onSelect: () => openQuickBinding(order.id),
  }));
  const rentalRows: QueueRow[] = model.activeRentalOrders.map((order) => ({
    id: order.id,
    primary: order.orderNumber,
    secondary: orderItemsLabel(order.items),
    meta: order.rentalPeriodLabel,
    state: <StatusBadge tone="green">{t('dashboard.activeRental')}</StatusBadge>,
    disabled: !canOpen('orders'),
    onSelect: () => setActiveTab('orders'),
  }));
  const reviewRows: QueueRow[] = model.openReviews.map((review) => ({
    id: review.id,
    primary: review.title,
    secondary: review.description,
    meta: review.createdTime,
    state: <StatusBadge tone={review.severity === 'high' ? 'red' : 'amber'}>{review.severity}</StatusBadge>,
    disabled: !canOpen('exceptions'),
    onSelect: () => setActiveTab('exceptions'),
  }));

  return (
    <div className="space-y-3">
      <FeaturePageHeader
        eyebrow={t('dashboard.eyebrow')}
        title={t('dashboard.title')}
        description={t('dashboard.description')}
        meta={(
          <StatusBadge
            tone={loadError ? 'amber' : isLoading ? 'blue' : 'green'}
            icon={loadError ? <AlertTriangle className="h-3 w-3" /> : <CheckCircle2 className="h-3 w-3" />}
          >
            {loadError ? t('sync.errorTitle') : isLoading ? t('sync.loadingTitle') : lastSyncSummary}
          </StatusBadge>
        )}
        actions={(
          <>
            {canOpen('schedule') && (
              <button type="button" onClick={() => setActiveTab('schedule')} className="inline-flex min-h-11 items-center gap-2 rounded-md border border-[var(--sc-border-strong)] bg-[var(--sc-surface)] px-3 text-xs font-bold text-[var(--sc-ink)]">
                <CalendarDays className="h-3.5 w-3.5 text-[var(--sc-blue)]" />
                {t('action.openSchedule')}
              </button>
            )}
            {canOpen('binding') && (
              <button type="button" onClick={() => setActiveTab('binding')} className="inline-flex min-h-11 items-center gap-2 rounded-md bg-[var(--sc-brand)] px-3 text-xs font-bold text-[var(--sc-surface)]">
                <Send className="h-3.5 w-3.5" />
                {t('action.openShipping')}
              </button>
            )}
          </>
        )}
      />

      <OperationalMetricGrid metrics={metrics} />

      <section className="sc-surface rounded-lg p-3">
        <div className="mb-3 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="text-sm font-black text-[var(--sc-ink)]">{t('dashboard.queueTitle')}</h2>
            <p className="mt-1 text-[11px] text-[var(--sc-ink-muted)]">{t('dashboard.queueDescription')}</p>
          </div>
          <span className="sc-data text-[9px] text-[var(--sc-ink-muted)]">{lastSyncSummary}</span>
        </div>
        <div className="grid min-w-0 grid-cols-1 gap-2 md:grid-cols-2 2xl:grid-cols-4">
          {canOpen('orders') && (
            <DashboardQueueCard step="01" title={t('dashboard.assignQueue')} count={model.unassignedOrders.length} emptyText={t('dashboard.emptyAssign')} actionLabel={t('action.openOrders')} onViewAll={() => setActiveTab('orders')} rows={assignmentRows} />
          )}
          {canOpen('binding') && (
            <DashboardQueueCard step="02" title={t('dashboard.shipQueue')} count={model.pendingShipOrders.length} emptyText={t('dashboard.emptyShip')} actionLabel={t('action.openShipping')} onViewAll={() => setActiveTab('binding')} rows={shippingRows} />
          )}
          {canOpen('orders') && (
            <DashboardQueueCard step="03" title={t('dashboard.rentalQueue')} count={model.activeRentalOrders.length} emptyText={t('dashboard.emptyRental')} actionLabel={t('action.openOrders')} onViewAll={() => setActiveTab('orders')} rows={rentalRows} />
          )}
          {canOpen('exceptions') && (
            <DashboardQueueCard step="04" title={t('dashboard.reviewQueue')} count={model.openReviews.length} emptyText={t('dashboard.emptyReview')} actionLabel={t('action.openExceptions')} onViewAll={() => setActiveTab('exceptions')} rows={reviewRows} />
          )}
        </div>
      </section>

      <div className="grid grid-cols-1 gap-3 lg:grid-cols-[minmax(0,1.5fr)_minmax(280px,0.7fr)]">
        {canOpen('devices') && <section className="sc-surface rounded-lg p-4">
          <div className="flex items-start gap-3">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-[var(--sc-amber-soft)] text-[var(--sc-amber)]">
              <Cpu className="h-4 w-4" />
            </span>
            <div>
              <p className="sc-data text-[9px] font-bold tracking-[0.14em] text-[var(--sc-amber)]">{t('dashboard.registeredBoundary')}</p>
              <h2 className="mt-1 text-sm font-black text-[var(--sc-ink)]">{t('dashboard.boundaryTitle')}</h2>
              <p className="mt-2 text-[11px] leading-5 text-[var(--sc-ink-soft)]">{t('dashboard.boundaryBody')}</p>
              <button type="button" onClick={() => setActiveTab('devices')} className="mt-3 inline-flex min-h-11 items-center gap-2 rounded-md border border-[var(--sc-border)] px-3 text-xs font-bold text-[var(--sc-blue)]">
                <Cpu className="h-3.5 w-3.5" />
                {t('action.openDevices')}
              </button>
            </div>
          </div>
        </section>}
        <section className="rounded-lg border border-[var(--sc-border)] bg-[var(--sc-brand)] p-4 text-[var(--sc-surface)]">
          <div className="flex items-center gap-2">
            <ClipboardCheck className="h-4 w-4" />
            <h2 className="text-sm font-black">{t('dashboard.healthTitle')}</h2>
          </div>
          <p className="mt-3 text-[11px] leading-5 opacity-75">
            {model.openReviews.length + model.maintenanceDevices.length > 0 ? t('dashboard.attention') : t('dashboard.noUrgent')}
          </p>
          <div className="mt-4 grid grid-cols-2 gap-2">
            {canOpen('devices') && (
              <button type="button" onClick={() => setActiveTab('devices')} className="min-h-11 rounded-md border border-white/20 bg-white/10 px-2 text-xs font-bold">
                <Wrench className="mr-1.5 inline h-3.5 w-3.5" />
                {model.maintenanceDevices.length} {t('unit.device')}
              </button>
            )}
            {canOpen('exceptions') && (
              <button type="button" onClick={() => setActiveTab('exceptions')} className="min-h-11 rounded-md border border-white/20 bg-white/10 px-2 text-xs font-bold">
                <AlertTriangle className="mr-1.5 inline h-3.5 w-3.5" />
                {model.openReviews.length} {t('unit.item')}
              </button>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
