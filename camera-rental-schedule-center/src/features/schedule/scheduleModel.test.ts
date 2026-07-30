import assert from 'node:assert/strict';
import test from 'node:test';

import type { DeviceInstance, RentalOrder, ScheduleBlock } from '../../types';
import { recommendDevicesForOrder } from '../../lib/scheduleEngine';
import {
  buildAllocationProgress,
  buildScheduleWindow,
  deriveOrderRanges,
  evaluateAllocationSubmit,
  filterScheduleDevices,
} from './scheduleModel';

const order: RentalOrder = {
  id: 'order-1',
  rentalOrderId: 41,
  orderNumber: 'XY-41',
  channel: 'XIANYU',
  customerName: '测*',
  customerPhone: '',
  startDate: '2026-07-31',
  endDate: '2026-08-05',
  occupyStartDate: '2026-07-29',
  occupyEndDateExclusive: '2026-08-07',
  rentalPeriodLabel: '2026-07-31 至 2026-08-05',
  rentalPeriodReady: true,
  status: 'PENDING_DISPATCH',
  items: [
    {
      rentalOrderItemId: 71,
      modelId: 'p4p',
      modelName: 'Pocket 3',
      quantity: 2,
      assignedDeviceIds: [],
    },
  ],
  totalPrice: 0,
  deposit: 0,
  createdTime: '2026-07-29 10:00:00',
  canAssign: true,
  canShip: true,
  canReturn: false,
};

const devices: DeviceInstance[] = [
  {
    id: '1',
    unitCode: '01号',
    sn: 'SN-ALPHA',
    modelId: 'p4p',
    modelName: 'Pocket 3',
    status: 'IDLE',
    note: '仓库: A',
  },
  {
    id: '2',
    unitCode: '02号',
    sn: 'SN-BETA',
    modelId: 'p4p',
    modelName: 'Pocket 3',
    status: 'REPAIR',
    note: '仓库: B',
  },
];

test('schedule window is generated from the provided local date', () => {
  const days = buildScheduleWindow(new Date(2026, 6, 29), 3, 'zh-CN');
  assert.deepEqual(
    days.map((day) => day.dateStr),
    ['2026-07-29', '2026-07-30', '2026-07-31']
  );
  assert.equal(days[0].isToday, true);
  assert.equal(days[0].weekday, '周三');
});

test('billable and occupied ranges stay distinct and display the inclusive occupied end', () => {
  assert.deepEqual(deriveOrderRanges(order), {
    billable: { startDate: '2026-07-31', endDate: '2026-08-05' },
    occupied: { startDate: '2026-07-29', endDate: '2026-08-06' },
    occupyEndDateExclusive: '2026-08-07',
  });
});

test('schedule filters match status, device identifier, SN, model, and warehouse text', () => {
  assert.deepEqual(
    filterScheduleDevices(devices, { status: 'IDLE', search: 'alpha' }).map((device) => device.id),
    ['1']
  );
  assert.deepEqual(
    filterScheduleDevices(devices, { status: 'ALL', search: '仓库: b' }).map((device) => device.id),
    ['2']
  );
});

test('allocation progress and submit state require complete server-backed details', () => {
  const partial = buildAllocationProgress(order, { p4p: ['1'] });
  assert.deepEqual(partial, {
    totalRequired: 2,
    totalAssigned: 1,
    complete: false,
    percent: 50,
  });
  assert.equal(
    evaluateAllocationSubmit({
      order,
      allocationMap: { p4p: ['1'] },
      hasPermission: true,
      isSubmitting: false,
    }).reason,
    'incomplete'
  );
  assert.equal(
    evaluateAllocationSubmit({
      order,
      allocationMap: { p4p: ['1', '2'] },
      hasPermission: false,
      isSubmitting: false,
    }).reason,
    'permission'
  );
  assert.deepEqual(
    evaluateAllocationSubmit({
      order,
      allocationMap: { p4p: ['1', '2'] },
      hasPermission: true,
      isSubmitting: false,
    }),
    { ready: true, reason: 'ready' }
  );
});

test('provisional recommendation checks the occupied range rather than only billable days', () => {
  const blocks: ScheduleBlock[] = [
    {
      id: 'block-1',
      deviceId: '1',
      type: 'RENTAL',
      startDate: '2026-07-29',
      endDate: '2026-07-29',
      orderNumber: 'OTHER',
    },
  ];
  const recommended = recommendDevicesForOrder(order, devices, blocks);
  assert.deepEqual(recommended.p4p, []);
});
