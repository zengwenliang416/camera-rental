import request from '@/config/axios'

export interface DeviceImportRow {
  fileName: string
  lineNumber: number
  categoryCode: string
  equipmentModelCode: string
  deviceNo: string
  serialNumber: string
}
export interface DeviceImportResultRow extends DeviceImportRow {
  deviceId?: number
  status: 'MATCHED' | 'NEW' | 'DUPLICATE' | 'CONFLICT' | 'MISSING'
  reason: string
}
export interface DeviceImportPreview {
  batchId: string
  rows: DeviceImportResultRow[]
  canSubmit: boolean
}
export interface DeviceLabelRequest {
  deviceIds: number[]
  shopName: string
  phone: string
  locale: 'zh-CN' | 'en'
}
export const previewDeviceImport = (data: {
  mode: 'REPRINT' | 'CREATE'
  rows: DeviceImportRow[]
}) => request.post<DeviceImportPreview>({ url: '/rental/device/import/preview', data })
export const commitDeviceImport = (batchId: string) =>
  request.post<number[]>({ url: '/rental/device/import/commit', params: { batchId } })
export const previewDeviceLabels = (data: DeviceLabelRequest) =>
  request.post<{ deviceNo: string; imageDataUrl: string }>({
    url: '/rental/device/labels/preview',
    data
  })
export const downloadDeviceLabels = (data: DeviceLabelRequest) =>
  request.download<Blob>({
    url: '/rental/device/labels/download',
    method: 'POST',
    data,
    timeout: 120000
  })
