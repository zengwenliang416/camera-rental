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
