import type { DeviceCandidate, RentalOrderScheduleDetail } from '@/api/rental/order'
import type { XianyuPendingShipOrder } from '@/api/rental/xianyu'

/** 按对象身份匹配，不把第一页或关键词近似命中当作已选订单。 */
export async function findPendingShipment(
  fetchPage: (pageNo: number) => Promise<{ list: XianyuPendingShipOrder[], total: number }>,
  target: { channelOrderId?: number, rentalOrderId?: number },
) {
  for (let page = 1; page <= 100; page++) {
    const result = await fetchPage(page)
    const match = result.list.find(item => target.channelOrderId
      ? item.id === target.channelOrderId && (!target.rentalOrderId || item.rentalOrderId === target.rentalOrderId)
      : item.rentalOrderId === target.rentalOrderId)
    if (match)
      return match
    if (!result.list.length || page * 100 >= result.total)
      break
  }
  throw new Error('此订单已不在待发货队列，或尚未关联租赁订单。请返回订单核对状态。')
}

export function reconcilePicked(picked: DeviceCandidate[], candidates: DeviceCandidate[], remaining: number) {
  return picked.flatMap((item) => {
    const fresh = candidates.find(row => row.id === item.id && row.eligible === true)
    return fresh ? [fresh] : []
  }).slice(0, Math.max(0, remaining))
}

export function singleShipmentBlocker(order?: RentalOrderScheduleDetail) {
  if (!order)
    return '正在获取订单明细，请稍候'
  const count = order.items?.reduce((sum, item) => sum + (item.requiredQuantity || 0), 0) || order.requiredQuantity || 0
  if (count < 1 || count > 100 || !order.items?.length)
    return '请核对实际设备台数（每次最多 100 台）'
  return ''
}

export function candidateReason(item?: DeviceCandidate) {
  const labels: Record<string, string> = {
    MODEL_MISMATCH: '设备型号不匹配',
    DEVICE_DISABLED: '设备已停用',
    DEVICE_NOT_AVAILABLE: '设备当前不可分配',
    SCHEDULE_CONFLICT: '设备排期冲突',
    DEVICE_LOCKED: '设备已锁定',
    MAINTENANCE: '设备处于维修中',
  }
  return item?.reasonCodes?.map(code => labels[code] || code).join('、') || '设备不在当前可分配候选中，请刷新后核对型号、状态和排期'
}
