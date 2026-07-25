import request from '@/config/axios'

export interface RentalDeviceVO {
  id: number
  deviceNo: string
  serialNumber?: string
  equipmentModelCode: string
  status: string
  warehouseCode?: string
  purchaseAmount?: number
  enabled: boolean
}

export interface RentalDeviceCreateReqVO {
  deviceNo: string
  serialNumber?: string
  equipmentModelCode: string
  status?: string
  warehouseCode?: string
  purchaseAmount?: number
  enabled?: boolean
}

export interface RentalDeviceAssignReqVO {
  rentalOrderItemId: number
  deviceId: number
  occupyStartDate: string
  occupyEndDateExclusive: string
  idempotencyKey: string
}

export interface RentalDevicePageReqVO extends PageParam {
  equipmentModelCode?: string
}

export interface RentalDeviceAssignmentResultVO {
  assignmentId: number
  scheduleId: number
  deviceId: number
  occupyStartDate: string
  occupyEndDateExclusive: string
}

export const getRentalDevicePage = (params: RentalDevicePageReqVO) => {
  return request.get<PageResult<RentalDeviceVO[]>>({ url: '/rental/device/page', params })
}

export const createRentalDevice = (data: RentalDeviceCreateReqVO) => {
  return request.post<number>({ url: '/rental/device/create', data })
}

export const assignRentalDevice = (data: RentalDeviceAssignReqVO) => {
  return request.post<RentalDeviceAssignmentResultVO>({ url: '/rental/device/assign', data })
}
