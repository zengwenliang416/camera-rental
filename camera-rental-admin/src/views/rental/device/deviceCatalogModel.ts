export interface RentalDeviceCategoryOption {
  id: number
  categoryCode: string
  categoryName: string
  models: RentalDeviceModelOption[]
}

export interface RentalDeviceModelOption {
  id: number
  modelCode: string
  modelName: string
  deviceNoPrefix: string
}

export const getModelsForCategory = (
  catalog: RentalDeviceCategoryOption[],
  categoryCode?: string
) => {
  if (!categoryCode) {
    return catalog.flatMap((category) => category.models)
  }
  return catalog.find((category) => category.categoryCode === categoryCode)?.models ?? []
}

export const isModelInCategory = (
  catalog: RentalDeviceCategoryOption[],
  categoryCode: string,
  modelCode: string
) => getModelsForCategory(catalog, categoryCode).some((model) => model.modelCode === modelCode)

export const findModel = (
  catalog: RentalDeviceCategoryOption[],
  categoryCode: string,
  modelCode: string
) =>
  getModelsForCategory(catalog, categoryCode).find((model) => model.modelCode === modelCode)

export const normalizeDeviceNoSuffix = (deviceNoSuffix: string) => {
  const normalized = deviceNoSuffix.trim()
  if (!/^(?:0?[1-9]|[1-9][0-9]{1,2})$/.test(normalized)) return ''
  return normalized.padStart(2, '0')
}

export const buildDeviceNoPreview = (deviceNoPrefix?: string, deviceNoSuffix = '') => {
  const normalizedSuffix = normalizeDeviceNoSuffix(deviceNoSuffix)
  return deviceNoPrefix && normalizedSuffix ? `${deviceNoPrefix}-${normalizedSuffix}` : ''
}
