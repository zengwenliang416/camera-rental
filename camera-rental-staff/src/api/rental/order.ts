import type { PageResult } from '@/http/types'
import { http } from '@/http/http'

/** 后端 LocalDate 可能是 'yyyy-MM-dd' 或 [year, month, day] */
export type ApiDate = string | number[]

/** 对齐 RentalPendingAllocationItemRespVO / RentalScheduleOrderItemRespVO */
export interface RentalOrderItem {
  id: number
  rentalOrderId?: number
  equipmentModelCode?: string
  requiredQuantity?: number
  assignedQuantity?: number
  remainingQuantity?: number
  assignments?: Array<{ id: number, deviceId: number, deviceNo: string, status: string }>
  rentAmount?: number
  billableStartDate?: ApiDate
  billableEndDate?: ApiDate
  occupyStartDate?: ApiDate
  occupyEndDateExclusive?: ApiDate
}

/** 对齐 RentalPendingAllocationOrderRespVO */
export interface PendingAllocationOrder {
  id: number
  orderNo?: string
  externalOrderNo?: string
  goodsTitle?: string
  receiverName?: string
  receiverMobile?: string
  receiverAddress?: string
  preparationStatus?: string
  preparationReasonCode?: string
  conversionStatus?: string
  shippingStatus?: string
  channelOrderId?: number
  sourceType?: string
  sourceOrderId?: string
  status?: string
  rentAmount?: number
  refundAmount?: number
  billableStartDate?: ApiDate
  billableEndDate?: ApiDate
  occupyStartDate?: ApiDate
  occupyEndDateExclusive?: ApiDate
  requiredQuantity?: number
  assignedQuantity?: number
  remainingQuantity?: number
  items?: RentalOrderItem[]
}

export interface RentalOrderScheduleDetail extends PendingAllocationOrder {
  buyerNick?: string
  receiverName?: string
  receiverMobile?: string
  receiverAddress?: string
  riskCodes?: string[]
  deliveries?: Array<{
    direction?: string
    sourceCarrierName?: string
    trackingStatus?: string
    stale?: boolean
    latestEventTime?: string
    estimatedDeliveryAt?: string
  }>
}

export function getPendingAllocationOrders(params: { pageNo?: number, pageSize?: number, keyword?: string } = {}) {
  const keyword = params.keyword?.trim()
  return http.get<PageResult<PendingAllocationOrder>>(
    '/rental/order/pending-allocation-page',
    {
      pageNo: params.pageNo || 1,
      pageSize: params.pageSize || 20,
      orderNo: keyword || undefined,
    },
  )
}

export function getOrderScheduleDetail(id: number) {
  return http.get<RentalOrderScheduleDetail>(`/rental/order/${id}`)
}

export interface DeviceCandidate {
  id: number
  deviceNo?: string
  serialNumber?: string
  equipmentModelCode?: string
  status?: string
  enabled?: boolean
  eligible?: boolean
  reasonCodes?: string[]
  occupyStartDate?: ApiDate
  occupyEndDateExclusive?: ApiDate
}

export interface DeviceCandidateResult {
  rentalOrderId?: number
  rentalOrderItemId?: number
  orderNo?: string
  externalOrderNo?: string
  equipmentModelCode?: string
  requiredQuantity?: number
  assignedQuantity?: number
  remainingQuantity?: number
  occupyStartDate?: ApiDate
  occupyEndDateExclusive?: ApiDate
  reasonCodes?: string[]
  candidates?: DeviceCandidate[]
}

export function getDeviceCandidates(itemId: number) {
  return http.get<DeviceCandidateResult>(`/rental/order-item/${itemId}/device-candidates`)
}

export function assignRentalDevice(data: { rentalOrderItemId: number, deviceId: number, occupyStartDate: string, occupyEndDateExclusive: string, idempotencyKey: string }) {
  return http<Record<string, unknown>>({ url: '/rental/device/assign', method: 'POST', data })
}

export type StaffOrderQueue = 'ALL' | 'PENDING_ALLOCATION' | 'UNSHIPPED' | 'PARTIAL' | 'SHIPPED'

export function getStaffOrders(params: { pageNo: number, pageSize: number, keyword?: string, queue: StaffOrderQueue }) {
  return http.get<PageResult<PendingAllocationOrder>>('/rental/order/staff-page', params)
}

export function updateDeviceQuantity(itemId: number, quantity: number, expectedQuantity: number) {
  return http.put<number>(`/rental/order-item/${itemId}/quantity`, { quantity, expectedQuantity })
}

/** Read-only channel records: id is not an internal rental order ID. */
export type StaffChannelOrder = Omit<PendingAllocationOrder, 'id'> & { channelOrderId: number }
export function getStaffChannelOrders(params: { pageNo: number, pageSize: number, keyword?: string }) {
  return http.get<PageResult<StaffChannelOrder>>('/rental/order/staff-channel-page', params)
}
