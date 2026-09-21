import type { ApiDate, PendingAllocationOrder, RentalOrderItem } from '@/api/rental/order'

const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING_ALLOCATION: '待分配',
  READY: '待出库',
  DISPATCHED: '已出库',
  IN_RENT: '租赁中',
  RETURNING: '归还中',
  RETURNED: '已回仓',
  CANCELLED: '已取消',
  CANCELED: '已取消',
  COMPLETED: '已完成',
}

const SOURCE_TYPE_LABEL: Record<string, string> = {
  XIANYU: '闲鱼',
  OFFLINE: '线下',
  ADMIN: '后台',
  WEB: '官网',
  UNIAPP: '小程序',
}

export function orderStatusLabel(status?: string) {
  if (!status)
    return '待处理'
  return ORDER_STATUS_LABEL[status] || status
}

export function sourceTypeLabel(sourceType?: string) {
  if (!sourceType)
    return ''
  return SOURCE_TYPE_LABEL[sourceType] || sourceType
}

export function modelSummary(order: Pick<PendingAllocationOrder, 'items'>) {
  const codes = Array.from(new Set((order.items || [])
    .map(item => item.equipmentModelCode)
    .filter((code): code is string => Boolean(code))))
  return codes.length ? codes.join(' / ') : '待分配设备'
}

export function remainingDeviceLabel(order: Pick<PendingAllocationOrder, 'remainingQuantity' | 'requiredQuantity' | 'assignedQuantity'>) {
  const remaining = order.remainingQuantity ?? 0
  const required = order.requiredQuantity
  if (required != null && required > 0)
    return `待配 ${remaining}/${required} 台`
  return `待配 ${remaining} 台`
}

export function formatApiDate(value?: ApiDate | null) {
  if (value == null || value === '')
    return ''
  if (Array.isArray(value) && value.length >= 3) {
    const year = value[0]
    const month = String(value[1]).padStart(2, '0')
    const day = String(value[2]).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  if (typeof value === 'string')
    return value.slice(0, 10)
  return ''
}

export function billableRangeLabel(order: Pick<PendingAllocationOrder, 'billableStartDate' | 'billableEndDate' | 'occupyStartDate'>) {
  const start = formatMonthDay(order.billableStartDate) || formatMonthDay(order.occupyStartDate)
  const end = formatMonthDay(order.billableEndDate)
  if (!start && !end)
    return '租期未确认'
  return `租期 ${start || '-'} 至 ${end || '-'}`
}

export function itemQuantityLabel(item: Pick<RentalOrderItem, 'remainingQuantity' | 'requiredQuantity' | 'assignedQuantity'>) {
  const remaining = item.remainingQuantity ?? 0
  const required = item.requiredQuantity
  const assigned = item.assignedQuantity ?? 0
  if (required != null)
    return `待配 ${remaining} 台 · 已配 ${assigned}/${required}`
  return `待配 ${remaining} 台`
}

export function formatQueueCount(count: number | null) {
  return count == null ? '—' : String(count)
}

export function maskMobile(value?: string | null) {
  const text = String(value || '').replace(/\s+/g, '')
  if (!text)
    return ''
  if (text.length >= 11)
    return `${text.slice(0, 3)}****${text.slice(-4)}`
  if (text.length >= 7)
    return `${text.slice(0, 2)}****${text.slice(-2)}`
  return text
}

export function displayReceiverName(order: { receiverName?: string, buyerNick?: string }) {
  return order.receiverName || order.buyerNick || ''
}

export function formatMonthDay(value?: ApiDate | null) {
  const iso = formatApiDate(value)
  if (!iso)
    return ''
  const parts = iso.split('-').map(Number)
  if (parts.length < 3)
    return iso
  return `${parts[1]}月${parts[2]}日`
}

export function dispatchByLabel(order: Pick<PendingAllocationOrder, 'occupyStartDate' | 'billableStartDate'>) {
  const day = formatMonthDay(order.occupyStartDate) || formatMonthDay(order.billableStartDate)
  if (!day)
    return '待安排发出'
  return `${day}前发出`
}

export function shiftIsoDate(value?: ApiDate | null, days = 0) {
  const iso = formatApiDate(value)
  if (!iso)
    return ''
  const parts = iso.split('-').map(Number)
  if (parts.length < 3)
    return iso
  const shifted = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2] + days))
  return `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, '0')}-${String(shifted.getUTCDate()).padStart(2, '0')}`
}

export function occupyRangeLabel(order: Pick<PendingAllocationOrder, 'occupyStartDate' | 'occupyEndDateExclusive'>) {
  const start = formatMonthDay(order.occupyStartDate)
  const inclusiveEnd = formatMonthDay(shiftIsoDate(order.occupyEndDateExclusive, -1))
  if (!start && !inclusiveEnd)
    return '占用期未确认'
  return `${start || '-'} – ${inclusiveEnd || '-'}`
}

export function displayOrderNo(order: Pick<PendingAllocationOrder, 'orderNo' | 'externalOrderNo' | 'sourceType'> & { id?: number }) {
  if (order.sourceType === 'XIANYU' && order.externalOrderNo)
    return order.externalOrderNo
  return order.orderNo || (order.id != null ? String(order.id) : '')
}

export function goodsLine(order: Pick<PendingAllocationOrder, 'goodsTitle' | 'items' | 'remainingQuantity' | 'requiredQuantity'>) {
  const qty = order.remainingQuantity ?? order.requiredQuantity
  if (order.goodsTitle)
    return qty ? `${order.goodsTitle} × ${qty}` : order.goodsTitle
  const models = modelSummary(order)
  if (qty)
    return `${models} × ${qty}`
  return models
}

export function orderMetaLine(order: Pick<PendingAllocationOrder, 'items' | 'orderNo' | 'sourceType'>) {
  const model = modelSummary(order)
  const source = sourceTypeLabel(order.sourceType)
  return [source, model !== '待分配设备' ? model : '', order.orderNo ? `内部 ${order.orderNo}` : '']
    .filter(Boolean)
    .join(' · ')
}

export function daysInclusive(start?: ApiDate | null, end?: ApiDate | null) {
  const from = formatApiDate(start)
  const to = formatApiDate(end)
  if (!from || !to)
    return null
  const a = Date.parse(`${from}T00:00:00+08:00`)
  const b = Date.parse(`${to}T00:00:00+08:00`)
  if (Number.isNaN(a) || Number.isNaN(b))
    return null
  return Math.max(1, Math.round((b - a) / 86400000) + 1)
}

export function shippingLabel(status?: string) {
  const labels: Record<string, string> = { UNSHIPPED: '未发货', PARTIAL: '部分发货', SHIPPED: '已发货', CANCELED: '已取消' }
  return status ? labels[status] || status : '发货状态待获取'
}
