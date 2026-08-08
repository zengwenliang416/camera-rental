import dayjs from 'dayjs'
import type {
  RentalScheduleCandidateResponseVO,
  RentalScheduleCandidateVO,
  RentalScheduleSegmentVO,
  RentalScheduleWorkbenchReqVO,
  ScheduleWindowDays
} from '@/api/rental/schedule'

export type SchedulePickerRange = [string, string]

export interface TimelineBounds {
  start: string
  endExclusive: string
  days: number
}

export interface TimelineColumn {
  date: string
  day: string
  weekday: string
  monthKey: string
  monthLabel: string
  isToday: boolean
}

export interface TimelineMonthGroup {
  key: string
  label: string
  startIndex: number
  span: number
}

export interface TimelineSegment {
  left: number
  width: number
  continuesLeft: boolean
  continuesRight: boolean
}

export interface ScheduleFilterDraft {
  keyword: string
  equipmentModelCode?: string
  deviceStatus?: string
  logisticsStatus?: string
}

const DATE_FORMAT = 'YYYY-MM-DD'
const DISPLAY_DATE_FORMAT = 'YYYY.MM.DD'

export const toExclusiveEnd = (endInclusive: string) =>
  dayjs(endInclusive).add(1, 'day').format(DATE_FORMAT)

export const inclusiveEndFromExclusive = (endExclusive: string) =>
  dayjs(endExclusive).subtract(1, 'day').format(DATE_FORMAT)

export function occupyDays(start: string, endExclusive: string): number {
  return Math.max(dayjs(endExclusive).diff(dayjs(start), 'day'), 0)
}

export function closedRangeDays(start: string, endInclusive: string): number {
  return Math.max(dayjs(endInclusive).diff(dayjs(start), 'day') + 1, 0)
}

export function formatDisplayDate(date: string): string {
  return dayjs(date).format(DISPLAY_DATE_FORMAT)
}

export function formatShortDate(date: string): string {
  return dayjs(date).format('M月D日')
}

export function formatDateTime(date?: string): string {
  return date ? dayjs(date).format('YYYY.MM.DD HH:mm') : '-'
}

export function formatOccupyRange(start: string, endExclusive: string): string {
  return `${formatDisplayDate(start)} → ${formatDisplayDate(inclusiveEndFromExclusive(endExclusive))}`
}

export function formatClosedRange(start: string, endInclusive: string): string {
  return `${formatDisplayDate(start)} → ${formatDisplayDate(endInclusive)}`
}

export function buildTimelineBounds(
  startDate: string,
  windowDays: ScheduleWindowDays
): TimelineBounds {
  const start = dayjs(startDate).format(DATE_FORMAT)
  const endExclusive = dayjs(start).add(windowDays, 'day').format(DATE_FORMAT)
  return { start, endExclusive, days: windowDays }
}

export function shiftTimelineWindow(
  startDate: string,
  windowDays: ScheduleWindowDays,
  direction: -1 | 1
): string {
  return dayjs(startDate)
    .add(windowDays * direction, 'day')
    .format(DATE_FORMAT)
}

export function todayDate(): string {
  return dayjs().format(DATE_FORMAT)
}

export function buildTimelineColumns(
  bounds: TimelineBounds,
  today: string = todayDate()
): TimelineColumn[] {
  return Array.from({ length: bounds.days }, (_, index) => {
    const date = dayjs(bounds.start).add(index, 'day')
    return {
      date: date.format(DATE_FORMAT),
      day: date.format('D'),
      weekday: date.format('dd'),
      monthKey: date.format('YYYY-MM'),
      monthLabel: date.format('YYYY年M月'),
      isToday: date.format(DATE_FORMAT) === today
    }
  })
}

export function buildTimelineMonthGroups(columns: readonly TimelineColumn[]): TimelineMonthGroup[] {
  return columns.reduce<TimelineMonthGroup[]>((groups, column, index) => {
    const previous = groups[groups.length - 1]
    if (previous?.key === column.monthKey) {
      previous.span += 1
      return groups
    }
    groups.push({
      key: column.monthKey,
      label: column.monthLabel,
      startIndex: index,
      span: 1
    })
    return groups
  }, [])
}

export function getTimelineSegment(
  start: string,
  endExclusive: string,
  bounds: TimelineBounds
): TimelineSegment {
  const rawStart = dayjs(start).diff(dayjs(bounds.start), 'day')
  const rawEnd = dayjs(endExclusive).diff(dayjs(bounds.start), 'day')
  const clampedStart = Math.min(Math.max(rawStart, 0), bounds.days)
  const clampedEnd = Math.min(Math.max(rawEnd, clampedStart), bounds.days)
  const width = clampedEnd - clampedStart

  if (bounds.days <= 0 || width <= 0) {
    return {
      left: 0,
      width: 0,
      continuesLeft: rawStart < 0,
      continuesRight: rawEnd > bounds.days
    }
  }

  return {
    left: Number(((clampedStart / bounds.days) * 100).toFixed(3)),
    width: Number(Math.max((width / bounds.days) * 100, 1.5).toFixed(3)),
    continuesLeft: rawStart < 0,
    continuesRight: rawEnd > bounds.days
  }
}

export function getBillableSegment(
  segment: RentalScheduleSegmentVO,
  bounds: TimelineBounds
): TimelineSegment | undefined {
  if (!segment.billableStartDate || !segment.billableEndDate) return undefined
  return getTimelineSegment(segment.billableStartDate, toExclusiveEnd(segment.billableEndDate), bounds)
}

export function toWorkbenchQuery(
  startDate: string,
  windowDays: ScheduleWindowDays,
  filters: ScheduleFilterDraft,
  pageNo: number,
  pageSize: number
): RentalScheduleWorkbenchReqVO {
  const bounds = buildTimelineBounds(startDate, windowDays)
  return {
    pageNo,
    pageSize,
    fromDate: bounds.start,
    toDateExclusive: bounds.endExclusive,
    viewMode: `${windowDays}D`,
    keyword: filters.keyword.trim() || undefined,
    equipmentModelCode: filters.equipmentModelCode || undefined,
    deviceStatus: filters.deviceStatus || undefined,
    logisticsStatus: filters.logisticsStatus || undefined
  }
}

export function buildDevicePageRange(
  total: number,
  pageNo: number,
  pageSize: number
): { start: number; end: number } {
  if (total <= 0) return { start: 0, end: 0 }
  const start = (pageNo - 1) * pageSize + 1
  return { start, end: Math.min(pageNo * pageSize, total) }
}

export function extractCandidates(
  response: RentalScheduleCandidateResponseVO
): RentalScheduleCandidateVO[] {
  return response.candidates
}
