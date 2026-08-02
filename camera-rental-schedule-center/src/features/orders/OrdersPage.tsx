import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Search, Send, ShoppingBag } from 'lucide-react';

import { useApp } from '../../context/AppContext';
import { usePreferences } from '../preferences/PreferenceContext';
import { EmptyState } from '../../shared/ui/EmptyState';
import { FeaturePageHeader } from '../../shared/ui/FeaturePageHeader';
import { FilterToolbar } from '../../shared/ui/FilterToolbar';
import { ResponsiveDataList } from '../../shared/ui/ResponsiveDataList';
import { StatusBadge } from '../../shared/ui/StatusBadge';
import {
  filterOrders,
  paginateOrders,
  type OrderChannelFilter,
  type OrderStatusFilter,
} from './orderModel';
import { OrderCard } from './components/OrderCard';
import { useDeliveryTracking } from '../tracking/TrackingContext';
import { DeliveryTrackingDrawer } from '../tracking/components/DeliveryTrackingDrawer';

export function OrdersPage() {
  const {
    orders,
    devices,
    openAllocationModal,
    openDeviceDetail,
    setActiveTab,
    openQuickBinding,
    hasPermission,
  } = useApp();
  const { t } = usePreferences();
  const { trackingByOrderId } = useDeliveryTracking();
  const [status, setStatus] = useState<OrderStatusFilter>('ALL');
  const [channel, setChannel] = useState<OrderChannelFilter>('ALL');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedTrackingOrderId, setSelectedTrackingOrderId] = useState<string | null>(null);
  const filtered = useMemo(
    () => filterOrders(orders, { status, channel, search }),
    [channel, orders, search, status]
  );
  const paged = useMemo(
    () => paginateOrders(filtered, page, pageSize),
    [filtered, page, pageSize]
  );
  const selectedTrackingOrder = useMemo(
    () => selectedTrackingOrderId
      ? orders.find(
          (order) => String(order.rentalOrderId ?? order.id) === selectedTrackingOrderId
        )
      : undefined,
    [orders, selectedTrackingOrderId]
  );
  const canShip = hasPermission('rental:xianyu:ship');
  const canAssign = hasPermission('rental:device:assign');
  const canViewDevice = hasPermission('rental:device:query');

  const openShipping = (orderId: string | null) => {
    openQuickBinding(orderId);
  };

  useEffect(() => {
    setPage(1);
  }, [channel, search, status]);

  useEffect(() => {
    if (page !== paged.page) setPage(paged.page);
  }, [page, paged.page]);

  const statusLabels = {
    UNASSIGNED: t('orders.statusUnassigned'),
    ASSIGNED: t('orders.statusAssigned'),
    PENDING_DISPATCH: t('orders.statusPendingDispatch'),
    RENTING: t('orders.statusRenting'),
    PENDING_RETURN: t('orders.statusPendingReturn'),
    COMPLETED: t('orders.statusCompleted'),
    EXCEPTION: t('orders.statusException'),
  };
  const channelLabels = {
    XIANYU: t('orders.channelXianyu'),
    OFFLINE: t('orders.channelOffline'),
    WEB: t('orders.channelWeb'),
    TAOBAO: t('orders.channelTaobao'),
  };

  return (
    <div className="space-y-4">
      <FeaturePageHeader
        eyebrow={t('orders.eyebrow')}
        title={t('orders.title')}
        description={t('orders.description')}
        meta={<StatusBadge tone="neutral">{t('orders.privateBoundary')}</StatusBadge>}
        actions={canShip ? (
          <button type="button" onClick={() => openShipping(null)} className="inline-flex min-h-11 items-center gap-2 rounded-lg bg-[var(--sc-ink)] px-4 text-xs font-black text-[var(--sc-surface)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]">
            <Send className="h-4 w-4" />
            {t('orders.openShipping')}
          </button>
        ) : undefined}
      />

      <FilterToolbar
        label={t('orders.filters')}
        summary={`${filtered.length} / ${orders.length} ${t('unit.order')} · ${t('orders.page')} ${paged.page}/${paged.totalPages}`}
      >
        <label className="grid gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
          {t('orders.status')}
          <select value={status} onChange={(event) => setStatus(event.target.value as OrderStatusFilter)} className="min-h-11 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold text-[var(--sc-ink)]">
            <option value="ALL">{t('orders.allStatuses')}</option>
            {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label className="grid gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
          {t('orders.channel')}
          <select value={channel} onChange={(event) => setChannel(event.target.value as OrderChannelFilter)} className="min-h-11 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold text-[var(--sc-ink)]">
            <option value="ALL">{t('orders.allChannels')}</option>
            {Object.entries(channelLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label className="grid min-w-0 gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)] lg:min-w-64">
          {t('orders.search')}
          <span className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)]" />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('orders.searchPlaceholder')} className="min-h-11 w-full rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] pl-10 pr-3 text-xs font-semibold text-[var(--sc-ink)]" />
          </span>
        </label>
      </FilterToolbar>

      {orders.length === 0 ? (
        <EmptyState icon={<ShoppingBag className="h-4 w-4" />} title={t('orders.empty')} description={t('orders.emptyDetail')} />
      ) : filtered.length === 0 ? (
        <EmptyState icon={<Search className="h-4 w-4" />} title={t('orders.noMatches')} description={t('orders.noMatchesDetail')} />
      ) : (
        <ResponsiveDataList label={t('orders.results')}>
          {paged.items.map((order) => {
            const trackingOrderId = String(order.rentalOrderId ?? order.id);
            return (
              <OrderCard
                key={order.id}
                order={order}
                devices={devices}
                trackingSummary={trackingByOrderId[trackingOrderId]}
                permissions={{ canAssign, canShip, canViewDevice }}
                labels={{
                  channel: channelLabels,
                  status: statusLabels,
                  customer: t('orders.customer'),
                  phone: t('orders.phone'),
                  address: t('orders.address'),
                  periodPending: t('orders.periodPending'),
                  billable: t('schedule.billable'),
                  occupied: t('schedule.occupied'),
                  billableHint: t('schedule.billableHint'),
                  occupiedHint: t('schedule.occupiedHint'),
                  requirements: t('orders.requirements'),
                  assigned: t('orders.assigned'),
                  unassigned: t('orders.unassigned'),
                  created: t('orders.created'),
                  assign: t('orders.assign'),
                  ship: t('orders.ship'),
                  openDevice: t('orders.openDevice'),
                  openTracking: t('orders.openTracking'),
                  returnOperational: t('orders.returnOperational'),
                  noAction: t('orders.noAction'),
                }}
                onAssign={() => openAllocationModal(order.id)}
                onShip={() => openShipping(order.id)}
                onOpenDevice={openDeviceDetail}
                onOpenTracking={() => setSelectedTrackingOrderId(trackingOrderId)}
              />
            );
          })}
        </ResponsiveDataList>
      )}

      {filtered.length > 0 && (
        <nav
          aria-label={t('orders.pagination')}
          className="flex flex-col gap-3 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-3 sm:flex-row sm:items-center sm:justify-between"
        >
          <div className="flex flex-wrap items-center gap-2 text-[10px] font-bold text-[var(--sc-ink-muted)]">
            <span>{t('orders.total').replace('{count}', String(paged.totalItems))}</span>
            <label className="flex items-center gap-2">
              {t('orders.pageSize')}
              <select
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value));
                  setPage(1);
                }}
                className="min-h-10 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold text-[var(--sc-ink)]"
              >
                {[10, 20, 50].map((size) => (
                  <option key={size} value={size}>{size}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="flex items-center justify-between gap-2 sm:justify-end">
            <button
              type="button"
              disabled={paged.page <= 1}
              onClick={() => setPage((current) => Math.max(1, current - 1))}
              className="inline-flex min-h-10 items-center gap-1 rounded-lg border border-[var(--sc-border-strong)] px-3 text-xs font-bold text-[var(--sc-ink)] disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft className="h-4 w-4" />
              {t('orders.previous')}
            </button>
            <span className="sc-data min-w-20 text-center text-xs font-black text-[var(--sc-ink)]">
              {paged.page} / {paged.totalPages}
            </span>
            <button
              type="button"
              disabled={paged.page >= paged.totalPages}
              onClick={() => setPage((current) => Math.min(paged.totalPages, current + 1))}
              className="inline-flex min-h-10 items-center gap-1 rounded-lg border border-[var(--sc-border-strong)] px-3 text-xs font-bold text-[var(--sc-ink)] disabled:cursor-not-allowed disabled:opacity-40"
            >
              {t('orders.next')}
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </nav>
      )}

      {selectedTrackingOrderId && (
        <DeliveryTrackingDrawer
          orderId={selectedTrackingOrderId}
          orderNumber={selectedTrackingOrder?.orderNumber}
          customerName={
            selectedTrackingOrder?.receiverName || selectedTrackingOrder?.customerName
          }
          onClose={() => setSelectedTrackingOrderId(null)}
        />
      )}
    </div>
  );
}
