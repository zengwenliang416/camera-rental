import dayjs from 'dayjs'

export type SchedulePickerRange = [string, string]

export interface ScheduleDateWindow {
  occupyStartDate: string
  occupyEndDateExclusive: string
}

export interface TimelineBounds {
  start: string
  endExclusive: string
}

export interface TimelineSegment {
  left: number
  width: number
}

const DATE_FORMAT = 'YYYY-MM-DD'

export const toExclusiveEnd = (endInclusive: string) =>
  dayjs(endInclusive).add(1, 'day').format(DATE_FORMAT)

export const inclusiveEndFromExclusive = (endExclusive: string) =>
  dayjs(endExclusive).subtract(1, 'day').format(DATE_FORMAT)

export function toQueryOccupyRange(range?: SchedulePickerRange): SchedulePickerRange | undefined {
  if (!range) return undefined
  return [range[0], toExclusiveEnd(range[1])]
}

export function toPickerOccupyRange(
  start?: string,
  endExclusive?: string
): SchedulePickerRange | undefined {
  if (!start || !endExclusive || !dayjs(endExclusive).isAfter(dayjs(start))) {
    return undefined
  }
  return [start, inclusiveEndFromExclusive(endExclusive)]
}

export function occupyDays(start: string, endExclusive: string): number {
  return Math.max(dayjs(endExclusive).diff(dayjs(start), 'day'), 0)
}

export function closedRangeDays(start: string, endInclusive: string): number {
  return Math.max(dayjs(endInclusive).diff(dayjs(start), 'day') + 1, 0)
}

export const formatDisplayDate = (date: string) => dayjs(date).format('YYYY.MM.DD')

export const formatOccupyRange = (start: string, endExclusive: string) =>
  `${formatDisplayDate(start)} → ${formatDisplayDate(inclusiveEndFromExclusive(endExclusive))}`

export const formatClosedRange = (start: string, endInclusive: string) =>
  `${formatDisplayDate(start)} → ${formatDisplayDate(endInclusive)}`

export function buildTimelineBounds(
  rows: readonly ScheduleDateWindow[],
  selectedRange?: SchedulePickerRange
): TimelineBounds {
  const selectedQueryRange = toQueryOccupyRange(selectedRange)
  if (selectedQueryRange) {
    return { start: selectedQueryRange[0], endExclusive: selectedQueryRange[1] }
  }

  if (rows.length > 0) {
    const start = rows.reduce(
      (earliest, row) =>
        dayjs(row.occupyStartDate).isBefore(dayjs(earliest)) ? row.occupyStartDate : earliest,
      rows[0].occupyStartDate
    )
    const endExclusive = rows.reduce(
      (latest, row) =>
        dayjs(row.occupyEndDateExclusive).isAfter(dayjs(latest))
          ? row.occupyEndDateExclusive
          : latest,
      rows[0].occupyEndDateExclusive
    )
    return { start, endExclusive }
  }

  const today = dayjs().format(DATE_FORMAT)
  return { start: today, endExclusive: toExclusiveEnd(today) }
}

export function getTimelineSegment(
  start: string,
  endExclusive: string,
  boundsStart: string,
  boundsEndExclusive: string
): TimelineSegment {
  const totalDays = occupyDays(boundsStart, boundsEndExclusive)
  if (totalDays <= 0) return { left: 0, width: 100 }

  const rawStart = dayjs(start).diff(dayjs(boundsStart), 'day')
  const rawEnd = dayjs(endExclusive).diff(dayjs(boundsStart), 'day')
  const clampedStart = Math.min(Math.max(rawStart, 0), totalDays)
  const clampedEnd = Math.min(Math.max(rawEnd, clampedStart), totalDays)
  const left = (clampedStart / totalDays) * 100
  const width = ((clampedEnd - clampedStart) / totalDays) * 100

  return {
    left: Number(left.toFixed(3)),
    width: Number(Math.max(width, width > 0 ? 1.5 : 0).toFixed(3))
  }
}
