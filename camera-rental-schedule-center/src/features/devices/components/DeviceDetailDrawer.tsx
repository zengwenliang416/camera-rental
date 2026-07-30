import { CalendarClock, Clock3, Cpu, PackageCheck, Truck, Wrench } from 'lucide-react';

import { useApp } from '../../../context/AppContext';
import type { DeviceInstance, ScheduleBlock } from '../../../types';
import { DateRangeDisplay } from '../../../shared/ui/DateRangeDisplay';
import { DetailDrawerShell } from '../../../shared/ui/DetailDrawerShell';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { IdentifierText } from '../../../shared/ui/IdentifierText';
import { PermissionAwareAction } from '../../../shared/ui/PermissionAwareAction';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { usePreferences } from '../../preferences/PreferenceContext';
import { deviceCardPresentation, deviceStatusTone } from '../deviceModel';
import { useDeviceQr } from '../useDeviceQr';
import { DeviceQrPanel } from './DeviceQrPanel';

function localDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function ScheduleHistory({
  title,
  emptyTitle,
  emptyDetail,
  blocks,
  icon,
}: {
  title: string;
  emptyTitle: string;
  emptyDetail: string;
  blocks: ScheduleBlock[];
  icon: 'future' | 'past';
}) {
  const Icon = icon === 'future' ? CalendarClock : Clock3;
  return (
    <section className="space-y-3">
      <h3 className="flex items-center gap-2 text-sm font-black text-[var(--sc-ink)]">
        <Icon className="h-4 w-4 text-[var(--sc-blue)]" />
        {title}
      </h3>
      {blocks.length === 0 ? (
        <EmptyState
          icon={<Icon className="h-4 w-4" />}
          title={emptyTitle}
          description={emptyDetail}
        />
      ) : (
        <div className="space-y-2">
          {blocks.map((block) => (
            <article key={block.id} className="rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <IdentifierText value={block.orderNumber || block.statusText || block.type} emphasis />
                <StatusBadge tone={block.type === 'REPAIR' ? 'red' : block.type === 'LOCK' ? 'neutral' : 'blue'}>
                  {block.statusText || block.type}
                </StatusBadge>
              </div>
              <p className="sc-data mt-2 text-[10px] text-[var(--sc-ink-soft)]">
                {block.startDate} → {block.endDate}
              </p>
              {block.logisticsNumber && (
                <p className="mt-2 inline-flex items-center gap-1.5 text-[10px] text-[var(--sc-ink-muted)]">
                  <Truck className="h-3.5 w-3.5" />
                  <span className="sc-data">{block.logisticsNumber}</span>
                </p>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export function DeviceDetailDrawer() {
  const {
    devices,
    blocks,
    selectedDeviceIdForDetail,
    openDeviceDetail,
    setActiveTab,
    hasPermission,
  } = useApp();
  const { t } = usePreferences();
  const device = devices.find((item) => item.id === selectedDeviceIdForDetail);
  const canReadQr = hasPermission('rental:device:query');
  const qrState = useDeviceQr(device?.id, canReadQr);

  if (!device || !selectedDeviceIdForDetail) return null;

  const today = localDateString(new Date());
  const deviceBlocks = blocks.filter((block) => block.deviceId === device.id);
  const futureBlocks = deviceBlocks.filter((block) => block.endDate >= today);
  const pastBlocks = deviceBlocks.filter((block) => block.endDate < today);
  const presentation = deviceCardPresentation(device);
  const availability = presentation.availability.kind === 'now'
    ? t('devices.availableNow')
    : presentation.availability.kind === 'date'
      ? presentation.availability.value
      : t('deviceDetail.notAvailable');
  const statusLabels: Record<DeviceInstance['status'], string> = {
    IDLE: t('devices.statusIdle'),
    RESERVED: t('devices.statusReserved'),
    RENTING: t('devices.statusRenting'),
    PENDING_RETURN: t('devices.statusPendingReturn'),
    REPAIR: t('devices.statusRepair'),
    LOCKED: t('devices.statusLocked'),
  };

  return (
    <DetailDrawerShell
      title={<span className="inline-flex items-center gap-2"><Cpu className="h-5 w-5 text-[var(--sc-blue)]" />{device.modelName}</span>}
      description={t('deviceDetail.description')}
      closeLabel={t('deviceDetail.close')}
      onClose={() => openDeviceDetail(null)}
    >
      <div className="space-y-5">
        <section className="rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="grid gap-2 sm:grid-cols-2">
              <IdentifierText value={device.unitCode} emphasis />
              <IdentifierText value={device.sn} />
            </div>
            <StatusBadge tone={deviceStatusTone(device.status)}>{statusLabels[device.status]}</StatusBadge>
          </div>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <DateRangeDisplay
              label={t('deviceDetail.currentPeriod')}
              startDate={device.currentPeriod?.startDate}
              endDate={device.currentPeriod?.endDate}
              hint={t('deviceDetail.serverRange')}
              tone="blue"
            />
            <div className="rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] px-3 py-2.5">
              <span className="text-[10px] font-bold uppercase tracking-[0.12em] text-[var(--sc-ink-muted)]">
                {t('deviceDetail.expectedAvailable')}
              </span>
              <strong className="sc-data mt-1 block text-xs text-[var(--sc-ink)]">
                {availability}
              </strong>
            </div>
          </div>
          {device.currentOrderId && (
            <div className="mt-4 grid gap-2 border-t border-[var(--sc-border)] pt-4 text-[11px] text-[var(--sc-ink-soft)] sm:grid-cols-2">
              <span>{t('deviceDetail.order')} <span className="sc-data font-bold text-[var(--sc-ink)]">{device.currentOrderId}</span></span>
              <span>{t('deviceDetail.customer')} {device.currentCustomer || '-'}</span>
              {device.logisticsNumber && <span className="sm:col-span-2">{t('deviceDetail.waybill')} <span className="sc-data font-bold text-[var(--sc-ink)]">{device.logisticsNumber}</span></span>}
            </div>
          )}
        </section>

        <DeviceQrPanel state={qrState} deviceNo={device.unitCode} serialNumber={device.sn} />

        <PermissionAwareAction
          allowed={hasPermission('rental:xianyu:ship')}
          label={t('deviceDetail.openShipping')}
          deniedLabel={t('deviceDetail.noShipping')}
          icon={<PackageCheck className="h-4 w-4" />}
          tone="primary"
          onSelect={() => {
            openDeviceDetail(null);
            setActiveTab('binding');
          }}
        />

        {presentation.note && (
          <section className="rounded-xl border border-[var(--sc-border)] bg-[var(--sc-amber-soft)] p-4">
            <h3 className="flex items-center gap-2 text-xs font-black text-[var(--sc-ink)]">
              <Wrench className="h-4 w-4 text-[var(--sc-amber)]" />
              {presentation.note.kind === 'warehouse' ? t('devices.warehouse') : t('deviceDetail.note')}
            </h3>
            <p className="mt-2 text-[11px] leading-5 text-[var(--sc-ink-soft)]">{presentation.note.value}</p>
          </section>
        )}

        <ScheduleHistory
          title={t('deviceDetail.future')}
          emptyTitle={t('deviceDetail.futureEmpty')}
          emptyDetail={t('deviceDetail.futureEmptyDetail')}
          blocks={futureBlocks}
          icon="future"
        />
        <ScheduleHistory
          title={t('deviceDetail.history')}
          emptyTitle={t('deviceDetail.historyEmpty')}
          emptyDetail={t('deviceDetail.historyEmptyDetail')}
          blocks={pastBlocks}
          icon="past"
        />

        <p className="rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] p-3 text-[10px] leading-5 text-[var(--sc-ink-muted)]">
          {t('deviceDetail.readOnlyBoundary')}
        </p>
      </div>
    </DetailDrawerShell>
  );
}
