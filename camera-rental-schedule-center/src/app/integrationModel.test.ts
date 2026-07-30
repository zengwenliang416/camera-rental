import assert from 'node:assert/strict';
import test from 'node:test';
import { integrationReadiness } from './integrationModel';

const config = {
  enabled: true,
  status: 'READY',
  appSecretConfigured: true,
  webhookBaseUrlConfigured: true,
  writeEnabled: false,
};

test('integration readiness distinguishes every visible state', () => {
  assert.equal(integrationReadiness(null, true, false), 'loading');
  assert.equal(integrationReadiness(null, false, true), 'unavailable');
  assert.equal(
    integrationReadiness({ ...config, enabled: false, status: 'DISABLED' }, false, false),
    'disabled'
  );
  assert.equal(integrationReadiness(config, false, false), 'read-only');
  assert.equal(integrationReadiness({ ...config, writeEnabled: true }, false, false), 'ready');
});
