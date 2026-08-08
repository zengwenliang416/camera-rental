import request from '@/config/axios'

export type RentalLogisticsSecretAction = 'KEEP' | 'REPLACE' | 'CLEAR'

export interface RentalLogisticsProviderCredentialVO {
  id: number
  providerCode: string
  credentialName: string
  enabled: boolean
  sortOrder: number
  customerCodeConfigured: boolean
  maskedCustomerCode?: string | null
  apiKeyConfigured: boolean
  maskedApiKey?: string | null
  configStatus: string
  lastVerifiedAt?: string | null
}

export interface RentalLogisticsProviderConfigVO {
  providerCode: string
  enabled: boolean
  queryEnabled: boolean
  subscribeEnabled: boolean
  callbackSecretConfigured: boolean
  maskedCallbackSecret?: string | null
  callbackBaseUrl?: string | null
  minimumQueryIntervalSeconds: number
  resultVersion: string
  configStatus: string
  lastVerifiedAt?: string | null
  credentials: RentalLogisticsProviderCredentialVO[]
}

export interface RentalLogisticsProviderConfigUpdateReqVO {
  providerCode: string
  enabled: boolean
  queryEnabled: boolean
  subscribeEnabled: boolean
  callbackSecretAction: RentalLogisticsSecretAction
  callbackSecret?: string
  callbackBaseUrl?: string | null
  minimumQueryIntervalSeconds: number
  resultVersion: string
}

export interface RentalLogisticsProviderCredentialSaveReqVO {
  id?: number
  providerCode: string
  credentialName: string
  enabled: boolean
  sortOrder: number
  customerCodeAction: RentalLogisticsSecretAction
  customerCode?: string
  apiKeyAction: RentalLogisticsSecretAction
  apiKey?: string
}

export interface RentalLogisticsProviderVerifyResultVO {
  valid: boolean
  reason: string
  verifiedAt?: string | null
}

export interface RentalLogisticsBackfillReqVO {
  dryRun: boolean
  limit: number
  enqueueProviderTasks: boolean
  consignDateStart: string
  consignDateEnd: string
}

export interface RentalLogisticsBackfillItemVO {
  shipmentId?: number | null
  deliveryId?: number | null
  maskedWaybillNo?: string | null
  status: string
  reason: string
}

export interface RentalLogisticsBackfillResultVO {
  dryRun: boolean
  requestedLimit: number
  candidateCount: number
  distinctWaybillCount: number
  createdOrReusedCount: number
  skippedCount: number
  providerTasksEnqueued: boolean
  providerTaskReason: string
  items: RentalLogisticsBackfillItemVO[]
}

export interface RentalDeliveryTrackingRiskVO {
  code: string
  severity: string
  safeMessage: string
  nextAction?: string
  deviceIds?: number[]
}

export interface RentalDeliveryTrackingPackageSummaryVO {
  deliveryId: number
  rentalOrderId?: number
  direction: string
  packageSeq?: number
  carrierName?: string
  maskedWaybillNo?: string
  trackingStatus?: string
  mappingStatus?: string
  subscribeStatus?: string
  queryStatus?: string
  latestTraceText?: string
  latestEventTime?: string
  lastSyncedAt?: string
  estimatedDeliveryAt?: string
  stale?: boolean
  risk?: RentalDeliveryTrackingRiskVO
}

export interface RentalDeliveryTrackingOrderSummaryVO {
  orderId: number
  rentalOrderId?: number
  packageCount: number
  statusCounts: Record<string, number>
  packages: RentalDeliveryTrackingPackageSummaryVO[]
  risks: RentalDeliveryTrackingRiskVO[]
}

export interface RentalDeliveryTrackingTraceVO {
  eventSeq: number
  businessTime?: string
  trackingStatus?: string
  traceText?: string
  location?: string
}

export interface RentalDeliveryTrackingDeviceVO {
  deviceId: number
  deviceNo: string
  equipmentModelCode: string
}

export interface RentalDeliveryTrackingDetailVO {
  deliveryId: number
  rentalOrderId?: number
  direction: string
  packageSeq?: number
  carrierName?: string
  maskedWaybillNo?: string
  trackingStatus?: string
  mappingStatus?: string
  subscribeStatus?: string
  queryStatus?: string
  latestTraceText?: string
  latestLocation?: string
  latestEventTime?: string
  lastSyncedAt?: string
  estimatedDeliveryAt?: string
  nextQueryAllowedAt?: string
  stale?: boolean
  devices: RentalDeliveryTrackingDeviceVO[]
  traces: RentalDeliveryTrackingTraceVO[]
  risks: RentalDeliveryTrackingRiskVO[]
}

export interface RentalDeliveryRefreshResultVO {
  accepted: boolean
  reason?: string
  nextAllowedAt?: string
}

const LOGISTICS_OPERATIONS_PATH = '/rental/logistics/operations'

export const getRentalLogisticsProviderConfig = (providerCode = 'KUAIDI100') => {
  return request.get<RentalLogisticsProviderConfigVO>({
    url: `${LOGISTICS_OPERATIONS_PATH}/provider-config/${providerCode}`
  })
}

export const updateRentalLogisticsProviderConfig = (
  data: RentalLogisticsProviderConfigUpdateReqVO
) => {
  return request.put<RentalLogisticsProviderConfigVO>({
    url: `${LOGISTICS_OPERATIONS_PATH}/provider-config`,
    data
  })
}

export const verifyRentalLogisticsProviderConfig = (providerCode = 'KUAIDI100') => {
  return request.post<RentalLogisticsProviderVerifyResultVO>({
    url: `${LOGISTICS_OPERATIONS_PATH}/provider-config/${providerCode}/verify`
  })
}

export const saveRentalLogisticsProviderCredential = (
  data: RentalLogisticsProviderCredentialSaveReqVO
) => {
  return request.put<RentalLogisticsProviderCredentialVO>({
    url: `${LOGISTICS_OPERATIONS_PATH}/provider-credential`,
    data
  })
}

export const deleteRentalLogisticsProviderCredential = (id: number) => {
  return request.delete<boolean>({
    url: `${LOGISTICS_OPERATIONS_PATH}/provider-credential/${id}`
  })
}

export const verifyRentalLogisticsProviderCredential = (id: number) => {
  return request.post<RentalLogisticsProviderVerifyResultVO>({
    url: `${LOGISTICS_OPERATIONS_PATH}/provider-credential/${id}/verify`
  })
}

export const backfillRentalLogistics = (data: RentalLogisticsBackfillReqVO) => {
  return request.post<RentalLogisticsBackfillResultVO>({
    url: `${LOGISTICS_OPERATIONS_PATH}/backfill`,
    data
  })
}

export const getRentalDeliveryTrackingSummaries = (orderIds: number[]) => {
  return request.post<Record<string, RentalDeliveryTrackingOrderSummaryVO>>({
    url: '/rental/delivery/tracking-summary/batch',
    data: { orderIds }
  })
}

export const getRentalDeliveryTracking = (deliveryId: number) => {
  return request.get<RentalDeliveryTrackingDetailVO>({
    url: `/rental/delivery/${deliveryId}/tracking`
  })
}

export const refreshRentalDeliveryTracking = (deliveryId: number) => {
  return request.post<RentalDeliveryRefreshResultVO>({
    url: `/rental/delivery/${deliveryId}/refresh`
  })
}
