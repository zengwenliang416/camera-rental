import assert from 'node:assert/strict';
import test from 'node:test';

import {
  initialOperationsResourceState,
  operationsResourceReducer,
} from './operationsState';

test('panel resources load and fail independently while preserving prior data', () => {
  const initial = initialOperationsResourceState(['provider']);
  const loading = operationsResourceReducer(initial, { type: 'load' });
  const failed = operationsResourceReducer(loading, {
    type: 'failure',
    error: 'network',
  });

  assert.equal(loading.status, 'loading');
  assert.deepEqual(failed, {
    status: 'error',
    data: ['provider'],
    error: 'network',
  });
});

test('panel resources distinguish ready and explicit empty states', () => {
  const initial = initialOperationsResourceState<string[]>();
  const empty = operationsResourceReducer(initial, {
    type: 'success',
    data: [],
    empty: true,
  });
  const ready = operationsResourceReducer(initial, {
    type: 'success',
    data: ['task'],
  });

  assert.equal(empty.status, 'empty');
  assert.equal(ready.status, 'ready');
});

