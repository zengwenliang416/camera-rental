import type {
  DeviceInstance,
  EquipmentModel,
  ExceptionItem,
  ModelCategory,
  RentalOrder,
  ScheduleBlock,
} from '../types';
import type {
  RentalDeviceVO,
  RentalDateValue,
  RentalManualReviewVO,
  RentalScheduleVO,
  XianyuOrderVO,
  XianyuPendingShipOrderVO,
} from './rental';

const CATEGORY_ID = 'cat-rental-equipment';

function modelName(modelCode: string) {
  const normalized = modelCode.toUpperCase();
  if (normalized === 'P3') return '大疆 Pocket 3';
  if (normalized === 'P4') return '大疆 Pocket 4';
  if (normalized === 'P4P' || normalized === 'POCKET4') return '大疆 Pocket 4 Pro';
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

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function normalizeTimestamp(value: number) {
  return value < 10_000_000_000 ? value * 1000 : value;
}

function dateFromString(value: string) {
  const text = value.trim();
  if (!text) return undefined;

  const normalizedMatch = text.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
  if (normalizedMatch) {
    return `${normalizedMatch[1]}-${padDatePart(Number(normalizedMatch[2]))}-${padDatePart(Number(normalizedMatch[3]))}`;
  }

  const parsed = new Date(text.replace(' ', 'T'));
  if (Number.isNaN(parsed.getTime())) return undefined;
  return dateFromDate(parsed);
}

function dateFromDate(date: Date) {
  if (Number.isNaN(date.getTime())) return undefined;
  const yyyy = date.getFullYear();
  const mm = padDatePart(date.getMonth() + 1);
  const dd = padDatePart(date.getDate());
  return `${yyyy}-${mm}-${dd}`;
}

function dateTimeFromDate(date: Date) {
  if (Number.isNaN(date.getTime())) return undefined;
  const yyyy = date.getFullYear();
  const mm = padDatePart(date.getMonth() + 1);
  const dd = padDatePart(date.getDate());
  const hh = padDatePart(date.getHours());
  const mi = padDatePart(date.getMinutes());
  const ss = padDatePart(date.getSeconds());
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
}

function dateFromArray(value: number[]) {
  if (value.length < 3) return undefined;
  const [year, month, day] = value;
  if (!year || !month || !day) return undefined;
  return `${year}-${padDatePart(month)}-${padDatePart(day)}`;
}

function dateTimeFromArray(value: number[]) {
  if (value.length < 3) return undefined;
  const [year, month, day, hour = 0, minute = 0, second = 0] = value;
  if (!year || !month || !day) return undefined;
  return `${year}-${padDatePart(month)}-${padDatePart(day)} ${padDatePart(hour)}:${padDatePart(minute)}:${padDatePart(second)}`;
}

function dateFromObject(value: Exclude<RentalDateValue, string | number | number[]>) {
  const year = value.year;
  const month = value.monthValue ?? value.month;
  const day = value.dayOfMonth ?? value.day;
  if (!year || !month || !day) return undefined;
  return `${year}-${padDatePart(month)}-${padDatePart(day)}`;
}

function dateTimeFromObject(value: Exclude<RentalDateValue, string | number | number[]>) {
  const date = dateFromObject(value);
  if (!date) return undefined;
  const hour = value.hour ?? 0;
  const minute = value.minute ?? 0;
  const second = value.second ?? 0;
  return `${date} ${padDatePart(hour)}:${padDatePart(minute)}:${padDatePart(second)}`;
}

function formatDateValue(value?: RentalDateValue) {
  if (value == null) return undefined;
  if (typeof value === 'string') return dateFromString(value);
  if (typeof value === 'number') return dateFromDate(new Date(normalizeTimestamp(value)));
  if (Array.isArray(value)) return dateFromArray(value);
  return dateFromObject(value);
}

function formatDateTimeValue(value?: RentalDateValue) {
  if (value == null) return undefined;
  if (typeof value === 'string') return dateTimeFromDate(new Date(value.replace(' ', 'T'))) || value;
  if (typeof value === 'number') return dateTimeFromDate(new Date(normalizeTimestamp(value)));
  if (Array.isArray(value)) return dateTimeFromArray(value);
  return dateTimeFromObject(value);
}

function firstDateTime(...values: (RentalDateValue | undefined)[]) {
  for (const value of values) {
    const dateTime = formatDateTimeValue(value);
    if (dateTime) return dateTime;
  }
  return '';
}

function inclusiveDateFromExclusive(value?: RentalDateValue) {
  const exclusiveDate = formatDateValue(value);
  if (!exclusiveDate) return undefined;
  const [year, month, day] = exclusiveDate.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  date.setUTCDate(date.getUTCDate() - 1);
  return `${date.getUTCFullYear()}-${padDatePart(date.getUTCMonth() + 1)}-${padDatePart(date.getUTCDate())}`;
}

function rentalPeriodFromOrder(order: XianyuOrderVO) {
  const startDate = formatDateValue(order.billableStartDate);
  const endDate = formatDateValue(order.billableEndDate);
  if (startDate && endDate) {
    return {
      startDate,
      endDate,
      rentalPeriodLabel: `${startDate} 至 ${endDate}`,
      rentalPeriodReady: true,
    };
  }

  const reason = order.rentalPeriodReasonCode || order.remarkParseStatus || 'RENTAL_PERIOD_NOT_FOUND';
  return {
    startDate: '',
    endDate: '',
    rentalPeriodLabel: `租期待复核 (${reason})`,
    rentalPeriodReady: false,
  };
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
  return schedules.map((schedule) => {
    const startDate = formatDateValue(schedule.occupyStartDate) || '';
    return {
      id: String(schedule.id),
      deviceId: String(schedule.deviceId),
      orderId: schedule.rentalOrderId ? String(schedule.rentalOrderId) : undefined,
      orderNumber: schedule.rentalOrderId ? `RO-${schedule.rentalOrderId}` : undefined,
      type: schedule.scheduleType === 'REPAIR' ? 'REPAIR' : 'RENTAL',
      startDate,
      endDate: inclusiveDateFromExclusive(schedule.occupyEndDateExclusive) || startDate,
      statusText: schedule.status,
    };
  });
}

function orderStatus(order: XianyuOrderVO, pendingShipIds: Set<number>): RentalOrder['status'] {
  const channelStatus = String(order.orderStatus || '').toUpperCase();
  if (
    order.cancelTime ||
    ['22', '23', '24', 'CLOSED', 'COMPLETED', 'REFUNDED'].includes(channelStatus)
  ) {
    return 'COMPLETED';
  }
  if (order.consignTime || ['21', 'SHIPPED', 'CONSIGNED'].includes(channelStatus)) {
    return 'RENTING';
  }
  if (channelStatus === '12' || pendingShipIds.has(order.id)) return 'PENDING_DISPATCH';
  if (
    order.remarkParseStatus === 'FAILED' ||
    order.rentalPeriodStatus === 'FAILED' ||
    order.conversionStatus === 'REVIEW_REQUIRED'
  ) {
    return 'EXCEPTION';
  }
  if (order.conversionStatus === 'CONVERTED' || order.rentalOrderId) return 'ASSIGNED';
  return 'UNASSIGNED';
}

function isRentalRelevant(order: XianyuOrderVO) {
  const channelStatus = String(order.orderStatus || '').toUpperCase();
  const activeChannelOrder = ['12', '21', 'PENDING_SHIPMENT', 'SHIPPED', 'CONSIGNED']
    .includes(channelStatus);
  const likelyRentalTitle = /(租赁|出租|免押|租机|租借)/.test(order.goodsTitle || '');
  return Boolean(
    order.rentalOrderId ||
    order.rentalPeriodStatus === 'SUCCESS' ||
    order.sellerRemark?.trim() ||
    (activeChannelOrder && likelyRentalTitle)
  );
}

function maskCustomerName(name?: string) {
  const value = name?.trim();
  if (!value) return '';
  return value.length <= 1 ? '*' : `${value.slice(0, 1)}*`;
}

function maskCustomerPhone(phone?: string) {
  const value = phone?.trim();
  if (!value) return '';
  return value.replace(/(\d{3})\d+(\d{4})$/, '$1****$2');
}

export function mapChannelOrders(
  orders: XianyuOrderVO[],
  pendingShipOrders: XianyuPendingShipOrderVO[]
): RentalOrder[] {
  const pendingShipIds = new Set(pendingShipOrders.map((order) => order.id));
  return orders
    .filter((order) => !order.externalOrderId.startsWith('SPECNAV-XGJ-'))
    .filter(isRentalRelevant)
    .map((order) => {
      const rentalPeriod = rentalPeriodFromOrder(order);
      const status = orderStatus(order, pendingShipIds);
      const modelCode = order.equipmentModelCode?.trim();
      const assignedDeviceIds = (order.assignedDeviceIds || []).map(String);
      const occupyStartDate = formatDateValue(order.occupyStartDate) || '';
      const occupyEndDateExclusive = formatDateValue(order.occupyEndDateExclusive) || '';
      const hasRentalItem = Boolean(order.rentalOrderId && order.rentalOrderItemId && modelCode);
      const canAssign = Boolean(
        hasRentalItem &&
        rentalPeriod.rentalPeriodReady &&
        occupyStartDate &&
        occupyEndDateExclusive &&
        status === 'PENDING_DISPATCH'
      );
      const canShip = Boolean(
        rentalPeriod.rentalPeriodReady &&
        occupyStartDate &&
        occupyEndDateExclusive &&
        status === 'PENDING_DISPATCH' &&
        !order.consignTime &&
        !order.waybillNo
      );
      const canReturn = Boolean(
        status === 'RENTING' &&
        assignedDeviceIds.length > 0
      );
      return {
        id: String(order.id),
        rentalOrderId: order.rentalOrderId,
        shopId: order.shopId,
        orderNumber: order.externalOrderId,
        orderStatus: order.orderStatus,
        conversionStatus: order.conversionStatus,
        channel: 'XIANYU',
        customerName: maskCustomerName(order.receiverName || order.sellerName) || '闲鱼买家',
        customerPhone: maskCustomerPhone(order.receiverMobile),
        receiverName: order.receiverName?.trim() || order.sellerName?.trim(),
        receiverPhone: order.receiverMobile?.trim(),
        receiverAddress: order.receiverAddress?.trim(),
        startDate: rentalPeriod.startDate,
        endDate: rentalPeriod.endDate,
        occupyStartDate,
        occupyEndDateExclusive,
        rentalPeriodLabel: rentalPeriod.rentalPeriodLabel,
        rentalPeriodReady: rentalPeriod.rentalPeriodReady,
        rentalPeriodStatus: order.rentalPeriodStatus,
        rentalPeriodReasonCode: order.rentalPeriodReasonCode,
        status,
        items: [
          {
            rentalOrderItemId: order.rentalOrderItemId,
            modelId: modelCode ? modelCode.toLowerCase() : '',
            modelName: order.goodsTitle || '待确认商品',
            quantity: order.rentalQuantity || 1,
            assignedDeviceIds,
          },
        ],
        totalPrice: order.payAmount ? Math.round(order.payAmount / 100) : 0,
        deposit: 0,
        createdTime: firstDateTime(order.orderTime, order.sourceCreatedAt, order.sourceUpdatedAt),
        consignTime: formatDateTimeValue(order.consignTime),
        logisticsNumber: order.waybillNo,
        expressCode: order.expressCode,
        expressName: order.expressName,
        canAssign,
        canShip,
        canReturn,
        note: `订单状态: ${order.orderStatus} / 转换状态: ${order.conversionStatus}`,
      };
    });
}

export function mapPendingShipOrders(orders: XianyuPendingShipOrderVO[]): RentalOrder[] {
  return orders.map((order) => ({
    id: String(order.id),
    orderNumber: order.externalOrderId,
    channel: 'XIANYU',
    customerName: maskCustomerName(order.buyerNick) || '闲鱼买家',
    customerPhone: '',
    startDate: '',
    endDate: '',
    occupyStartDate: '',
    occupyEndDateExclusive: '',
    rentalPeriodLabel: '租期待复核',
    rentalPeriodReady: false,
    rentalPeriodStatus: 'FAILED',
    rentalPeriodReasonCode: 'RENTAL_PERIOD_NOT_FOUND',
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
    createdTime: firstDateTime(order.orderTime, order.sourceUpdatedAt),
    canAssign: false,
    canShip: false,
    canReturn: false,
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
    createdTime: firstDateTime(review.resolvedAt),
    resolved: review.status !== 'PENDING',
  }));
}
