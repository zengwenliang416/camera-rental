import request from '@/config/axios'
import type { RentalScheduleDeviceLockVO, RentalScheduleSegmentVO } from '@/api/rental/schedule'
import type { RentalDeviceCategoryVO } from './catalog'

export type { RentalDeviceCategoryVO, RentalDeviceModelVO } from './catalog'

export interface RentalDeviceVO {
  id: number
  deviceNo: string
  serialNumber?: string
  categoryCode?: string
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
  serialNumber?: string
  categoryCode: string
  equipmentModelCode: string
  deviceNoSuffix: string
  status?: string
  warehouseCode?: string
  purchaseAmount?: number
  enabled?: boolean
}

export interface RentalDeviceUpdateReqVO {
  id: number
  serialNumber: string
  warehouseCode: string
  purchaseAmount?: number
  enabled: boolean
}

export interface RentalDeviceAssignReqVO {
  rentalOrderItemId: number
  deviceId: number
  occupyStartDate: string
  occupyEndDateExclusive: string
  idempotencyKey: string
}

export interface RentalDevicePageReqVO extends PageParam {
  categoryCode?: string
  equipmentModelCode?: string
}

export interface RentalDeviceCategoryCreateReqVO {
  categoryCode: string
  categoryName: string
  sortOrder?: number
}

export interface RentalDeviceModelCreateReqVO {
  categoryId: number
  modelCode: string
  modelName: string
  deviceNoPrefix: string
  sortOrder?: number
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

export const getRentalDeviceCatalog = () => {
  return request.get<RentalDeviceCategoryVO[]>({ url: '/rental/device/catalog' })
}

export const createRentalDeviceCategory = (data: RentalDeviceCategoryCreateReqVO) => {
  return request.post<number>({ url: '/rental/device/catalog/category/create', data })
}

export const createRentalDeviceModel = (data: RentalDeviceModelCreateReqVO) => {
  return request.post<number>({ url: '/rental/device/catalog/model/create', data })
}

export const getRentalDeviceDetail = (id: number) => {
  return request.get<RentalDeviceScheduleDetailVO>({
    url: `/rental/device/${id}/schedule-detail`
  })
}

export const createRentalDevice = (data: RentalDeviceCreateReqVO) => {
  return request.post<number>({ url: '/rental/device/create', data })
}

export const updateRentalDevice = (data: RentalDeviceUpdateReqVO) => {
  return request.put<boolean>({ url: '/rental/device/update', data })
}

export const deleteRentalDevice = (id: number) => {
  return request.delete<boolean>({ url: '/rental/device/delete', params: { id } })
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
