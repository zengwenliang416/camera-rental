import type { DeviceInstance, RentalOrder, ScheduleBlock } from '../../types';
import { inclusiveDateFromExclusive } from '../../lib/scheduleEngine';

export type ScheduleStatusFilter = DeviceInstance['status'] | 'ALL';
export type ScheduleViewMode = 'gantt' | 'table';
export type AllocationMap = Record<string, string[]>;

export interface ScheduleDay {
  dateStr: string;
  displayDay: string;
  weekday: string;
  isToday: boolean;
}

export interface AllocationProgress {
  totalRequired: number;
  totalAssigned: number;
  complete: boolean;
  percent: number;
}

export type AllocationSubmitReason =
  | 'ready'
  | 'submitting'
  | 'permission'
  | 'period'
  | 'details'
  | 'incomplete';

function toLocalDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function buildScheduleWindow(
  baseDate = new Date(),
  count = 14,
  locale: 'zh-CN' | 'en' = 'zh-CN'
): ScheduleDay[] {
  const start = new Date(baseDate);
  start.setHours(0, 0, 0, 0);

  return Array.from({ length: count }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return {
      dateStr: toLocalDateString(date),
      displayDay: `${date.getMonth() + 1}/${date.getDate()}`,
      weekday: new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(date),
      isToday: index === 0,
    };
  });
}

export function filterScheduleDevices(
  devices: DeviceInstance[],
  filters: { status: ScheduleStatusFilter; search: string }
) {
  const query = filters.search.trim().toLocaleLowerCase();
  return devices.filter((device) => {
    if (filters.status !== 'ALL' && device.status !== filters.status) return false;
    if (!query) return true;
    return [device.unitCode, device.sn, device.modelName, device.currentCustomer, device.note]
      .filter(Boolean)
      .some((value) => String(value).toLocaleLowerCase().includes(query));
  });
}

export function deriveOrderRanges(order: RentalOrder) {
  return {
    billable: {
      startDate: order.startDate,
      endDate: order.endDate,
    },
    occupied: {
      startDate: order.occupyStartDate,
      endDate: inclusiveDateFromExclusive(order.occupyEndDateExclusive) || '',
    },
    occupyEndDateExclusive: order.occupyEndDateExclusive,
  };
}

export function buildAllocationProgress(
  order: RentalOrder,
  allocationMap: AllocationMap
): AllocationProgress {
  const totalRequired = order.items.reduce((sum, item) => sum + item.quantity, 0);
  const totalAssigned = order.items.reduce(
    (sum, item) => sum + (allocationMap[item.modelId]?.length || 0),
    0
  );
  const complete = totalRequired > 0 && totalAssigned >= totalRequired;
  return {
    totalRequired,
    totalAssigned,
    complete,
    percent:
      totalRequired > 0 ? Math.min(100, Math.round((totalAssigned / totalRequired) * 100)) : 0,
  };
}

export function evaluateAllocationSubmit({
  order,
  allocationMap,
  hasPermission,
  isSubmitting,
}: {
  order: RentalOrder;
  allocationMap: AllocationMap;
  hasPermission: boolean;
  isSubmitting: boolean;
}): { ready: boolean; reason: AllocationSubmitReason } {
  if (isSubmitting) return { ready: false, reason: 'submitting' };
  if (!hasPermission) return { ready: false, reason: 'permission' };
  if (
    !order.rentalPeriodReady ||
    !order.occupyStartDate ||
    !order.occupyEndDateExclusive
  ) {
    return { ready: false, reason: 'period' };
  }
  if (!order.canAssign || order.items.some((item) => !item.rentalOrderItemId)) {
    return { ready: false, reason: 'details' };
  }
  if (!buildAllocationProgress(order, allocationMap).complete) {
    return { ready: false, reason: 'incomplete' };
  }
  return { ready: true, reason: 'ready' };
}

export function blocksForDevice(blocks: ScheduleBlock[], deviceId: string) {
  return blocks.filter((block) => block.deviceId === deviceId);
}
