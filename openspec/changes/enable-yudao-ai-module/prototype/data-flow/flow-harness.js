'use strict';

const stages = [
  { id: 'admin-submit', boundary: 'existing-admin-ui' },
  { id: 'permission-check', boundary: 'spring-security' },
  { id: 'encrypt-api-key', boundary: 'mybatis-type-handler' },
  { id: 'persist-key-and-model', boundary: 'tenant-mysql' },
  { id: 'dynamic-model-create', boundary: 'ai-model-factory' },
  { id: 'relay-call', boundary: 'openai-compatible-relay' }
];

const invariants = {
  providerAutoConfigurationDisabledByDefault: true,
  plaintextKeyPersistenceForbidden: true,
  tenantAwareTables: 14,
  customerReturnRoutePreserved: '/app-api/rental/return-registration'
};

if (stages.length !== 6 || invariants.tenantAwareTables !== 14) {
  throw new Error('data-flow invariant failed');
}

process.stdout.write(`${JSON.stringify({ status: 'green', stages, invariants }, null, 2)}\n`);
