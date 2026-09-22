import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import type { PendingAllocationOrder } from '@/api/rental/order'
import { formatApiDate } from './orderDisplay'

dayjs.extend(utc)
dayjs.extend(timezone)
export function businessToday() {
  return dayjs().tz('Asia/Shanghai').format('YYYY-MM-DD')
}
export function shiftDate(value: string, days: number) {
  return dayjs(value).add(days, 'day').format('YYYY-MM-DD')
}
export type TaskKind = 'SHIP' | 'RETURN' | 'OVERDUE'
/** 仅分组展示服务端日期与状态，不推断设备归还或修改履约状态。 */
export function matchesDailyPlan(order: PendingAllocationOrder, kind: TaskKind, today: string) {
  if (['CANCELED', 'CANCELLED', 'COMPLETED', 'RETURNED'].includes(order.status || ''))
    return false
  const start = formatApiDate(order.occupyStartDate)
  const end = formatApiDate(order.occupyEndDateExclusive)
  if (kind === 'SHIP')
    return !!start && start <= today && ['UNSHIPPED', 'PARTIAL'].includes(order.shippingStatus || '')
  if (!end || !['SHIPPED', 'PARTIAL'].includes(order.shippingStatus || ''))
    return false
  const due = shiftDate(end, -1)
  return kind === 'RETURN' ? due === today : due < today
}
export function trustedRelease(value: unknown) {
  if (!value || typeof value !== 'object')
    throw new Error('更新信息格式错误')
  const data = value as Record<string, unknown>
  if (data.package !== 'com.motioncover.rental.staff' || !Number.isSafeInteger(data.versionCode) || Number(data.versionCode) <= 0
    || typeof data.version !== 'string' || !/^\d+\.\d+\.\d+$/.test(data.version)
    || typeof data.download !== 'string' || data.download !== `https://rental.motion-cover.com/downloads/jiezuda/jiezuda-${data.version}.apk`) {
    throw new Error('更新信息校验失败，请稍后重试')
  }
  return {
    version: data.version,
    versionCode: Number(data.versionCode),
    download: data.download,
    notes: typeof data.notes === 'string' ? data.notes.slice(0, 1000) : '',
  }
}
