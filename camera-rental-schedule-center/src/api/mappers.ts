import {
  DeviceInstance,
  EquipmentModel,
  ExceptionItem,
  ModelCategory,
  RentalOrder,
  ScheduleBlock,
} from '../types';
import {
  RentalDeviceVO,
  RentalManualReviewVO,
  RentalScheduleVO,
  XianyuOrderVO,
  XianyuPendingShipOrderVO,
} from './rental';

const CATEGORY_ID = 'cat-rental-equipment';

function modelName(modelCode: string) {
  const normalized = modelCode.toUpperCase();
  if (normalized === 'P4') return '大疆 P4P';
  if (normalized === 'A7M4') return 'Sony A7M4';
  if (normalized.includes('LENS')) return normalized;
  return normalized;
}

function toDeviceStatus(status: string): DeviceInstance['status'] {
  switch (status) {
    case 'AVAILABLE':
      return 'IDLE';
    case 'RENTED':
    case 'DISPATCHED':
      return 'RENTING';
    case 'MAINTENANCE':
    case 'REPAIR':
      return 'REPAIR';
    case 'LOCKED':
    case 'DISABLED':
      return 'LOCKED';
    default:
      return 'IDLE';
  }
}

function unitCode(deviceNo: string) {
  const matched = deviceNo.match(/^([A-Z0-9]+)-(\d{1,4})-/i);
  if (matched) return `${matched[2].padStart(2, '0')}号`;
  return deviceNo;
}

export function mapDevices(devices: RentalDeviceVO[]): DeviceInstance[] {
  return devices.map((device) => ({
    id: String(device.id),
    unitCode: unitCode(device.deviceNo),
    sn: device.serialNumber || device.deviceNo,
    modelId: device.equipmentModelCode.toLowerCase(),
    modelName: modelName(device.equipmentModelCode),
    status: device.enabled ? toDeviceStatus(device.status) : 'LOCKED',
    expectedAvailableDate: device.status === 'AVAILABLE' ? '立即可用' : undefined,
    note: device.warehouseCode ? `仓库: ${device.warehouseCode}` : undefined,
    qrCode: device.deviceNo,
  }));
}

export function deriveCategories(): ModelCategory[] {
  return [{ id: CATEGORY_ID, name: '租赁设备' }];
}

export function deriveModels(devices: RentalDeviceVO[]): EquipmentModel[] {
  const groups = new Map<string, number>();
  devices.forEach((device) => {
    groups.set(device.equipmentModelCode, (groups.get(device.equipmentModelCode) || 0) + 1);
  });
  return Array.from(groups.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([code, totalUnits]) => ({
      id: code.toLowerCase(),
      name: modelName(code),
      categoryId: CATEGORY_ID,
      totalUnits,
    }));
}

export function mapSchedules(schedules: RentalScheduleVO[]): ScheduleBlock[] {
  return schedules.map((schedule) => ({
    id: String(schedule.id),
    deviceId: String(schedule.deviceId),
    orderId: schedule.rentalOrderId ? String(schedule.rentalOrderId) : undefined,
    orderNumber: schedule.rentalOrderId ? `RO-${schedule.rentalOrderId}` : undefined,
    type: schedule.scheduleType === 'REPAIR' ? 'REPAIR' : 'RENTAL',
    startDate: schedule.occupyStartDate,
    endDate: schedule.occupyEndDateExclusive,
    statusText: schedule.status,
  }));
}

function orderStatus(order: XianyuOrderVO, pendingShipIds: Set<number>): RentalOrder['status'] {
  if (order.cancelTime || order.orderStatus === 'CLOSED') return 'COMPLETED';
  if (order.remarkParseStatus === 'FAILED' || order.conversionStatus === 'REVIEW_REQUIRED') {
    return 'EXCEPTION';
  }
  if (pendingShipIds.has(order.id)) return 'PENDING_DISPATCH';
  if (order.consignTime) return 'RENTING';
  if (order.conversionStatus === 'CONVERTED' || order.rentalOrderId) return 'ASSIGNED';
  return 'UNASSIGNED';
}

export function mapChannelOrders(
  orders: XianyuOrderVO[],
  pendingShipOrders: XianyuPendingShipOrderVO[]
): RentalOrder[] {
  const pendingShipIds = new Set(pendingShipOrders.map((order) => order.id));
  return orders.map((order) => ({
    id: String(order.id),
    rentalOrderId: order.rentalOrderId,
    shopId: order.shopId,
    orderNumber: order.externalOrderId,
    orderStatus: order.orderStatus,
    conversionStatus: order.conversionStatus,
    channel: 'XIANYU',
    customerName: order.receiverName || order.sellerName || '闲鱼买家',
    customerPhone: '',
    startDate: order.orderTime?.slice(0, 10) || order.sourceCreatedAt?.slice(0, 10) || new Date().toISOString().slice(0, 10),
    endDate: order.orderTime?.slice(0, 10) || order.sourceUpdatedAt?.slice(0, 10) || new Date().toISOString().slice(0, 10),
    status: orderStatus(order, pendingShipIds),
    items: [
      {
        modelId: 'p4',
        modelName: order.goodsTitle || '待确认商品',
        quantity: order.goodsQuantity || 1,
        assignedDeviceIds: [],
      },
    ],
    totalPrice: order.payAmount ? Math.round(order.payAmount / 100) : 0,
    deposit: 0,
    createdTime: order.orderTime || order.sourceCreatedAt || order.sourceUpdatedAt || '',
    logisticsNumber: order.expressName && order.expressCode ? `${order.expressName}: ${order.expressCode}` : undefined,
    expressCode: order.expressCode,
    expressName: order.expressName,
    note: `订单状态: ${order.orderStatus} / 转换状态: ${order.conversionStatus}`,
  }));
}

export function mapPendingShipOrders(orders: XianyuPendingShipOrderVO[]): RentalOrder[] {
  return orders.map((order) => ({
    id: String(order.id),
    orderNumber: order.externalOrderId,
    channel: 'XIANYU',
    customerName: order.buyerNick || '闲鱼买家',
    customerPhone: '',
    startDate: order.orderTime?.slice(0, 10) || new Date().toISOString().slice(0, 10),
    endDate: order.orderTime?.slice(0, 10) || new Date().toISOString().slice(0, 10),
    status: 'UNASSIGNED',
    items: [
      {
        modelId: 'p4',
        modelName: order.goodsTitle || '待确认商品',
        quantity: order.goodsQuantity || 1,
        assignedDeviceIds: [],
      },
    ],
    totalPrice: order.payAmount ? Math.round(order.payAmount / 100) : 0,
    deposit: 0,
    createdTime: order.orderTime || order.sourceUpdatedAt || '',
    note: `转换状态: ${order.conversionStatus}`,
  }));
}

export function mapReviews(reviews: RentalManualReviewVO[]): ExceptionItem[] {
  return reviews.map((review) => ({
    id: String(review.id),
    type: 'UNASSIGNED_ALERT',
    title: `${review.reviewType} / ${review.reasonCode || '待复核'}`,
    description: review.reasonMessage || `${review.sourceType}: ${review.sourceIdentifier}`,
    relatedOrderId: undefined,
    relatedDeviceId: undefined,
    severity: review.status === 'PENDING' ? 'high' : 'medium',
    createdTime: review.resolvedAt || '',
    resolved: review.status !== 'PENDING',
  }));
}
