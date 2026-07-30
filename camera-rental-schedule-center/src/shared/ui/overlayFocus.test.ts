import assert from 'node:assert/strict';
import test from 'node:test';

import { overlayKeyAction } from './ConfirmDialogShell';

test('overlay key model dismisses on Escape and wraps both Tab boundaries', () => {
  assert.deepEqual(overlayKeyAction('Escape', false, 0, 3), { kind: 'dismiss' });
  assert.deepEqual(overlayKeyAction('Tab', true, 0, 3), {
    kind: 'focus',
    index: 2,
  });
  assert.deepEqual(overlayKeyAction('Tab', false, 2, 3), {
    kind: 'focus',
    index: 0,
  });
  assert.deepEqual(overlayKeyAction('Tab', false, 1, 3), { kind: 'none' });
});
