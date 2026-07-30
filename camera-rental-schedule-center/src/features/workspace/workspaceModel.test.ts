import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';

import { resolveSelectedModelId, resolveWorkspaceTab } from './workspaceModel';

test('invalid or unauthorized routes fall back to dashboard', () => {
  assert.equal(resolveWorkspaceTab([], 'devices'), 'dashboard');
  assert.equal(
    resolveWorkspaceTab(['rental:device:query'], 'devices'),
    'devices'
  );
});

test('selected model follows the registered model collection', () => {
  assert.equal(resolveSelectedModelId(['p3', 'p4'], 'p4'), 'p4');
  assert.equal(resolveSelectedModelId(['p3'], 'missing'), 'p3');
  assert.equal(resolveSelectedModelId([], 'p3'), '');
});

test('useApp remains a state-free compatibility facade', () => {
  const source = readFileSync(
    new URL('../../context/AppContext.tsx', import.meta.url),
    'utf8'
  );
  assert.doesNotMatch(source, /createContext|useState|useReducer/);
  assert.match(source, /useSession\(\)/);
  assert.match(source, /usePermissions\(\)/);
  assert.match(source, /useScheduleCenterData\(\)/);
  assert.match(source, /useScheduleCenterCommands\(\)/);
  assert.match(source, /useWorkspace\(\)/);
});
