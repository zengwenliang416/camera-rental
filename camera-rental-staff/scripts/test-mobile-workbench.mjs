import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'

const require = createRequire(import.meta.url)
function load(file, mocks = {}, expose = '') {
  let source = fs.readFileSync(new URL(file, import.meta.url), 'utf8')
  if (file.endsWith('.vue'))
    source = parse(source).descriptor.scriptSetup.content
  if (expose)
    source += `\nexport { ${expose} }`
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
  const exports = {}
  vm.runInNewContext(js, { exports, require: name => name in mocks ? mocks[name] : require(name), definePage() {}, uni: globalThis.uni })
  return exports
}
const display = load('../src/models/rental/orderDisplay.ts')
const model = load('../src/models/rental/mobileWorkbench.ts', { './orderDisplay': display })
assert.equal(model.shiftDate('2028-03-01', -1), '2028-02-29')
assert.equal(model.shiftDate('2026-12-31', 1), '2027-01-01')
const order = { shippingStatus: 'SHIPPED', occupyEndDateExclusive: [2026, 9, 23] }
assert.equal(model.matchesDailyPlan(order, 'RETURN', '2026-09-22'), true)
assert.equal(model.matchesDailyPlan(order, 'OVERDUE', '2026-09-22'), false)
assert.equal(model.matchesDailyPlan(order, 'OVERDUE', '2026-09-23'), true)
assert.equal(model.matchesDailyPlan({ ...order, status: 'RETURNED' }, 'OVERDUE', '2026-09-23'), false)
assert.equal(model.matchesDailyPlan({ ...order, shippingStatus: 'UNSHIPPED' }, 'RETURN', '2026-09-22'), false)
assert.equal(model.matchesDailyPlan({ shippingStatus: 'UNSHIPPED', occupyStartDate: '2026-09-21' }, 'SHIP', '2026-09-22'), true)
assert.equal(model.matchesDailyPlan({ shippingStatus: 'UNSHIPPED', occupyStartDate: '2026-09-23' }, 'SHIP', '2026-09-22'), false)
const release = { package: 'com.motioncover.rental.staff', version: '1.0.4', versionCode: 104, download: 'https://rental.motion-cover.com/downloads/jiezuda/jiezuda-1.0.4.apk' }
assert.equal(model.trustedRelease(release).versionCode, 104)
for (const patch of [{ download: 'https://example.org/test.apk' }, { package: 'another.app' }, { versionCode: '104' }, { download: 'javascript:alert(1)' }]) assert.throws(() => model.trustedRelease({ ...release, ...patch }))
console.log('PASS calendar boundaries, half-open occupancy dates, task exclusions, update origin/package validation')
let call = ''
let copied = ''
globalThis.uni = {
  makePhoneCall: (o) => {
    call = o.phoneNumber
  },
  showToast() {},
  setClipboardData: (o) => {
    copied = o.data
  },
}
const contact = load('../src/utils/staffContact.ts')
contact.callReceiver('123****8900')
assert.equal(call, '')
contact.callReceiver('010-12345678')
assert.equal(call, '01012345678')
contact.copyStaffField(' TEST-ORDER ')
assert.equal(copied, 'TEST-ORDER')
console.log('PASS masked phone blocked, valid phone normalized, explicit clipboard copy')

const calls = []
let deferred
const vue = { ref: value => ({ value }) }
const mocks = {
  vue,
  '@dcloudio/uni-app': { onLoad() {}, onUnload() {} },
  '@/hooks/useStaffPageStyle': { useStaffPageStyle: () => ({}) },
  '@/hooks/useAccess': { useAccess: () => ({ hasAccessByCodes: () => true }) },
  '@/hooks/useStaffScanner': { useStaffScanner() {} },
  '@/api/rental/device': { lookupRentalDevice: async () => ({ deviceNo: 'TEST-1' }) },
  '@/api/rental/workbench': { getWorkbench: async (args) => {
    calls.push(args)
    if (args.keyword === 'slow')
      return await new Promise((resolve) => { deferred = resolve })
    return { devicePage: { list: [{ deviceId: 2, deviceNo: 'TEST-2', segments: [] }], total: 1 } }
  } },
  '@/models/rental/mobileWorkbench': model,
  '@/models/rental/orderDisplay': display,
  '@/models/rental/staffOperations': { deviceStatusLabel: x => x, staffError: e => e.message },
  '@/utils/staffScan': { maskOrderId: x => x },
  '@/utils/url': { setTabParams() {} },
}
const page = load('../src/pages-rental/schedule/index.vue', mocks, 'load, keyword, start, rows, days')
page.start.value = '2026-09-22'
page.days.value = 14
page.keyword.value = 'slow'
const first = page.load()
page.keyword.value = 'fast'
await page.load()
deferred({ devicePage: { list: [{ deviceId: 1 }], total: 1 } })
await first
assert.equal(page.rows.value[0].deviceId, 2)
assert.equal(calls[1].toDateExclusive, '2026-10-06')
assert.equal(calls[1].keyword, 'fast')
console.log('PASS schedule uses date window and rejects stale filter responses')

// A single tap only loads one server page; retry and old tab responses cannot corrupt the queue.
let orderCalls = 0
let detailCalls = 0
let rejectNext = false
const taskMocks = {
  ...mocks,
  '@dcloudio/uni-app': { onShow() {}, onHide() {} },
  '@/api/rental/order': {
    getStaffOrders: async ({ pageNo, pageSize }) => {
      orderCalls++
      assert.equal(pageSize, 20)
      if (rejectNext) {
        rejectNext = false
        throw new Error('offline')
      }
      return { total: 40, list: [{ id: pageNo, shippingStatus: 'UNSHIPPED', occupyStartDate: '2000-01-01' }] }
    },
    getOrderScheduleDetail: async () => {
      detailCalls++
      return { items: [{ assignments: [{ status: 'RETURNED' }] }] }
    },
  },
  '@/utils/url': { setTabParams() {} },
}
const tasksPage = load('../src/pages-rental/tasks/index.vue', taskMocks, 'load, tasks, hasMore, current, error')
await tasksPage.load(true)
assert.equal(orderCalls, 1)
assert.equal(tasksPage.tasks.value.length, 1)
assert.equal(tasksPage.hasMore.value, true)
rejectNext = true
await tasksPage.load(false)
assert.equal(tasksPage.tasks.value.length, 1)
assert.equal(tasksPage.hasMore.value, true)
await tasksPage.load(false)
assert.equal(tasksPage.tasks.value.length, 2)
assert.equal(tasksPage.hasMore.value, false)
assert.equal(detailCalls, 0)
console.log('PASS task pagination is bounded per action, failure retains prior results, retry advances correct page')
