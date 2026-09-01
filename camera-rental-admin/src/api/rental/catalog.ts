export interface RentalDeviceCategoryVO {
  id: number
  categoryCode: string
  categoryName: string
  sortOrder?: number
  enabled?: boolean
  lockVersion: number
  models: RentalDeviceModelVO[]
}

export interface RentalDeviceModelVO {
  id: number
  categoryId?: number
  modelCode: string
  modelName: string
  deviceNoPrefix: string
  sortOrder?: number
  enabled?: boolean
  lockVersion: number
}
