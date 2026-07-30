import assert from 'node:assert/strict';
import test from 'node:test';

import '../features/shipping/shippingModel.test';
import { mapChannelOrders, mapDevices, mapPendingShipOrders, mapSchedules } from './mappers';
import type { XianyuOrderVO } from './rental';
import { expressCodeFromName } from '../lib/expressCompanies';

function order(overrides: Partial<XianyuOrderVO> = {}): XianyuOrderVO {
  return {
    id: 69,
    shopId: 3,
    externalOrderId: 'test-order-6425',
    orderStatus: '12',
    payAmount: 14000,
    currency: 'CNY',
    conversionStatus: 'REVIEW_REQUIRED',
    sellerRemark: '发货7.28/收货7.28/发回8.05',
    ...overrides,
  };
}

test('Pocket 3 devices use the expected model name and never expose an unsigned QR payload', () => {
  const [device] = mapDevices([{
    id: 60,
    deviceNo: 'P3-05-5WTCN7F002B088',
    serialNumber: '5WTCN7F002B088',
    equipmentModelCode: 'P3',
    status: 'AVAILABLE',
    enabled: true,
  }]);

  assert.equal(device.modelName, '大疆 Pocket 3');
  assert.equal(device.unitCode, '05号');
  assert.equal(device.sn, '5WTCN7F002B088');
  assert.equal(device.qrCode, undefined);
});

test('shipped channel status takes precedence over conversion review', () => {
  const [mapped] = mapChannelOrders(
    [order({ orderStatus: '21', consignTime: '2026-07-28 10:00:00' })],
    []
  );

  assert.equal(mapped.status, 'RENTING');
});

test('completed and refunded channel statuses are not shown as exceptions', () => {
  for (const orderStatus of ['22', '23', '24']) {
    const [mapped] = mapChannelOrders([order({ orderStatus })], []);
    assert.equal(mapped.status, 'COMPLETED');
  }
});

test('pending shipment remains pending even when rental conversion needs review', () => {
  const pending = order();
  const [mapped] = mapChannelOrders([pending], [{ ...pending, buyerNick: 'buyer' }]);

  assert.equal(mapped.status, 'PENDING_DISPATCH');
});

test('pending shipment without a remark stays visible but cannot ship yet', () => {
  const [mapped] = mapChannelOrders(
    [order({
      sellerRemark: undefined,
      goodsTitle: '长沙免押大疆 Pocket 4 租赁',
      rentalPeriodStatus: 'FAILED',
      rentalPeriodReasonCode: 'RENTAL_PERIOD_NOT_FOUND',
    })],
    []
  );

  assert.equal(mapped.status, 'PENDING_DISPATCH');
  assert.equal(mapped.rentalPeriodReady, false);
  assert.equal(mapped.canShip, false);
});

test('pending non-rental merchandise without a remark stays out of scheduling', () => {
  const mapped = mapChannelOrders(
    [order({
      sellerRemark: undefined,
      goodsTitle: '全新大疆 Pocket 3 收纳包',
      rentalPeriodStatus: 'FAILED',
      rentalPeriodReasonCode: 'RENTAL_PERIOD_NOT_FOUND',
    })],
    []
  );

  assert.deepEqual(mapped, []);
});

test('order mapping keeps masked shared snapshots and authorized delivery details separately', () => {
  const [mapped] = mapChannelOrders(
    [order({
      receiverName: '张三',
      receiverMobile: '13800138000',
      receiverAddress: '测试地址',
      goodsTitle: '大疆 Pocket 3 租赁',
      goodsQuantity: 2,
    })],
    []
  );

  assert.equal(mapped.customerName, '张*');
  assert.equal(mapped.customerPhone, '138****8000');
  assert.equal(mapped.receiverName, '张三');
  assert.equal(mapped.receiverPhone, '13800138000');
  assert.equal(mapped.receiverAddress, '测试地址');
  assert.equal('shipmentDetails' in mapped, false);
});

test('fallback seller and buyer names remain masked without receiver snapshots', () => {
  const [mappedOrder] = mapChannelOrders([order({ sellerName: '小疆同学' })], []);
  const [pendingOrder] = mapPendingShipOrders([{ ...order(), buyerNick: '测试买家' }]);

  assert.equal(mappedOrder.customerName, '小*');
  assert.equal(pendingOrder.customerName, '测*');
});

test('rental item and active device assignments come from backend authority', () => {
  const [mapped] = mapChannelOrders(
    [order({
      conversionStatus: 'CONVERTED',
      rentalOrderId: 99,
      rentalOrderItemId: 100,
      equipmentModelCode: 'POCKET4',
      rentalQuantity: 1,
      goodsQuantity: 7,
      billableStartDate: '2026-07-29',
      billableEndDate: '2026-08-05',
      occupyStartDate: '2026-07-28',
      occupyEndDateExclusive: '2026-08-06',
      assignedDeviceIds: [300],
      waybillNo: 'SF1234567890',
    })],
    []
  );

  assert.equal(mapped.items[0].modelId, 'pocket4');
  assert.equal(mapped.items[0].quantity, 1);
  assert.deepEqual(mapped.items[0].assignedDeviceIds, ['300']);
  assert.equal(mapped.logisticsNumber, 'SF1234567890');
  assert.equal(mapped.occupyStartDate, '2026-07-28');
  assert.equal(mapped.occupyEndDateExclusive, '2026-08-06');
});

test('pending orders with a complete backend period can ship before product mapping', () => {
  const [unmapped] = mapChannelOrders(
    [order({
      billableStartDate: '2026-07-29',
      billableEndDate: '2026-08-05',
      occupyStartDate: '2026-07-28',
      occupyEndDateExclusive: '2026-08-06',
    })],
    []
  );
  const [ready] = mapChannelOrders(
    [order({
      conversionStatus: 'CONVERTED',
      rentalOrderId: 99,
      rentalOrderItemId: 100,
      equipmentModelCode: 'POCKET4',
      rentalQuantity: 1,
      billableStartDate: '2026-07-29',
      billableEndDate: '2026-08-05',
      occupyStartDate: '2026-07-28',
      occupyEndDateExclusive: '2026-08-06',
    })],
    []
  );
  const [shipped] = mapChannelOrders(
    [order({
      orderStatus: '21',
      conversionStatus: 'CONVERTED',
      rentalOrderId: 99,
      rentalOrderItemId: 100,
      equipmentModelCode: 'POCKET4',
      rentalQuantity: 1,
      billableStartDate: '2026-07-29',
      billableEndDate: '2026-08-05',
      occupyStartDate: '2026-07-28',
      occupyEndDateExclusive: '2026-08-06',
      consignTime: '2026-07-28 10:00:00',
    })],
    []
  );

  assert.equal(unmapped.canAssign, false);
  assert.equal(unmapped.canShip, true);
  assert.equal(unmapped.items[0].modelId, '');
  assert.equal(ready.canShip, true);
  assert.equal(shipped.canShip, false);
});

test('Java LocalDate array fields keep a pending order shippable before product mapping', () => {
  const [mapped] = mapChannelOrders(
    [order({
      externalOrderId: 'test-order-array-dates',
      billableStartDate: [2026, 7, 30],
      billableEndDate: [2026, 8, 5],
      rentalPeriodStatus: 'SUCCESS',
      occupyStartDate: [2026, 7, 28],
      occupyEndDateExclusive: [2026, 8, 6],
    })],
    []
  );

  assert.equal(mapped.rentalPeriodLabel, '2026-07-30 至 2026-08-05');
  assert.equal(mapped.status, 'PENDING_DISPATCH');
  assert.equal(mapped.canShip, true);
});

test('schedule end date is displayed as the inclusive day before the backend exclusive boundary', () => {
  const [schedule] = mapSchedules([{
    id: 1,
    deviceId: 2,
    scheduleType: 'RENTAL',
    status: 'EFFECTIVE',
    occupyStartDate: '2026-07-28',
    occupyEndDateExclusive: '2026-08-06',
  }]);

  assert.equal(schedule.startDate, '2026-07-28');
  assert.equal(schedule.endDate, '2026-08-05');
});

test('fallback express codes match the XianGuanJia company list', () => {
  assert.equal(expressCodeFromName('顺丰速运'), 'shunfeng');
  assert.equal(expressCodeFromName('京东物流'), 'jd');
  assert.equal(expressCodeFromName('德邦物流'), 'debangwuliu');
  assert.equal(expressCodeFromName('德邦快递'), 'debangkuaidi');
  assert.equal(expressCodeFromName('极兔速递'), 'jtexpress');
  assert.equal(expressCodeFromName('同城闪送/自提'), 'other');
});
