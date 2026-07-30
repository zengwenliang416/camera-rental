import assert from 'node:assert/strict';
import test from 'node:test';

import { deviceQrStateFromResult, isSignedDeviceQr } from './deviceQrModel';

test('signed QR state separates permission, loading, success, and safe error', () => {
  assert.deepEqual(deviceQrStateFromResult(false, {}), { status: 'forbidden' });
  assert.deepEqual(deviceQrStateFromResult(true, { loading: true }), { status: 'loading' });
  assert.deepEqual(deviceQrStateFromResult(true, {
    response: {
      payload: 'CRD1|device|model|sig',
      payloadVersion: 'CRD1',
      signed: true,
    },
  }), {
    status: 'ready',
    payload: 'CRD1|device|model|sig',
  });
  assert.deepEqual(deviceQrStateFromResult(true, { failed: true }), { status: 'error' });
});

test('unsigned or malformed QR responses are never rendered', () => {
  assert.equal(isSignedDeviceQr({
    payload: 'device-only',
    payloadVersion: 'legacy',
    signed: false,
  }), false);
  assert.deepEqual(deviceQrStateFromResult(true, {
    response: {
      payload: 'device-only',
      payloadVersion: 'legacy',
      signed: false,
    },
  }), { status: 'error' });
});
