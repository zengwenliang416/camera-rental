import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'
import * as vue from 'vue'
const component = parse(fs.readFileSync('src/components/rental/staff-device-quantity.vue', 'utf8')).descriptor.scriptSetup.content
const js = ts.transpileModule(`${component}\nexport { editable, editing, saving, quantity, error, edit, save }`, {
  compilerOptions: { module: ts.ModuleKind.CommonJS },
}).outputText
const props = vue.reactive({ sourceType: 'XIANYU', status: 'PENDING_ALLOCATION', item: { id: 1, requiredQuantity: 28, assignments: [] } })
const emitted = [], requests = [], exports = {}
let resolveRequest, rejectRequest
vm.runInNewContext(js, { exports, defineProps: () => props, defineEmits: () => event => emitted.push(event),
  uni: { showToast() {} },
  require: name => ({ vue, '@/hooks/useAccess': { useAccess: () => ({ hasAccessByCodes: () => true }) },
    '@/api/rental/order': { updateDeviceQuantity: (...args) => {
      requests.push(args); return new Promise((resolve, reject) => { resolveRequest = resolve; rejectRequest = reject })
    } },
  })[name],
})
assert.equal(exports.editable.value, true)
exports.edit(); exports.quantity.value = 1
const save = exports.save()
await exports.save()
assert.equal(requests.length, 1, 'double tap sends one mutation')
assert.deepEqual(requests[0], [1, 1, 28], 'server receives expected quantity from edit start')
assert.equal(props.item.requiredQuantity, 28, 'no optimistic authority before server success')
resolveRequest(1); await save
assert.deepEqual(emitted, ['updated'], 'parent refresh requested only after server success')
exports.edit(); exports.quantity.value = 3
const stale = exports.save(); rejectRequest(new Error('changed')); await stale
assert.equal(emitted.length, 1, 'failed mutation does not report success')
assert.ok(exports.error.value)
exports.quantity.value = 0; await exports.save()
assert.equal(requests.length, 2, 'invalid input rejected locally')
props.item.assignments = [{ status: 'CANCELED' }]
assert.equal(exports.editable.value, false, 'even historical assignment hides quantity edit')
props.item.assignments = []; props.status = 'DISPATCHED'
assert.equal(exports.editable.value, false)
console.log('quantity editor: optimistic concurrency, double tap, server failure, input bounds and fulfillment guards passed')
