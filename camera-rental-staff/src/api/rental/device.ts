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
