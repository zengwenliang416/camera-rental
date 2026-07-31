import assert from 'node:assert/strict';
import test, { after } from 'node:test';
import { flushSync } from 'react-dom';
import { Window } from 'happy-dom';

import { PreferenceProvider } from '../../preferences/PreferenceContext';
import { ScheduleDeviceTable } from './ScheduleDeviceTable';

const browser = new Window();
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

after(async () => {
  await browser.happyDOM.close();
});

test('renders device and tracking actions as sibling buttons', () => {
  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  const openedDevices: string[] = [];
  const openedTracking: string[] = [];

  flushSync(() => root.render(
    <PreferenceProvider>
      <ScheduleDeviceTable
        devices={[{
          id: '101',
          unitCode: '001号',
          sn: 'P4P-001-TEST',
          modelId: 'p4p',
          modelName: 'Pocket 4 Pro',
          status: 'RENTING',
        }]}
        blocks={[{
          id: '201',
          deviceId: '101',
          orderId: '71002',
          orderNumber: 'RO-71002',
          type: 'RENTAL',
          startDate: '2026-07-31',
          endDate: '2026-08-02',
        }]}
        days={[{
          dateStr: '2026-07-31',
          displayDay: '7/31',
          weekday: 'Fri',
          isToday: true,
        }]}
        viewMode="gantt"
        labels={{
          internalScroller: 'Timeline',
          noMatches: 'No devices',
          noMatchesDetail: 'No device matches',
          deviceIdentity: 'Device / SN',
          currentStatus: 'State',
          relatedOrder: 'Order',
          customer: 'Customer',
          expectedAvailable: 'Available',
          openDetail: 'Open detail',
          availableNow: 'Available now',
          free: 'Free',
          blockRental: 'Rental',
          blockReserve: 'Reserve',
          blockRepair: 'Repair',
          blockLock: 'Lock',
          statusIdle: 'Idle',
          statusRenting: 'Renting',
          statusReserved: 'Reserved',
          statusRepair: 'Repair',
          statusLocked: 'Locked',
        }}
        onOpenDevice={(deviceId) => openedDevices.push(deviceId)}
        onOpenOrder={() => undefined}
        onOpenTracking={(orderId) => openedTracking.push(orderId)}
        trackingByOrderId={{
          '71002': {
            rentalOrderId: 71002,
            packageCount: 1,
            statusCounts: { IN_TRANSIT: 1 },
            packages: [{
              deliveryId: 91002,
              rentalOrderId: 71002,
              direction: 'OUTBOUND',
              packageSeq: 1,
              trackingStatus: 'IN_TRANSIT',
              mappingStatus: 'READY',
              subscribeStatus: 'SUBSCRIBED',
              queryStatus: 'READY_QUERY',
              stale: false,
            }],
            risks: [],
          },
        }}
      />
    </PreferenceProvider>
  ));

  assert.equal(document.querySelectorAll('button button').length, 0);
  const buttons = Array.from(document.querySelectorAll<HTMLButtonElement>('button'));
  const deviceButton = buttons.find((button) => button.textContent?.includes('001号'));
  const trackingButton = buttons.find((button) => button.textContent?.includes('运输中'));
  assert.ok(deviceButton);
  assert.ok(trackingButton);

  flushSync(() => deviceButton.click());
  flushSync(() => trackingButton.click());
  assert.deepEqual(openedDevices, ['101']);
  assert.deepEqual(openedTracking, ['71002']);

  flushSync(() => root.unmount());
});
