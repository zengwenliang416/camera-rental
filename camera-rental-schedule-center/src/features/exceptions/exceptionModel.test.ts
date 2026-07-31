import assert from 'node:assert/strict';
import test from 'node:test';

import type { ExceptionItem } from '../../types';
import type { DeliveryOrderSummary } from '../tracking/trackingModel';
import {
  exceptionActions,
  exceptionSeverityTone,
  filterExceptions,
  mergeExceptionItems,
} from './exceptionModel';

const item: ExceptionItem = {
  id: '7',
  type: 'CONFLICT',
  title: '排期冲突',
  description: '服务端复核记录',
  relatedOrderId: '11',
  relatedDeviceId: '22',
  severity: 'high',
  createdTime: '2026-07-29 10:00:00',
  resolved: false,
};

const manualItem = mergeExceptionItems([item], [])[0]!;

test('exception filters and severity use server review state', () => {
  assert.equal(filterExceptions([manualItem], 'OPEN').length, 1);
  assert.equal(filterExceptions([manualItem], 'RESOLVED').length, 0);
  assert.equal(exceptionSeverityTone('high'), 'red');
});

test('exception actions require both relation and permission', () => {
  assert.deepEqual(exceptionActions(manualItem, {
    canResolve: false,
    canAssign: true,
    canViewDevice: true,
    canReadTracking: false,
  }), {
    canResolve: false,
    canAssign: true,
    canViewDevice: true,
    canOpenTracking: false,
  });
});

test('merges logistics risks into exception list with tracking-specific actions', () => {
  const trackingSummary: DeliveryOrderSummary = {
    rentalOrderId: 77,
    packageCount: 1,
    statusCounts: { EXCEPTION: 1 },
    packages: [{
      deliveryId: 9,
      rentalOrderId: 77,
      direction: 'RETURN',
      packageSeq: 1,
      carrierName: 'SF',
      maskedWaybillNo: null,
      trackingStatus: 'EXCEPTION',
      mappingStatus: 'READY',
      subscribeStatus: 'SUBSCRIBED',
      queryStatus: 'READY',
      latestTraceText: undefined,
      latestEventTime: '2026-07-31T09:00:00',
      lastSyncedAt: '2026-07-31T08:00:00',
      estimatedDeliveryAt: undefined,
      stale: false,
      risk: undefined,
    }],
    risks: [{
      code: 'LOGISTICS_EXCEPTION',
      severity: 'high',
      safeMessage: 'safe logistics detail',
      deviceIds: [22],
    }],
  };

  const merged = mergeExceptionItems([item], [trackingSummary]);

  assert.equal(merged.length, 2);
  assert.equal(merged[0]?.kind, 'tracking');
  assert.equal(merged[0]?.relatedOrderId, '77');
  assert.equal(merged[0]?.trackingRiskCode, 'LOGISTICS_EXCEPTION');
  assert.deepEqual(exceptionActions(merged[0]!, {
    canResolve: true,
    canAssign: true,
    canViewDevice: true,
    canReadTracking: true,
  }), {
    canResolve: false,
    canAssign: false,
    canViewDevice: true,
    canOpenTracking: true,
  });
  assert.equal(filterExceptions(merged, 'OPEN').length, 2);
  assert.equal(filterExceptions(merged, 'RESOLVED').length, 0);
});
