import request from '@/config/axios'

export interface XianyuConfigVO {
  enabled: boolean
  status: string
  baseUrl: string
  appKeyMasked: string
  appSecretConfigured: boolean
  webhookBaseUrlConfigured: boolean
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
  externalOrderId: string
  externalProductId?: string
  externalSkuId?: string
  orderStatus: string
  payAmount: number
  currency: string
  sellerRemark?: string
  remarkParseStatus?: string
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

export const getXianyuConfig = () => {
  return request.get<XianyuConfigVO>({ url: '/rental/xianyu/config/get' })
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
    conversionStatus?: string
    externalProductId?: string
    externalSkuId?: string
    startDate?: string
    endDate?: string
  }
) => {
  return request.get<PageResult<XianyuOrderVO[]>>({ url: '/rental/xianyu/order/page', params })
}

export const syncXianyuOrderPage = (data: XianyuOrderSyncReqVO) => {
  return request.post<XianyuOrderSyncRespVO>({ url: '/rental/xianyu/order/sync-page', data })
}

export const convertXianyuOrder = (channelOrderId: number) => {
  return request.post<RentalConversionResultVO>({
    url: '/rental/xianyu/order/convert',
    params: { channelOrderId }
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
