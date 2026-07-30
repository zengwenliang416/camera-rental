import { useEffect, useState } from 'react';

import { fetchRentalDeviceQr } from '../../api/rental';
import {
  deviceQrStateFromResult,
  type DeviceQrResult,
  type DeviceQrState,
} from './deviceQrModel';

export function useDeviceQr(deviceId: string | undefined, allowed: boolean): DeviceQrState {
  const [response, setResponse] = useState<DeviceQrResult>();
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    setResponse(undefined);
    setFailed(false);
    if (!deviceId || !allowed) {
      setLoading(false);
      return () => {
        active = false;
      };
    }

    setLoading(true);
    fetchRentalDeviceQr(Number(deviceId))
      .then((result) => {
        if (!active) return;
        setResponse(result);
      })
      .catch(() => {
        if (active) setFailed(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [allowed, deviceId]);

  return deviceQrStateFromResult(allowed, { loading, response, failed });
}
