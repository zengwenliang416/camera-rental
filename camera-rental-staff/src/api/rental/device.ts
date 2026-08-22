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
