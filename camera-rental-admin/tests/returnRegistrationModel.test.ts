import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildReturnRegistrationPageParams,
  canReviewReturnRegistration,
  canRevokeReturnRegistration,
  formatReturnRegistrationDate,
  RETURN_REGISTRATION_STATUSES,
  returnRegistrationStatusLabel
} from '../src/views/rental/return-registration/returnRegistrationModel.ts'

test('draft rows expose only revoke before customer submission', () => {
  assert.equal(canRevokeReturnRegistration('DRAFT'), true)
  assert.equal(canReviewReturnRegistration('DRAFT'), false)
})

test('expired and revoked registrations cannot be revoked again', () => {
  for (const status of ['EXPIRED', 'REVOKED']) {
    assert.equal(canRevokeReturnRegistration(status), false)
  }
})

test('review action is limited to review-required registrations', () => {
  assert.equal(canReviewReturnRegistration('REVIEW_REQUIRED'), true)
  assert.equal(canReviewReturnRegistration('ACCEPTED'), false)
  assert.equal(returnRegistrationStatusLabel('REVIEW_REQUIRED'), '待人工复核')
  assert.equal(RETURN_REGISTRATION_STATUSES.includes('DRAFT'), true)
})

test('pagination query preserves page and submitted time range', () => {
  const params = buildReturnRegistrationPageParams(
    { pageNo: 3, pageSize: 20, keyword: 'RR-001' },
    ['2026-08-01T00:00:00', '2026-08-01T23:59:59']
  )

  assert.equal(params.pageNo, 3)
  assert.equal(params.pageSize, 20)
  assert.equal(params.submittedStart, '2026-08-01T00:00:00')
  assert.equal(params.submittedEnd, '2026-08-01T23:59:59')
})

test('return registration dates accept legacy arrays and ISO strings', () => {
  assert.equal(formatReturnRegistrationDate([2026, 8, 3]), '2026-08-03')
  assert.equal(formatReturnRegistrationDate('2026-08-03'), '2026-08-03')
  assert.equal(formatReturnRegistrationDate(undefined), '—')
})
