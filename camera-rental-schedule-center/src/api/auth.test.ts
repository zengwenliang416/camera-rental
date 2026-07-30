import assert from 'node:assert/strict';
import test from 'node:test';

import {
  clearCachedPermissionInfo,
  getAccessToken,
  removeTokenPair,
  setTokenPair,
} from './auth';

test('authentication cache access is safe when browser storage is unavailable', () => {
  assert.doesNotThrow(() => {
    getAccessToken();
    setTokenPair({ accessToken: 'temporary', refreshToken: 'temporary' });
    removeTokenPair();
    clearCachedPermissionInfo();
  });
});
