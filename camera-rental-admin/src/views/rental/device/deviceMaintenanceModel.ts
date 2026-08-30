import type {
  RentalDeviceUpdateReqVO,
  RentalDeviceVO
} from '@/api/rental/device'

export interface DeviceMaintenanceForm {
  id: number
  serialNumber: string
  warehouseCode: string
  purchaseAmountYuan?: number
  enabled: boolean
}

export const createDeviceMaintenanceForm = (
  device: RentalDeviceVO
): DeviceMaintenanceForm => ({
  id: device.id,
  serialNumber: device.serialNumber ?? '',
  warehouseCode: device.warehouseCode ?? '',
  purchaseAmountYuan:
    device.purchaseAmount === undefined ? undefined : device.purchaseAmount / 100,
  enabled: device.enabled
})

export const buildDeviceUpdatePayload = (
  form: DeviceMaintenanceForm
): RentalDeviceUpdateReqVO => {
  const purchaseAmount =
    form.purchaseAmountYuan === undefined
      ? undefined
      : Math.round(form.purchaseAmountYuan * 100)
  if (
    purchaseAmount !== undefined &&
    (!Number.isSafeInteger(purchaseAmount) || purchaseAmount < 0)
  ) {
    throw new RangeError('purchaseAmount must be a non-negative integer number of cents')
  }
  return {
    id: form.id,
    serialNumber: form.serialNumber.trim(),
    warehouseCode: form.warehouseCode.trim(),
    purchaseAmount,
    enabled: form.enabled
  }
}

export const formatPurchaseAmount = (purchaseAmount?: number) =>
  purchaseAmount === undefined ? '-' : (purchaseAmount / 100).toFixed(2)
