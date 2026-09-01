import request from '@/config/axios'
import type { RentalDeviceCategoryVO } from './catalog'

export type RentalRuleHandlingPolicy = 'CREATE_RENTAL' | 'CONFIG_SKIPPED'
export type RentalRuleMappingMode = 'NONE' | 'SINGLE' | 'MULTI'

export interface RentalConfigurationCatalogRespVO {
  categories: RentalDeviceCategoryVO[]
}

export interface RentalConfigurationShopVO {
  id: number
  shopName: string
  authorizationStatus: string
}

export interface RentalDeviceCategoryCreateReqVO {
  categoryCode: string
  categoryName: string
  sortOrder?: number
}

export interface RentalDeviceCategoryUpdateReqVO extends RentalDeviceCategoryCreateReqVO {
  id: number
  lockVersion: number
}

export interface RentalDeviceModelCreateReqVO {
  categoryId: number
  modelCode: string
  modelName: string
  deviceNoPrefix: string
  sortOrder?: number
}

export interface RentalDeviceModelUpdateReqVO extends RentalDeviceModelCreateReqVO {
  id: number
  lockVersion: number
}

export interface RentalDeviceCatalogStatusReqVO {
  id: number
  enabled: boolean
  lockVersion: number
}

export interface RentalChannelProductSkuVO {
  productSkuId: number
  xgjSkuId: string
  xianyuSkuId?: string
  skuName?: string
  status?: string
  deviceModelId?: number
  mappingEnabled?: boolean
}

export interface RentalChannelProductRuleVO {
  id: number
  shopId: number
  xianyuItemId: string
  xgjProductId?: string
  productTitleSnapshot?: string
  handlingPolicy: RentalRuleHandlingPolicy
  mappingMode: RentalRuleMappingMode
  singleDeviceModelId?: number
  enabled: boolean
  ruleNote?: string
  lockVersion: number
  skuMappings: RentalChannelProductSkuVO[]
}

export interface RentalChannelProductRulePageReqVO extends PageParam {
  shopId?: number
  handlingPolicy?: RentalRuleHandlingPolicy
  enabled?: boolean
  keyword?: string
}

export interface RentalChannelProductRuleSkuReqVO {
  productSkuId: number
  deviceModelId: number
  enabled: boolean
}

export interface RentalChannelProductRuleSaveReqVO {
  id?: number
  shopId: number
  xianyuItemId: string
  handlingPolicy: RentalRuleHandlingPolicy
  mappingMode?: RentalRuleMappingMode
  singleDeviceModelId?: number
  enabled: boolean
  ruleNote?: string
  lockVersion?: number
  skuMappings?: RentalChannelProductRuleSkuReqVO[]
}

export interface RentalChannelProductRuleImpactVO {
  scannedCount: number
  withoutInternalOrderCount: number
  mutableInternalOrderCount: number
  protectedOrderCount: number
  reviewRequiredCount: number
}

export interface RentalChannelProductRuleSaveRespVO {
  ruleId: number
  lockVersion: number
  impact: RentalChannelProductRuleImpactVO
  reconciliationRunId: number
}

export interface RentalChannelProductRuleStatusReqVO {
  id: number
  enabled: boolean
  lockVersion: number
}

export type RentalChannelReconciliationStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'COMPLETED_WITH_ERRORS'
  | 'FAILED'

export interface RentalChannelReconciliationRunVO {
  runId: number
  productRuleId: number
  shopId: number
  xianyuItemId: string
  triggerType: 'RULE_CHANGE'
  status: RentalChannelReconciliationStatus
  scannedCount: number
  skippedCount: number
  createdCount: number
  updatedCount: number
  unchangedCount: number
  conflictCount: number
  failedCount: number
  reviewRequiredCount: number
  lastErrorCode?: string
}

const CONFIGURATION_PATH = '/rental/configuration'

export const getRentalConfigurationCatalog = () =>
  request.get<RentalConfigurationCatalogRespVO>({ url: `${CONFIGURATION_PATH}/catalog` })

export const getRentalConfigurationShops = () =>
  request.get<RentalConfigurationShopVO[]>({ url: `${CONFIGURATION_PATH}/shops` })

export const createRentalConfigurationCategory = (data: RentalDeviceCategoryCreateReqVO) =>
  request.post<number>({ url: `${CONFIGURATION_PATH}/catalog/category/create`, data })

export const updateRentalConfigurationCategory = (data: RentalDeviceCategoryUpdateReqVO) =>
  request.put<number>({ url: `${CONFIGURATION_PATH}/catalog/category/update`, data })

export const updateRentalConfigurationCategoryStatus = (data: RentalDeviceCatalogStatusReqVO) =>
  request.put<number>({ url: `${CONFIGURATION_PATH}/catalog/category/status`, data })

export const createRentalConfigurationModel = (data: RentalDeviceModelCreateReqVO) =>
  request.post<number>({ url: `${CONFIGURATION_PATH}/catalog/model/create`, data })

export const updateRentalConfigurationModel = (data: RentalDeviceModelUpdateReqVO) =>
  request.put<number>({ url: `${CONFIGURATION_PATH}/catalog/model/update`, data })

export const updateRentalConfigurationModelStatus = (data: RentalDeviceCatalogStatusReqVO) =>
  request.put<number>({ url: `${CONFIGURATION_PATH}/catalog/model/status`, data })

export const getRentalChannelProductRulePage = (params: RentalChannelProductRulePageReqVO) =>
  request.get<PageResult<RentalChannelProductRuleVO[]>>({
    url: `${CONFIGURATION_PATH}/product-rule/page`,
    params
  })

export const getRentalChannelProductRule = (id: number) =>
  request.get<RentalChannelProductRuleVO>({
    url: `${CONFIGURATION_PATH}/product-rule/get`,
    params: { id }
  })

export const getRentalChannelProductSkus = (shopId: number, xianyuItemId: string) =>
  request.get<RentalChannelProductSkuVO[]>({
    url: `${CONFIGURATION_PATH}/product-rule/synced-skus`,
    params: { shopId, xianyuItemId }
  })

export const previewRentalChannelProductRuleImpact = (shopId: number, xianyuItemId: string) =>
  request.get<RentalChannelProductRuleImpactVO>({
    url: `${CONFIGURATION_PATH}/product-rule/impact`,
    params: { shopId, xianyuItemId }
  })

export const createRentalChannelProductRule = (data: RentalChannelProductRuleSaveReqVO) =>
  request.post<RentalChannelProductRuleSaveRespVO>({
    url: `${CONFIGURATION_PATH}/product-rule/create`,
    data
  })

export const updateRentalChannelProductRule = (data: RentalChannelProductRuleSaveReqVO) =>
  request.put<RentalChannelProductRuleSaveRespVO>({
    url: `${CONFIGURATION_PATH}/product-rule/update`,
    data
  })

export const updateRentalChannelProductRuleStatus = (data: RentalChannelProductRuleStatusReqVO) =>
  request.put<RentalChannelProductRuleSaveRespVO>({
    url: `${CONFIGURATION_PATH}/product-rule/status`,
    data
  })

export const getRentalChannelProductRuleReconciliation = (runId: number) =>
  request.get<RentalChannelReconciliationRunVO>({
    url: `${CONFIGURATION_PATH}/product-rule/reconciliation`,
    params: { runId }
  })
