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

export const buildDeviceNoHint = (deviceNoPrefix?: string) =>
  deviceNoPrefix ? `${deviceNoPrefix}-01 ~ ${deviceNoPrefix}-99` : ''
