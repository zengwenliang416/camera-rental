import assert from 'node:assert/strict';
import test from 'node:test';

import { canStartCommand, shipmentGuard } from './commandModel';

test('shipment guard requires permission, persisted config, credentials, and write enablement', () => {
  assert.match(shipmentGuard(false, null) || '', /rental:xianyu:ship/);
  assert.match(shipmentGuard(true, null) || '', /写配置/);
  assert.equal(shipmentGuard(true, null, false), null);
  assert.equal(
    shipmentGuard(true, {
      enabled: true,
      writeEnabled: true,
      status: 'READY',
      appSecretConfigured: true,
      webhookBaseUrlConfigured: true,
    }),
    null
  );
});

test('duplicate command keys cannot start while pending', () => {
  assert.equal(canStartCommand(new Set(['assign:1']), 'assign:1'), false);
  assert.equal(canStartCommand(new Set(['assign:1']), 'assign:2'), true);
});
