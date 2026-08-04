import { apiClient, PageResult } from './client';
import { AdminUserCache, setCachedPermissionInfo } from './auth';
import { fetchAllPages } from './pagination';
import { loadAuthorizedSnapshot, type SnapshotLoaders } from './snapshotLoader';

export interface PermissionInfoVO extends AdminUserCache {
  menus?: unknown[];
}

export interface XianyuConfigVO {
  enabled: boolean;
  status: 'DISABLED' | 'MISSING_CREDENTIALS' | 'READY' | string;
  baseUrl?: string;
  appKeyMasked?: string;
  appSecretConfigured: boolean;
  webhookBaseUrlConfigured: boolean;
  writeEnabled: boolean;
}

export interface XianyuExpressCompanyVO {
  code: string;
  expressName: string;
  expressAlias?: string;
  hot?: boolean;
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

export interface RentalDeviceQrVO {
  deviceId: number;
  deviceNo: string;
  equipmentModelCode: string;
  payload: string;
  payloadVersion: string;
  signed: boolean;
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
  billableStartDate?: RentalDateValue;
  billableEndDate?: RentalDateValue;
  occupyStartDate: RentalDateValue;
  occupyEndDateExclusive: RentalDateValue;
}

export type RentalDateValue =
  | string
  | number
  | number[]
  | {
      year?: number;
      month?: number;
      monthValue?: number;
      day?: number;
      dayOfMonth?: number;
      hour?: number;
      minute?: number;
      second?: number;
      nano?: number;
    };

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
  orderTime?: RentalDateValue;
  sourceUpdatedAt?: RentalDateValue;
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
  billableStartDate?: RentalDateValue;
  billableEndDate?: RentalDateValue;
  rentalPeriodStatus?: string;
  rentalPeriodReasonCode?: string;
  conversionStatus: string;
  rentalOrderId?: number;
  sourceCreatedAt?: RentalDateValue;
  sourceUpdatedAt?: RentalDateValue;
  orderType?: number;
  orderTime?: RentalDateValue;
  totalAmount?: number;
  payTime?: RentalDateValue;
  refundStatus?: number;
  refundAmount?: number;
  refundTime?: RentalDateValue;
  expressCode?: string;
  expressName?: string;
  waybillNo?: string;
  consignType?: number;
  consignTime?: RentalDateValue;
  confirmTime?: RentalDateValue;
  cancelReason?: string;
  cancelTime?: RentalDateValue;
  sellerName?: string;
  goodsTitle?: string;
  goodsQuantity?: number;
  goodsPrice?: number;
  rentalOrderItemId?: number;
  equipmentModelCode?: string;
  rentalQuantity?: number;
  occupyStartDate?: RentalDateValue;
  occupyEndDateExclusive?: RentalDateValue;
  assignedDeviceIds?: number[];
}

export interface RentalManualReviewVO {
  id: number;
  reviewType: string;
  sourceType: string;
  sourceIdentifier: string;
  status: string;
  reasonCode?: string;
  reasonMessage?: string;
  resolvedAt?: RentalDateValue;
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

export interface RentalLogisticsRiskVO {
  code: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW' | string;
  safeMessage: string;
  nextAction?: string;
  deviceIds?: number[];
}

export interface RentalDeliveryTrackingPackageSummaryVO {
  deliveryId: number;
  direction: 'OUTBOUND' | 'RETURN' | 'EXCHANGE_OUT' | 'EXCHANGE_RETURN';
  packageSeq: number;
  carrierName?: string;
  maskedWaybillNo?: string | null;
  trackingStatus: string;
  mappingStatus: string;
  subscribeStatus: string;
  queryStatus: string;
  latestTraceText?: string;
  latestEventTime?: string;
  lastSyncedAt?: string;
  estimatedDeliveryAt?: string;
  stale: boolean;
  risk?: RentalLogisticsRiskVO;
}

export interface RentalDeliveryTrackingOrderSummaryRespVO {
  orderId: number;
  packageCount: number;
  statusCounts: Record<string, number>;
  packages: RentalDeliveryTrackingPackageSummaryVO[];
  risks: RentalLogisticsRiskVO[];
}

export interface RentalDeliveryTrackingDeviceRespVO {
  deviceId?: number;
  deviceNo: string;
  equipmentModelCode?: string;
}

export interface RentalDeliveryTrackingTraceRespVO {
  eventSeq?: number;
  businessTime?: string;
  trackingStatus: string;
  traceText?: string;
  location?: string;
}

export interface RentalDeliveryTrackingDetailRespVO {
  deliveryId: number;
  rentalOrderId: number;
  direction: 'OUTBOUND' | 'RETURN' | 'EXCHANGE_OUT' | 'EXCHANGE_RETURN';
  packageSeq: number;
  carrierName?: string;
  maskedWaybillNo?: string | null;
  trackingStatus: string;
  mappingStatus: string;
  subscribeStatus: string;
  queryStatus: string;
  latestTraceText?: string;
  latestEventTime?: string;
  lastSyncedAt?: string;
  estimatedDeliveryAt?: string;
  stale: boolean;
  risks: RentalLogisticsRiskVO[];
  devices: RentalDeliveryTrackingDeviceRespVO[];
  traces: RentalDeliveryTrackingTraceRespVO[];
}

export interface RentalDeliveryRefreshRespVO {
  accepted: boolean;
  reason: string;
  nextAllowedAt?: string;
}

export type RentalLogisticsSecretAction = 'KEEP' | 'REPLACE' | 'CLEAR';

export interface RentalLogisticsProviderCredentialVO {
  id: number;
  providerCode: string;
  credentialName: string;
  enabled: boolean;
  sortOrder: number;
  customerCodeConfigured: boolean;
  maskedCustomerCode?: string | null;
  apiKeyConfigured: boolean;
  maskedApiKey?: string | null;
  configStatus: string;
  lastVerifiedAt?: string | null;
}

export interface RentalLogisticsProviderConfigVO {
  providerCode: string;
  enabled: boolean;
  queryEnabled: boolean;
  subscribeEnabled: boolean;
  callbackSecretConfigured: boolean;
  maskedCallbackSecret?: string | null;
  callbackBaseUrl?: string | null;
  minimumQueryIntervalSeconds: number;
  resultVersion: string;
  configStatus: string;
  lastVerifiedAt?: string | null;
  credentials: RentalLogisticsProviderCredentialVO[];
}

export interface RentalLogisticsProviderConfigUpdateReqVO {
  providerCode: string;
  enabled: boolean;
  queryEnabled: boolean;
  subscribeEnabled: boolean;
  callbackSecretAction: RentalLogisticsSecretAction;
  callbackSecret?: string;
  callbackBaseUrl?: string | null;
  minimumQueryIntervalSeconds: number;
  resultVersion: string;
}

export interface RentalLogisticsProviderCredentialSaveReqVO {
  id?: number;
  providerCode: string;
  credentialName: string;
  enabled: boolean;
  sortOrder: number;
  customerCodeAction: RentalLogisticsSecretAction;
  customerCode?: string;
  apiKeyAction: RentalLogisticsSecretAction;
  apiKey?: string;
}

export interface RentalLogisticsProviderVerifyResultVO {
  valid: boolean;
  reason: string;
  verifiedAt?: string | null;
}

export interface RentalLogisticsCarrierMappingVO {
  id: number;
  sourceType: string;
  sourceCarrierCode: string;
  canonicalCarrierCode: string;
  displayName: string;
  providerCode: string;
  providerCarrierCode: string;
  phoneRequirement: string;
  status: string;
}

export interface RentalLogisticsCarrierMappingSaveReqVO {
  id?: number;
  sourceType: string;
  sourceCarrierCode: string;
  canonicalCarrierCode: string;
  displayName: string;
  providerCode: string;
  providerCarrierCode: string;
  phoneRequirement: string;
  status: string;
}

export interface RentalLogisticsFailedTaskVO {
  taskType: 'INBOX' | 'OUTBOX' | string;
  id: number;
  deliveryId?: number | null;
  providerCode?: string | null;
  eventType?: string | null;
  processingStatus: string;
  retryCount: number;
  nextAttemptAt?: string | null;
  errorCode?: string | null;
  safeErrorMessage?: string | null;
  occurredAt?: string | null;
}

export interface RentalLogisticsRetryResultVO {
  accepted: boolean;
  reason: string;
  processingStatus: string;
}

export interface RentalLogisticsReconcileResultVO {
  requestedLimit: number;
  enqueuedCount: number;
  deliveryIds: number[];
}

export interface RentalLogisticsMetricsVO {
  deliveryCount: number;
  deliveryStatusCounts: Record<string, number>;
  outboxStatusCounts: Record<string, number>;
  inboxStatusCounts: Record<string, number>;
  staleDeliveryCount: number;
  failedOutboxCount: number;
  failedInboxCount: number;
  retriedOutboxCount: number;
  retriedInboxCount: number;
  averageOutboxDelaySeconds: number;
  lastOutboxSuccessAt?: string | null;
  lastInboxSuccessAt?: string | null;
}

export interface RentalLogisticsBackfillItemVO {
  shipmentId: number;
  deliveryId?: number | null;
  maskedWaybillNo?: string | null;
  status: string;
  reason: string;
}

export interface RentalLogisticsBackfillResultVO {
  dryRun: boolean;
  requestedLimit: number;
  candidateCount: number;
  createdOrReusedCount: number;
  skippedCount: number;
  providerTasksEnqueued: boolean;
  providerTaskReason: string;
  items: RentalLogisticsBackfillItemVO[];
}

export interface RentalLogisticsCleanupResultVO {
  dryRun: boolean;
  retentionDays: number;
  limit: number;
  traceCount: number;
  inboxCount: number;
  outboxCount: number;
}

export interface SnapshotAccess {
  devices: boolean;
  schedules: boolean;
  orders: boolean;
  pendingShipOrders: boolean;
  reviews: boolean;
  xianyuConfig: boolean;
}

// 后端 PageParam 限制 pageSize 最大为 200；这里循环分页拉完整列表。
const PAGE_SIZE = 200;

async function fetchAllApiPages<T>(
  path: string,
  params: Record<string, unknown> = {},
  pageSize = PAGE_SIZE
) {
  return fetchAllPages<T>(
    (pageNo, currentPageSize) => apiClient.get<PageResult<T>>(path, {
      ...params,
      pageNo,
      pageSize: currentPageSize,
    }),
    pageSize
  );
}

export async function fetchPermissionInfo() {
  const info = await apiClient.get<PermissionInfoVO>('/system/auth/get-permission-info');
  setCachedPermissionInfo(info);
  return info;
}

export function fetchXianyuConfig() {
  return apiClient.get<XianyuConfigVO>('/rental/xianyu/config/get');
}

export function fetchXianyuExpressCompanies() {
  return apiClient.get<XianyuExpressCompanyVO[]>('/rental/xianyu/express-company/list');
}

const snapshotLoaders: SnapshotLoaders = {
  devices: () => fetchAllApiPages<RentalDeviceVO>('/rental/device/page'),
  schedules: () => fetchAllApiPages<RentalScheduleVO>('/rental/schedule/page', { status: 'EFFECTIVE' }),
  orders: () => fetchAllApiPages<XianyuOrderVO>('/rental/xianyu/order/page'),
  pendingShipOrders: () =>
    fetchAllApiPages<XianyuPendingShipOrderVO>('/rental/xianyu/order/pending-ship/page'),
  reviews: () => fetchAllApiPages<RentalManualReviewVO>('/rental/manual-review/page', { status: 'PENDING' }),
};

export function fetchScheduleCenterSnapshot(access: SnapshotAccess) {
  return loadAuthorizedSnapshot(access, snapshotLoaders);
}

export function resolveRentalDeviceQr(payload: string) {
  return apiClient.post<RentalDeviceVO>('/rental/device/resolve-qr', { payload });
}

export function fetchRentalDeviceQr(id: number) {
  return apiClient.get<RentalDeviceQrVO>('/rental/device/get-qr', { id });
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

export function fetchDeliveryTrackingSummaries(orderIds: number[]) {
  return apiClient.post<Record<string, RentalDeliveryTrackingOrderSummaryRespVO>>(
    '/rental/delivery/tracking-summary/batch',
    { orderIds }
  );
}

export function fetchDeliveryTrackingDetail(deliveryId: number) {
  return apiClient.get<RentalDeliveryTrackingDetailRespVO | null>(
    `/rental/delivery/${deliveryId}/tracking`
  );
}

export function requestDeliveryTrackingRefresh(deliveryId: number) {
  return apiClient.post<RentalDeliveryRefreshRespVO>(
    `/rental/delivery/${deliveryId}/refresh`
  );
}

const LOGISTICS_OPERATIONS_PATH = '/rental/logistics/operations';

export function fetchRentalLogisticsProviderConfig(providerCode = 'KUAIDI100') {
  return apiClient.get<RentalLogisticsProviderConfigVO>(
    `${LOGISTICS_OPERATIONS_PATH}/provider-config/${providerCode}`
  );
}

export function saveRentalLogisticsProviderConfig(
  data: RentalLogisticsProviderConfigUpdateReqVO
) {
  return apiClient.put<RentalLogisticsProviderConfigVO>(
    `${LOGISTICS_OPERATIONS_PATH}/provider-config`,
    data
  );
}

export function verifyRentalLogisticsProviderConfig(providerCode = 'KUAIDI100') {
  return apiClient.post<RentalLogisticsProviderVerifyResultVO>(
    `${LOGISTICS_OPERATIONS_PATH}/provider-config/${providerCode}/verify`
  );
}

export function saveRentalLogisticsProviderCredential(
  data: RentalLogisticsProviderCredentialSaveReqVO
) {
  return apiClient.put<RentalLogisticsProviderCredentialVO>(
    `${LOGISTICS_OPERATIONS_PATH}/provider-credential`,
    data
  );
}

export function deleteRentalLogisticsProviderCredential(id: number) {
  return apiClient.delete<boolean>(
    `${LOGISTICS_OPERATIONS_PATH}/provider-credential/${id}`
  );
}

export function verifyRentalLogisticsProviderCredential(id: number) {
  return apiClient.post<RentalLogisticsProviderVerifyResultVO>(
    `${LOGISTICS_OPERATIONS_PATH}/provider-credential/${id}/verify`
  );
}

export function fetchRentalLogisticsCarrierMappings() {
  return apiClient.get<RentalLogisticsCarrierMappingVO[]>(
    `${LOGISTICS_OPERATIONS_PATH}/carrier-mapping`
  );
}

export function saveRentalLogisticsCarrierMapping(
  data: RentalLogisticsCarrierMappingSaveReqVO
) {
  return apiClient.put<RentalLogisticsCarrierMappingVO>(
    `${LOGISTICS_OPERATIONS_PATH}/carrier-mapping`,
    data
  );
}

export function deleteRentalLogisticsCarrierMapping(id: number) {
  return apiClient.delete<boolean>(
    `${LOGISTICS_OPERATIONS_PATH}/carrier-mapping/${id}`
  );
}

export function fetchRentalLogisticsFailedTasks(
  taskType: 'ALL' | 'INBOX' | 'OUTBOX' = 'ALL',
  limit = 50
) {
  return apiClient.get<RentalLogisticsFailedTaskVO[]>(
    `${LOGISTICS_OPERATIONS_PATH}/failed-task`,
    { taskType, limit }
  );
}

export function retryRentalLogisticsFailedTask(taskType: string, id: number) {
  return apiClient.post<RentalLogisticsRetryResultVO>(
    `${LOGISTICS_OPERATIONS_PATH}/failed-task/${taskType}/${id}/retry`
  );
}

export function reconcileRentalLogistics(limit = 20) {
  return apiClient.post<RentalLogisticsReconcileResultVO>(
    `${LOGISTICS_OPERATIONS_PATH}/reconcile`,
    { limit }
  );
}

export function fetchRentalLogisticsMetrics() {
  return apiClient.get<RentalLogisticsMetricsVO>(
    `${LOGISTICS_OPERATIONS_PATH}/metrics`
  );
}

export function backfillRentalLogistics(data: {
  dryRun: boolean;
  limit: number;
  enqueueProviderTasks: boolean;
}) {
  return apiClient.post<RentalLogisticsBackfillResultVO>(
    `${LOGISTICS_OPERATIONS_PATH}/backfill`,
    data
  );
}

export function cleanupRentalLogistics(data: {
  dryRun: boolean;
  retentionDays: number;
  limit: number;
}) {
  return apiClient.post<RentalLogisticsCleanupResultVO>(
    `${LOGISTICS_OPERATIONS_PATH}/cleanup`,
    data
  );
}
