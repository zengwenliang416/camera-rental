import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'

function load(file, mocks = {}, expose = '') {
  let code = fs.readFileSync(new URL(file, import.meta.url), 'utf8')
  if (file.endsWith('.vue'))
    code = parse(code).descriptor.scriptSetup.content
  code += `\n${expose ? `export { ${expose} }` : ''}`
  const js = ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const exports = {}
  vm.runInNewContext(js, { exports, require: (name) => {
    if (!(name in mocks))
      throw new Error(`Missing mock ${name}`)
    return mocks[name]
  }, definePage: () => {}, uni: globalThis.uni, setTimeout: fn => fn(), getCurrentPages: () => [] })
  return exports
}
const model = load('../src/models/rental/staffWorkflow.ts')
const seen = []
const found = await model.findPendingShipment(async (page) => {
  seen.push(page)
  return page === 1 ? { list: [{ id: 11, rentalOrderId: 12 }], total: 101 } : { list: [{ id: 22, rentalOrderId: 33 }], total: 101 }
}, { rentalOrderId: 33 })
assert.equal(found.id, 22)
assert.deepEqual(seen, [1, 2])
await assert.rejects(() => model.findPendingShipment(async () => ({ list: [{ id: 22, rentalOrderId: 44 }], total: 1 }), { channelOrderId: 22, rentalOrderId: 33 }))
assert.equal(model.reconcilePicked([{ id: 1 }, { id: 2 }], [{ id: 1, eligible: false }, { id: 2, eligible: true }], 1)[0].id, 2)
assert.equal(model.reconcilePicked([{ id: 1 }], [{ id: 1, eligible: true }], 0).length, 0)
assert.equal(model.singleShipmentBlocker({ items: [{ requiredQuantity: 1 }] }), '')
assert.match(model.singleShipmentBlocker({ items: [{ requiredQuantity: 2 }] }), /后台/)
console.log('PASS exact order selection beyond page one, mismatched IDs, refreshed candidates, multi-device guard')

const apiCalls = []
const api = load('../src/api/rental/device.ts', { '@/http/http': { http: {
  post: async (url, body) => {
    apiCalls.push({ url, body })
    return { id: 3 }
  },
  get: async () => ({ list: [{ id: 1, deviceNo: 'TEST-10' }, { id: 2, deviceNo: 'TEST-1' }], total: 2 }),
} } })
assert.equal((await api.lookupRentalDevice('TEST-1')).id, 2)
assert.equal((await api.lookupRentalDevice('CRD1|TEST-1|MODEL|signature')).id, 3)
assert.equal(apiCalls[0].body.payload, 'CRD1|TEST-1|MODEL|signature')
await assert.rejects(() => api.lookupRentalDevice('TEST'), /完全匹配/)
console.log('PASS exact manual device lookup, signed QR preserved, partial number rejected')

const hooks = { load: [], show: [] }
const draft = { draft: null, setDraft: () => {} }
const vue = { ref: value => ({ value }), computed: fn => ({
  get value() {
    return fn()
  },
}) }
let navigate = ''
globalThis.uni = { navigateTo: ({ url }) => {
  navigate = url
}, navigateBack() {}, switchTab() {}, showModal: ({ success }) => success({ confirm: true }) }
const details = { id: 33, items: [{ requiredQuantity: 1, equipmentModelCode: 'MODEL', assignments: [] }] }
const mocks = {
  vue,
  '@dcloudio/uni-app': { onLoad: fn => hooks.load.push(fn), onShow: fn => hooks.show.push(fn), onUnload() {} },
  '@wot-ui/ui/components/wd-toast': { useToast: () => ({ warning() {} }) },
  '@/api/rental/order': { getOrderScheduleDetail: async () => details },
  '@/api/rental/device': { lookupRentalDevice: async () => ({ id: 3, deviceNo: 'TEST-1', equipmentModelCode: 'MODEL', status: 'AVAILABLE' }) },
  '@/api/rental/xianyu': { getXianyuExpressCompanyList: async () => [{ code: 'SF', expressName: '顺丰速运' }], getXianyuPendingShipOrderPage: async () => ({ list: [{ id: 22, rentalOrderId: 33, externalOrderId: 'TEST-ORDER' }], total: 1 }) },
  '@/hooks/useStaffPageStyle': { useStaffPageStyle: () => ({}) },
  '@/hooks/useStaffScanner': { useStaffScanner: () => ({ scan: async () => {} }) },
  '@/hooks/useAccess': { useAccess: () => ({ hasAccessByCodes: () => true }) },
  '@/models/rental/staffOperations': { deviceStatusLabel: x => x, staffError: e => e.message },
  '@/models/rental/staffWorkflow': model,
  '@/store/shipDraft': { useShipDraftStore: () => draft },
  '@/utils/staffScan': { extractDeviceNo: x => x, extractWaybillNo: x => x, maskOrderId: x => x, maskWaybill: x => x },
}
const page = load('../src/pages-rental/xianyu-ship/index.vue', mocks, 'selectOrder, selectedOrder, acceptDeviceScan, applyWaybill, waybillNo, resolvedDevice, loadExpress, blocker, searchOrders, clearScans')
await page.selectOrder({ id: 22, rentalOrderId: 33 })
await page.loadExpress()
await page.acceptDeviceScan('TEST-1')
page.applyWaybill('SF1234567890123')
assert.equal(page.blocker.value, '')
await page.searchOrders()
assert.equal(page.selectedOrder.value.id, 22, 'search must preserve selected order')
await page.acceptDeviceScan('TEST-1')
assert.equal(page.waybillNo.value, 'SF1234567890123', 'rescan device preserves waybill')
page.clearScans()
assert.equal(page.selectedOrder.value.id, 22, 'reset scans preserves order')
assert.match(page.blocker.value, /扫描/)
console.log('PASS shipping page preserves order across searches/rescans; blocker follows actual state')

const returnPage = load('../src/pages-rental/device-return/index.vue', {
  vue,
  '@wot-ui/ui/components/wd-toast': { useToast: () => ({ info() {}, warning() {} }) },
  '@/hooks/useStaffPageStyle': mocks['@/hooks/useStaffPageStyle'],
  '@/api/rental/device': { lookupRentalDevice: async code => ({ id: code === 'TEST-A' ? 1 : 2, deviceNo: code }) },
  '@/components/rental/scan-banner.vue': {},
  '@/components/rental/staff-header.vue': {},
  '@/store/staffException': { useStaffExceptionStore: () => ({ record() {} }) },
  '@/hooks/useStaffScanner': mocks['@/hooks/useStaffScanner'],
  '@/utils/staffScan': { extractDeviceNo: x => x, normalizeCode: x => x },
  '@/models/rental/staffOperations': mocks['@/models/rental/staffOperations'],
  '@/hooks/useAccess': mocks['@/hooks/useAccess'],
}, 'resolveScannedDevice, note, resolvedDevice')
await returnPage.resolveScannedDevice('TEST-A')
returnPage.note.value = '测试缺件说明'
await returnPage.resolveScannedDevice('TEST-A')
assert.equal(returnPage.note.value, '测试缺件说明', 'same device retains note')
await returnPage.resolveScannedDevice('TEST-B')
assert.equal(returnPage.note.value, '', 'changing device must discard previous note')
console.log('PASS return note ownership follows device identity')

let detailCalls = 0
const detailHooks = { load: [], show: [] }
const detailPage = load('../src/pages-rental/orders/detail.vue', {
  vue,
  '@dcloudio/uni-app': { onLoad: fn => detailHooks.load.push(fn), onShow: fn => detailHooks.show.push(fn) },
  '@/hooks/useStaffPageStyle': mocks['@/hooks/useStaffPageStyle'],
  '@/hooks/useAccess': mocks['@/hooks/useAccess'],
  '@/api/rental/order': { getOrderScheduleDetail: async (id) => {
    detailCalls++
    return { id, items: [] }
  } },
  '@/models/rental/orderDisplay': {},
  '@/utils/staffContact': { callReceiver() {}, copyStaffField() {} },
}, 'openShipping, orderId')
detailHooks.load[0]({ id: '33' })
detailPage.openShipping()
assert.equal(navigate, '/pages-rental/xianyu-ship/index?rentalOrderId=33')
detailHooks.show[0]()
await Promise.resolve()
detailHooks.show[0]()
await Promise.resolve()
assert.equal(detailCalls, 2, 'detail reloads after returning from assignment')
console.log('PASS detail preserves order in shipping route and reloads on return')
