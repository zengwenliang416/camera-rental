import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'

const src = parse(fs.readFileSync('src/pages-rental/tasks/index.vue', 'utf8')).descriptor.scriptSetup.content
const code = ts.transpileModule(`${src}\nexport {load,change,changeScope,rows,total,error,open}`, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const exports = {}, routes = [], events = {}, calls = []
let request = async () => ({ list: [], total: 0 })
const mocks = {
  vue: { ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }) },
  '@dcloudio/uni-app': { onShow: f => { events.show = f }, onHide: f => { events.hide = f }, onLoad: f => { events.load = f } },
  '@/api/rental/warehouse': { getTasks: (queue, page) => { calls.push({ queue, page }); return request() } },
  '@/hooks/useStaffPageStyle': { useStaffPageStyle: () => ({}) },
  '@/hooks/useAccess': { useAccess: () => ({ hasAccessByCodes: () => true }) },
  '@/models/rental/staffOperations': { staffError: e => e.message },
  '@/models/rental/orderDisplay': {}, '@/models/rental/orderSearch': {}, '@/models/rental/mobileWorkbench': {}, '@/utils/staffScan': {},
}
vm.runInNewContext(code, { exports, require: n => { assert.ok(n in mocks, n); return mocks[n] }, definePage() {}, uni: { navigateTo: o => routes.push(o.url) } })
await exports.load()
assert.equal(calls.at(-1).queue, 'SHIP_TODAY')
exports.changeScope({ detail: { value: 1 } })
assert.equal(calls.at(-1).queue, 'SHIP_OVERDUE')
exports.changeScope({ detail: { value: 2 } })
assert.equal(calls.at(-1).queue, 'SHIP_ALL')
events.load({ queue: 'RETURN' }); await exports.load()
assert.equal(calls.at(-1).queue, 'RETURN')
exports.change('SHIP')
exports.open({ sourceType: 'XIANYU', channelOrderId: 8 })
assert.equal(routes.at(-1), '/pages-rental/xianyu-ship/index?channelOrderId=8')
exports.open({ sourceType: 'XIANYU', orderId: 5, channelOrderId: 8 })
assert.equal(routes.at(-1), '/pages-rental/xianyu-ship/index?rentalOrderId=5')
let resolve
request = () => new Promise(r => { resolve = r })
const stale = exports.load()
events.hide()
resolve({ list: [{ orderId: 9 }], total: 1 }); await stale
assert.equal(exports.rows.value.length, 0)
request = async () => { throw new Error('offline') }
await exports.load()
assert.equal(exports.error.value, 'offline')
assert.equal(exports.total.value, 0)
console.log('PASS daily/backlog/all scopes, return shortcut, correct order identity, stale response and error states')
