import type { OrderChannel, OrderStatus, RentalOrder } from '../../types';
import { inclusiveDateFromExclusive } from '../../lib/scheduleEngine';

export type PresentationTone = 'neutral' | 'blue' | 'green' | 'amber' | 'red';
export type OrderStatusFilter = OrderStatus | 'ALL';
export type OrderChannelFilter = OrderChannel | 'ALL';

export interface OrderFilters {
  status: OrderStatusFilter;
  channel: OrderChannelFilter;
  search: string;
}

export interface OrderActionAvailability {
  canAssign: boolean;
  canShip: boolean;
  detailDeviceId?: string;
  returnRequiresOperationalFlow: boolean;
}

export interface OrderPage<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
}

export function filterOrders(orders: RentalOrder[], filters: OrderFilters) {
  const search = filters.search.trim().toLocaleLowerCase();
  return orders.filter((order) => {
    if (filters.status !== 'ALL' && order.status !== filters.status) return false;
    if (filters.channel !== 'ALL' && order.channel !== filters.channel) return false;
    if (!search) return true;
    return [
      order.orderNumber,
      order.receiverName,
      order.receiverPhone,
      order.receiverAddress,
      ...order.items.map((item) => item.modelName),
    ]
      .filter((value): value is string => Boolean(value))
      .some((value) => value.toLocaleLowerCase().includes(search));
  });
}

export function paginateOrders<T>(
  items: T[],
  requestedPage: number,
  pageSize: number
): OrderPage<T> {
  const safePageSize = Math.max(1, Math.floor(pageSize));
  const totalPages = Math.max(1, Math.ceil(items.length / safePageSize));
  const page = Math.min(Math.max(1, Math.floor(requestedPage)), totalPages);
  const start = (page - 1) * safePageSize;
  return {
    items: items.slice(start, start + safePageSize),
    page,
    pageSize: safePageSize,
    totalPages,
    totalItems: items.length,
  };
}

export function orderStatusTone(status: OrderStatus): PresentationTone {
  const tones: Record<OrderStatus, PresentationTone> = {
    UNASSIGNED: 'amber',
    ASSIGNED: 'blue',
    PENDING_DISPATCH: 'blue',
    RENTING: 'green',
    PENDING_RETURN: 'amber',
    COMPLETED: 'neutral',
    EXCEPTION: 'red',
  };
  return tones[status];
}

export function orderChannelTone(channel: OrderChannel): PresentationTone {
  const tones: Record<OrderChannel, PresentationTone> = {
    XIANYU: 'amber',
    OFFLINE: 'neutral',
    WEB: 'blue',
    TAOBAO: 'red',
  };
  return tones[channel];
}

export function getOrderActionAvailability(
  order: RentalOrder,
  permissions: { canAssign: boolean; canShip: boolean; canViewDevice: boolean }
): OrderActionAvailability {
  const assignedDeviceId = order.items.flatMap((item) => item.assignedDeviceIds)[0];
  return {
    canAssign: order.canAssign && permissions.canAssign,
    canShip: order.canShip && permissions.canShip,
    detailDeviceId: permissions.canViewDevice ? assignedDeviceId : undefined,
    returnRequiresOperationalFlow: order.canReturn,
  };
}

export function orderDisplayRanges(order: RentalOrder) {
  return {
    billable: { startDate: order.startDate, endDate: order.endDate },
    occupied: {
      startDate: order.occupyStartDate,
      endDate: inclusiveDateFromExclusive(order.occupyEndDateExclusive) || '',
    },
  };
}
