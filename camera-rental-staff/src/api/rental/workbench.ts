import type { ApiDate } from './order'
import type { PageResult } from '@/http/types'
import { http } from '@/http/http'

export interface ScheduleSegment {
  scheduleId?: number
  assignmentId?: number
  rentalOrderId?: number
  orderNo?: string
  segmentType?: string
  status?: string
  occupyStartDate?: ApiDate
  occupyEndDateExclusive?: ApiDate
  billableStartDate?: ApiDate
  billableEndDate?: ApiDate
}
export interface DeviceLane {
  deviceId: number
  deviceNo: string
  equipmentModelCode: string
  warehouseCode?: string
  deviceStatus: string
  enabled: boolean
  logisticsStatus?: string
  expectedReleaseDate?: ApiDate
  segments: ScheduleSegment[]
}
export interface Workbench {
  window: { fromDate: ApiDate, toDateExclusive: ApiDate, timezone: string }
  metrics: { totalDevices: number, availableDevices: number, occupiedDevices: number }
  devicePage: PageResult<DeviceLane>
  exceptions: Array<{ code: string, message: string, deviceId?: number, deviceNo?: string, rentalOrderId?: number }>
}
export function getWorkbench(params: { pageNo: number, pageSize: number, fromDate: string, toDateExclusive: string, viewMode: string, keyword?: string, equipmentModelCode?: string, deviceStatus?: string, logisticsStatus?: string }) {
  return http.get<Workbench>('/rental/schedule/workbench', params)
}
