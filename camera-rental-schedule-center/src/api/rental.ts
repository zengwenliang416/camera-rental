import { apiClient, PageResult } from './client';
import { AdminUserCache, setCachedPermissionInfo } from './auth';

export interface PermissionInfoVO extends AdminUserCache {
  menus?: unknown[];
}

export interface RentalDeviceVO {
  id: number;
  deviceNo: string;
  serialNumber?: string;
  equipmentModelCode: string;
  status: string;
  warehouseCode?: string;
  purchaseAmount?: number;
  enabled: boolean;
}

export interface RentalScheduleVO {
  id: number;
  deviceId: number;
  deviceNo?: string;
  equipmentModelCode?: string;
  rentalOrderId?: number;
  rentalOrderItemId?: number;
  scheduleType: string;
  status: string;
  billableStartDate?: string;
  billableEndDate?: string;
  occupyStartDate: string;
  occupyEndDateExclusive: string;
}

export interface XianyuPendingShipOrderVO {
  id: number;
  shopId: number;
  externalOrderId: string;
  orderStatus: string;
  goodsTitle?: string;
  goodsQuantity?: number;
  payAmount?: number;
  buyerNick?: string;
  rentalOrderId?: number;
  conversionStatus: string;
  orderTime?: string;
  sourceUpdatedAt?: string;
}

export interface XianyuOrderVO {
  id: number;
  shopId: number;
  externalOrderId: string;
  externalProductId?: string;
  externalSkuId?: string;
  orderStatus: string;
  payAmount: number;
  currency: string;
  sellerRemark?: string;
  receiverName?: string;
  receiverMobile?: string;
  receiverAddress?: string;
  remarkParseStatus?: string;
  conversionStatus: string;
  rentalOrderId?: number;
  sourceCreatedAt?: string;
  sourceUpdatedAt?: string;
  orderType?: number;
  orderTime?: string;
  totalAmount?: number;
  payTime?: string;
  refundStatus?: number;
  refundAmount?: number;
  refundTime?: string;
  expressCode?: string;
  expressName?: string;
  consignType?: number;
  consignTime?: string;
  confirmTime?: string;
  cancelReason?: string;
  cancelTime?: string;
  sellerName?: string;
  goodsTitle?: string;
  goodsQuantity?: number;
  goodsPrice?: number;
}

export interface RentalManualReviewVO {
  id: number;
  reviewType: string;
  sourceType: string;
  sourceIdentifier: string;
  status: string;
  reasonCode?: string;
  reasonMessage?: string;
  resolvedAt?: string;
}

export interface XianyuShipmentOcrRespVO {
  waybillNo?: string;
  expressCode?: string;
  expressName?: string;
  confidence?: number;
  extractionSource?: string;
}

export interface XianyuOrderShipReqVO {
  channelOrderId: number;
  deviceId?: number;
  deviceNo?: string;
  idempotencyKey: string;
  expressCode: string;
  expressName: string;
  waybillNo: string;
  source: 'ADMIN' | 'STAFF';
  ocrConfirmed?: boolean;
}

export interface RentalDeviceAssignmentResultVO {
  assignmentId: number;
  scheduleId: number;
  deviceId: number;
  occupyStartDate: string;
  occupyEndDateExclusive: string;
}

export interface RentalDeviceOpsResultVO {
  deviceId: number;
  deviceNo: string;
  deviceStatus: string;
  assignmentId: number;
  assignmentStatus: string;
}

// 后端 PageParam 限制 pageSize 最大为 200；这里循环分页拉完整列表。
const PAGE_SIZE = 200;

async function fetchAllPages<T>(
  path: string,
  params: Record<string, unknown> = {},
  pageSize = PAGE_SIZE
) {
  const list: T[] = [];
  let pageNo = 1;
  let total = 0;

  while (true) {
    const page = await apiClient.get<PageResult<T>>(path, {
      ...params,
      pageNo,
      pageSize,
    });
    const pageList = page.list || [];
    if (pageNo === 1) {
      total = page.total || 0;
    }
    list.push(...pageList);

    if (pageList.length === 0) break;
    if (total > 0 && list.length >= total) break;
    if (pageList.length < pageSize) break;
    pageNo += 1;
  }

  return {
    list,
    total: Math.max(total, list.length),
  };
}

export async function fetchPermissionInfo() {
  const info = await apiClient.get<PermissionInfoVO>('/system/auth/get-permission-info');
  setCachedPermissionInfo(info);
  return info;
}

export async function fetchScheduleCenterSnapshot() {
  const [devicePage, schedulePage, orderPage, pendingShipPage, reviewPage] = await Promise.all([
    fetchAllPages<RentalDeviceVO>('/rental/device/page'),
    fetchAllPages<RentalScheduleVO>('/rental/schedule/page', {
      status: 'EFFECTIVE',
    }),
    fetchAllPages<XianyuOrderVO>('/rental/xianyu/order/page'),
    fetchAllPages<XianyuPendingShipOrderVO>('/rental/xianyu/order/pending-ship/page'),
    fetchAllPages<RentalManualReviewVO>('/rental/manual-review/page', {
      status: 'PENDING',
    }),
  ]);

  return {
    devices: devicePage.list || [],
    schedules: schedulePage.list || [],
    channelOrders: orderPage.list || [],
    pendingShipOrders: pendingShipPage.list || [],
    reviews: reviewPage.list || [],
    totals: {
      devices: devicePage.total || 0,
      schedules: schedulePage.total || 0,
      channelOrders: orderPage.total || 0,
      pendingShipOrders: pendingShipPage.total || 0,
      reviews: reviewPage.total || 0,
    },
  };
}

export function resolveRentalDeviceQr(payload: string) {
  return apiClient.post<RentalDeviceVO>('/rental/device/resolve-qr', { payload });
}

export function recognizeXianyuShipmentImage(file: File) {
  return apiClient.upload<XianyuShipmentOcrRespVO>('/rental/xianyu/order/ship/ocr', file);
}

export function shipXianyuOrder(data: XianyuOrderShipReqVO) {
  return apiClient.post('/rental/xianyu/order/ship', data);
}

export function assignRentalDevice(data: {
  rentalOrderItemId: number;
  deviceId: number;
  occupyStartDate: string;
  occupyEndDateExclusive: string;
  idempotencyKey: string;
}) {
  return apiClient.post<RentalDeviceAssignmentResultVO>('/rental/device/assign', data);
}

export function dispatchRentalDevice(data: { deviceId: number; assignmentId?: number }) {
  return apiClient.post<RentalDeviceOpsResultVO>('/rental/device/dispatch', data);
}

export function returnRentalDevice(data: {
  deviceId: number;
  inspectPassed?: boolean;
  note?: string;
}) {
  return apiClient.post<RentalDeviceOpsResultVO>('/rental/device/return', data);
}

export function resolveManualReview(data: { id: number; resolutionNote: string }) {
  return apiClient.put('/rental/manual-review/resolve', data);
}

export function closeManualReview(data: { id: number; resolutionNote: string }) {
  return apiClient.put('/rental/manual-review/close', data);
}
