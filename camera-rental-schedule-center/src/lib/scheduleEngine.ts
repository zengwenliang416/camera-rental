import { DeviceInstance, ScheduleBlock, RentalOrder, DeviceStatus } from '../types';

function toLocalDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

/**
 * Check whether a date range overlaps with another date range
 */
export function isDateOverlap(startA: string, endA: string, startB: string, endB: string): boolean {
  return startA <= endB && endA >= startB;
}

/**
 * Check if a specific device instance is available for a given order period
 */
export function checkDeviceAvailability(
  device: DeviceInstance,
  blocks: ScheduleBlock[],
  startDate: string,
  endDate: string,
  excludeOrderId?: string
): { available: boolean; reason?: string } {
  // Hard locks
  if (device.status === 'REPAIR') {
    return { available: false, reason: '设备处于维修/检测状态' };
  }
  if (device.status === 'LOCKED') {
    return { available: false, reason: '设备已被人工锁定' };
  }

  // Check schedule block overlaps
  const deviceBlocks = blocks.filter(
    (b) => b.deviceId === device.id && (!excludeOrderId || b.orderId !== excludeOrderId)
  );

  for (const block of deviceBlocks) {
    if (isDateOverlap(startDate, endDate, block.startDate, block.endDate)) {
      if (block.type === 'RENTAL') {
        return { available: false, reason: `在 ${block.startDate}~${block.endDate} 已被订单 ${block.orderNumber || ''} 租用` };
      } else if (block.type === 'RESERVE') {
        return { available: false, reason: `在 ${block.startDate}~${block.endDate} 已被预留 (${block.customerName || ''})` };
      } else if (block.type === 'REPAIR') {
        return { available: false, reason: `在 ${block.startDate}~${block.endDate} 安排了维保检修` };
      } else if (block.type === 'LOCK') {
        return { available: false, reason: `在 ${block.startDate}~${block.endDate} 已锁定` };
      }
    }
  }

  return { available: true };
}

/**
 * Smart Auto-Allocation Algorithm for Order Items ("一键智能排机")
 * Recommends available devices for each required model item in an order.
 */
export function recommendDevicesForOrder(
  order: RentalOrder,
  allDevices: DeviceInstance[],
  blocks: ScheduleBlock[]
): Record<string, DeviceInstance[]> {
  const result: Record<string, DeviceInstance[]> = {};
  if (!order.rentalPeriodReady || !order.startDate || !order.endDate) {
    return result;
  }

  for (const item of order.items) {
    const candidates = allDevices.filter((d) => d.modelId === item.modelId);

    // Filter available ones
    const availableCandidates = candidates.filter((d) => {
      const check = checkDeviceAvailability(d, blocks, order.startDate, order.endDate, order.id);
      return check.available;
    });

    // Sort priority: IDLE > PENDING_RETURN (if dates work) > others, then by numeric unitCode
    availableCandidates.sort((a, b) => {
      const scoreA = a.status === 'IDLE' ? 2 : 1;
      const scoreB = b.status === 'IDLE' ? 2 : 1;
      if (scoreA !== scoreB) return scoreB - scoreA;

      const numA = parseInt(a.unitCode.replace(/[^0-9]/g, '')) || 0;
      const numB = parseInt(b.unitCode.replace(/[^0-9]/g, '')) || 0;
      return numA - numB;
    });

    result[item.modelId] = availableCandidates.slice(0, item.quantity);
  }

  return result;
}

/**
 * Calculate 7-day utilization rate and status counts for a model
 */
export function calculateModelStats(
  modelId: string,
  devices: DeviceInstance[],
  blocks: ScheduleBlock[]
) {
  const modelDevices = devices.filter((d) => d.modelId === modelId);
  const totalUnits = modelDevices.length;

  let idleCount = 0;
  let reservedCount = 0;
  let rentingCount = 0;
  let pendingReturnCount = 0;
  let repairCount = 0;
  let lockedCount = 0;

  modelDevices.forEach((d) => {
    switch (d.status) {
      case 'IDLE': idleCount++; break;
      case 'RESERVED': reservedCount++; break;
      case 'RENTING': rentingCount++; break;
      case 'PENDING_RETURN': pendingReturnCount++; break;
      case 'REPAIR': repairCount++; break;
      case 'LOCKED': lockedCount++; break;
    }
  });

  // Calculate next 7 days occupancy from the real client date.
  const now = new Date();
  const today = toLocalDateString(now);
  const sevenDaysLater = toLocalDateString(addDays(now, 6));

  const busyDeviceIds = new Set<string>();
  blocks.forEach((b) => {
    const dev = modelDevices.find((d) => d.id === b.deviceId);
    if (dev && isDateOverlap(today, sevenDaysLater, b.startDate, b.endDate)) {
      busyDeviceIds.add(dev.id);
    }
  });

  modelDevices.forEach((d) => {
    if (d.status === 'RENTING' || d.status === 'RESERVED') {
      busyDeviceIds.add(d.id);
    }
  });

  const utilizationRate = totalUnits > 0 ? Math.round((busyDeviceIds.size / totalUnits) * 100) : 0;

  return {
    totalUnits,
    idleCount,
    reservedCount,
    rentingCount,
    pendingReturnCount,
    repairCount,
    lockedCount,
    utilizationRate,
  };
}
