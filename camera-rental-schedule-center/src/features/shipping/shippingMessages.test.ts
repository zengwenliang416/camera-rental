import assert from 'node:assert/strict';
import test from 'node:test';

import {
  shippingMessage,
  shippingMessageKeys,
} from './shippingMessages';

test('shipping feature provides complete Chinese and English copy', () => {
  assert.ok(shippingMessageKeys.length > 100);
  for (const key of shippingMessageKeys) {
    assert.ok(shippingMessage('zh-CN', key).trim(), `missing zh-CN copy for ${key}`);
    const english = shippingMessage('en', key);
    assert.ok(english.trim(), `missing English copy for ${key}`);
    assert.doesNotMatch(english, /[\u4e00-\u9fff]/, `English copy contains Chinese for ${key}`);
  }
});

test('shipping messages interpolate identifiers without changing business data', () => {
  assert.equal(
    shippingMessage('en', 'runtime.deviceSelected', {
      unit: '05',
      sn: '5WTCN7F002B088',
    }),
    'Selected 05 · SN 5WTCN7F002B088'
  );
  assert.equal(shippingMessage('zh-CN', 'order.quantity', { count: 4 }), '4 件');
});
