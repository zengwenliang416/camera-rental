import request from '@/config/axios'
import type {
  RentalScheduleDeviceLockVO,
  RentalScheduleSegmentVO
} from '@/api/rental/schedule'

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

export interface RentalDeviceScheduleDetailVO {
  id: number
  deviceNo: string
  serialNumber?: string
  equipmentModelCode: string
  status: string
  enabled: boolean
  inspectionState?: string
  maintenanceState?: string
  expectedReleaseDate?: string
  reasonCodes?: string[]
  schedules: RentalScheduleSegmentVO[]
  currentAssignment?: {
    id: number
    rentalOrderId: number
    rentalOrderItemId: number
    status: string
    occupyStartDate: string
    occupyEndDateExclusive: string
  }
  deliveries?: Array<{
    id: number
    direction: string
    sourceCarrierName?: string
    trackingStatus?: string
    latestEventTime?: string
    estimatedDeliveryAt?: string
    stale?: boolean
  }>
  activeLocks?: RentalScheduleDeviceLockVO[]
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

export const getRentalDeviceDetail = (id: number) => {
  return request.get<RentalDeviceScheduleDetailVO>({
    url: `/rental/device/${id}/schedule-detail`
  })
}

export const createRentalDevice = (data: RentalDeviceCreateReqVO) => {
  return request.post<number>({ url: '/rental/device/create', data })
}

export const assignRentalDevice = (data: RentalDeviceAssignReqVO) => {
  return request.post<RentalDeviceAssignmentResultVO>({ url: '/rental/device/assign', data })
}

export interface RentalDeviceQrVO {
  deviceId: number
  deviceNo: string
  equipmentModelCode: string
  payload: string
  payloadVersion: string
  signed: boolean
}

export const getRentalDeviceQr = (id: number) => {
  return request.get<RentalDeviceQrVO>({ url: '/rental/device/get-qr', params: { id } })
}

export const resolveRentalDeviceQr = (payload: string) => {
  return request.post<RentalDeviceVO>({ url: '/rental/device/resolve-qr', data: { payload } })
}

export interface RentalDeviceOpsResultVO {
  deviceId: number
  deviceNo: string
  deviceStatus: string
  assignmentId: number
  assignmentStatus: string
}

export const dispatchRentalDevice = (data: { deviceId: number; assignmentId?: number }) => {
  return request.post<RentalDeviceOpsResultVO>({ url: '/rental/device/dispatch', data })
}

export const returnRentalDevice = (data: {
  deviceId: number
  inspectPassed?: boolean
  note?: string
}) => {
  return request.post<RentalDeviceOpsResultVO>({ url: '/rental/device/return', data })
}

export const unassignRentalDevice = (data: { assignmentId: number }) => {
  return request.post<RentalDeviceOpsResultVO>({ url: '/rental/device/unassign', data })
}

export interface RentalDeviceGenerateFromPurchaseReqVO {
  purchaseInId: number
  purchaseInItemId?: number
  deviceNoPrefix?: string
  equipmentModelCode?: string
  warehouseCode?: string
}

export interface RentalDeviceGenerateFromPurchaseRespVO {
  purchaseInId: number
  purchaseInNo: string
  requestedCount: number
  alreadyExistedCount: number
  createdCount: number
  createdDeviceIds: number[]
  createdDeviceNos: string[]
}

export const generateDevicesFromPurchaseIn = (data: RentalDeviceGenerateFromPurchaseReqVO) => {
  return request.post<RentalDeviceGenerateFromPurchaseRespVO>({
    url: '/rental/device/generate-from-purchase-in',
    data
  })
}
