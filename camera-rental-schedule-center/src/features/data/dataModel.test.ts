import assert from 'node:assert/strict';
import test from 'node:test';

import { queryHealth } from './dataModel';

test('query health keeps partial collection failures distinct from success', () => {
  assert.equal(queryHealth([]), 'ready');
  assert.equal(queryHealth(['schedules']), 'partial');
});
