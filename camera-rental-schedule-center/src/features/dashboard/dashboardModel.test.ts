import assert from 'node:assert/strict';
import test from 'node:test';
import type { DeviceInstance, ExceptionItem, RentalOrder } from '../../types';
import { buildDashboardReadModel, formatSyncSummary } from './dashboardModel';

const order = (id: string, status: RentalOrder['status']): RentalOrder => ({
  id,
  orderNumber: `order-${id}`,
  channel: 'XIANYU',
  customerName: '已脱敏客户',
  customerPhone: '',
  startDate: '',
  endDate: '',
  occupyStartDate: '',
  occupyEndDateExclusive: '',
  rentalPeriodLabel: '待解析',
  rentalPeriodReady: false,
  status,
  items: [],
  totalPrice: 0,
  deposit: 0,
  createdTime: '',
  canAssign: false,
  canShip: false,
  canReturn: false,
});

const device = (id: string, status: DeviceInstance['status']): DeviceInstance => ({
  id,
  unitCode: id,
  sn: `sn-${id}`,
  modelId: 'model',
  modelName: 'model',
  status,
});

test('dashboard metrics are derived only from the mapped server snapshot', () => {
  const review: ExceptionItem = {
    id: 'review-1',
    type: 'CONFLICT',
    title: 'review',
    description: 'review',
    severity: 'high',
    createdTime: '',
    resolved: false,
  };
  const model = buildDashboardReadModel(
    [
      order('1', 'UNASSIGNED'),
      order('2', 'PENDING_DISPATCH'),
      order('3', 'RENTING'),
      order('4', 'PENDING_RETURN'),
    ],
    [
      device('1', 'IDLE'),
      device('2', 'RENTING'),
      device('3', 'PENDING_RETURN'),
      device('4', 'LOCKED'),
    ],
    [review, { ...review, id: 'review-2', resolved: true }]
  );

  assert.equal(model.registeredDevices, 4);
  assert.equal(model.availableDevices, 1);
  assert.equal(model.rentingDevices, 2);
  assert.equal(model.utilizationPercent, 50);
  assert.equal(model.maintenanceDevices.length, 1);
  assert.equal(model.unassignedOrders.length, 1);
  assert.equal(model.pendingShipOrders.length, 1);
  assert.equal(model.activeRentalOrders.length, 2);
  assert.equal(model.openReviews.length, 1);
});

test('empty registered inventory reports zero utilization instead of inventing capacity', () => {
  const model = buildDashboardReadModel([], [], []);
  assert.equal(model.registeredDevices, 0);
  assert.equal(model.utilizationPercent, 0);
});

test('sync summary uses the active locale and supplied localized units', () => {
  const syncedAt = Date.UTC(2026, 6, 29, 9, 12, 13);
  const english = formatSyncSummary({
    locale: 'en',
    syncedLabel: 'Management snapshot synced',
    deviceUnit: 'devices',
    orderUnit: 'orders',
    syncedAt,
    deviceCount: 60,
    orderCount: 1007,
  });
  const chinese = formatSyncSummary({
    locale: 'zh-CN',
    syncedLabel: '管理端快照已同步',
    deviceUnit: '台',
    orderUnit: '单',
    syncedAt,
    deviceCount: 60,
    orderCount: 1007,
  });

  assert.match(english, /Management snapshot synced/);
  assert.match(english, /60 devices \/ 1007 orders/);
  assert.doesNotMatch(english, /刚才|台设备|渠道订单/);
  assert.match(chinese, /管理端快照已同步/);
  assert.match(chinese, /60 台 \/ 1007 单/);
});
