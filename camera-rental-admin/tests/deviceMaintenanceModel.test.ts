import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildDeviceUpdatePayload,
  createDeviceMaintenanceForm,
  formatPurchaseAmount
} from '../src/views/rental/device/deviceMaintenanceModel.ts'

const device = {
  id: 8,
  deviceNo: 'P4P-08',
  serialNumber: ' SN-8 ',
  categoryCode: 'DJI',
  equipmentModelCode: 'P4P',
  status: 'AVAILABLE',
  warehouseCode: ' WH-A ',
  purchaseAmount: 12345,
  enabled: true
}

test('device edit form converts integer cents to display yuan', () => {
  assert.deepEqual(createDeviceMaintenanceForm(device), {
    id: 8,
    serialNumber: ' SN-8 ',
    warehouseCode: ' WH-A ',
    purchaseAmountYuan: 123.45,
    enabled: true
  })
  assert.equal(formatPurchaseAmount(12345), '123.45')
  assert.equal(formatPurchaseAmount(), '-')
})

test('device update payload contains only mutable fields and integer cents', () => {
  assert.deepEqual(
    buildDeviceUpdatePayload({
      id: 8,
      serialNumber: '  SN-NEW  ',
      warehouseCode: '  WH-B  ',
      purchaseAmountYuan: 88.09,
      enabled: false
    }),
    {
      id: 8,
      serialNumber: 'SN-NEW',
      warehouseCode: 'WH-B',
      purchaseAmount: 8809,
      enabled: false
    }
  )
})

test('empty optional values stay empty for server-side null normalization', () => {
  assert.deepEqual(
    buildDeviceUpdatePayload({
      id: 8,
      serialNumber: '   ',
      warehouseCode: '',
      purchaseAmountYuan: undefined,
      enabled: true
    }),
    {
      id: 8,
      serialNumber: '',
      warehouseCode: '',
      purchaseAmount: undefined,
      enabled: true
    }
  )
})

test('invalid purchase amount is rejected before the request', () => {
  assert.throws(
    () =>
      buildDeviceUpdatePayload({
        id: 8,
        serialNumber: '',
        warehouseCode: '',
        purchaseAmountYuan: -1,
        enabled: true
      }),
    RangeError
  )
})
