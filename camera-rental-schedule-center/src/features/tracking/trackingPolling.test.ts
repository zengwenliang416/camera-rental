import assert from 'node:assert/strict';
import test from 'node:test';

import { startVisibleSummaryPolling } from './trackingPolling';

test('polls every sixty seconds only while visible and refreshes immediately on return', () => {
  let hidden = false;
  let visibilityListener: (() => void) | null = null;
  let intervalCallback: (() => void) | null = null;
  let intervalMs = 0;
  let clearCount = 0;
  let refreshCount = 0;

  const stop = startVisibleSummaryPolling({
    visibility: {
      get hidden() {
        return hidden;
      },
      addEventListener: (_type, listener) => {
        visibilityListener = listener;
      },
      removeEventListener: () => {
        visibilityListener = null;
      },
    },
    scheduler: {
      setInterval: (callback, nextIntervalMs) => {
        intervalCallback = callback;
        intervalMs = nextIntervalMs;
        return 1 as unknown as ReturnType<typeof setInterval>;
      },
      clearInterval: () => {
        clearCount += 1;
        intervalCallback = null;
      },
    },
    refresh: () => {
      refreshCount += 1;
    },
  });

  assert.equal(intervalMs, 60_000);
  intervalCallback?.();
  assert.equal(refreshCount, 1);

  hidden = true;
  visibilityListener?.();
  assert.equal(clearCount, 1);
  assert.equal(intervalCallback, null);

  hidden = false;
  visibilityListener?.();
  assert.equal(refreshCount, 2);
  assert.equal(intervalMs, 60_000);

  stop();
  assert.equal(clearCount, 2);
  assert.equal(visibilityListener, null);
});

test('does not start a timer while initially hidden', () => {
  let timerCreated = false;
  const stop = startVisibleSummaryPolling({
    visibility: {
      hidden: true,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    },
    scheduler: {
      setInterval: () => {
        timerCreated = true;
        return 1 as unknown as ReturnType<typeof setInterval>;
      },
      clearInterval: () => undefined,
    },
    refresh: () => undefined,
  });

  assert.equal(timerCreated, false);
  stop();
});
