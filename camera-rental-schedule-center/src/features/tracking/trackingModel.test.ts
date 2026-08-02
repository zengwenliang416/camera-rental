import assert from 'node:assert/strict';
import test from 'node:test';

import type {
  RentalDeliveryTrackingDetailRespVO,
  RentalDeliveryTrackingOrderSummaryRespVO,
} from '../../api/rental';
import type { RentalOrder, ScheduleBlock } from '../../types';
import {
  groupTrackingByOrderId,
  onlyTrackedSummaries,
  summarizeMultiPackageStatus,
  trackingStatusPresentation,
  toDeliveryOrderSummary,
  toDeliveryPackageDetail,
  visibleRentalOrderIds,
  type DeliveryOrderSummary,
} from './trackingModel';

const summary: DeliveryOrderSummary = {
  rentalOrderId: 30,
  packageCount: 2,
  statusCounts: {
    DELIVERED: 1,
    IN_TRANSIT: 1,
  },
  packages: [],
  risks: [],
};

test('groups one shared summary per rental order', () => {
  const grouped = groupTrackingByOrderId([summary]);

  assert.equal(grouped['30'], summary);
  assert.equal(Object.keys(grouped).length, 1);
});

test('excludes API placeholders that do not contain a local delivery', () => {
  assert.deepEqual(
    onlyTrackedSummaries([
      {
        ...summary,
        packageCount: 1,
        packages: [{
          deliveryId: 1,
          rentalOrderId: 30,
          direction: 'OUTBOUND',
          packageSeq: 1,
          trackingStatus: 'IN_TRANSIT',
          mappingStatus: 'READY',
          subscribeStatus: 'SUBSCRIBED',
          queryStatus: 'READY_QUERY',
          stale: false,
        }],
      },
      {
        rentalOrderId: 31,
        packageCount: 0,
        statusCounts: {},
        packages: [],
        risks: [],
      },
    ]).map((item) => item.rentalOrderId),
    [30]
  );
});

test('creates deterministic multi-package status copy input', () => {
  assert.equal(summarizeMultiPackageStatus(summary), '2|DELIVERED:1,IN_TRANSIT:1');
});

test('maps normalized server states to non-color-only presentation metadata', () => {
  assert.deepEqual(trackingStatusPresentation('DELIVERED'), {
    tone: 'green',
    icon: 'check',
  });
  assert.deepEqual(trackingStatusPresentation('EXCEPTION'), {
    tone: 'red',
    icon: 'alert',
  });
});

test('collects scheduled rental orders and shipped channel orders with waybills', () => {
  const blocks: ScheduleBlock[] = [
    {
      id: 'one',
      deviceId: '1',
      orderId: '30',
      type: 'RENTAL',
      startDate: '2026-07-31',
      endDate: '2026-08-03',
    },
    {
      id: 'duplicate',
      deviceId: '2',
      orderId: '30',
      type: 'RENTAL',
      startDate: '2026-08-01',
      endDate: '2026-08-04',
    },
    {
      id: 'outside',
      deviceId: '3',
      orderId: '31',
      type: 'RENTAL',
      startDate: '2026-08-20',
      endDate: '2026-08-22',
    },
    {
      id: 'invalid',
      deviceId: '4',
      orderId: 'external-order',
      type: 'RENTAL',
      startDate: '2026-07-31',
      endDate: '2026-08-01',
    },
  ];

  const shippedOrder = {
    id: '88',
    rentalOrderId: 99,
    orderNumber: 'XIANYU-88',
    channel: 'XIANYU',
    customerName: '客*',
    customerPhone: '',
    startDate: '',
    endDate: '',
    occupyStartDate: '',
    occupyEndDateExclusive: '',
    rentalPeriodLabel: '待复核',
    rentalPeriodReady: false,
    status: 'RENTING',
    items: [],
    totalPrice: 0,
    deposit: 0,
    createdTime: '2026-07-31 10:00:00',
    logisticsNumber: 'SF1234567890',
    canAssign: false,
    canShip: false,
    canReturn: false,
  } satisfies RentalOrder;

  assert.deepEqual(
    visibleRentalOrderIds(blocks, [shippedOrder], '2026-07-31', '2026-08-13'),
    [30, 99]
  );
});

test('maps order summary using backend risk and nullable waybill fields', () => {
  const input: RentalDeliveryTrackingOrderSummaryRespVO = {
    orderId: 88,
    packageCount: 1,
    statusCounts: { IN_TRANSIT: 1 },
    packages: [{
      deliveryId: 9,
      direction: 'OUTBOUND',
      packageSeq: 1,
      carrierName: 'SF',
      maskedWaybillNo: null,
      trackingStatus: 'IN_TRANSIT',
      mappingStatus: 'READY',
      subscribeStatus: 'SUBSCRIBED',
      queryStatus: 'READY',
      stale: false,
      risk: {
        code: 'TRACKING_STALE',
        severity: 'HIGH',
        safeMessage: 'safe',
        deviceIds: [101],
      },
    }],
    risks: [{
      code: 'TRACKING_STALE',
      severity: 'HIGH',
      safeMessage: 'safe',
      deviceIds: [101],
    }],
  };

  assert.deepEqual(toDeliveryOrderSummary(input), {
    rentalOrderId: 88,
    packageCount: 1,
    statusCounts: { IN_TRANSIT: 1 },
    packages: [{
      deliveryId: 9,
      rentalOrderId: 88,
      direction: 'OUTBOUND',
      packageSeq: 1,
      carrierName: 'SF',
      maskedWaybillNo: null,
      trackingStatus: 'IN_TRANSIT',
      mappingStatus: 'READY',
      subscribeStatus: 'SUBSCRIBED',
      queryStatus: 'READY',
      latestTraceText: undefined,
      latestEventTime: undefined,
      lastSyncedAt: undefined,
      estimatedDeliveryAt: undefined,
      stale: false,
      risk: {
        code: 'TRACKING_STALE',
        severity: 'high',
        safeMessage: 'safe',
        nextAction: undefined,
        deviceIds: [101],
      },
    }],
    risks: [{
      code: 'TRACKING_STALE',
      severity: 'high',
      safeMessage: 'safe',
      nextAction: undefined,
      deviceIds: [101],
    }],
  });
});

test('maps detail using rentalOrderId and normalized trace fields only', () => {
  const input: RentalDeliveryTrackingDetailRespVO = {
    deliveryId: 9,
    rentalOrderId: 88,
    direction: 'RETURN',
    packageSeq: 2,
    carrierName: 'SF',
    maskedWaybillNo: null,
    trackingStatus: 'exception',
    mappingStatus: 'READY',
    subscribeStatus: 'SUBSCRIBED',
    queryStatus: 'QUEUED',
    stale: true,
    risks: [{
      code: 'LOGISTICS_EXCEPTION',
      severity: 'MEDIUM',
      safeMessage: 'safe detail',
      deviceIds: [101, 102],
    }],
    devices: [{
      deviceId: 101,
      deviceNo: 'P4P-01',
      equipmentModelCode: 'P4P',
    }],
    traces: [{
      eventSeq: 3,
      businessTime: '2026-07-31T10:00:00',
      trackingStatus: 'picked_up',
      traceText: 'picked',
      location: 'Shanghai',
    }],
  };

  assert.deepEqual(toDeliveryPackageDetail(input), {
    deliveryId: 9,
    rentalOrderId: 88,
    direction: 'RETURN',
    packageSeq: 2,
    carrierName: 'SF',
    maskedWaybillNo: null,
    trackingStatus: 'EXCEPTION',
    mappingStatus: 'READY',
    subscribeStatus: 'SUBSCRIBED',
    queryStatus: 'QUEUED',
    latestTraceText: undefined,
    latestEventTime: undefined,
    lastSyncedAt: undefined,
    estimatedDeliveryAt: undefined,
    stale: true,
    devices: [{
      deviceId: 101,
      deviceNo: 'P4P-01',
      equipmentModelCode: 'P4P',
    }],
    traces: [{
      eventSeq: 3,
      businessTime: '2026-07-31T10:00:00',
      trackingStatus: 'PICKED_UP',
      traceText: 'picked',
      location: 'Shanghai',
    }],
    risks: [{
      code: 'LOGISTICS_EXCEPTION',
      severity: 'medium',
      safeMessage: 'safe detail',
      nextAction: undefined,
      deviceIds: [101, 102],
    }],
  });
});
