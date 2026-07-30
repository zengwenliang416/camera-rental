import assert from 'node:assert/strict';
import test from 'node:test';
import type {
  RentalDeviceVO,
  RentalManualReviewVO,
  RentalScheduleVO,
  XianyuOrderVO,
  XianyuPendingShipOrderVO,
} from './rental';
import { loadAuthorizedSnapshot } from './snapshotLoader';

const page = <T>(list: T[]) => Promise.resolve({ list, total: list.length });
const emptyAccess = {
  devices: false,
  schedules: false,
  orders: false,
  pendingShipOrders: false,
  reviews: false,
  xianyuConfig: false,
};

test('one permission-scoped query failure preserves other authorized results', async () => {
  const device: RentalDeviceVO = {
    id: 1,
    deviceNo: 'P3-001-SN',
    equipmentModelCode: 'P3',
    status: 'AVAILABLE',
    enabled: true,
  };
  const order: XianyuOrderVO = {
    id: 2,
    shopId: 1,
    externalOrderId: 'order-2',
    orderStatus: '12',
    payAmount: 0,
    currency: 'CNY',
    conversionStatus: 'PENDING',
  };
  const result = await loadAuthorizedSnapshot(
    { ...emptyAccess, devices: true, schedules: true, orders: true },
    {
      devices: () => page([device]),
      schedules: () => Promise.reject(new Error('forbidden')),
      orders: () => page([order]),
      pendingShipOrders: () => page<XianyuPendingShipOrderVO>([]),
      reviews: () => page<RentalManualReviewVO>([]),
    }
  );

  assert.equal(result.devices.length, 1);
  assert.equal(result.channelOrders.length, 1);
  assert.deepEqual(result.schedules, []);
  assert.deepEqual(result.failures, ['schedules']);
});

test('unauthorized snapshot parts are skipped without being reported as failures', async () => {
  let calls = 0;
  const skipped = () => {
    calls += 1;
    return page<never>([]);
  };
  const result = await loadAuthorizedSnapshot(emptyAccess, {
    devices: skipped as () => Promise<{ list: RentalDeviceVO[]; total: number }>,
    schedules: skipped as () => Promise<{ list: RentalScheduleVO[]; total: number }>,
    orders: skipped as () => Promise<{ list: XianyuOrderVO[]; total: number }>,
    pendingShipOrders: skipped as () => Promise<{ list: XianyuPendingShipOrderVO[]; total: number }>,
    reviews: skipped as () => Promise<{ list: RentalManualReviewVO[]; total: number }>,
  });

  assert.equal(calls, 0);
  assert.deepEqual(result.failures, []);
});
