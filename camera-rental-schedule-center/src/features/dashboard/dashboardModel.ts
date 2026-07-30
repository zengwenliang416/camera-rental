import type { DeviceInstance, ExceptionItem, RentalOrder } from '../../types';

export interface DashboardReadModel {
  registeredDevices: number;
  availableDevices: number;
  rentingDevices: number;
  utilizationPercent: number;
  maintenanceDevices: DeviceInstance[];
  unassignedOrders: RentalOrder[];
  pendingShipOrders: RentalOrder[];
  activeRentalOrders: RentalOrder[];
  openReviews: ExceptionItem[];
}

export function formatSyncSummary({
  locale,
  syncedLabel,
  deviceUnit,
  orderUnit,
  syncedAt,
  deviceCount,
  orderCount,
}: {
  locale: string;
  syncedLabel: string;
  deviceUnit: string;
  orderUnit: string;
  syncedAt: number;
  deviceCount: number;
  orderCount: number;
}) {
  const time = new Intl.DateTimeFormat(locale, {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    timeZone: 'Asia/Shanghai',
  }).format(syncedAt);
  return `${syncedLabel} · ${time} · ${deviceCount} ${deviceUnit} / ${orderCount} ${orderUnit}`;
}

export function buildDashboardReadModel(
  orders: RentalOrder[],
  devices: DeviceInstance[],
  exceptions: ExceptionItem[]
): DashboardReadModel {
  const rentingDevices = devices.filter(
    (device) => device.status === 'RENTING' || device.status === 'PENDING_RETURN'
  ).length;

  return {
    registeredDevices: devices.length,
    availableDevices: devices.filter((device) => device.status === 'IDLE').length,
    rentingDevices,
    utilizationPercent: devices.length > 0 ? Math.round((rentingDevices / devices.length) * 100) : 0,
    maintenanceDevices: devices.filter(
      (device) => device.status === 'REPAIR' || device.status === 'LOCKED'
    ),
    unassignedOrders: orders.filter((order) => order.status === 'UNASSIGNED'),
    pendingShipOrders: orders.filter((order) => order.status === 'PENDING_DISPATCH'),
    activeRentalOrders: orders.filter(
      (order) => order.status === 'RENTING' || order.status === 'PENDING_RETURN'
    ),
    openReviews: exceptions.filter((item) => !item.resolved),
  };
}
