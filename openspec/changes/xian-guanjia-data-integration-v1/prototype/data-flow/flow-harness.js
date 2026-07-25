'use strict';

const assert = require('node:assert/strict');

function overlaps(candidate, existing) {
  return candidate.start < existing.endExclusive
    && candidate.endExclusive > existing.start;
}

function convertOrder(input, existingRentalOrderIds) {
  assert.ok(input.externalOrderId, 'external order id is required');

  if (existingRentalOrderIds.has(input.externalOrderId)) {
    return { status: 'converted', rentalOrderId: existingRentalOrderIds.get(input.externalOrderId), replay: true };
  }

  if (!input.productMapped) {
    return { status: 'review_required', reasonCode: 'PRODUCT_MAPPING_REQUIRED' };
  }
  if (!input.remarkDatesValid) {
    return { status: 'review_required', reasonCode: 'RENT_DATE_REVIEW_REQUIRED' };
  }

  const rentalOrderId = `rental-${input.externalOrderId}`;
  existingRentalOrderIds.set(input.externalOrderId, rentalOrderId);
  return { status: 'converted', rentalOrderId, replay: false };
}

function assignDevice(input, schedules, idempotencyResults) {
  assert.ok(input.idempotencyKey, 'idempotency key is required');
  assert.ok(input.start < input.endExclusive, 'occupied range must be half-open and non-empty');

  if (idempotencyResults.has(input.idempotencyKey)) {
    return { ...idempotencyResults.get(input.idempotencyKey), replay: true };
  }

  const conflict = schedules.some((schedule) => schedule.deviceId === input.deviceId
    && schedule.status === 'effective'
    && overlaps(input, schedule));
  if (conflict) {
    return { status: 'conflict', reasonCode: 'DEVICE_SCHEDULE_CONFLICT' };
  }

  const result = {
    status: 'assigned',
    assignmentId: `assignment-${input.idempotencyKey}`,
    scheduleId: `schedule-${input.idempotencyKey}`,
    replay: false
  };
  schedules.push({
    deviceId: input.deviceId,
    start: input.start,
    endExclusive: input.endExclusive,
    status: 'effective'
  });
  idempotencyResults.set(input.idempotencyKey, result);
  return result;
}

const rentalOrders = new Map();
const schedules = [{
  deviceId: 'device-A7M4-0001',
  start: '2026-07-22',
  endExclusive: '2026-07-30',
  status: 'effective'
}];
const assignments = new Map();

const unmapped = convertOrder({
  externalOrderId: 'xgj-order-1001',
  productMapped: false,
  remarkDatesValid: true
}, rentalOrders);
assert.deepEqual(unmapped, { status: 'review_required', reasonCode: 'PRODUCT_MAPPING_REQUIRED' });

const invalidDates = convertOrder({
  externalOrderId: 'xgj-order-1002',
  productMapped: true,
  remarkDatesValid: false
}, rentalOrders);
assert.deepEqual(invalidDates, { status: 'review_required', reasonCode: 'RENT_DATE_REVIEW_REQUIRED' });

const converted = convertOrder({
  externalOrderId: 'xgj-order-1003',
  productMapped: true,
  remarkDatesValid: true
}, rentalOrders);
assert.equal(converted.status, 'converted');
assert.equal(converted.replay, false);
assert.equal(rentalOrders.size, 1);

const conversionReplay = convertOrder({
  externalOrderId: 'xgj-order-1003',
  productMapped: true,
  remarkDatesValid: true
}, rentalOrders);
assert.equal(conversionReplay.rentalOrderId, converted.rentalOrderId);
assert.equal(conversionReplay.replay, true);
assert.equal(rentalOrders.size, 1);

const conflict = assignDevice({
  deviceId: 'device-A7M4-0001',
  start: '2026-07-29',
  endExclusive: '2026-08-02',
  idempotencyKey: 'assign-overlap'
}, schedules, assignments);
assert.deepEqual(conflict, { status: 'conflict', reasonCode: 'DEVICE_SCHEDULE_CONFLICT' });
assert.equal(schedules.length, 1);

const adjacent = assignDevice({
  deviceId: 'device-A7M4-0001',
  start: '2026-07-30',
  endExclusive: '2026-08-02',
  idempotencyKey: 'assign-adjacent'
}, schedules, assignments);
assert.equal(adjacent.status, 'assigned');
assert.equal(schedules.length, 2);

const assignmentReplay = assignDevice({
  deviceId: 'device-A7M4-0001',
  start: '2026-07-30',
  endExclusive: '2026-08-02',
  idempotencyKey: 'assign-adjacent'
}, schedules, assignments);
assert.equal(assignmentReplay.assignmentId, adjacent.assignmentId);
assert.equal(assignmentReplay.replay, true);
assert.equal(schedules.length, 2);

process.stdout.write(`${JSON.stringify({
  status: 'green',
  checks: {
    reviewRequiredForUnmappedProduct: true,
    reviewRequiredForInvalidDates: true,
    channelOrderConversionIdempotent: true,
    overlappingSchedulesRejected: true,
    adjacentSchedulesAccepted: true,
    assignmentIdempotent: true
  }
}, null, 2)}\n`);
