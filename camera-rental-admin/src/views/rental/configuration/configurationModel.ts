import type {
  RentalChannelProductRuleSaveReqVO,
  RentalChannelProductRuleSkuReqVO,
  RentalChannelProductSkuVO,
  RentalRuleHandlingPolicy,
  RentalRuleMappingMode
} from '@/api/rental/configuration'

export interface RentalChannelProductRuleDraft {
  id?: number
  shopId?: number
  xianyuItemId: string
  handlingPolicy: RentalRuleHandlingPolicy
  mappingMode: Exclude<RentalRuleMappingMode, 'NONE'>
  singleDeviceModelId?: number
  enabled: boolean
  ruleNote: string
  lockVersion?: number
  skuMappings: RentalChannelProductSkuVO[]
  synchronizedProductSkuIds: number[]
}

export const normalizeExternalIdentifier = (value?: string | null) => (value ?? '').trim()

export const buildSkuMappingRequest = (
  skus: RentalChannelProductSkuVO[],
  synchronizedProductSkuIds: number[]
): RentalChannelProductRuleSkuReqVO[] =>
  skus
    .filter(
      (sku) =>
        synchronizedProductSkuIds.includes(sku.productSkuId) && sku.deviceModelId !== undefined
    )
    .map((sku) => ({
      productSkuId: sku.productSkuId,
      deviceModelId: sku.deviceModelId as number,
      enabled: sku.mappingEnabled !== false
    }))

export const buildProductRuleSaveRequest = (
  draft: RentalChannelProductRuleDraft
): RentalChannelProductRuleSaveReqVO => {
  if (draft.shopId === undefined) {
    throw new Error('shopId is required')
  }
  if (draft.id !== undefined && draft.lockVersion === undefined) {
    throw new Error('lockVersion is required for updates')
  }
  const base: RentalChannelProductRuleSaveReqVO = {
    id: draft.id,
    shopId: draft.shopId,
    xianyuItemId: normalizeExternalIdentifier(draft.xianyuItemId),
    handlingPolicy: draft.handlingPolicy,
    enabled: draft.enabled,
    ruleNote: draft.ruleNote.trim() || undefined,
    lockVersion: draft.lockVersion
  }
  if (draft.handlingPolicy === 'CONFIG_SKIPPED') {
    return { ...base, mappingMode: 'NONE', skuMappings: [] }
  }
  if (draft.mappingMode === 'SINGLE') {
    return {
      ...base,
      mappingMode: 'SINGLE',
      singleDeviceModelId: draft.singleDeviceModelId,
      skuMappings: []
    }
  }
  return {
    ...base,
    mappingMode: 'MULTI',
    skuMappings: buildSkuMappingRequest(draft.skuMappings, draft.synchronizedProductSkuIds)
  }
}

export const buildImpactPreviewKey = (draft: RentalChannelProductRuleDraft) =>
  JSON.stringify(buildProductRuleSaveRequest(draft))

export const isImpactPreviewFresh = (draft: RentalChannelProductRuleDraft, previewKey?: string) =>
  Boolean(previewKey && previewKey === buildImpactPreviewKey(draft))

export const countConfiguredSkus = (skus: RentalChannelProductSkuVO[]) =>
  skus.filter((sku) => sku.deviceModelId !== undefined && sku.mappingEnabled !== false).length

export const hasMissingSkuMappings = (skus: RentalChannelProductSkuVO[]) =>
  skus.length === 0 || countConfiguredSkus(skus) < skus.length

export const normalizeHandlingPolicy = (value?: string): RentalRuleHandlingPolicy =>
  value === 'CONFIG_SKIPPED' ? 'CONFIG_SKIPPED' : 'CREATE_RENTAL'

export const buildRuleScopeKey = (shopId?: number, xianyuItemId?: string) =>
  shopId === undefined ? '' : `${shopId}:${normalizeExternalIdentifier(xianyuItemId)}`

export const isTerminalReconciliationStatus = (status?: string) =>
  status === 'SUCCEEDED' || status === 'COMPLETED_WITH_ERRORS' || status === 'FAILED'

export const isConfigurationVersionConflict = (error: unknown) => {
  const candidate = error as {
    code?: number
    message?: string
    response?: { data?: { code?: number } }
  }
  return (
    candidate?.code === 1_040_002_026 ||
    candidate?.response?.data?.code === 1_040_002_026 ||
    candidate?.message?.includes('租赁配置已被其他管理员修改') === true
  )
}

export const recoverConfigurationVersionConflict = async (
  error: unknown,
  closeEditor: () => void,
  reload: () => Promise<void>
) => {
  if (!isConfigurationVersionConflict(error)) return false
  closeEditor()
  await reload()
  return true
}
