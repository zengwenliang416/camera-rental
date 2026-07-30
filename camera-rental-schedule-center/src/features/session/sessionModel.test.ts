import assert from 'node:assert/strict';
import test from 'node:test';

import { createSessionState, resetSessionState } from './sessionModel';

test('session bootstrap follows the existing access-token cache', () => {
  assert.deepEqual(createSessionState('token', { username: 'operator' }), {
    isLoggedIn: true,
    authRequired: false,
    currentUser: { username: 'operator' },
  });
  assert.deepEqual(createSessionState(undefined), {
    isLoggedIn: false,
    authRequired: true,
    currentUser: undefined,
  });
});

test('session reset removes identity and requires authentication', () => {
  assert.deepEqual(resetSessionState(), {
    isLoggedIn: false,
    authRequired: true,
    currentUser: undefined,
  });
});
