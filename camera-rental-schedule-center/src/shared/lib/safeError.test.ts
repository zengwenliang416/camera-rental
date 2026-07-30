import assert from 'node:assert/strict';
import test from 'node:test';
import { classifySafeError } from './safeError';

test('transport and authorization failures map to safe categories', () => {
  assert.equal(classifySafeError('Failed to fetch'), 'network');
  assert.equal(classifySafeError('HTTP 401'), 'authentication');
  assert.equal(classifySafeError('当前账号无权访问'), 'permission');
  assert.equal(classifySafeError('request timeout'), 'timeout');
  assert.equal(classifySafeError('PARTIAL_SYNC_FAILED'), 'partial');
  assert.equal(classifySafeError('third-party stack detail'), 'unknown');
});
