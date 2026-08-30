#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const MAVEN = '/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn';
const MAVEN_REPO = '/Volumes/zwl/maven-repository';

const commands = {
  'return-backend-security': [
    {
      cwd: 'camera-rental-server',
      argv: [
        MAVEN,
        '-o',
        `-Dmaven.repo.local=${MAVEN_REPO}`,
        '-pl',
        'yudao-module-rental/yudao-module-rental-biz',
        '-am',
        '-Dtest=ReturnRegistrationTokenServiceTest,ReturnRegistrationOrderVerificationServiceTest,ReturnRegistrationSessionCookieServiceTest,ReturnRegistrationRateLimitServiceTest',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        'test'
      ]
    }
  ],
  'admin-return-operations': [
    {
      cwd: 'camera-rental-server',
      argv: [
        MAVEN,
        '-o',
        `-Dmaven.repo.local=${MAVEN_REPO}`,
        '-pl',
        'yudao-module-rental/yudao-module-rental-biz',
        '-am',
        '-Dtest=ReturnRegistrationAdminServiceTest,RentalReturnRegistrationControllerTest',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        'test'
      ]
    },
    {
      cwd: 'camera-rental-admin',
      argv: ['pnpm', 'test:return-registration']
    }
  ],
  'device-catalog': [
    {
      cwd: 'camera-rental-server',
      argv: [
        MAVEN,
        '-o',
        `-Dmaven.repo.local=${MAVEN_REPO}`,
        '-pl',
        'yudao-module-rental/yudao-module-rental-biz',
        '-am',
        '-Dtest=RentalDeviceCatalogServiceTest,RentalDeviceAdminServiceTest,RentalDeviceInboundServiceTest,RentalDeviceInboundCategoryTest,RentalDeviceCodeStandTest,RentalDeviceCodeTest,ReturnSerialNormalizerTest',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        'test'
      ]
    },
    {
      cwd: 'camera-rental-admin',
      argv: [
        'node',
        '--test',
        '--experimental-strip-types',
        'tests/deviceCatalogModel.test.ts'
      ]
    }
  ],
  'device-edit': [
    {
      cwd: 'camera-rental-server',
      argv: [
        MAVEN,
        '-o',
        `-Dmaven.repo.local=${MAVEN_REPO}`,
        '-pl',
        'yudao-module-rental/yudao-module-rental-biz',
        '-am',
        '-Dtest=RentalDeviceAdminServiceTest',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        'test'
      ]
    },
    {
      cwd: 'camera-rental-admin',
      argv: [
        'node',
        '--test',
        '--experimental-strip-types',
        'tests/deviceMaintenanceModel.test.ts'
      ]
    }
  ],
  'device-delete': [
    {
      cwd: 'camera-rental-server',
      argv: [
        MAVEN,
        '-o',
        `-Dmaven.repo.local=${MAVEN_REPO}`,
        '-pl',
        'yudao-module-rental/yudao-module-rental-biz',
        '-am',
        '-Dtest=RentalDeviceDeletionGuardTest,RentalDeviceAdminServiceTest',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        'test'
      ]
    },
    {
      cwd: 'camera-rental-admin',
      argv: [
        'node',
        '--test',
        '--experimental-strip-types',
        'tests/deviceMaintenanceModel.test.ts'
      ]
    }
  ],
  'migration-integrity': [
    {
      cwd: '.',
      argv: ['bash', 'ops/github-deploy/tests/migration-runner-test.sh']
    },
    {
      cwd: '.',
      argv: ['bash', '-n', 'ops/github-deploy/apply-migrations.sh']
    },
    {
      cwd: '.',
      argv: [
        'node',
        'tests/specnav/verification-command.js',
        'migration-hash-only'
      ]
    }
  ],
  'migration-hash-only': []
};

function fail(message) {
  process.stderr.write(`${message}\n`);
  process.exit(1);
}

function verifyMigrationHashes(root) {
  const manifestPath = path.join(
    root,
    'openspec/changes/add-customer-return-registration/development/migrations/manifest.json'
  );
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  const crypto = require('node:crypto');
  for (const migration of manifest.migrations) {
    const auditPath = path.join(
      root,
      'openspec/changes/add-customer-return-registration',
      migration.path
    );
    const productionPath = path.join(root, migration.production_path);
    const audit = fs.readFileSync(auditPath);
    const production = fs.readFileSync(productionPath);
    const auditHash = crypto.createHash('sha256').update(audit).digest('hex');
    const productionHash = crypto
      .createHash('sha256')
      .update(production)
      .digest('hex');
    if (
      auditHash !== migration.sha256
      || productionHash !== migration.sha256
      || !audit.equals(production)
    ) {
      fail(`migration hash mismatch: ${migration.id}`);
    }
  }
}

const caseId = process.argv[2];
if (!Object.prototype.hasOwnProperty.call(commands, caseId)) {
  fail(`unknown verification command case: ${caseId || '<missing>'}`);
}

const projectRoot = path.resolve(__dirname, '../..');
if (caseId === 'migration-hash-only') {
  verifyMigrationHashes(projectRoot);
  process.exit(0);
}

for (const command of commands[caseId]) {
  const [entrypoint, ...args] = command.argv;
  const result = spawnSync(entrypoint, args, {
    cwd: path.resolve(projectRoot, command.cwd),
    env: process.env,
    encoding: 'utf8',
    stdio: 'inherit',
    timeout: 900000
  });
  if (result.error) fail(result.error.message);
  if (result.status !== 0) {
    fail(`${caseId} command failed with exit ${result.status}`);
  }
}

if (caseId === 'migration-integrity') verifyMigrationHashes(projectRoot);

const assertionFile = process.env.SPECNAV_VERIFICATION_ASSERTION_RESULT_FILE;
const assertionIds = (
  process.env.SPECNAV_VERIFICATION_ASSERTION_IDS || ''
).split(',').filter(Boolean);
if (!assertionFile || assertionIds.length === 0) {
  fail('SpecNav assertion protocol environment is missing');
}

fs.mkdirSync(path.dirname(assertionFile), { recursive: true });
fs.writeFileSync(
  assertionFile,
  assertionIds.map((assertionId) => JSON.stringify({
    assertion_id: assertionId,
    method: 'equal',
    expected: true,
    actual: true,
    status: 'passed'
  })).join('\n') + '\n',
  { encoding: 'utf8', flag: 'w', mode: 0o600 }
);
