import assert from 'node:assert/strict';
import test, { after } from 'node:test';
import { Window } from 'happy-dom';

const browser = new Window();
browser.happyDOM.setURL('http://localhost');
Object.assign(globalThis, {
  window: browser,
  document: browser.document,
  localStorage: browser.localStorage,
});

const {
  getAccessToken,
  getRefreshToken,
  setTenantId,
  setTokenPair,
} = await import('./auth');
const { apiClient } = await import('./client');
const originalFetch = globalThis.fetch;

after(async () => {
  globalThis.fetch = originalFetch;
  await browser.happyDOM.close();
});

test('a second 401 after token refresh resets authentication', async () => {
  setTokenPair({ accessToken: 'expired', refreshToken: 'refresh' });
  let calls = 0;
  globalThis.fetch = async () => {
    calls += 1;
    if (calls === 2) {
      return new Response(JSON.stringify({
        code: 0,
        data: { accessToken: 'retried', refreshToken: 'refresh-2' },
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    }
    return new Response(JSON.stringify({ code: 401, data: null }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' },
    });
  };

  await assert.rejects(
    () => apiClient.get('/test/repeated-401'),
    (error: unknown) => error instanceof Error && error.message === 'AUTH_REQUIRED'
  );
  assert.equal(calls, 3);
  assert.equal(getAccessToken(), undefined);
  assert.equal(getRefreshToken(), undefined);
});

test('delete requests use the shared authenticated request path', async () => {
  setTokenPair({
    accessToken: 'delete-access-token',
    refreshToken: 'delete-refresh-token',
  });
  setTenantId(7);

  let request: RequestInit | undefined;
  globalThis.fetch = async (_input, init) => {
    request = init;
    return new Response(JSON.stringify({ code: 0, data: true }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  };

  assert.equal(await apiClient.delete<boolean>('/rental/logistics/mapping/1'), true);
  assert.equal(request?.method, 'DELETE');
  assert.equal((request?.headers as Record<string, string>).Authorization, 'Bearer delete-access-token');
  assert.equal((request?.headers as Record<string, string>)['tenant-id'], '7');
});
