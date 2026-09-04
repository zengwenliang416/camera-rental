import request from '@/config/axios'

export interface XianyuConfigVO {
  enabled: boolean
  status: string
  baseUrl: string
  appKeyMasked: string
  appSecretConfigured: boolean
  webhookBaseUrlConfigured: boolean
  webhookBaseUrl?: string
  writeEnabled: boolean
  jobEnabled: boolean
  lookbackDays: number
  overlapMinutes: number
  maxPagesPerShop: number
  pageSize: number
  pushRetryStaleSeconds: number
  pushRetryBatchSize: number
}

export interface XianyuConfigUpdateReqVO {
  enabled: boolean
  baseUrl: string
  /** Empty means keep the persisted value. */
  appKey?: string
  /** Empty means keep the encrypted persisted value. */
  appSecret?: string
  webhookBaseUrl?: string
  writeEnabled: boolean
  jobEnabled: boolean
  lookbackDays: number
  overlapMinutes: number
  maxPagesPerShop: number
  pageSize: number
  pushRetryStaleSeconds: number
  pushRetryBatchSize: number
}

export interface XianyuShopVO {
  id: number
  applicationId: number
  externalShopId: string
  authorizeId: string
  shopName: string
  authorizationStatus: string
  authorizationExpiresAt?: string
  guaranteeStatus?: string
}

export interface XianyuOrderVO {
  id: number
  shopId: number
  /** Full order no. (not redacted) for ops lookup */
  externalOrderId: string
  externalProductId?: string
  externalSkuId?: string
  orderStatus: string
  payAmount: number
  currency: string
  sellerRemark?: string
  receiverName?: string
  receiverMobile?: string
  receiverAddress?: string
  buyerNick?: string
  remarkParseVersion?: string
  remarkParseStatus?: string
  remarkParseSource?: 'RULE' | 'AI'
  remarkParseConfidence?: number
  remarkParseModel?: string
  shipDate?: string
  billableStartDate?: string
  billableEndDate?: string
  rentalPeriodStatus?: string
  rentalPeriodReasonCode?: string
  conversionStatus: string
  rentalOrderId?: number
  sourceCreatedAt?: string
  sourceUpdatedAt?: string
  orderType?: number
  orderTime?: string
  totalAmount?: number
  payTime?: string
  refundStatus?: number
  refundAmount?: number
  refundTime?: string
  expressCode?: string
  expressName?: string
  waybillNo?: string
  expressFee?: number
  consignType?: number
  consignTime?: string
  confirmTime?: string
  cancelReason?: string
  cancelTime?: string
  sellerName?: string
  goodsTitle?: string
  goodsQuantity?: number
  goodsPrice?: number
  xybSellerAmount?: number
  taxIncluded?: boolean
  idleBizType?: number
  pinGroupStatus?: number
  rentalOrderItemId?: number
  equipmentModelCode?: string
  rentalQuantity?: number
  occupyStartDate?: string
  occupyEndDateExclusive?: string
  assignedDeviceIds?: number[]
}

/**
 * windowStart/windowEnd must be epoch millis numbers so Jackson
 * TimestampLocalDateTimeDeserializer can parse the request body.
 */
export interface XianyuOrderSyncReqVO {
  shopId: number
  windowStart: number
  windowEnd: number
  pageNo: number
  pageSize: number
}

export interface XianyuOrderSyncRespVO {
  syncRunId: number
  receivedCount: number
  succeededCount: number
}

export interface XianyuAfterSaleVO {
  id: number
  shopId: number
  externalAfterSaleId: string
  externalOrderId: string
  afterSaleStatus: string
  refundAmount?: number
  amountUnitStatus: string
  timeoutAt?: string
  sourceUpdatedAt?: string
}

export interface XianyuAfterSaleSyncReqVO {
  shopId: number
  applyStart?: number | null
  applyEnd?: number | null
  refundStart?: number | null
  refundEnd?: number | null
  pageNo: number
  pageSize: number
}

export interface XianyuAfterSaleSyncRespVO {
  syncRunId: number
  receivedCount: number
  succeededCount: number
  hasNextPage: boolean
}

export interface XianyuProductSyncReqVO {
  shopId: number
  windowStart: number
  windowEnd: number
  pageNo: number
  pageSize: number
}

export interface XianyuProductSyncRespVO {
  syncRunId: number
  receivedCount: number
  succeededCount: number
  deduplicatedCount: number
  skuCount: number
}

export interface XianyuExpressCompanyVO {
  code: string
  expressName: string
  expressAlias: string
  hot: boolean
}

export interface XianyuExpressCandidateVO {
  code: string
  name: string
}

export interface XianyuAlertVO {
  id: number
  shopId?: number
  alertType: string
  severity: string
  status: string
  sourceIdentifier?: string
  message: string
  firstSeenAt?: string
  lastSeenAt?: string
  resolvedAt?: string
}

export interface XianyuRawPayloadVO {
  id: number
  sourceType: string
  sourceIdentifier: string
  payloadHash: string
  schemaVersion: string
  redactionVersion: string
  receivedAt?: string
  maskedPayload?: string
}

export interface XianyuPushReplayRespVO {
  eventId: number
  status: string
  safeErrorCode?: string
  message: string
}

export interface XianyuRawPayloadReplayRespVO {
  rawPayloadId: number
  orderId?: number
  status: string
  safeErrorCode?: string
  message: string
}

export interface RentalConversionResultVO {
  rentalOrderId?: number
  reviewId?: number
  status: string
  reasonCode?: string
}

export interface XianyuPendingShipOrderVO {
  id: number
  shopId: number
  externalOrderId: string
  orderStatus: string
  goodsTitle?: string
  goodsQuantity?: number
  payAmount?: number
  buyerNick?: string
  receiverName?: string
  receiverMobile?: string
  receiverAddress?: string
  sellerRemark?: string
  xianyuItemId?: string
  rentalOrderId?: number
  conversionStatus: string
  preparationStatus?: string
  preparationReasonCode?: string
  orderTime?: string
  sourceUpdatedAt?: string
}

export interface XianyuShipmentOcrRespVO {
  waybillNo?: string
  expressCode?: string
  expressName?: string
  confidence?: number
  extractionSource?: string
}

export interface XianyuOrderShipReqVO {
  channelOrderId: number
  deviceId?: number
  deviceNo?: string
  idempotencyKey: string
  expressCode: string
  expressName: string
  waybillNo: string
  source: 'ADMIN' | 'STAFF'
  ocrConfirmed?: boolean
  bindProductRuleIfMissing?: boolean
  allowPendingPlan?: boolean
}

export interface XianyuOrderDispatchBackfillReqVO {
  channelOrderId: number
  deviceId?: number
  deviceNo?: string
  idempotencyKey: string
  expressCode: string
  expressName: string
  waybillNo: string
  consignTime: string
  reason: string
}

export interface XianyuOrderShipRespVO {
  shipmentId: number
  channelOrderId: number
  assignmentId?: number
  deviceId: number
  deviceNo: string
  maskedWaybillNo: string
  expressCode: string
  expressName: string
  remoteCode?: number
  remoteMsg?: string
  assignmentStatus?: string
  source: string
  deliveryId?: number
  trackingMappingStatus?: string
  trackingSubscribeStatus?: string
  trackingQueryStatus?: string
  trackingReason?: string
  trackingPendingEvents?: string[]
}

export const getXianyuConfig = () => {
  return request.get<XianyuConfigVO>({ url: '/rental/xianyu/config/get' })
}

export const updateXianyuConfig = (data: XianyuConfigUpdateReqVO) => {
  return request.put({ url: '/rental/xianyu/config/update', data })
}

export const getXianyuShopPage = (params: PageParam) => {
  return request.get<PageResult<XianyuShopVO[]>>({ url: '/rental/xianyu/shop/page', params })
}

export const syncAuthorizedShops = () => {
  return request.post<number>({ url: '/rental/xianyu/shop/sync-authorized' })
}

export const getXianyuOrderPage = (
  params: PageParam & {
    shopId?: number
    orderStatus?: string
    conversionStatus?: string
    externalOrderId?: string
    externalProductId?: string
    externalSkuId?: string
    startDate?: string
    endDate?: string
    shipDate?: string
    rentalStartDate?: string
    rentalEndDate?: string
  }
) => {
  return request.get<PageResult<XianyuOrderVO[]>>({ url: '/rental/xianyu/order/page', params })
}

export const syncXianyuOrderPage = (data: XianyuOrderSyncReqVO) => {
  return request.post<XianyuOrderSyncRespVO>({ url: '/rental/xianyu/order/sync-page', data })
}

export const reparseXianyuSellerRemarks = (maxOrders = 5000) => {
  return request.post<number>({
    url: '/rental/xianyu/order/reparse-remarks',
    params: { maxOrders }
  })
}

export const convertXianyuOrder = (channelOrderId: number) => {
  return request.post<RentalConversionResultVO>({
    url: '/rental/xianyu/order/convert',
    params: { channelOrderId }
  })
}

export const getXianyuPendingShipOrderPage = (
  params: PageParam & {
    shopId?: number
    keyword?: string
  }
) => {
  return request.get<PageResult<XianyuPendingShipOrderVO[]>>({
    url: '/rental/xianyu/order/pending-ship/page',
    params
  })
}

export const recognizeXianyuShipmentImage = async (file: File) => {
  const data = new FormData()
  data.append('file', file)
  const res = await request.upload<{ data: XianyuShipmentOcrRespVO }>({
    url: '/rental/xianyu/order/ship/ocr',
    data
  })
  return res.data
}

export const shipXianyuOrder = (data: XianyuOrderShipReqVO) => {
  return request.post<XianyuOrderShipRespVO>({ url: '/rental/xianyu/order/ship', data })
}

export const backfillXianyuOrderDispatch = (data: XianyuOrderDispatchBackfillReqVO) => {
  return request.post<XianyuOrderShipRespVO>({
    url: '/rental/xianyu/order/dispatch-backfill',
    data
  })
}

export const getXianyuAfterSalePage = (
  params: PageParam & {
    shopId?: number
    afterSaleStatus?: string
  }
) => {
  return request.get<PageResult<XianyuAfterSaleVO[]>>({
    url: '/rental/xianyu/after-sale/page',
    params
  })
}

export const syncXianyuAfterSalePage = (data: XianyuAfterSaleSyncReqVO) => {
  return request.post<XianyuAfterSaleSyncRespVO>({
    url: '/rental/xianyu/after-sale/sync-page',
    data
  })
}

export const syncXianyuProductPage = (data: XianyuProductSyncReqVO) => {
  return request.post<XianyuProductSyncRespVO>({
    url: '/rental/xianyu/product/sync-page',
    data
  })
}

export const getXianyuExpressCompanyList = () => {
  return request.get<XianyuExpressCompanyVO[]>({ url: '/rental/xianyu/express-company/list' })
}

export const recognizeXianyuExpress = (waybillNo: string) => {
  return request.get<XianyuExpressCandidateVO[]>({
    url: '/rental/xianyu/express-company/recognize',
    params: { waybillNo }
  })
}

export const getXianyuAlertPage = (
  params: PageParam & {
    shopId?: number
    alertType?: string
    status?: string
    severity?: string
  }
) => {
  return request.get<PageResult<XianyuAlertVO[]>>({
    url: '/rental/xianyu/alert/page',
    params
  })
}

export const resolveXianyuAlert = (id: number) => {
  return request.put<boolean>({ url: '/rental/xianyu/alert/resolve', data: { id } })
}

export const getXianyuRawPayloadPage = (
  params: PageParam & {
    sourceType?: string
    sourceIdentifier?: string
  }
) => {
  return request.get<PageResult<XianyuRawPayloadVO[]>>({
    url: '/rental/xianyu/raw-payload/page',
    params
  })
}

export const getXianyuRawPayload = (id: number) => {
  return request.get<XianyuRawPayloadVO>({ url: '/rental/xianyu/raw-payload/get', params: { id } })
}

export const replayXianyuPushEvent = (eventId: number) => {
  return request.post<XianyuPushReplayRespVO>({
    url: '/rental/xianyu/replay/push-event',
    data: { eventId }
  })
}

export const replayXianyuRawPayload = (rawPayloadId: number) => {
  return request.post<XianyuRawPayloadReplayRespVO>({
    url: '/rental/xianyu/replay/raw-payload',
    data: { rawPayloadId }
  })
}
