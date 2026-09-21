import { http } from '@/http/http'
import type { PageResult } from '@/http/types'

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

/** 永久二维码仍由服务端验签；手输完整编号走已有查询接口并精确匹配。 */
export async function lookupRentalDevice(payload: string): Promise<RentalDevice> {
  const value = payload.trim()
  if (value.startsWith('CRD1|'))
    return resolveRentalDeviceQr(value)
  if (!value || value.includes('|') || value.length > 100)
    throw new Error('请输入完整设备编号，或扫描设备永久二维码')
  for (let pageNo = 1; pageNo <= 20; pageNo++) {
    const page = await http.get<PageResult<RentalDevice>>('/rental/device/page', { keyword: value, pageNo, pageSize: 100 })
    const exact = (page.list || []).filter(item => item.deviceNo === value || item.serialNumber === value)
    if (exact.length === 1)
      return exact[0]
    if (exact.length > 1)
      throw new Error('编号对应多台设备，请扫描设备永久二维码')
    if (!page.list?.length || pageNo * 100 >= page.total)
      break
  }
  throw new Error('未找到完全匹配的设备，请核对完整编号或扫描永久二维码')
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
