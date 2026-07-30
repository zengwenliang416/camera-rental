import assert from 'node:assert/strict';
import test from 'node:test';

import type { ExceptionItem } from '../../types';
import { exceptionActions, exceptionSeverityTone, filterExceptions } from './exceptionModel';

const item: ExceptionItem = {
  id: '7',
  type: 'CONFLICT',
  title: '排期冲突',
  description: '服务端复核记录',
  relatedOrderId: '11',
  relatedDeviceId: '22',
  severity: 'high',
  createdTime: '2026-07-29 10:00:00',
  resolved: false,
};

test('exception filters and severity use server review state', () => {
  assert.equal(filterExceptions([item], 'OPEN').length, 1);
  assert.equal(filterExceptions([item], 'RESOLVED').length, 0);
  assert.equal(exceptionSeverityTone('high'), 'red');
});

test('exception actions require both relation and permission', () => {
  assert.deepEqual(exceptionActions(item, {
    canResolve: false,
    canAssign: true,
    canViewDevice: true,
  }), {
    canResolve: false,
    canAssign: true,
    canViewDevice: true,
  });
});
