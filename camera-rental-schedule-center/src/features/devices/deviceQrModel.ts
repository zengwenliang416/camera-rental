export type DeviceQrState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; payload: string }
  | { status: 'forbidden' }
  | { status: 'error' };

export interface DeviceQrResult {
  payload?: string;
  payloadVersion?: string;
  signed?: boolean;
}

export function isSignedDeviceQr(result: DeviceQrResult | undefined) {
  return Boolean(
    result?.signed
    && result.payloadVersion === 'CRD1'
    && result.payload?.startsWith('CRD1|')
  );
}

export function deviceQrStateFromResult(
  hasPermission: boolean,
  result: {
    loading?: boolean;
    response?: DeviceQrResult;
    failed?: boolean;
  }
): DeviceQrState {
  if (!hasPermission) return { status: 'forbidden' };
  if (result.loading) return { status: 'loading' };
  if (isSignedDeviceQr(result.response)) {
    return { status: 'ready', payload: result.response!.payload! };
  }
  if (result.response) return { status: 'error' };
  if (result.failed) return { status: 'error' };
  return { status: 'idle' };
}
