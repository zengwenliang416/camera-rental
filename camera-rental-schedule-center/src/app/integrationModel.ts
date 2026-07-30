import type { XianyuConfigVO } from '../api/rental';

export type IntegrationReadiness = 'loading' | 'ready' | 'read-only' | 'disabled' | 'unavailable';

export function integrationReadiness(
  config: XianyuConfigVO | null,
  loading: boolean,
  unavailable: boolean
): IntegrationReadiness {
  if (loading && !config && !unavailable) return 'loading';
  if (unavailable || !config) return 'unavailable';
  if (!config.enabled || config.status === 'DISABLED') return 'disabled';
  if (config.writeEnabled && config.status === 'READY') return 'ready';
  return 'read-only';
}
