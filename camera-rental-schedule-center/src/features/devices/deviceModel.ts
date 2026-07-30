import type { DeviceInstance, DeviceStatus } from '../../types';
import type { PresentationTone } from '../orders/orderModel';

export type DeviceStatusFilter = DeviceStatus | 'ALL';

export interface DeviceCardPresentation {
  availability: { kind: 'now' | 'date' | 'unavailable'; value?: string };
  note?: { kind: 'warehouse' | 'note'; value: string };
}

export function filterDevices(
  devices: DeviceInstance[],
  filters: { modelId: string; status: DeviceStatusFilter; search: string }
) {
  const search = filters.search.trim().toLocaleLowerCase();
  return devices.filter((device) => {
    if (device.modelId !== filters.modelId) return false;
    if (filters.status !== 'ALL' && device.status !== filters.status) return false;
    if (!search) return true;
    return [device.unitCode, device.sn, device.modelName]
      .some((value) => value.toLocaleLowerCase().includes(search));
  });
}

export function deviceStatusTone(status: DeviceStatus): PresentationTone {
  const tones: Record<DeviceStatus, PresentationTone> = {
    IDLE: 'green',
    RESERVED: 'amber',
    RENTING: 'blue',
    PENDING_RETURN: 'amber',
    REPAIR: 'red',
    LOCKED: 'neutral',
  };
  return tones[status];
}

export function registeredAssetSummary(devices: DeviceInstance[]) {
  return {
    registeredCount: devices.length,
    availableCount: devices.filter((device) => device.status === 'IDLE').length,
    maintenanceCount: devices.filter(
      (device) => device.status === 'REPAIR' || device.status === 'LOCKED'
    ).length,
  };
}

export function deviceCardPresentation(device: DeviceInstance): DeviceCardPresentation {
  const expectedDate = device.expectedAvailableDate?.trim();
  const hasConcreteDate = Boolean(expectedDate && expectedDate !== '立即可用');
  const warehouseMatch = device.note?.match(/^(?:仓库|Warehouse)\s*:\s*(.+)$/i);

  return {
    availability: hasConcreteDate
      ? { kind: 'date', value: expectedDate }
      : device.status === 'IDLE'
        ? { kind: 'now' }
        : { kind: 'unavailable' },
    note: warehouseMatch
      ? { kind: 'warehouse', value: warehouseMatch[1].trim() }
      : device.note
        ? { kind: 'note', value: device.note }
        : undefined,
  };
}
