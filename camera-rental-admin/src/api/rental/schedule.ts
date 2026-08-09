import request from '@/config/axios'

export const SCHEDULE_WINDOW_DAYS = [14, 30, 90] as const
export type ScheduleWindowDays = (typeof SCHEDULE_WINDOW_DAYS)[number]

export type ScheduleSegmentKind =
  | 'OCCUPIED'
  | 'OUTBOUND_TRANSIT'
  | 'CUSTOMER_POSSESSION'
  | 'RETURN_TRANSIT'
  | 'RETURN_INSPECTION'
  | 'LOCKED'

export interface RentalScheduleWindowVO {
  fromDate: string
  toDateExclusive: string
  viewMode: '14D' | '30D' | '90D'
  dayCount: ScheduleWindowDays
  timezone: string
}

export interface RentalScheduleWorkbenchReqVO extends PageParam {
  fromDate: string
  toDateExclusive: string
  viewMode: '14D' | '30D' | '90D'
  keyword?: string
  equipmentModelCode?: string
  deviceStatus?: string
  logisticsStatus?: string
}

export interface RentalScheduleMetricsVO {
  totalDevices: number
  availableDevices: number
  occupiedDevices: number
  inTransitDevices: number
  pendingAllocationCount: number
  pendingAllocationOrders: number
  pendingAllocationItems: number
  exceptionCount: number
  occupiedDeviceDays: number
  totalDeviceDays: number
  utilizationRate: number
}

export interface RentalScheduleDeviceLockVO {
  id: number
  deviceId: number
  lockType: string
  reason: string
  rentalOrderId?: number
  rentalOrderItemId?: number
  sourceType?: string
  startTime?: string
  plannedEndTime?: string
  releasedAt?: string
  releasedBy?: number
  releaseReason?: string
  status: string
}

export interface RentalScheduleSegmentVO {
  id?: number
  scheduleId?: number
  occupyStartDate: string
  occupyEndDateExclusive: string
  billableStartDate?: string
  billableEndDate?: string
  status?: string
  segmentType?: ScheduleSegmentKind | string
  scheduleType?: string
  label?: string
  rentalOrderId?: number
  rentalOrderItemId?: number
  logisticsStatus?: string
  lockType?: string
  expectedReleaseAt?: string
  displayStartDate?: string
  displayEndDateExclusive?: string
  leftContinuation?: boolean
  rightContinuation?: boolean
}

export interface RentalScheduleDeviceLaneVO {
  deviceId: number
  deviceNo: string
  serialNumber?: string
  equipmentModelCode: string
  deviceStatus: string
  logisticsStatus?: string
  enabled: boolean
  expectedReleaseDate?: string
  occupied?: boolean
  segments: RentalScheduleSegmentVO[]
}

export interface RentalSchedulePendingAllocationVO {
  rentalOrderId: number
  rentalOrderItemId: number
  orderNo?: string
  orderStatus?: string
  equipmentModelCode: string
  requiredQuantity: number
  assignedQuantity: number
  remainingQuantity: number
  billableStartDate?: string
  billableEndDate?: string
  occupyStartDate: string
  occupyEndDateExclusive: string
}

export interface RentalScheduleExceptionVO {
  code: string
  severity: string
  message: string
  nextAction?: string
  deviceId?: number
  deviceNo?: string
  rentalOrderId?: number
  rentalOrderItemId?: number
  sourceType?: string
  sourceId?: string
  expectedReleaseDate?: string
}

export interface RentalScheduleWorkbenchVO {
  window: RentalScheduleWindowVO
  metrics: RentalScheduleMetricsVO
  devicePage: PageResult<RentalScheduleDeviceLaneVO[]>
  pendingAllocations?: RentalSchedulePendingAllocationVO[]
  exceptions: RentalScheduleExceptionVO[]
}

export interface RentalSchedulePendingAllocationPageReqVO extends PageParam {
  orderNo?: string
  equipmentModelCode?: string
}

export interface RentalScheduleAssignmentVO {
  id: number
  rentalOrderId: number
  rentalOrderItemId: number
  deviceId: number
  deviceNo?: string
  serialNumber?: string
  deviceStatus?: string
  deviceEnabled?: boolean
  status: string
  scheduleId?: number
  scheduleStatus?: string
  occupyStartDate: string
  occupyEndDateExclusive: string
  assignedAt?: string
}

export interface RentalScheduleOrderItemVO {
  id: number
  rentalOrderId: number
  equipmentModelCode: string
  requiredQuantity: number
  assignedQuantity: number
  remainingQuantity: number
  rentAmount?: number
  billableStartDate?: string
  billableEndDate?: string
  occupyStartDate?: string
  occupyEndDateExclusive?: string
  assignments: RentalScheduleAssignmentVO[]
}

export interface RentalScheduleOrderDetailVO {
  id: number
  orderNo?: string
  externalOrderNo?: string
  sourceType?: string
  sourceOrderId?: string
  status: string
  rentAmount?: number
  refundAmount?: number
  billableStartDate?: string
  billableEndDate?: string
  occupyStartDate?: string
  occupyEndDateExclusive?: string
  requiredQuantity: number
  assignedQuantity: number
  remainingQuantity: number
  riskCodes: string[]
  items: RentalScheduleOrderItemVO[]
  deliveries: RentalScheduleDeliveryVO[]
}

export interface RentalPendingAllocationItemVO {
  id: number
  rentalOrderId: number
  equipmentModelCode: string
  requiredQuantity: number
  assignedQuantity: number
  remainingQuantity: number
  rentAmount?: number
  billableStartDate?: string
  billableEndDate?: string
  occupyStartDate: string
  occupyEndDateExclusive: string
}

export interface RentalPendingAllocationOrderVO {
  id: number
  orderNo?: string
  externalOrderNo?: string
  sourceType?: string
  sourceOrderId?: string
  status: string
  rentAmount?: number
  refundAmount?: number
  billableStartDate?: string
  billableEndDate?: string
  occupyStartDate?: string
  occupyEndDateExclusive?: string
  requiredQuantity: number
  assignedQuantity: number
  remainingQuantity: number
  items: RentalPendingAllocationItemVO[]
}

export interface RentalScheduleDeliveryVO {
  id: number
  rentalOrderId?: number
  direction: string
  packageSeq?: number
  sourceCarrierName?: string
  trackingStatus?: string
  mappingStatus?: string
  subscribeStatus?: string
  queryStatus?: string
  latestEventTime?: string
  lastSyncedAt?: string
  estimatedDeliveryAt?: string
  stale?: boolean
  deviceIds?: number[]
}

export interface RentalScheduleCandidateVO {
  id: number
  deviceNo: string
  serialNumber?: string
  equipmentModelCode: string
  eligible: boolean
  reasonCodes: string[]
  status: string
  enabled: boolean
  neighboringSchedules?: RentalScheduleSegmentVO[]
  nextAvailableDate?: string
  activeLocks?: RentalScheduleDeviceLockVO[]
  logistics?: RentalScheduleDeliveryVO[]
}

export interface RentalScheduleCandidateResponseVO {
  rentalOrderId: number
  rentalOrderItemId: number
  orderNo?: string
  externalOrderNo?: string
  equipmentModelCode: string
  requiredQuantity: number
  assignedQuantity: number
  remainingQuantity: number
  occupyStartDate: string
  occupyEndDateExclusive: string
  reasonCodes?: string[]
  candidates: RentalScheduleCandidateVO[]
}

export interface RentalDeviceLockCreateReqVO {
  deviceId: number
  lockType: 'ORDER_HOLD' | 'MANUAL_HOLD'
  reason: string
  rentalOrderId?: number
  rentalOrderItemId?: number
  plannedEndTime: string
}

export interface RentalDeviceLockReleaseReqVO {
  reason: string
}

export const getRentalScheduleWorkbench = (params: RentalScheduleWorkbenchReqVO) => {
  return request.get<RentalScheduleWorkbenchVO>({
    url: '/rental/schedule/workbench',
    params
  })
}

export const getRentalPendingAllocationOrders = (
  params: RentalSchedulePendingAllocationPageReqVO
) => {
  return request.get<PageResult<RentalPendingAllocationOrderVO[]>>({
    url: '/rental/order/pending-allocation-page',
    params
  })
}

export const getRentalOrderDetail = (id: number) => {
  return request.get<RentalScheduleOrderDetailVO>({
    url: `/rental/order/${id}`
  })
}

export const getRentalDeviceCandidates = (rentalOrderItemId: number) => {
  return request.get<RentalScheduleCandidateResponseVO>({
    url: `/rental/order-item/${rentalOrderItemId}/device-candidates`
  })
}

export const createRentalDeviceLock = (data: RentalDeviceLockCreateReqVO) => {
  return request.post<RentalScheduleDeviceLockVO>({
    url: '/rental/device-lock',
    data
  })
}

export const releaseRentalDeviceLock = (id: number, data: RentalDeviceLockReleaseReqVO) => {
  return request.put<RentalScheduleDeviceLockVO>({
    url: `/rental/device-lock/${id}/release`,
    data
  })
}
