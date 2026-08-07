import type { XianyuOrderVO } from '../../../../api/rental/xianyu'

export type XianyuOrderRawFieldKey =
  | 'detailJson'
  | 'detailJsonId'
  | 'detailPayload'
  | 'payload'
  | 'payloadHash'
  | 'rawDetail'
  | 'rawDetailJson'
  | 'rawPayload'
  | 'rawPayloadId'

export type XianyuOrderColumnKey = Exclude<keyof XianyuOrderVO, XianyuOrderRawFieldKey> | 'shopName'

export type XianyuOrderColumnGroup =
  | 'identity'
  | 'product'
  | 'amount'
  | 'status'
  | 'receiver'
  | 'rental'
  | 'logistics'
  | 'remark'
  | 'time'

export type XianyuOrderColumnFormat =
  | 'text'
  | 'integer'
  | 'amount-fen'
  | 'date'
  | 'datetime'
  | 'status'
  | 'boolean'
  | 'confidence'
  | 'array'

export interface XianyuOrderColumnDefinition {
  key: XianyuOrderColumnKey
  group: XianyuOrderColumnGroup
  labelKey?: string
  label: string
  width: number
  minWidth?: number
  format: XianyuOrderColumnFormat
  defaultVisible: boolean
  locked?: boolean
}

export interface XianyuOrderColumnGroupDefinition {
  key: XianyuOrderColumnGroup
  label: string
}

export const XIANYU_ORDER_COLUMN_GROUPS: readonly XianyuOrderColumnGroupDefinition[] = [
  { key: 'identity', label: '订单识别' },
  { key: 'product', label: '商品与渠道' },
  { key: 'amount', label: '金额' },
  { key: 'status', label: '状态' },
  { key: 'receiver', label: '收货信息' },
  { key: 'rental', label: '租赁关联' },
  { key: 'logistics', label: '物流' },
  { key: 'remark', label: '备注解析' },
  { key: 'time', label: '时间' }
]

const XIANYU_ORDER_COLUMN_MAP = {
  id: {
    key: 'id',
    group: 'identity',
    label: 'ID',
    width: 80,
    format: 'integer',
    defaultVisible: true,
    locked: true
  },
  shopId: {
    key: 'shopId',
    group: 'identity',
    labelKey: 'rental.order.shopId',
    label: '店铺 ID',
    width: 90,
    format: 'integer',
    defaultVisible: true
  },
  shopName: {
    key: 'shopName',
    group: 'identity',
    labelKey: 'rental.xianyu.shopName',
    label: '店铺名称',
    width: 110,
    minWidth: 110,
    format: 'text',
    defaultVisible: true
  },
  externalOrderId: {
    key: 'externalOrderId',
    group: 'identity',
    labelKey: 'rental.order.externalOrderId',
    label: '外部订单号',
    width: 180,
    minWidth: 180,
    format: 'text',
    defaultVisible: true
  },
  externalProductId: {
    key: 'externalProductId',
    group: 'identity',
    labelKey: 'rental.order.externalProductId',
    label: '外部商品 ID',
    width: 160,
    format: 'text',
    defaultVisible: false
  },
  externalSkuId: {
    key: 'externalSkuId',
    group: 'identity',
    labelKey: 'rental.order.externalSkuId',
    label: '外部 SKU ID',
    width: 150,
    format: 'text',
    defaultVisible: false
  },
  orderStatus: {
    key: 'orderStatus',
    group: 'status',
    labelKey: 'rental.order.orderStatus',
    label: '订单状态',
    width: 110,
    format: 'status',
    defaultVisible: true
  },
  payAmount: {
    key: 'payAmount',
    group: 'amount',
    labelKey: 'rental.order.payAmountFen',
    label: '实付（分）',
    width: 130,
    format: 'amount-fen',
    defaultVisible: true
  },
  currency: {
    key: 'currency',
    group: 'amount',
    label: '币种',
    width: 90,
    format: 'text',
    defaultVisible: false
  },
  sellerRemark: {
    key: 'sellerRemark',
    group: 'remark',
    labelKey: 'rental.order.sellerRemark',
    label: '卖家备注',
    width: 180,
    minWidth: 140,
    format: 'text',
    defaultVisible: true
  },
  receiverName: {
    key: 'receiverName',
    group: 'receiver',
    labelKey: 'rental.order.receiverName',
    label: '收货人',
    width: 100,
    format: 'text',
    defaultVisible: true
  },
  receiverMobile: {
    key: 'receiverMobile',
    group: 'receiver',
    labelKey: 'rental.order.receiverMobile',
    label: '收货电话',
    width: 140,
    format: 'text',
    defaultVisible: true
  },
  receiverAddress: {
    key: 'receiverAddress',
    group: 'receiver',
    labelKey: 'rental.order.receiverAddress',
    label: '收货地址',
    width: 180,
    minWidth: 180,
    format: 'text',
    defaultVisible: true
  },
  buyerNick: {
    key: 'buyerNick',
    group: 'identity',
    labelKey: 'rental.xianyu.buyerNick',
    label: '买家昵称',
    width: 110,
    minWidth: 110,
    format: 'text',
    defaultVisible: true
  },
  remarkParseVersion: {
    key: 'remarkParseVersion',
    group: 'remark',
    labelKey: 'rental.order.remarkParseVersion',
    label: '解析版本',
    width: 150,
    format: 'text',
    defaultVisible: false
  },
  remarkParseStatus: {
    key: 'remarkParseStatus',
    group: 'remark',
    labelKey: 'rental.order.remarkParseStatus',
    label: '备注解析',
    width: 150,
    format: 'status',
    defaultVisible: true
  },
  remarkParseSource: {
    key: 'remarkParseSource',
    group: 'remark',
    label: '解析来源',
    width: 100,
    format: 'text',
    defaultVisible: false
  },
  remarkParseConfidence: {
    key: 'remarkParseConfidence',
    group: 'remark',
    label: '解析置信度',
    width: 110,
    format: 'confidence',
    defaultVisible: false
  },
  remarkParseModel: {
    key: 'remarkParseModel',
    group: 'remark',
    label: '解析模型',
    width: 150,
    format: 'text',
    defaultVisible: false
  },
  shipDate: {
    key: 'shipDate',
    group: 'rental',
    labelKey: 'rental.order.shipDate',
    label: '发货日期',
    width: 120,
    format: 'date',
    defaultVisible: true
  },
  billableStartDate: {
    key: 'billableStartDate',
    group: 'rental',
    labelKey: 'rental.schedule.billableStartDate',
    label: '计租开始日',
    width: 120,
    format: 'date',
    defaultVisible: false
  },
  billableEndDate: {
    key: 'billableEndDate',
    group: 'rental',
    labelKey: 'rental.schedule.billableEndDate',
    label: '计租结束日',
    width: 120,
    format: 'date',
    defaultVisible: false
  },
  rentalPeriodStatus: {
    key: 'rentalPeriodStatus',
    group: 'status',
    label: '租期状态',
    width: 130,
    format: 'status',
    defaultVisible: false
  },
  rentalPeriodReasonCode: {
    key: 'rentalPeriodReasonCode',
    group: 'remark',
    label: '租期原因',
    width: 180,
    minWidth: 180,
    format: 'text',
    defaultVisible: false
  },
  conversionStatus: {
    key: 'conversionStatus',
    group: 'status',
    labelKey: 'rental.order.conversionStatus',
    label: '转换状态',
    width: 120,
    format: 'status',
    defaultVisible: true
  },
  rentalOrderId: {
    key: 'rentalOrderId',
    group: 'rental',
    labelKey: 'rental.order.rentalOrderId',
    label: '内部租赁单 ID',
    width: 130,
    format: 'integer',
    defaultVisible: false
  },
  sourceCreatedAt: {
    key: 'sourceCreatedAt',
    group: 'time',
    label: '源创建时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  sourceUpdatedAt: {
    key: 'sourceUpdatedAt',
    group: 'time',
    labelKey: 'rental.xianyu.sourceUpdatedAt',
    label: '源更新时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  orderType: {
    key: 'orderType',
    group: 'product',
    label: '订单类型',
    width: 100,
    format: 'integer',
    defaultVisible: false
  },
  orderTime: {
    key: 'orderTime',
    group: 'time',
    labelKey: 'rental.order.orderTime',
    label: '下单时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  totalAmount: {
    key: 'totalAmount',
    group: 'amount',
    labelKey: 'rental.order.totalAmount',
    label: '订单原价（分）',
    width: 130,
    format: 'amount-fen',
    defaultVisible: false
  },
  payTime: {
    key: 'payTime',
    group: 'time',
    label: '支付时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  refundStatus: {
    key: 'refundStatus',
    group: 'status',
    label: '退款状态',
    width: 110,
    format: 'status',
    defaultVisible: false
  },
  refundAmount: {
    key: 'refundAmount',
    group: 'amount',
    labelKey: 'rental.order.refundAmount',
    label: '退款金额（分）',
    width: 130,
    format: 'amount-fen',
    defaultVisible: false
  },
  refundTime: {
    key: 'refundTime',
    group: 'time',
    label: '退款时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  expressCode: {
    key: 'expressCode',
    group: 'logistics',
    labelKey: 'rental.xianyu.expressCode',
    label: '快递代码',
    width: 110,
    format: 'text',
    defaultVisible: false
  },
  expressName: {
    key: 'expressName',
    group: 'logistics',
    labelKey: 'rental.order.expressName',
    label: '快递',
    width: 100,
    format: 'text',
    defaultVisible: true
  },
  waybillNo: {
    key: 'waybillNo',
    group: 'logistics',
    labelKey: 'rental.xianyu.waybillNo',
    label: '运单号',
    width: 150,
    format: 'text',
    defaultVisible: false
  },
  expressFee: {
    key: 'expressFee',
    group: 'amount',
    label: '运费（分）',
    width: 130,
    format: 'amount-fen',
    defaultVisible: false
  },
  consignType: {
    key: 'consignType',
    group: 'logistics',
    label: '发货类型',
    width: 110,
    format: 'integer',
    defaultVisible: false
  },
  consignTime: {
    key: 'consignTime',
    group: 'time',
    labelKey: 'rental.order.consignTime',
    label: '发货时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  confirmTime: {
    key: 'confirmTime',
    group: 'time',
    label: '确认时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  cancelReason: {
    key: 'cancelReason',
    group: 'logistics',
    label: '取消原因',
    width: 180,
    minWidth: 180,
    format: 'text',
    defaultVisible: false
  },
  cancelTime: {
    key: 'cancelTime',
    group: 'time',
    label: '取消时间',
    width: 180,
    format: 'datetime',
    defaultVisible: false
  },
  sellerName: {
    key: 'sellerName',
    group: 'identity',
    label: '卖家名称',
    width: 120,
    format: 'text',
    defaultVisible: false
  },
  goodsTitle: {
    key: 'goodsTitle',
    group: 'product',
    labelKey: 'rental.order.goodsTitle',
    label: '商品标题',
    width: 180,
    minWidth: 140,
    format: 'text',
    defaultVisible: true
  },
  goodsQuantity: {
    key: 'goodsQuantity',
    group: 'product',
    labelKey: 'rental.order.goodsQuantity',
    label: '商品数量',
    width: 100,
    format: 'integer',
    defaultVisible: false
  },
  goodsPrice: {
    key: 'goodsPrice',
    group: 'amount',
    label: '商品单价（分）',
    width: 130,
    format: 'amount-fen',
    defaultVisible: false
  },
  xybSellerAmount: {
    key: 'xybSellerAmount',
    group: 'amount',
    label: '卖家到账金额（分）',
    width: 150,
    format: 'amount-fen',
    defaultVisible: false
  },
  taxIncluded: {
    key: 'taxIncluded',
    group: 'product',
    label: '含税',
    width: 100,
    format: 'boolean',
    defaultVisible: false
  },
  idleBizType: {
    key: 'idleBizType',
    group: 'product',
    label: '闲鱼业务类型',
    width: 130,
    format: 'integer',
    defaultVisible: false
  },
  pinGroupStatus: {
    key: 'pinGroupStatus',
    group: 'product',
    label: '拼团状态',
    width: 110,
    format: 'integer',
    defaultVisible: false
  },
  rentalOrderItemId: {
    key: 'rentalOrderItemId',
    group: 'rental',
    labelKey: 'rental.schedule.rentalOrderItemId',
    label: '订单明细 ID',
    width: 130,
    format: 'integer',
    defaultVisible: false
  },
  equipmentModelCode: {
    key: 'equipmentModelCode',
    group: 'rental',
    labelKey: 'rental.order.equipmentModelCode',
    label: '设备型号',
    width: 140,
    format: 'text',
    defaultVisible: false
  },
  rentalQuantity: {
    key: 'rentalQuantity',
    group: 'rental',
    label: '租赁数量',
    width: 100,
    format: 'integer',
    defaultVisible: false
  },
  occupyStartDate: {
    key: 'occupyStartDate',
    group: 'rental',
    labelKey: 'rental.schedule.occupyStartDate',
    label: '占用开始日',
    width: 120,
    format: 'date',
    defaultVisible: false
  },
  occupyEndDateExclusive: {
    key: 'occupyEndDateExclusive',
    group: 'rental',
    labelKey: 'rental.schedule.occupyEndDateExclusive',
    label: '占用结束日（不含）',
    width: 150,
    format: 'date',
    defaultVisible: false
  },
  assignedDeviceIds: {
    key: 'assignedDeviceIds',
    group: 'rental',
    labelKey: 'rental.order.assignedDevices',
    label: '已分配设备 ID',
    width: 160,
    format: 'array',
    defaultVisible: false
  }
} satisfies Record<XianyuOrderColumnKey, XianyuOrderColumnDefinition>

export const XIANYU_ORDER_COLUMNS: readonly XianyuOrderColumnDefinition[] =
  Object.values(XIANYU_ORDER_COLUMN_MAP)

export const XIANYU_ORDER_COLUMN_KEYS: readonly XianyuOrderColumnKey[] = XIANYU_ORDER_COLUMNS.map(
  (column) => column.key
)

export const XIANYU_ORDER_DEFAULT_VISIBLE_KEYS: readonly XianyuOrderColumnKey[] =
  XIANYU_ORDER_COLUMNS.filter((column) => column.defaultVisible).map((column) => column.key)

const XIANYU_ORDER_COLUMN_KEY_SET = new Set<XianyuOrderColumnKey>(XIANYU_ORDER_COLUMN_KEYS)

function parsePersistedColumnKeys(value: unknown): unknown {
  if (typeof value !== 'string') return value

  try {
    return JSON.parse(value)
  } catch {
    return undefined
  }
}

export function selectAllXianyuOrderColumnKeys(): XianyuOrderColumnKey[] {
  return [...XIANYU_ORDER_COLUMN_KEYS]
}

export function resetXianyuOrderColumnKeys(): XianyuOrderColumnKey[] {
  return [...XIANYU_ORDER_DEFAULT_VISIBLE_KEYS]
}

export function sanitizePersistedXianyuOrderColumnKeys(value: unknown): XianyuOrderColumnKey[] {
  const parsed = parsePersistedColumnKeys(value)
  if (!Array.isArray(parsed)) return resetXianyuOrderColumnKeys()

  const requestedKeys = parsed.filter(
    (key): key is XianyuOrderColumnKey =>
      typeof key === 'string' && XIANYU_ORDER_COLUMN_KEY_SET.has(key as XianyuOrderColumnKey)
  )

  if (requestedKeys.length === 0 && parsed.length > 0) {
    return resetXianyuOrderColumnKeys()
  }

  const requestedKeySet = new Set<XianyuOrderColumnKey>(requestedKeys)
  return XIANYU_ORDER_COLUMNS.filter(
    (column) => column.locked || requestedKeySet.has(column.key)
  ).map((column) => column.key)
}
