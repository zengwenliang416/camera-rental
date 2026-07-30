import assert from 'node:assert/strict';
import test from 'node:test';

import type { DeviceInstance, RentalOrder } from '../../types';
import {
  buildPendingOrderCandidates,
  buildShippingReadiness,
  filterAvailableDevices,
  filterPendingOrders,
  getOrderBlockReasons,
} from './shippingModel';
import { safeShippingError } from './shippingErrors';

const device: DeviceInstance = {
  id: '1',
  unitCode: 'P3-05-5WTCN7F002B088',
  sn: '5WTCN7F002B088',
  modelId: 'p3',
  modelName: '大疆 Pocket 3',
  status: 'IDLE',
  note: '仓库: A-03',
};

function pendingOrder(overrides: Partial<RentalOrder> = {}): RentalOrder {
  return {
    id: '69',
    orderNumber: '3314882821301007893',
    channel: 'XIANYU',
    customerName: '张*',
    customerPhone: '138****8000',
    startDate: '2026-07-30',
    endDate: '2026-08-05',
    occupyStartDate: '2026-07-29',
    occupyEndDateExclusive: '2026-08-07',
    rentalPeriodLabel: '2026-07-30 至 2026-08-05',
    rentalPeriodReady: true,
    status: 'PENDING_DISPATCH',
    items: [{
      modelId: 'p3',
      modelName: '大疆 Pocket 3',
      quantity: 1,
      assignedDeviceIds: [],
    }],
    totalPrice: 140,
    deposit: 0,
    createdTime: '2026-07-29 10:00:00',
    canAssign: false,
    canShip: true,
    canReturn: false,
    ...overrides,
  };
}

test('available-device search covers ID, SN, model, and warehouse text', () => {
  assert.equal(filterAvailableDevices([device], '5WTC').length, 1);
  assert.equal(filterAvailableDevices([device], 'Pocket 3').length, 1);
  assert.equal(filterAvailableDevices([device], 'A-03').length, 1);
  assert.equal(filterAvailableDevices([{ ...device, status: 'RENTING' }], 'P3').length, 0);
});

test('unprivileged pending-order search only matches the order number', () => {
  const orders = [pendingOrder()];
  assert.equal(filterPendingOrders(orders, '张三').length, 0);
  assert.equal(filterPendingOrders(orders, '13800138000').length, 0);
  assert.equal(filterPendingOrders(orders, '331488').length, 1);
});

test('authorized pending-order results keep complete verification fields in a short-lived candidate', () => {
  const orders = [pendingOrder()];
  const candidates = buildPendingOrderCandidates([{
    id: 69,
    shopId: 3,
    externalOrderId: '3314882821301007893',
    orderStatus: '12',
    conversionStatus: 'CONVERTED',
    receiverName: '测试收货人',
    receiverMobile: '19900000000',
    receiverAddress: '测试省测试市测试地址',
    buyerNick: '测试买家',
    goodsTitle: '大疆 Pocket 3 租赁',
    goodsQuantity: 1,
    payAmount: 14000,
    rentalOrderId: 99,
  }], orders);

  assert.equal(candidates.length, 1);
  assert.equal(candidates[0].order, orders[0]);
  assert.deepEqual(candidates[0].details, {
    receiverName: '测试收货人',
    receiverPhone: '19900000000',
    receiverAddress: '测试省测试市测试地址',
    buyerNick: '测试买家',
    goodsTitle: '大疆 Pocket 3 租赁',
    goodsQuantity: 1,
    amountCents: 14000,
    sellerRemark: undefined,
    shopId: 3,
    rentalOrderId: 99,
    channelStatus: '12',
    conversionStatus: 'CONVERTED',
  });
});

test('pending orders remain searchable when rental readiness blocks shipment', () => {
  const blocked = pendingOrder({
    rentalPeriodReady: false,
    rentalPeriodLabel: '租期待复核',
    occupyStartDate: '',
    occupyEndDateExclusive: '',
    canShip: false,
  });

  assert.equal(filterPendingOrders([blocked], '331488').length, 1);
  assert.deepEqual(getOrderBlockReasons(blocked, device), [
    '租期尚未解析完成',
    '设备占用周期不完整',
  ]);
});

test('readiness requires every user and server gate', () => {
  const ready = buildShippingReadiness({
    waybillNo: 'SF1234567890',
    carrier: '顺丰速运',
    device,
    order: pendingOrder(),
    permissionAllowed: true,
    integrationBlockReason: null,
    isSubmitting: false,
  });
  const blocked = buildShippingReadiness({
    waybillNo: '',
    carrier: '顺丰速运',
    permissionAllowed: false,
    integrationBlockReason: '服务器已关闭写操作',
    isSubmitting: false,
  });

  assert.equal(ready.canSubmit, true);
  assert.equal(ready.primaryBlockReason, null);
  assert.equal(blocked.canSubmit, false);
  assert.equal(blocked.primaryBlockReason, '当前账号缺少真实发货权限');
});

test('transport failures are converted to safe localized copy', () => {
  assert.equal(
    safeShippingError(new Error('Failed to fetch'), 'fallback'),
    '服务暂时不可用，已保留当前录入内容，请稍后重试。'
  );
  assert.equal(
    safeShippingError(new Error('secret upstream detail'), '发货失败，请重试。'),
    '发货失败，请重试。'
  );
});
