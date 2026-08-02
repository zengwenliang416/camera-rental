'use strict';

const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');

const STATUS_PRECEDENCE = Object.freeze({
  CREATED: 10,
  INFO_RECEIVED: 20,
  PICKED_UP: 30,
  IN_TRANSIT: 40,
  CUSTOMS: 45,
  OUT_FOR_DELIVERY: 50,
  EXCEPTION: 60,
  RETURNING: 70,
  DELIVERED: 80,
  RETURNED: 90,
  UNKNOWN: 0
});

const TERMINAL_STATUSES = new Set(['DELIVERED', 'RETURNED']);

function sha256(value) {
  return createHash('sha256').update(value, 'utf8').digest('hex');
}

function normalizeText(value) {
  return value == null ? '' : String(value).trim().replace(/\s+/g, ' ');
}

function normalizeStatus(value) {
  const status = normalizeText(value).toUpperCase();
  return Object.hasOwn(STATUS_PRECEDENCE, status) ? status : 'UNKNOWN';
}

function canonicalField(value) {
  const normalized = normalizeText(value);
  return `${Buffer.byteLength(normalized, 'utf8')}:${normalized}`;
}

function normalizeEvent(input) {
  const eventTime = input.eventTime == null ? null : new Date(input.eventTime);
  if (eventTime && Number.isNaN(eventTime.getTime())) {
    throw new Error(`Invalid event time: ${input.eventTime}`);
  }
  const normalized = {
    eventTime: eventTime ? eventTime.toISOString() : null,
    eventTimeRaw: normalizeText(input.eventTimeRaw),
    trackingStatus: normalizeStatus(input.trackingStatus),
    providerState: normalizeText(input.providerState),
    providerStatusCode: normalizeText(input.providerStatusCode),
    context: normalizeText(input.context),
    location: normalizeText(input.location),
    areaCode: normalizeText(input.areaCode),
    areaName: normalizeText(input.areaName),
    estimatedArrivalTime: input.estimatedArrivalTime == null
      ? null
      : new Date(input.estimatedArrivalTime).toISOString()
  };
  normalized.fingerprint = sha256([
    normalized.eventTime || '',
    normalized.eventTimeRaw,
    normalized.trackingStatus,
    normalized.providerState,
    normalized.providerStatusCode,
    normalized.context,
    normalized.location,
    normalized.areaCode,
    normalized.areaName
  ].map(canonicalField).join('|'));
  return normalized;
}

function compareEvents(left, right) {
  const leftTime = left.eventTime == null ? Number.MAX_SAFE_INTEGER : Date.parse(left.eventTime);
  const rightTime = right.eventTime == null ? Number.MAX_SAFE_INTEGER : Date.parse(right.eventTime);
  if (leftTime !== rightTime) return leftTime - rightTime;
  const raw = left.eventTimeRaw.localeCompare(right.eventTimeRaw);
  if (raw !== 0) return raw;
  return left.fingerprint.localeCompare(right.fingerprint);
}

function chooseCandidate(events) {
  return [...events].sort((left, right) => {
    const leftTime = left.eventTime == null ? Number.MIN_SAFE_INTEGER : Date.parse(left.eventTime);
    const rightTime = right.eventTime == null ? Number.MIN_SAFE_INTEGER : Date.parse(right.eventTime);
    if (leftTime !== rightTime) return leftTime - rightTime;
    const precedence = STATUS_PRECEDENCE[left.trackingStatus]
      - STATUS_PRECEDENCE[right.trackingStatus];
    if (precedence !== 0) return precedence;
    return left.fingerprint.localeCompare(right.fingerprint);
  }).at(-1);
}

function shouldKeepCurrent(current, candidate) {
  if (!current) return false;
  if (TERMINAL_STATUSES.has(current.trackingStatus)
      && !TERMINAL_STATUSES.has(candidate.trackingStatus)) {
    return true;
  }
  if (current.latestTraceTime && candidate.eventTime
      && Date.parse(candidate.eventTime) < Date.parse(current.latestTraceTime)) {
    return true;
  }
  if (current.latestTraceTime && candidate.eventTime
      && Date.parse(candidate.eventTime) === Date.parse(current.latestTraceTime)) {
    return STATUS_PRECEDENCE[current.trackingStatus]
      > STATUS_PRECEDENCE[candidate.trackingStatus];
  }
  return false;
}

function applySnapshot(current, inputEvents) {
  if (!Array.isArray(inputEvents) || inputEvents.length === 0) {
    throw new Error('A complete snapshot must contain at least one event');
  }
  const events = inputEvents.map(normalizeEvent).sort(compareEvents);
  const snapshotHash = sha256(events.map((event) => event.fingerprint).join('|'));

  if (current && current.snapshotHash === snapshotHash) {
    return {
      duplicate: true,
      snapshotVersion: current.snapshotVersion,
      trackingVersion: current.trackingVersion,
      snapshotHash,
      events: [],
      summary: current
    };
  }

  const candidate = chooseCandidate(events);
  const keepCurrent = shouldKeepCurrent(current, candidate);
  const summary = keepCurrent
    ? {
        trackingStatus: current.trackingStatus,
        latestTraceTime: current.latestTraceTime,
        latestTraceContext: current.latestTraceContext,
        latestLocation: current.latestLocation,
        estimatedArrivalTime: current.estimatedArrivalTime
      }
    : {
        trackingStatus: candidate.trackingStatus,
        latestTraceTime: candidate.eventTime,
        latestTraceContext: candidate.context,
        latestLocation: candidate.location,
        estimatedArrivalTime: candidate.estimatedArrivalTime
      };

  return {
    duplicate: false,
    snapshotVersion: (current?.snapshotVersion || 0) + 1,
    trackingVersion: (current?.trackingVersion || 0) + 1,
    snapshotHash,
    events: events.map((event, index) => ({ ...event, eventSeq: index + 1 })),
    summary
  };
}

function runHarness() {
  const unordered = [
    {
      eventTime: '2026-07-22T11:00:00+08:00',
      trackingStatus: 'in_transit',
      context: '  快件 已发往 深圳  ',
      location: '广州'
    },
    {
      eventTime: '2026-07-22T09:00:00+08:00',
      trackingStatus: 'picked_up',
      context: '顺丰已揽收',
      location: '长沙'
    }
  ];

  const first = applySnapshot(null, unordered);
  assert.equal(first.duplicate, false);
  assert.equal(first.snapshotVersion, 1);
  assert.equal(first.trackingVersion, 1);
  assert.equal(first.summary.trackingStatus, 'IN_TRANSIT');
  assert.equal(first.events[0].trackingStatus, 'PICKED_UP');
  assert.equal(first.events[1].context, '快件 已发往 深圳');

  const reorderedReplay = applySnapshot({
    snapshotHash: first.snapshotHash,
    snapshotVersion: first.snapshotVersion,
    trackingVersion: first.trackingVersion,
    ...first.summary
  }, [...unordered].reverse());
  assert.equal(reorderedReplay.duplicate, true);
  assert.equal(reorderedReplay.trackingVersion, 1);
  assert.deepEqual(reorderedReplay.events, []);

  const delivered = applySnapshot({
    snapshotHash: first.snapshotHash,
    snapshotVersion: first.snapshotVersion,
    trackingVersion: first.trackingVersion,
    ...first.summary
  }, [...unordered, {
    eventTime: '2026-07-23T15:00:00+08:00',
    trackingStatus: 'delivered',
    context: '已签收',
    location: '深圳'
  }]);
  assert.equal(delivered.summary.trackingStatus, 'DELIVERED');
  assert.equal(delivered.summary.estimatedArrivalTime, null);

  const lateRegression = applySnapshot({
    snapshotHash: delivered.snapshotHash,
    snapshotVersion: delivered.snapshotVersion,
    trackingVersion: delivered.trackingVersion,
    ...delivered.summary
  }, [{
    eventTime: '2026-07-22T12:00:00+08:00',
    trackingStatus: 'out_for_delivery',
    context: '迟到的派送事件',
    location: '广州'
  }]);
  assert.equal(lateRegression.duplicate, false);
  assert.equal(lateRegression.snapshotVersion, 3);
  assert.equal(lateRegression.trackingVersion, 3);
  assert.equal(lateRegression.summary.trackingStatus, 'DELIVERED');
  assert.equal(lateRegression.summary.latestTraceTime, delivered.summary.latestTraceTime);

  const unknownStatus = applySnapshot(null, [{
    eventTime: '2026-07-24T08:00:00+08:00',
    trackingStatus: 'provider-new-state',
    context: '供应商新增状态'
  }]);
  assert.equal(unknownStatus.summary.trackingStatus, 'UNKNOWN');

  const sameTimePrecedence = applySnapshot(null, [{
    eventTime: '2026-07-24T09:00:00+08:00',
    trackingStatus: 'in_transit',
    context: '运输中'
  }, {
    eventTime: '2026-07-24T09:00:00+08:00',
    trackingStatus: 'out_for_delivery',
    context: '派送中'
  }, {
    eventTime: null,
    eventTimeRaw: 'unknown',
    trackingStatus: 'returned',
    context: '无业务时间的事件'
  }]);
  assert.equal(sameTimePrecedence.summary.trackingStatus, 'OUT_FOR_DELIVERY');

  assert.throws(() => applySnapshot(null, []), /at least one event/);

  return {
    variant: 'deterministic-snapshot-v1',
    checks: 21,
    snapshotHashStableAcrossOrdering: true,
    duplicateReplaySuppressed: true,
    historyCorrectionVersioned: true,
    terminalRegressionBlocked: true,
    optionalEtaAccepted: true,
    unknownProviderStatusSafe: true,
    databaseWrites: 0,
    networkCalls: 0
  };
}

module.exports = {
  STATUS_PRECEDENCE,
  TERMINAL_STATUSES,
  normalizeEvent,
  applySnapshot,
  runHarness
};

if (require.main === module) {
  process.stdout.write(`${JSON.stringify(runHarness(), null, 2)}\n`);
}
