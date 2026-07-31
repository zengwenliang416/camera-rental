import assert from 'node:assert/strict';
import test from 'node:test';

import { fetchAllPages } from './pagination';

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((nextResolve) => {
    resolve = nextResolve;
  });
  return { promise, resolve };
}

test('known page counts load remaining pages concurrently and preserve order', async () => {
  const pending = new Map<number, ReturnType<typeof deferred<{ list: number[]; total: number }>>>();
  const started: number[] = [];
  const load = fetchAllPages(async (pageNo) => {
    started.push(pageNo);
    if (pageNo === 1) {
      return { list: [1, 2], total: 8 };
    }
    const page = deferred<{ list: number[]; total: number }>();
    pending.set(pageNo, page);
    return page.promise;
  }, 2, 3);

  await new Promise((resolve) => setTimeout(resolve, 0));
  assert.deepEqual(started, [1, 2, 3, 4]);

  pending.get(4)!.resolve({ list: [7, 8], total: 8 });
  pending.get(2)!.resolve({ list: [3, 4], total: 8 });
  pending.get(3)!.resolve({ list: [5, 6], total: 8 });

  assert.deepEqual(await load, {
    list: [1, 2, 3, 4, 5, 6, 7, 8],
    total: 8,
  });
});

test('unknown totals retain sequential short-page termination', async () => {
  const pages = new Map([
    [1, [1, 2]],
    [2, [3, 4]],
    [3, [5]],
  ]);
  const started: number[] = [];

  const result = await fetchAllPages(async (pageNo) => {
    started.push(pageNo);
    return { list: pages.get(pageNo) || [], total: 0 };
  }, 2);

  assert.deepEqual(started, [1, 2, 3]);
  assert.deepEqual(result, {
    list: [1, 2, 3, 4, 5],
    total: 5,
  });
});
