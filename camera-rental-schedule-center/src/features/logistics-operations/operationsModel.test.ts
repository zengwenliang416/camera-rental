import assert from 'node:assert/strict';
import test from 'node:test';

import type { RentalLogisticsProviderConfigVO } from '../../api/rental';
import {
  buildLogisticsOperationsAccess,
  hasLogisticsOperationsAccess,
  normalizeBackfillCommand,
  normalizeCleanupCommand,
  providerCredentialDraftFromConfig,
  providerCredentialPayload,
  providerDraftFromConfig,
  providerUpdatePayload,
} from './operationsModel';

const providerConfig: RentalLogisticsProviderConfigVO = {
  providerCode: 'KUAIDI100',
  enabled: false,
  queryEnabled: false,
  subscribeEnabled: false,
  callbackSecretConfigured: true,
  maskedCallbackSecret: '********',
  callbackBaseUrl: 'https://api.example.com/rental/webhooks',
  minimumQueryIntervalSeconds: 1800,
  resultVersion: '4',
  configStatus: 'READY_UNVERIFIED',
  credentials: [{
    id: 7,
    providerCode: 'KUAIDI100',
    credentialName: 'primary',
    enabled: true,
    sortOrder: 100,
    customerCodeConfigured: true,
    maskedCustomerCode: '********',
    apiKeyConfigured: true,
    maskedApiKey: '********',
    configStatus: 'READY_UNVERIFIED',
  }, {
    id: 8,
    providerCode: 'KUAIDI100',
    credentialName: 'backup',
    enabled: false,
    sortOrder: 200,
    customerCodeConfigured: true,
    maskedCustomerCode: '********',
    apiKeyConfigured: true,
    maskedApiKey: '********',
    configStatus: 'LOCALLY_VERIFIED',
  }],
};

test('operations permissions remain independent for every query and mutation', () => {
  const access = buildLogisticsOperationsAccess([
    'rental:logistics:task:query',
    'rental:logistics:cleanup',
  ]);

  assert.equal(access.canQueryTasks, true);
  assert.equal(access.canRetryTasks, false);
  assert.equal(access.canCleanup, true);
  assert.equal(access.canBackfill, false);
  assert.equal(access.canQueryConfig, false);
  assert.equal(hasLogisticsOperationsAccess(['rental:logistics:cleanup']), true);
});

test('provider drafts never receive masked or plaintext secret values', () => {
  const draft = providerDraftFromConfig(providerConfig);

  assert.deepEqual(draft.callbackSecret, { action: 'KEEP', replacement: '' });
  assert.equal(JSON.stringify(draft).includes('********'), false);

  const payload = providerUpdatePayload(draft);
  assert.equal(payload.callbackSecret, undefined);

  const credentialDraft = providerCredentialDraftFromConfig(
    providerConfig.credentials[0]
  );
  credentialDraft.apiKey = { action: 'REPLACE', replacement: 'new-key' };
  const credentialPayload = providerCredentialPayload(credentialDraft);
  assert.equal(credentialPayload.apiKeyAction, 'REPLACE');
  assert.equal(credentialPayload.apiKey, 'new-key');
  assert.equal(credentialPayload.customerCode, undefined);
  assert.equal(JSON.stringify(credentialDraft).includes('********'), false);
});

test('multiple credential rows keep independent identity and secret drafts', () => {
  const drafts = providerConfig.credentials.map(providerCredentialDraftFromConfig);

  assert.deepEqual(drafts.map((draft) => draft.id), [7, 8]);
  assert.deepEqual(drafts.map((draft) => draft.credentialName), ['primary', 'backup']);
  assert.notEqual(drafts[0].customerCode, drafts[1].customerCode);
  assert.notEqual(drafts[0].apiKey, drafts[1].apiKey);
  assert.equal(JSON.stringify(drafts).includes('********'), false);
});

test('backfill and cleanup commands enforce safe defaults and backend bounds', () => {
  assert.deepEqual(
    normalizeBackfillCommand({
      dryRun: true,
      limit: 500,
      enqueueProviderTasks: true,
    }),
    { dryRun: true, limit: 100, enqueueProviderTasks: false }
  );
  assert.deepEqual(
    normalizeCleanupCommand({
      dryRun: false,
      retentionDays: 1,
      limit: 5000,
    }),
    { dryRun: false, retentionDays: 30, limit: 1000 }
  );
});
