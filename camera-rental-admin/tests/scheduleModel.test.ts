import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildTimelineBounds,
  closedRangeDays,
  formatOccupyRange,
  getTimelineSegment,
  inclusiveEndFromExclusive,
  occupyDays,
  toPickerOccupyRange,
  toQueryOccupyRange
} from '../src/views/rental/schedule/scheduleModel.ts'

test('picker dates stay inclusive while API query uses an exclusive end', () => {
  assert.deepEqual(toQueryOccupyRange(['2026-08-03', '2026-08-05']), ['2026-08-03', '2026-08-06'])
  assert.deepEqual(toPickerOccupyRange('2026-08-03', '2026-08-06'), ['2026-08-03', '2026-08-05'])
})

test('occupy and billable day counts preserve their different interval semantics', () => {
  assert.equal(occupyDays('2026-08-03', '2026-08-06'), 3)
  assert.equal(closedRangeDays('2026-08-03', '2026-08-05'), 3)
  assert.equal(inclusiveEndFromExclusive('2026-08-06'), '2026-08-05')
  assert.equal(formatOccupyRange('2026-08-03', '2026-08-06'), '2026.08.03 → 2026.08.05')
})

test('timeline bounds prefer an explicit user range', () => {
  const bounds = buildTimelineBounds(
    [
      {
        occupyStartDate: '2026-08-01',
        occupyEndDateExclusive: '2026-08-12'
      }
    ],
    ['2026-08-03', '2026-08-08']
  )

  assert.deepEqual(bounds, {
    start: '2026-08-03',
    endExclusive: '2026-08-09'
  })
})

test('timeline segment is clipped to the visible bounds', () => {
  assert.deepEqual(getTimelineSegment('2026-08-01', '2026-08-06', '2026-08-03', '2026-08-09'), {
    left: 0,
    width: 50
  })
  assert.deepEqual(getTimelineSegment('2026-08-07', '2026-08-10', '2026-08-03', '2026-08-09'), {
    left: 66.667,
    width: 33.333
  })
})
