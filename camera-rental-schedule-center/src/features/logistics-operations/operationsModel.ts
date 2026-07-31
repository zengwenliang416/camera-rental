import type {
  RentalLogisticsCarrierMappingSaveReqVO,
  RentalLogisticsMetricsVO,
  RentalLogisticsProviderConfigUpdateReqVO,
  RentalLogisticsProviderConfigVO,
  RentalLogisticsProviderCredentialSaveReqVO,
  RentalLogisticsProviderCredentialVO,
  RentalLogisticsSecretAction,
} from '../../api/rental';
import type { StatusTone } from '../../shared/ui/StatusBadge';

export const LOGISTICS_OPERATIONS_PERMISSIONS = {
  configQuery: 'rental:logistics:config:query',
  configUpdate: 'rental:logistics:config:update',
  configVerify: 'rental:logistics:config:verify',
  mappingQuery: 'rental:logistics:mapping:query',
  mappingUpdate: 'rental:logistics:mapping:update',
  mappingDelete: 'rental:logistics:mapping:delete',
  taskQuery: 'rental:logistics:task:query',
  taskRetry: 'rental:logistics:task:retry',
  reconcile: 'rental:logistics:reconcile',
  metricsQuery: 'rental:logistics:metrics:query',
  backfill: 'rental:logistics:backfill',
  cleanup: 'rental:logistics:cleanup',
} as const;

export const LOGISTICS_OPERATIONS_PERMISSION_LIST = Object.values(
  LOGISTICS_OPERATIONS_PERMISSIONS
);

export interface LogisticsOperationsAccess {
  canQueryConfig: boolean;
  canUpdateConfig: boolean;
  canVerifyConfig: boolean;
  canQueryMappings: boolean;
  canUpdateMappings: boolean;
  canDeleteMappings: boolean;
  canQueryTasks: boolean;
  canRetryTasks: boolean;
  canReconcile: boolean;
  canQueryMetrics: boolean;
  canBackfill: boolean;
  canCleanup: boolean;
}

function permissionGranted(permissions: string[], permission: string) {
  return permissions.includes('*:*:*') || permissions.includes(permission);
}

export function buildLogisticsOperationsAccess(
  permissions: string[]
): LogisticsOperationsAccess {
  return {
    canQueryConfig: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.configQuery
    ),
    canUpdateConfig: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.configUpdate
    ),
    canVerifyConfig: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.configVerify
    ),
    canQueryMappings: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.mappingQuery
    ),
    canUpdateMappings: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.mappingUpdate
    ),
    canDeleteMappings: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.mappingDelete
    ),
    canQueryTasks: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.taskQuery
    ),
    canRetryTasks: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.taskRetry
    ),
    canReconcile: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.reconcile
    ),
    canQueryMetrics: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.metricsQuery
    ),
    canBackfill: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.backfill
    ),
    canCleanup: permissionGranted(
      permissions,
      LOGISTICS_OPERATIONS_PERMISSIONS.cleanup
    ),
  };
}

export function hasLogisticsOperationsAccess(permissions: string[]) {
  return LOGISTICS_OPERATIONS_PERMISSION_LIST.some((permission) =>
    permissionGranted(permissions, permission)
  );
}

export interface SecretDraft {
  action: RentalLogisticsSecretAction;
  replacement: string;
}

export interface ProviderConfigDraft {
  providerCode: string;
  enabled: boolean;
  queryEnabled: boolean;
  subscribeEnabled: boolean;
  callbackBaseUrl: string;
  minimumQueryIntervalSeconds: number;
  resultVersion: string;
  callbackSecret: SecretDraft;
}

export interface ProviderCredentialDraft {
  id?: number;
  providerCode: string;
  credentialName: string;
  enabled: boolean;
  sortOrder: number;
  customerCode: SecretDraft;
  apiKey: SecretDraft;
}

function emptySecretDraft(): SecretDraft {
  return { action: 'KEEP', replacement: '' };
}

export function providerDraftFromConfig(
  config: RentalLogisticsProviderConfigVO
): ProviderConfigDraft {
  return {
    providerCode: config.providerCode,
    enabled: config.enabled,
    queryEnabled: config.queryEnabled,
    subscribeEnabled: config.subscribeEnabled,
    callbackBaseUrl: config.callbackBaseUrl || '',
    minimumQueryIntervalSeconds: config.minimumQueryIntervalSeconds,
    resultVersion: config.resultVersion || '4',
    callbackSecret: emptySecretDraft(),
  };
}

function secretPayload(draft: SecretDraft) {
  return draft.action === 'REPLACE' ? draft.replacement.trim() : undefined;
}

export function providerUpdatePayload(
  draft: ProviderConfigDraft
): RentalLogisticsProviderConfigUpdateReqVO {
  return {
    providerCode: draft.providerCode,
    enabled: draft.enabled,
    queryEnabled: draft.queryEnabled,
    subscribeEnabled: draft.subscribeEnabled,
    callbackSecretAction: draft.callbackSecret.action,
    callbackSecret: secretPayload(draft.callbackSecret),
    callbackBaseUrl: draft.callbackBaseUrl.trim() || null,
    minimumQueryIntervalSeconds: boundedInteger(
      draft.minimumQueryIntervalSeconds,
      1800,
      86400,
      1800
    ),
    resultVersion: draft.resultVersion.trim() || '4',
  };
}

export function providerDraftIsValid(draft: ProviderConfigDraft) {
  const replacementIsValid = (secret: SecretDraft) =>
    secret.action !== 'REPLACE' || secret.replacement.trim().length > 0;
  return (
    replacementIsValid(draft.callbackSecret)
    && draft.minimumQueryIntervalSeconds >= 1800
    && draft.minimumQueryIntervalSeconds <= 86400
    && draft.resultVersion.trim().length > 0
  );
}

export function emptyProviderCredentialDraft(): ProviderCredentialDraft {
  return {
    providerCode: 'KUAIDI100',
    credentialName: '',
    enabled: false,
    sortOrder: 100,
    customerCode: emptySecretDraft(),
    apiKey: emptySecretDraft(),
  };
}

export function providerCredentialDraftFromConfig(
  credential: RentalLogisticsProviderCredentialVO
): ProviderCredentialDraft {
  return {
    id: credential.id,
    providerCode: credential.providerCode,
    credentialName: credential.credentialName,
    enabled: credential.enabled,
    sortOrder: credential.sortOrder,
    customerCode: emptySecretDraft(),
    apiKey: emptySecretDraft(),
  };
}

export function providerCredentialPayload(
  draft: ProviderCredentialDraft
): RentalLogisticsProviderCredentialSaveReqVO {
  return {
    id: draft.id,
    providerCode: draft.providerCode,
    credentialName: draft.credentialName.trim(),
    enabled: draft.enabled,
    sortOrder: boundedInteger(draft.sortOrder, 0, 10000, 100),
    customerCodeAction: draft.customerCode.action,
    customerCode: secretPayload(draft.customerCode),
    apiKeyAction: draft.apiKey.action,
    apiKey: secretPayload(draft.apiKey),
  };
}

export function providerCredentialDraftIsValid(draft: ProviderCredentialDraft) {
  const replacementIsValid = (secret: SecretDraft) =>
    secret.action !== 'REPLACE' || secret.replacement.trim().length > 0;
  return draft.credentialName.trim().length > 0
    && draft.sortOrder >= 0
    && draft.sortOrder <= 10000
    && replacementIsValid(draft.customerCode)
    && replacementIsValid(draft.apiKey);
}

export function emptyCarrierMappingDraft(): RentalLogisticsCarrierMappingSaveReqVO {
  return {
    sourceType: 'XIANYU',
    sourceCarrierCode: '',
    canonicalCarrierCode: '',
    displayName: '',
    providerCode: 'KUAIDI100',
    providerCarrierCode: '',
    phoneRequirement: 'OPTIONAL',
    status: 'ENABLED',
  };
}

export function carrierMappingDraftIsValid(
  draft: RentalLogisticsCarrierMappingSaveReqVO
) {
  return [
    draft.sourceType,
    draft.sourceCarrierCode,
    draft.canonicalCarrierCode,
    draft.displayName,
    draft.providerCode,
    draft.providerCarrierCode,
  ].every((value) => value.trim().length > 0);
}

export function boundedInteger(
  value: number,
  minimum: number,
  maximum: number,
  fallback: number
) {
  if (!Number.isFinite(value)) return fallback;
  return Math.min(maximum, Math.max(minimum, Math.trunc(value)));
}

export const DEFAULT_BACKFILL_COMMAND = {
  dryRun: true,
  limit: 20,
  enqueueProviderTasks: false,
} as const;

export const DEFAULT_CLEANUP_COMMAND = {
  dryRun: true,
  retentionDays: 90,
  limit: 500,
} as const;

export function normalizeBackfillCommand(input: {
  dryRun: boolean;
  limit: number;
  enqueueProviderTasks: boolean;
}) {
  return {
    dryRun: Boolean(input.dryRun),
    limit: boundedInteger(input.limit, 1, 100, 20),
    enqueueProviderTasks:
      !input.dryRun && Boolean(input.enqueueProviderTasks),
  };
}

export function normalizeCleanupCommand(input: {
  dryRun: boolean;
  retentionDays: number;
  limit: number;
}) {
  return {
    dryRun: Boolean(input.dryRun),
    retentionDays: boundedInteger(input.retentionDays, 30, 3650, 90),
    limit: boundedInteger(input.limit, 1, 1000, 500),
  };
}

export function operationsStatusTone(status?: string | null): StatusTone {
  const normalized = String(status || '').toUpperCase();
  if (
    normalized.includes('SUCCEEDED')
    || normalized.includes('READY')
    || normalized.includes('VERIFIED')
    || normalized.includes('ENABLED')
    || normalized.includes('REQUEUED')
    || normalized.includes('CREATED')
    || normalized.includes('REUSED')
    || normalized.includes('ELIGIBLE')
  ) {
    return 'green';
  }
  if (
    normalized.includes('FAILED')
    || normalized.includes('DEAD')
    || normalized.includes('INCOMPLETE')
    || normalized.includes('INVALID')
  ) {
    return 'red';
  }
  if (
    normalized.includes('WAIT')
    || normalized.includes('PENDING')
    || normalized.includes('DISABLED')
    || normalized.includes('SKIPPED')
    || normalized.includes('DEFERRED')
  ) {
    return 'amber';
  }
  if (normalized.includes('RUNNING') || normalized.includes('RECEIVED')) {
    return 'blue';
  }
  return 'neutral';
}

export function metricsAreEmpty(metrics: RentalLogisticsMetricsVO) {
  return (
    metrics.deliveryCount === 0
    && metrics.staleDeliveryCount === 0
    && metrics.failedOutboxCount === 0
    && metrics.failedInboxCount === 0
    && metrics.retriedOutboxCount === 0
    && metrics.retriedInboxCount === 0
    && Object.keys(metrics.deliveryStatusCounts || {}).length === 0
    && Object.keys(metrics.outboxStatusCounts || {}).length === 0
    && Object.keys(metrics.inboxStatusCounts || {}).length === 0
  );
}
