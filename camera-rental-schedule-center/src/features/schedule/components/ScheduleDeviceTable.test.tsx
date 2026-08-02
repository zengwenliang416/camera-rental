import assert from 'node:assert/strict';
import test, { after } from 'node:test';
import { flushSync } from 'react-dom';
import { Window } from 'happy-dom';

import { PreferenceProvider } from '../../preferences/PreferenceContext';
import { ScheduleDeviceTable } from './ScheduleDeviceTable';
import {
  filterDevicesByTracking,
  matchesTrackingFilter,
} from './ScheduleTrackingWorkspace';

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
          occupiedInRange: 'Occupied',
          statusIdle: 'Idle',
          statusRenting: 'Renting',
          statusReserved: 'Reserved',
          statusRepair: 'Repair',
          statusLocked: 'Locked',
        }}
        onOpenDevice={(deviceId) => openedDevices.push(deviceId)}
        onOpenOrder={() => undefined}
        onOpenTracking={(orderId) => openedTracking.push(orderId)}
        orderNumberByOrderId={{ '71002': '3313011255890094097' }}
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
  const trackingButton = buttons.find(
    (button) => button.textContent?.includes('3313011255890094097')
      && button.textContent?.includes('运输中')
  );
  assert.ok(deviceButton);
  assert.ok(trackingButton);

  flushSync(() => deviceButton.click());
  flushSync(() => trackingButton.click());
  assert.deepEqual(openedDevices, ['101']);
  assert.deepEqual(openedTracking, ['71002']);

  flushSync(() => root.unmount());
});

test('filters schedule devices by order-linked tracking state', () => {
  const summaries = {
    '71002': {
      rentalOrderId: 71002,
      packageCount: 1,
      statusCounts: { IN_TRANSIT: 1 },
      packages: [{
        deliveryId: 91002,
        rentalOrderId: 71002,
        direction: 'OUTBOUND' as const,
        packageSeq: 1,
        trackingStatus: 'IN_TRANSIT' as const,
        mappingStatus: 'READY',
        subscribeStatus: 'SUBSCRIBED',
        queryStatus: 'READY_QUERY',
        stale: false,
      }],
      risks: [],
    },
    '71003': {
      rentalOrderId: 71003,
      packageCount: 1,
      statusCounts: { DELIVERED: 1 },
      packages: [{
        deliveryId: 91003,
        rentalOrderId: 71003,
        direction: 'OUTBOUND' as const,
        packageSeq: 1,
        trackingStatus: 'DELIVERED' as const,
        mappingStatus: 'READY',
        subscribeStatus: 'SUBSCRIBED',
        queryStatus: 'READY_QUERY',
        stale: false,
      }],
      risks: [],
    },
  };
  const devices = [{ id: '101' }, { id: '102' }, { id: '103' }];
  const blocks = [{
    id: '201',
    deviceId: '101',
    orderId: '71002',
    type: 'RENTAL' as const,
    startDate: '2026-08-01',
    endDate: '2026-08-02',
  }, {
    id: '202',
    deviceId: '102',
    orderId: '71003',
    type: 'RENTAL' as const,
    startDate: '2026-08-01',
    endDate: '2026-08-02',
  }];

  assert.equal(matchesTrackingFilter(summaries['71002'], 'ACTIVE'), true);
  assert.equal(matchesTrackingFilter(summaries['71003'], 'ACTIVE'), false);
  assert.deepEqual(
    filterDevicesByTracking(devices, blocks, summaries, 'DELIVERED').map((item) => item.id),
    ['102']
  );
  assert.deepEqual(
    filterDevicesByTracking(devices, blocks, summaries, 'ALL').map((item) => item.id),
    ['101', '102', '103']
  );
});
