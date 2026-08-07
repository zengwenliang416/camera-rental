import assert from 'node:assert/strict'
import test from 'node:test'
import {
  XIANYU_ORDER_COLUMNS,
  XIANYU_ORDER_DEFAULT_VISIBLE_KEYS,
  resetXianyuOrderColumnKeys,
  sanitizePersistedXianyuOrderColumnKeys,
  selectAllXianyuOrderColumnKeys
} from '../src/views/rental/order/components/xianyuOrderColumns.ts'

const STANDARDIZED_XIANYU_ORDER_KEYS = [
  'id',
  'shopId',
  'externalOrderId',
  'externalProductId',
  'externalSkuId',
  'orderStatus',
  'payAmount',
  'currency',
  'sellerRemark',
  'receiverName',
  'receiverMobile',
  'receiverAddress',
  'buyerNick',
  'remarkParseVersion',
  'remarkParseStatus',
  'remarkParseSource',
  'remarkParseConfidence',
  'remarkParseModel',
  'shipDate',
  'billableStartDate',
  'billableEndDate',
  'rentalPeriodStatus',
  'rentalPeriodReasonCode',
  'conversionStatus',
  'rentalOrderId',
  'sourceCreatedAt',
  'sourceUpdatedAt',
  'orderType',
  'orderTime',
  'totalAmount',
  'payTime',
  'refundStatus',
  'refundAmount',
  'refundTime',
  'expressCode',
  'expressName',
  'waybillNo',
  'expressFee',
  'consignType',
  'consignTime',
  'confirmTime',
  'cancelReason',
  'cancelTime',
  'sellerName',
  'goodsTitle',
  'goodsQuantity',
  'goodsPrice',
  'xybSellerAmount',
  'taxIncluded',
  'idleBizType',
  'pinGroupStatus',
  'rentalOrderItemId',
  'equipmentModelCode',
  'rentalQuantity',
  'occupyStartDate',
  'occupyEndDateExclusive',
  'assignedDeviceIds'
]

const CURRENT_PAGE_DEFAULT_KEYS = [
  'id',
  'shopId',
  'shopName',
  'externalOrderId',
  'goodsTitle',
  'orderStatus',
  'shipDate',
  'payAmount',
  'receiverName',
  'receiverMobile',
  'receiverAddress',
  'buyerNick',
  'expressName',
  'conversionStatus',
  'sellerRemark',
  'remarkParseStatus'
]

test('column metadata covers every standardized XianyuOrderVO field', () => {
  const columnKeys = XIANYU_ORDER_COLUMNS.map((column) => column.key)

  assert.deepEqual(
    columnKeys.filter((key) => key !== 'shopName').sort(),
    [...STANDARDIZED_XIANYU_ORDER_KEYS].sort()
  )
  assert.equal(new Set(columnKeys).size, columnKeys.length)
  assert.equal(columnKeys.includes('action' as never), false)

  for (const rawKey of ['rawDetail', 'detailJson', 'rawPayload', 'payload']) {
    assert.equal(columnKeys.includes(rawKey as never), false)
  }

  for (const column of XIANYU_ORDER_COLUMNS) {
    assert.equal(typeof column.label, 'string')
    assert.ok(column.label.length > 0)
    assert.equal(typeof column.width, 'number')
    assert.ok(column.group)
    assert.ok(column.format)
  }
})

test('current order page columns are visible by default and ID is locked', () => {
  assert.deepEqual(
    [...XIANYU_ORDER_DEFAULT_VISIBLE_KEYS].sort(),
    [...CURRENT_PAGE_DEFAULT_KEYS].sort()
  )
  assert.equal(XIANYU_ORDER_COLUMNS.find((column) => column.key === 'id')?.locked, true)
})

test('persisted column keys are sanitized, deduplicated, ordered, and keep locked columns', () => {
  assert.deepEqual(
    sanitizePersistedXianyuOrderColumnKeys([
      'goodsTitle',
      'goodsTitle',
      'action',
      'rawPayload',
      'id'
    ]),
    ['id', 'goodsTitle']
  )
  assert.deepEqual(sanitizePersistedXianyuOrderColumnKeys('["goodsTitle", "id", "goodsTitle"]'), [
    'id',
    'goodsTitle'
  ])
  assert.deepEqual(sanitizePersistedXianyuOrderColumnKeys([]), ['id'])
  assert.deepEqual(sanitizePersistedXianyuOrderColumnKeys(['not-a-column']), [
    ...XIANYU_ORDER_DEFAULT_VISIBLE_KEYS
  ])
  assert.deepEqual(sanitizePersistedXianyuOrderColumnKeys('not-json'), [
    ...XIANYU_ORDER_DEFAULT_VISIBLE_KEYS
  ])
})

test('select all and reset default return fresh pure results', () => {
  const allKeys = selectAllXianyuOrderColumnKeys()
  assert.deepEqual(
    allKeys,
    XIANYU_ORDER_COLUMNS.map((column) => column.key)
  )

  const resetKeys = resetXianyuOrderColumnKeys()
  assert.deepEqual(resetKeys, XIANYU_ORDER_DEFAULT_VISIBLE_KEYS)

  allKeys.pop()
  resetKeys.pop()
  assert.equal(selectAllXianyuOrderColumnKeys().length, XIANYU_ORDER_COLUMNS.length)
  assert.equal(resetXianyuOrderColumnKeys().length, XIANYU_ORDER_DEFAULT_VISIBLE_KEYS.length)
})
