import { http } from '@/http/http'

export interface RentalDevice {
  id: number
  deviceNo: string
  serialNumber?: string
  equipmentModelCode: string
  status: string
  warehouseCode?: string
  enabled: boolean
}

export function resolveRentalDeviceQr(payload: string) {
  return http.post<RentalDevice>('/rental/device/resolve-qr', { payload })
}

export interface RentalDeviceOpsResult {
  deviceId: number
  deviceNo: string
  deviceStatus: string
  assignmentId: number
  assignmentStatus: string
}

export function returnRentalDevice(data: {
  deviceId?: number
  deviceNo?: string
  inspectPassed?: boolean
  note?: string
}) {
  return http.post<RentalDeviceOpsResult>('/rental/device/return', data)
}

export interface RentalDeviceScheduleDetail extends RentalDevice {
  inspectionState?: string
  maintenanceState?: string
  expectedReleaseDate?: string | number[]
  reasonCodes?: string[]
  currentAssignment?: {
    id?: number
    rentalOrderId?: number
    deviceNo?: string
    status?: string
    occupyStartDate?: string | number[]
    occupyEndDateExclusive?: string | number[]
    assignedAt?: string
  }
  schedules?: Array<{
    occupyStartDate?: string | number[]
    occupyEndDateExclusive?: string | number[]
    status?: string
    label?: string
  }>
  activeLocks?: Array<{ lockType?: string, reason?: string, status?: string }>
}

export function getDeviceScheduleDetail(id: number) {
  return http.get<RentalDeviceScheduleDetail>(`/rental/device/${id}/schedule-detail`)
}
