import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildSnapshotAccess,
  canAccessTab,
  permittedTabs,
} from './accessModel';

test('workspace access uses one permission model for every navigation intent', () => {
  const permissions = ['rental:device:query', 'rental:schedule:query'];
  assert.equal(canAccessTab(permissions, 'dashboard'), true);
  assert.equal(canAccessTab(permissions, 'devices'), true);
  assert.equal(canAccessTab(permissions, 'schedule'), true);
  assert.equal(canAccessTab(permissions, 'orders'), false);
  assert.deepEqual(
    permittedTabs(permissions, ['dashboard', 'schedule', 'orders', 'devices', 'binding']),
    ['dashboard', 'schedule', 'devices']
  );
});

test('snapshot access separates permission-scoped queries', () => {
  assert.deepEqual(buildSnapshotAccess(['rental:xianyu:ship']), {
    devices: false,
    schedules: false,
    orders: false,
    pendingShipOrders: true,
    reviews: false,
    xianyuConfig: false,
  });
});

test('wildcard permission enables every workspace query', () => {
  assert.equal(Object.values(buildSnapshotAccess(['*:*:*'])).every(Boolean), true);
});
