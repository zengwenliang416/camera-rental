import assert from 'node:assert/strict';
import test from 'node:test';

import type { RentalOrder } from '../../types';
import {
  filterOrders,
  getOrderActionAvailability,
  orderChannelTone,
  orderStatusTone,
  paginateOrders,
} from './orderModel';

const order: RentalOrder = {
  id: '1',
  orderNumber: 'RO-20260729-01',
  channel: 'XIANYU',
  customerName: '张**',
  customerPhone: '138****0000',
  receiverName: '张三',
  receiverPhone: '13800138000',
  receiverAddress: '测试省测试市测试路 1 号',
  startDate: '2026-07-30',
  endDate: '2026-08-02',
  occupyStartDate: '2026-07-29',
  occupyEndDateExclusive: '2026-08-04',
  rentalPeriodLabel: '2026-07-30 至 2026-08-02',
  rentalPeriodReady: true,
  status: 'PENDING_RETURN',
  items: [{
    rentalOrderItemId: 12,
    modelId: 'p3',
    modelName: 'Osmo Pocket 3',
    quantity: 1,
    assignedDeviceIds: ['device-9'],
  }],
  totalPrice: 12000,
  deposit: 0,
  createdTime: '2026-07-29 10:00:00',
  canAssign: true,
  canShip: false,
  canReturn: true,
};

test('authorized order filters include complete delivery details', () => {
  assert.equal(filterOrders([order], { status: 'PENDING_RETURN', channel: 'XIANYU', search: 'pocket' }).length, 1);
  assert.equal(filterOrders([order], { status: 'ALL', channel: 'ALL', search: '张三' }).length, 1);
  assert.equal(filterOrders([order], { status: 'ALL', channel: 'ALL', search: '13800138000' }).length, 1);
  assert.equal(filterOrders([order], { status: 'ALL', channel: 'ALL', search: '测试路' }).length, 1);
  assert.equal(filterOrders([order], { status: 'COMPLETED', channel: 'ALL', search: '' }).length, 0);
});

test('order pagination clamps pages and never renders the full result set at once', () => {
  const items = Array.from({ length: 23 }, (_, index) => index + 1);
  assert.deepEqual(paginateOrders(items, 2, 10), {
    items: [11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
    page: 2,
    pageSize: 10,
    totalPages: 3,
    totalItems: 23,
  });
  assert.deepEqual(paginateOrders(items, 99, 10).items, [21, 22, 23]);
});

test('order status and channel presentations use stable semantic tones', () => {
  assert.equal(orderStatusTone('EXCEPTION'), 'red');
  assert.equal(orderStatusTone('PENDING_RETURN'), 'amber');
  assert.equal(orderChannelTone('XIANYU'), 'amber');
});

test('general order cards never expose a direct return mutation', () => {
  assert.deepEqual(
    getOrderActionAvailability(order, { canAssign: true, canShip: true, canViewDevice: true }),
    {
      canAssign: true,
      canShip: false,
      detailDeviceId: 'device-9',
      returnRequiresOperationalFlow: true,
    }
  );
});

test('order device detail intent requires device query permission', () => {
  assert.equal(
    getOrderActionAvailability(order, {
      canAssign: true,
      canShip: true,
      canViewDevice: false,
    }).detailDeviceId,
    undefined
  );
});
