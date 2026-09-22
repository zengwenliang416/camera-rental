import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'
import * as vue from 'vue'
const source = parse(fs.readFileSync('src/pages-rental/xianyu-ship/confirm.vue', 'utf8')).descriptor.scriptSetup.content
const js = ts.transpileModule(source + '\nexport { submit, confirmResult, resultUnknown, resultHint }', { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function boot(state) {
  const exports = {}, hooks = {}, issues = []
  let writes = 0, reads = 0, returned = 0
  const store = { draft: vue.ref({ channelOrderId: 1, rentalOrderId: 2, deviceNo: 'TEST', devices: [{ id: 3, deviceNo: 'TEST' }], idempotencyKey: 'unique', waybillNo: 'TEST000000000', expressCode: 'TEST', expressName: 'TEST' }), clear() { store.draft.value = null } }
  const mocks = {
    vue: { ...vue, onUnmounted: fn => { hooks.unmount = fn } },
    '@dcloudio/uni-app': { onHide: fn => { hooks.hide = fn }, onShow: fn => { hooks.show = fn } },
    pinia: { storeToRefs: () => ({ draft: store.draft }) },
    '@/api/rental/xianyu': { shipXianyuOrder: async () => { writes++; throw new Error('network lost') }, getShipmentStatus: async () => { reads++; return typeof state === 'function' ? state() : { state, result: null } } },
    '@/api/rental/warehouse': { createIssue: async value => { issues.push(value); return 9 } },
    '@/utils/staffContact': {}, '@/hooks/useStaffPageStyle': { useStaffPageStyle: () => ({}) },
    '@/store/user': { useUserStore: () => ({ userInfo: {} }) },
    '@/store/shipDraft': { useShipDraftStore: () => store },
    '@/store/staffException': { useStaffExceptionStore: () => ({ record() {} }) },
    '@/utils/staffScan': { maskOrderId: v => v, maskWaybill: v => v },
    '@/models/rental/staffOperations': { staffError: e => e.message },
    '@/hooks/useAccess': { useAccess: () => ({ hasAccessByCodes: () => true }) },
  }
  vm.runInNewContext(js, { exports, definePage() {}, require: name => mocks[name],
    setTimeout: fn => { queueMicrotask(fn); return 1 }, clearTimeout() {},
    uni: { showToast() {}, navigateBack() { returned++ } },
  })
  return { api: exports, hooks, issues, store, counts: () => ({ writes, reads, returned }) }
}
async function flush() { for (let i = 0; i < 30; i++) await Promise.resolve() }
{
  const h = boot('SUCCEEDED'); await h.api.submit(); await flush()
  assert.deepEqual(h.counts(), { writes: 1, reads: 1, returned: 1 })
  assert.equal(h.store.draft.value, null)
}
{
  const h = boot('UNKNOWN'); await h.api.submit(); await flush(); await h.api.submit()
  assert.equal(h.counts().writes, 1, 'unknown receipt cannot trigger a second shipment')
  assert.equal(h.counts().reads, 3, 'bounded read-only reconciliation')
  assert.equal(h.issues.length, 1); assert.equal(h.issues[0].requestKey, 'ship-unknown:unique')
  assert.equal(h.api.resultUnknown.value, true)
}
{
  const h = boot('REJECTED'); await h.api.submit(); await flush()
  assert.equal(h.api.resultUnknown.value, false, 'explicit channel rejection permits corrected retry')
  assert.equal(h.counts().writes, 1, 'never retries the mutation automatically')
}
{
  let resolve
  const h = boot(() => new Promise(r => { resolve = r }))
  const check = h.api.confirmResult(); h.hooks.hide(); resolve({ state: 'SUCCEEDED' }); await check
  assert.equal(h.counts().returned, 0); assert.ok(h.store.draft.value, 'late response while hidden does not navigate or erase work')
}
console.log('PASS timeout receipt reconciliation, unknown-write block, persisted issue, rejection and background response isolation')
