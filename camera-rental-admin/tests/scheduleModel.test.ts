import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildDevicePageRange,
  buildTimelineBounds,
  buildTimelineColumns,
  buildTimelineMonthGroups,
  closedRangeDays,
  formatOccupyRange,
  formatOptionalOccupyRange,
  getOrCreateAssignmentIdempotencyKey,
  getScheduleOrderDisplayNo,
  getTimelineSegment,
  inclusiveEndFromExclusive,
  occupyDays,
  shiftTimelineWindow,
  toExclusiveEnd,
  toWorkbenchQuery,
  type TimelineBounds
} from '../src/views/rental/schedule/scheduleModel.ts'

test('timeline windows use the requested 14, 30, or 90 day half-open range', () => {
  assert.deepEqual(buildTimelineBounds('2026-08-20', 14), {
    start: '2026-08-20',
    endExclusive: '2026-09-03',
    days: 14
  })
  assert.deepEqual(buildTimelineBounds('2026-08-20', 30), {
    start: '2026-08-20',
    endExclusive: '2026-09-19',
    days: 30
  })
  assert.deepEqual(buildTimelineBounds('2026-08-20', 90), {
    start: '2026-08-20',
    endExclusive: '2026-11-18',
    days: 90
  })
})

test('closed display dates and half-open occupancy dates keep their distinct semantics', () => {
  assert.equal(toExclusiveEnd('2026-08-05'), '2026-08-06')
  assert.equal(inclusiveEndFromExclusive('2026-08-06'), '2026-08-05')
  assert.equal(occupyDays('2026-08-03', '2026-08-06'), 3)
  assert.equal(closedRangeDays('2026-08-03', '2026-08-05'), 3)
  assert.equal(formatOccupyRange('2026-08-03', '2026-08-06'), '2026.08.03 → 2026.08.05')
})

test('optional occupancy ranges reject missing or invalid dates', () => {
  assert.equal(formatOptionalOccupyRange(undefined, undefined), undefined)
  assert.equal(formatOptionalOccupyRange('invalid', '2026-08-14'), undefined)
  assert.equal(formatOptionalOccupyRange('2026-08-14', '2026-08-14'), undefined)
  assert.equal(
    formatOptionalOccupyRange('2026-08-09', '2026-08-14'),
    '2026.08.09 → 2026.08.13'
  )
})

test('previous and next navigation shifts by the complete selected window', () => {
  assert.equal(shiftTimelineWindow('2026-08-20', 14, -1), '2026-08-06')
  assert.equal(shiftTimelineWindow('2026-08-20', 14, 1), '2026-09-03')
  assert.equal(shiftTimelineWindow('2026-12-15', 30, 1), '2027-01-14')
})

test('month groups retain cross-month boundaries', () => {
  const bounds = buildTimelineBounds('2026-08-29', 14)
  const columns = buildTimelineColumns(bounds, '2099-01-01')

  assert.equal(columns.length, 14)
  assert.deepEqual(buildTimelineMonthGroups(columns), [
    { key: '2026-08', label: '2026年8月', startIndex: 0, span: 3 },
    { key: '2026-09', label: '2026年9月', startIndex: 3, span: 11 }
  ])
})

test('timeline segments are clipped and expose continuation markers', () => {
  const bounds: TimelineBounds = {
    start: '2026-08-03',
    endExclusive: '2026-08-10',
    days: 7
  }

  assert.deepEqual(getTimelineSegment('2026-08-01', '2026-08-06', bounds), {
    left: 0,
    width: 42.857,
    continuesLeft: true,
    continuesRight: false
  })
  assert.deepEqual(getTimelineSegment('2026-08-08', '2026-08-12', bounds), {
    left: 71.429,
    width: 28.571,
    continuesLeft: false,
    continuesRight: true
  })
})

test('workbench query maps filters and server pagination without client-side authority', () => {
  const query = toWorkbenchQuery(
    '2026-08-20',
    30,
    {
      keyword: '  A7M4-0001  ',
      equipmentModelCode: '  A7M4  ',
      deviceStatus: 'AVAILABLE',
      logisticsStatus: 'READY'
    },
    2,
    50
  )

  assert.deepEqual(query, {
    pageNo: 2,
    pageSize: 50,
    fromDate: '2026-08-20',
    toDateExclusive: '2026-09-19',
    viewMode: '30D',
    keyword: 'A7M4-0001',
    equipmentModelCode: 'A7M4',
    deviceStatus: 'AVAILABLE',
    logisticsStatus: 'READY'
  })

  const blankQuery = toWorkbenchQuery(
    '2026-08-20',
    14,
    { keyword: '   ' },
    1,
    25
  )
  assert.equal(blankQuery.keyword, undefined)
  assert.equal(blankQuery.equipmentModelCode, undefined)
  assert.equal(blankQuery.deviceStatus, undefined)
  assert.equal(blankQuery.logisticsStatus, undefined)
})

test('assignment retries reuse one idempotency key until the intent succeeds', () => {
  const store = new Map<string, string>()
  let sequence = 0
  const createToken = () => `token-${++sequence}`

  const first = getOrCreateAssignmentIdempotencyKey(store, 501, 901, createToken)
  const retry = getOrCreateAssignmentIdempotencyKey(store, 501, 901, createToken)
  const anotherDevice = getOrCreateAssignmentIdempotencyKey(store, 501, 902, createToken)

  assert.equal(first, 'schedule-v2-501-901-token-1')
  assert.equal(retry, first)
  assert.equal(anotherDevice, 'schedule-v2-501-902-token-2')
})

test('schedule order labels prefer the real external order number', () => {
  assert.equal(
    getScheduleOrderDisplayNo({
      id: 501,
      orderNo: 'XY-0000000000000000501',
      externalOrderNo: '  3892746501234567890  '
    }),
    '3892746501234567890'
  )
  assert.equal(
    getScheduleOrderDisplayNo({ id: 502, orderNo: 'XY-0000000000000000502' }),
    'XY-0000000000000000502'
  )
  assert.equal(getScheduleOrderDisplayNo({ rentalOrderId: 503 }), '#503')
})

test('device page ranges support the allowed 25, 50, and 100 page sizes', () => {
  assert.deepEqual(buildDevicePageRange(231, 1, 25), { start: 1, end: 25 })
  assert.deepEqual(buildDevicePageRange(231, 2, 50), { start: 51, end: 100 })
  assert.deepEqual(buildDevicePageRange(231, 3, 100), { start: 201, end: 231 })
  assert.deepEqual(buildDevicePageRange(0, 1, 25), { start: 0, end: 0 })
})
