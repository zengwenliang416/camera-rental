import assert from 'node:assert/strict';
import test, { after, afterEach } from 'node:test';
import { useEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import { Window } from 'happy-dom';

import {
  clearCachedPermissionInfo,
  getCachedPermissionInfo,
  removeTokenPair,
  setCachedPermissionInfo,
  setTokenPair,
} from '../../api/auth';
import type { SnapshotAccess } from '../../api/rental';
import { ScheduleCenterCommandsProvider } from '../commands/ScheduleCenterCommandsContext';
import { useScheduleCenterCommands } from '../commands/ScheduleCenterCommandsContext';
import { ScheduleCenterDataProvider, useScheduleCenterData } from '../data/ScheduleCenterDataContext';
import { PermissionProvider, usePermissions } from '../permissions/PermissionContext';
import { SessionProvider, useSession } from '../session/SessionContext';
import { DeliveryTrackingProvider, useDeliveryTracking } from '../tracking/TrackingContext';

const browser = new Window();
const originalFetch = globalThis.fetch;
Object.assign(globalThis, {
  window: browser,
  document: browser.document,
  HTMLElement: browser.HTMLElement,
  Event: browser.Event,
  MouseEvent: browser.MouseEvent,
});
Object.defineProperty(globalThis, 'navigator', {
  configurable: true,
  value: browser.navigator,
});
const { createRoot } = await import('react-dom/client');

afterEach(() => {
  globalThis.fetch = originalFetch;
  removeTokenPair();
  clearCachedPermissionInfo();
  document.body.innerHTML = '';
});

after(async () => {
  await browser.happyDOM.close();
});

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((nextResolve, nextReject) => {
    resolve = nextResolve;
    reject = nextReject;
  });
  return { promise, reject, resolve };
}

async function waitFor(assertion: () => void, timeoutMs = 2000) {
  const started = Date.now();
  while (true) {
    try {
      assertion();
      return;
    } catch (error) {
      if (Date.now() - started >= timeoutMs) throw error;
      await new Promise((resolve) => setTimeout(resolve, 5));
    }
  }
}

function emptySnapshot(channelOrders: number) {
  return {
    devices: [],
    schedules: [],
    channelOrders: [],
    pendingShipOrders: [],
    reviews: [],
    totals: {
      devices: 0,
      schedules: 0,
      channelOrders,
      pendingShipOrders: 0,
      reviews: 0,
    },
    failures: [],
  };
}

function PermissionProbe() {
  const permission = usePermissions();
  const session = useSession();
  return (
    <>
      <output data-testid="permissions">{permission.permissions.join(',')}</output>
      <output data-testid="logged-in">{String(session.isLoggedIn)}</output>
      <output data-testid="current-user">
        {session.currentUser?.username || 'none'}
      </output>
      <output data-testid="permission-error">
        {permission.permissionError || 'none'}
      </output>
      <button type="button" id="refresh-permissions" onClick={() => void permission.refreshPermissions()}>
        Refresh
      </button>
      <button type="button" id="require-auth" onClick={session.requireAuthentication}>
        Reset
      </button>
      <button
        type="button"
        id="switch-session"
        onClick={() => void session.login({
          username: 'new-user',
          password: 'secret',
        })}
      >
        Switch
      </button>
    </>
  );
}

function DataProbe() {
  const data = useScheduleCenterData();
  const session = useSession();
  return (
    <>
      <output data-testid="sync-orders">{data.lastSyncOrderCount}</output>
      <output data-testid="sync-at">{data.lastSyncAt || 'none'}</output>
      <output data-testid="config-unavailable">
        {String(data.xianyuConfigUnavailable)}
      </output>
      <output data-testid="data-error">{data.dataError || 'none'}</output>
      <button type="button" id="refresh-data" onClick={() => void data.refreshData()}>
        Refresh
      </button>
      <button type="button" id="reset-data-session" onClick={session.requireAuthentication}>
        Reset
      </button>
    </>
  );
}

function CommandProbe() {
  const command = useScheduleCenterCommands();
  const session = useSession();
  const [completed, setCompleted] = useState(0);
  return (
    <>
      <output data-testid="command-error">{command.commandError || 'none'}</output>
      <output data-testid="auth-required">{String(session.authRequired)}</output>
      <output data-testid="command-logged-in">{String(session.isLoggedIn)}</output>
      <output data-testid="command-user">
        {session.currentUser?.username || 'none'}
      </output>
      <output data-testid="command-pending">{String(command.isCommandPending)}</output>
      <output data-testid="completed">{completed}</output>
      <button
        type="button"
        id="resolve-command"
        onClick={() => {
          void command.resolveException('1').then(() => {
            setCompleted((value) => value + 1);
          });
        }}
      >
        Resolve
      </button>
      <button
        type="button"
        id="resolve-command-2"
        onClick={() => {
          void command.resolveException('2').then(() => {
            setCompleted((value) => value + 1);
          });
        }}
      >
        Resolve second
      </button>
      <button
        type="button"
        id="switch-command-session"
        onClick={() => void session.login({
          username: 'new-user',
          password: 'secret',
        })}
      >
        Switch
      </button>
    </>
  );
}

function TrackingDetailProbe() {
  const { getDetailState, loadDetail } = useDeliveryTracking();
  const detailState = getDetailState(91002);

  useEffect(() => {
    void loadDetail(91002);
  }, [loadDetail]);

  return (
    <>
      <output data-testid="tracking-loading">{String(detailState.isLoading)}</output>
      <output data-testid="tracking-status">
        {detailState.detail?.trackingStatus || 'none'}
      </output>
      <output data-testid="tracking-traces">
        {detailState.detail?.traces.length || 0}
      </output>
    </>
  );
}

test('permission provider ignores superseded and logged-out responses', async () => {
  setTokenPair({ accessToken: 'access', refreshToken: 'refresh' });
  setCachedPermissionInfo({ permissions: [] });
  const first = deferred<{ permissions: string[]; user?: { username?: string } }>();
  const third = deferred<{ permissions: string[]; user?: { username?: string } }>();
  let calls = 0;
  const loadPermissionInfo = () => {
    calls += 1;
    if (calls === 1) return first.promise;
    if (calls === 3) return third.promise;
    return Promise.resolve({
      permissions: ['rental:device:query'],
      user: { username: 'new-user' },
    });
  };

  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(
    <SessionProvider>
      <PermissionProvider loadPermissionInfo={loadPermissionInfo}>
        <PermissionProbe />
      </PermissionProvider>
    </SessionProvider>
  ));
  await waitFor(() => assert.equal(calls, 1));

  flushSync(() => document.querySelector<HTMLButtonElement>('#refresh-permissions')!.click());
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="permissions"]')?.textContent,
    'rental:device:query'
  ));
  first.resolve({ permissions: ['rental:xianyu:query'] });
  await new Promise((resolve) => setTimeout(resolve, 0));
  assert.equal(
    document.querySelector('[data-testid="permissions"]')?.textContent,
    'rental:device:query'
  );

  flushSync(() => document.querySelector<HTMLButtonElement>('#refresh-permissions')!.click());
  await waitFor(() => assert.equal(calls, 3));
  flushSync(() => document.querySelector<HTMLButtonElement>('#require-auth')!.click());
  third.resolve({ permissions: ['*:*:*'] });
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="logged-in"]')?.textContent,
    'false'
  ));
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="permissions"]')?.textContent,
    ''
  ));

  flushSync(() => root.unmount());
});

test('data provider ignores old snapshots and skips config without query permission', async () => {
  setTokenPair({ accessToken: 'access', refreshToken: 'refresh' });
  const first = deferred<ReturnType<typeof emptySnapshot>>();
  const third = deferred<ReturnType<typeof emptySnapshot>>();
  let snapshotCalls = 0;
  let configCalls = 0;
  const loadPermissionInfo = () => Promise.resolve({
    permissions: ['rental:xianyu:ship'],
  });
  const loadSnapshot = (_access: SnapshotAccess) => {
    snapshotCalls += 1;
    if (snapshotCalls === 1) return first.promise;
    if (snapshotCalls === 3) return third.promise;
    return Promise.resolve(emptySnapshot(2));
  };

  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(
    <SessionProvider>
      <PermissionProvider loadPermissionInfo={loadPermissionInfo}>
        <ScheduleCenterDataProvider
          loadSnapshot={loadSnapshot}
          loadXianyuConfig={() => {
            configCalls += 1;
            return Promise.reject(new Error('should not run'));
          }}
          now={() => 1234}
        >
          <DataProbe />
        </ScheduleCenterDataProvider>
      </PermissionProvider>
    </SessionProvider>
  ));
  await waitFor(() => assert.equal(snapshotCalls, 1));

  flushSync(() => document.querySelector<HTMLButtonElement>('#refresh-data')!.click());
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="sync-orders"]')?.textContent,
    '2'
  ));
  first.resolve(emptySnapshot(1));
  await new Promise((resolve) => setTimeout(resolve, 0));
  assert.equal(
    document.querySelector('[data-testid="sync-orders"]')?.textContent,
    '2'
  );
  assert.equal(configCalls, 0);
  assert.equal(
    document.querySelector('[data-testid="config-unavailable"]')?.textContent,
    'false'
  );

  flushSync(() => document.querySelector<HTMLButtonElement>('#refresh-data')!.click());
  await waitFor(() => assert.equal(snapshotCalls, 3));
  flushSync(() => document.querySelector<HTMLButtonElement>('#reset-data-session')!.click());
  third.resolve(emptySnapshot(9));
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="data-error"]')?.textContent,
    'AUTH_REQUIRED'
  ));
  assert.equal(
    document.querySelector('[data-testid="sync-at"]')?.textContent,
    'none'
  );

  flushSync(() => root.unmount());
});

test('command provider refreshes after success and resets session on authentication failure', async () => {
  setTokenPair({ accessToken: 'access', refreshToken: 'refresh' });
  let snapshotCalls = 0;
  let commandCalls = 0;
  let authFailure = false;
  const loadPermissionInfo = () => Promise.resolve({
    permissions: ['rental:review:query', 'rental:review:update'],
  });
  const loadSnapshot = () => {
    snapshotCalls += 1;
    return Promise.resolve(emptySnapshot(snapshotCalls));
  };

  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(
    <SessionProvider>
      <PermissionProvider loadPermissionInfo={loadPermissionInfo}>
        <ScheduleCenterDataProvider
          loadSnapshot={loadSnapshot}
          now={() => snapshotCalls}
        >
          <ScheduleCenterCommandsProvider
            services={{
              resolveManualReview: async () => {
                commandCalls += 1;
                if (authFailure) throw new Error('AUTH_REQUIRED');
              },
            }}
          >
            <CommandProbe />
          </ScheduleCenterCommandsProvider>
        </ScheduleCenterDataProvider>
      </PermissionProvider>
    </SessionProvider>
  ));
  await waitFor(() => assert.equal(snapshotCalls, 1));

  flushSync(() => document.querySelector<HTMLButtonElement>('#resolve-command')!.click());
  await waitFor(() => assert.equal(snapshotCalls, 2));
  assert.equal(commandCalls, 1);
  assert.equal(
    document.querySelector('[data-testid="command-error"]')?.textContent,
    'none'
  );

  authFailure = true;
  flushSync(() => document.querySelector<HTMLButtonElement>('#resolve-command')!.click());
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="auth-required"]')?.textContent,
    'true'
  ));
  assert.equal(commandCalls, 2);
  assert.equal(snapshotCalls, 2);
  assert.equal(
    document.querySelector('[data-testid="command-error"]')?.textContent,
    'none'
  );

  flushSync(() => root.unmount());
});

test('successful relogin hides prior identity, permissions, and data without old-cache fallback', async () => {
  setTokenPair({ accessToken: 'old-access', refreshToken: 'old-refresh' });
  setCachedPermissionInfo({
    permissions: ['rental:xianyu:query'],
    user: { username: 'old-user' },
  });
  const nextPermission = deferred<{
    permissions: string[];
    user?: { username?: string };
  }>();
  let permissionCalls = 0;
  const loadPermissionInfo = () => {
    permissionCalls += 1;
    if (permissionCalls === 1) {
      return Promise.resolve({
        permissions: ['rental:xianyu:query'],
        user: { username: 'old-user' },
      });
    }
    return nextPermission.promise;
  };

  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(
    <SessionProvider
      loginService={async () => {
        setTokenPair({ accessToken: 'new-access', refreshToken: 'new-refresh' });
        return { accessToken: 'new-access', refreshToken: 'new-refresh' };
      }}
    >
      <PermissionProvider loadPermissionInfo={loadPermissionInfo}>
        <ScheduleCenterDataProvider
          loadSnapshot={(access) => Promise.resolve(
            emptySnapshot(access.orders ? 4 : 0)
          )}
          now={() => 1234}
        >
          <PermissionProbe />
          <DataProbe />
        </ScheduleCenterDataProvider>
      </PermissionProvider>
    </SessionProvider>
  ));

  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="sync-orders"]')?.textContent,
    '4'
  ));
  assert.equal(
    document.querySelector('[data-testid="current-user"]')?.textContent,
    'old-user'
  );

  flushSync(() => document.querySelector<HTMLButtonElement>('#switch-session')!.click());
  await waitFor(() => assert.equal(permissionCalls, 2));
  assert.equal(getCachedPermissionInfo(), undefined);
  assert.equal(
    document.querySelector('[data-testid="current-user"]')?.textContent,
    'none'
  );
  assert.equal(
    document.querySelector('[data-testid="permissions"]')?.textContent,
    ''
  );
  assert.equal(
    document.querySelector('[data-testid="sync-orders"]')?.textContent,
    '0'
  );

  nextPermission.reject(new Error('new-session permission unavailable'));
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="permission-error"]')?.textContent,
    'PERMISSION_SYNC_FAILED'
  ));
  assert.equal(
    document.querySelector('[data-testid="permissions"]')?.textContent,
    ''
  );
  assert.equal(
    document.querySelector('[data-testid="current-user"]')?.textContent,
    'none'
  );

  flushSync(() => root.unmount());
});

test('distinct commands remain independently current within one provider revision', async () => {
  setTokenPair({ accessToken: 'access', refreshToken: 'refresh' });
  const first = deferred<void>();
  const second = deferred<void>();
  let snapshotCalls = 0;

  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(
    <SessionProvider>
      <PermissionProvider
        loadPermissionInfo={() => Promise.resolve({
          permissions: ['rental:review:query', 'rental:review:update'],
        })}
      >
        <ScheduleCenterDataProvider
          loadSnapshot={() => {
            snapshotCalls += 1;
            return Promise.resolve(emptySnapshot(snapshotCalls));
          }}
          now={() => snapshotCalls}
        >
          <ScheduleCenterCommandsProvider
            services={{
              resolveManualReview: ({ id }) => id === 1 ? first.promise : second.promise,
            }}
          >
            <CommandProbe />
          </ScheduleCenterCommandsProvider>
        </ScheduleCenterDataProvider>
      </PermissionProvider>
    </SessionProvider>
  ));

  await waitFor(() => assert.equal(snapshotCalls, 1));
  flushSync(() => {
    document.querySelector<HTMLButtonElement>('#resolve-command')!.click();
    document.querySelector<HTMLButtonElement>('#resolve-command-2')!.click();
  });
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="command-pending"]')?.textContent,
    'true'
  ));

  first.resolve();
  await waitFor(() => assert.equal(snapshotCalls, 2));
  assert.equal(
    document.querySelector('[data-testid="command-pending"]')?.textContent,
    'true'
  );

  second.resolve();
  await waitFor(() => assert.equal(snapshotCalls, 3));
  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="command-pending"]')?.textContent,
    'false'
  ));
  assert.equal(
    document.querySelector('[data-testid="completed"]')?.textContent,
    '2'
  );

  flushSync(() => root.unmount());
});

test('tracking detail loader stays stable after detail state updates', async () => {
  setTokenPair({ accessToken: 'access', refreshToken: 'refresh' });
  let summaryCalls = 0;
  let detailCalls = 0;

  globalThis.fetch = async (input) => {
    const url = String(input instanceof Request ? input.url : input);
    if (url.includes('/rental/delivery/tracking-summary/batch')) {
      summaryCalls += 1;
      return new Response(JSON.stringify({
        code: 0,
        data: {
          '71002': {
            orderId: 71002,
            packageCount: 1,
            statusCounts: { IN_TRANSIT: 1 },
            packages: [{
              deliveryId: 91002,
              direction: 'OUTBOUND',
              packageSeq: 1,
              carrierName: 'Test carrier',
              maskedWaybillNo: 'TEST****0002',
              trackingStatus: 'IN_TRANSIT',
              mappingStatus: 'READY',
              subscribeStatus: 'SUBSCRIBED',
              queryStatus: 'READY_QUERY',
              stale: false,
            }],
            risks: [],
          },
        },
      }), {
        headers: { 'Content-Type': 'application/json' },
        status: 200,
      });
    }
    if (url.includes('/rental/delivery/91002/tracking')) {
      detailCalls += 1;
      return new Response(JSON.stringify({
        code: 0,
        data: {
          deliveryId: 91002,
          rentalOrderId: 71002,
          direction: 'OUTBOUND',
          packageSeq: 1,
          carrierName: 'Test carrier',
          maskedWaybillNo: 'TEST****0002',
          trackingStatus: 'IN_TRANSIT',
          mappingStatus: 'READY',
          subscribeStatus: 'SUBSCRIBED',
          queryStatus: 'READY_QUERY',
          stale: false,
          risks: [],
          devices: [{
            deviceId: 101,
            deviceNo: 'P4P-001-TEST',
            equipmentModelCode: 'P4P',
          }],
          traces: [{
            eventSeq: 1,
            businessTime: '2026-07-31T10:00:00+08:00',
            trackingStatus: 'IN_TRANSIT',
            traceText: 'In transit',
          }],
        },
      }), {
        headers: { 'Content-Type': 'application/json' },
        status: 200,
      });
    }
    throw new Error(`Unexpected request: ${url}`);
  };

  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(
    <SessionProvider>
      <PermissionProvider
        loadPermissionInfo={() => Promise.resolve({
          permissions: [
            'rental:device:query',
            'rental:schedule:query',
            'rental:delivery:tracking',
          ],
        })}
      >
        <ScheduleCenterDataProvider
          loadSnapshot={() => Promise.resolve({
            devices: [],
            schedules: [{
              id: 201,
              deviceId: 101,
              rentalOrderId: 71002,
              scheduleType: 'RENTAL',
              status: 'EFFECTIVE',
              occupyStartDate: '2026-07-31',
              occupyEndDateExclusive: '2026-08-06',
            }],
            channelOrders: [],
            pendingShipOrders: [],
            reviews: [],
            totals: {
              devices: 0,
              schedules: 1,
              channelOrders: 0,
              pendingShipOrders: 0,
              reviews: 0,
            },
            failures: [],
          })}
        >
          <DeliveryTrackingProvider>
            <TrackingDetailProbe />
          </DeliveryTrackingProvider>
        </ScheduleCenterDataProvider>
      </PermissionProvider>
    </SessionProvider>
  ));

  await waitFor(() => assert.equal(
    document.querySelector('[data-testid="tracking-status"]')?.textContent,
    'IN_TRANSIT'
  ));
  await new Promise((resolve) => setTimeout(resolve, 30));

  assert.equal(summaryCalls, 1);
  assert.equal(detailCalls, 1);
  assert.equal(
    document.querySelector('[data-testid="tracking-loading"]')?.textContent,
    'false'
  );
  assert.equal(
    document.querySelector('[data-testid="tracking-traces"]')?.textContent,
    '1'
  );

  flushSync(() => root.unmount());
});

for (const outcome of ['success', 'failure', 'authentication'] as const) {
  test(`stale ${outcome} command completion cannot affect a newer session`, async () => {
    setTokenPair({ accessToken: 'old-access', refreshToken: 'old-refresh' });
    const command = deferred<void>();
    let permissionCalls = 0;
    let snapshotCalls = 0;

    document.body.innerHTML = '<div id="root"></div>';
    const root = createRoot(document.querySelector('#root')!);
    flushSync(() => root.render(
      <SessionProvider
        loginService={async () => {
          setTokenPair({ accessToken: 'new-access', refreshToken: 'new-refresh' });
          return { accessToken: 'new-access', refreshToken: 'new-refresh' };
        }}
      >
        <PermissionProvider
          loadPermissionInfo={() => {
            permissionCalls += 1;
            return Promise.resolve({
              permissions: ['rental:review:query', 'rental:review:update'],
              user: { username: permissionCalls === 1 ? 'old-user' : 'new-user' },
            });
          }}
        >
          <ScheduleCenterDataProvider
            loadSnapshot={() => {
              snapshotCalls += 1;
              return Promise.resolve(emptySnapshot(snapshotCalls));
            }}
            now={() => snapshotCalls}
          >
            <ScheduleCenterCommandsProvider
              services={{
                resolveManualReview: () => command.promise,
              }}
            >
              <CommandProbe />
            </ScheduleCenterCommandsProvider>
          </ScheduleCenterDataProvider>
        </PermissionProvider>
      </SessionProvider>
    ));

    await waitFor(() => assert.equal(snapshotCalls, 1));
    flushSync(() => document.querySelector<HTMLButtonElement>('#resolve-command')!.click());
    await waitFor(() => assert.equal(
      document.querySelector('[data-testid="command-pending"]')?.textContent,
      'true'
    ));

    flushSync(() => {
      document.querySelector<HTMLButtonElement>('#switch-command-session')!.click();
    });
    await waitFor(() => assert.equal(permissionCalls, 2));
    await waitFor(() => assert.equal(
      document.querySelector('[data-testid="command-pending"]')?.textContent,
      'false'
    ));
    await waitFor(() => assert.equal(
      document.querySelector('[data-testid="command-logged-in"]')?.textContent,
      'true'
    ));
    await waitFor(() => assert.equal(
      document.querySelector('[data-testid="command-user"]')?.textContent,
      'new-user'
    ));
    await new Promise((resolve) => setTimeout(resolve, 20));
    const snapshotsAfterSwitch = snapshotCalls;

    if (outcome === 'success') {
      command.resolve();
    } else if (outcome === 'authentication') {
      command.reject(new Error('AUTH_REQUIRED'));
    } else {
      command.reject(new Error('stale command failed'));
    }
    await new Promise((resolve) => setTimeout(resolve, 20));

    assert.equal(
      document.querySelector('[data-testid="command-logged-in"]')?.textContent,
      'true'
    );
    assert.equal(
      document.querySelector('[data-testid="auth-required"]')?.textContent,
      'false'
    );
    assert.equal(
      document.querySelector('[data-testid="command-error"]')?.textContent,
      'none'
    );
    assert.equal(snapshotCalls, snapshotsAfterSwitch);

    flushSync(() => root.unmount());
  });
}
