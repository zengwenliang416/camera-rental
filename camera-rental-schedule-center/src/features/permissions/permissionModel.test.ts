import assert from 'node:assert/strict';
import test from 'node:test';

import { hasScheduleCenterAccess, permissionAllows } from './permissionModel';

test('permission checks support wildcard and any-of requirements', () => {
  assert.equal(permissionAllows(['*:*:*'], 'rental:device:query'), true);
  assert.equal(
    permissionAllows(['rental:xianyu:query'], ['rental:device:query', 'rental:xianyu:query']),
    true
  );
  assert.equal(permissionAllows([], 'rental:device:query'), false);
});

test('schedule center access requires one approved read permission', () => {
  assert.equal(hasScheduleCenterAccess(['rental:schedule:query']), true);
  assert.equal(hasScheduleCenterAccess(['rental:xianyu:ship']), true);
  assert.equal(hasScheduleCenterAccess(['rental:review:query']), true);
  assert.equal(hasScheduleCenterAccess(['system:user:query']), false);
});
