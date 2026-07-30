import assert from 'node:assert/strict';
import test from 'node:test';

import type { DeviceInstance } from '../../types';
import {
  deviceCardPresentation,
  deviceStatusTone,
  filterDevices,
  registeredAssetSummary,
} from './deviceModel';

const devices: DeviceInstance[] = [
  {
    id: '8',
    unitCode: 'P3-27',
    sn: '5WTCN7F002B088',
    modelId: 'p3',
    modelName: 'Osmo Pocket 3',
    status: 'IDLE',
  },
  {
    id: '99',
    unitCode: 'P3-105',
    sn: 'ANHXP6V002-9NP3',
    modelId: 'p3',
    modelName: 'Osmo Pocket 3',
    status: 'LOCKED',
  },
];

test('device filters match registered identity and do not infer sequential inventory', () => {
  assert.equal(filterDevices(devices, { modelId: 'p3', status: 'ALL', search: '105' }).length, 1);
  assert.equal(filterDevices(devices, { modelId: 'p3', status: 'IDLE', search: '' }).length, 1);
  assert.deepEqual(registeredAssetSummary(devices), {
    registeredCount: 2,
    availableCount: 1,
    maintenanceCount: 1,
  });
});

test('device status tones distinguish available, attention, and locked states', () => {
  assert.equal(deviceStatusTone('IDLE'), 'green');
  assert.equal(deviceStatusTone('PENDING_RETURN'), 'amber');
  assert.equal(deviceStatusTone('LOCKED'), 'neutral');
});

test('device card availability never treats blocked devices as immediately available', () => {
  assert.deepEqual(deviceCardPresentation(devices[0]).availability, { kind: 'now' });
  assert.deepEqual(deviceCardPresentation(devices[1]).availability, { kind: 'unavailable' });
});

test('device card normalizes mapper-created Chinese warehouse and availability copy', () => {
  assert.deepEqual(
    deviceCardPresentation({
      ...devices[0],
      expectedAvailableDate: '立即可用',
      note: '仓库: A-03',
    }),
    {
      availability: { kind: 'now' },
      note: { kind: 'warehouse', value: 'A-03' },
    }
  );
});
