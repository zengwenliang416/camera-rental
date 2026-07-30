import assert from 'node:assert/strict';
import test from 'node:test';

import { loginErrorPresentation, validateLoginCredentials } from './loginModel';

test('management password login requires username and password', () => {
  assert.equal(validateLoginCredentials(' ', 'secret'), 'username');
  assert.equal(validateLoginCredentials('admin', ''), 'password');
  assert.equal(validateLoginCredentials(' admin ', 'secret'), 'ready');
});

test('login errors expose only stable localized categories', () => {
  assert.equal(loginErrorPresentation('network'), 'network');
  assert.equal(loginErrorPresentation('authentication'), 'authentication');
  assert.equal(loginErrorPresentation('raw backend detail'), 'unknown');
});
