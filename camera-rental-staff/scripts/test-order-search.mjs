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
const events = { show: [], hide: [] }
const vue = { ref: value => ({ value }) }
let metric = { statusBarHeight: 32, safeAreaInsets: { top: 28 } }
globalThis.uni = { getSystemInfoSync: () => metric }
const inset = load('../src/hooks/useStaffPageStyle.ts', { vue, '@dcloudio/uni-app': { onShow: fn => events.show.push(fn) } })
const style = inset.useStaffPageStyle()
assert.equal(style.value['--staff-status-bar-height'], '32px')
metric = { statusBarHeight: 0, safeAreaInsets: { top: 0 } }
events.show[0]()
assert.equal(style.value['--staff-status-bar-height'], '32px')
metric = { statusBarHeight: 24, safeAreaInsets: { top: 40 } }
events.show[0]()
assert.equal(style.value['--staff-status-bar-height'], '40px')
const hints = load('../src/models/rental/orderSearch.ts')
assert.match(hints.preparationHint({ preparationReasonCode: 'LOGISTICS_DATE_BEFORE_ORDER' }), /早于/)
assert.match(hints.preparationHint({ preparationReasonCode: 'MISSING_RECEIVE_DATE' }), /实际约定/)
assert.match(hints.preparationHint({ conversionStatus: 'CONFIG_SKIPPED' }, true), /不生成/)
assert.equal(hints.preparationHint({ preparationStatus: 'READY' }), '')
let channelRequest = async () => ({ list: [{ channelOrderId: 8, goodsTitle: 'TEST' }], total: 1 })
const page = load('../src/pages-rental/orders/index.vue', {
  vue, '@dcloudio/uni-app': { onShow() {}, onHide: fn => events.hide.push(fn) },
  '@/hooks/useStaffPageStyle': { useStaffPageStyle: () => ({}) },
  '@/utils/url': { getAndClearTabParams: () => null },
  '@/api/rental/order': { getStaffOrders: async () => ({ list: [], total: 0 }), getStaffChannelOrders: params => channelRequest(params) },
  '@/models/rental/orderDisplay': {}, '@/models/rental/staffOperations': { staffError: e => e.message },
  '@/models/rental/orderSearch': hints,
  '@/components/rental/order-task-card.vue': {}, '@/components/rental/scan-banner.vue': {}, '@/components/rental/staff-header.vue': {},
}, 'loadChannels, channelOrders, channelTotal, channelError, queue')
await page.loadChannels()
assert.equal(page.channelTotal.value, 1)
assert.equal(page.channelOrders.value[0].channelOrderId, 8)
channelRequest = async () => { throw new Error('offline') }
await page.loadChannels()
assert.equal(page.channelError.value, 'offline')
let resolveOld
channelRequest = () => new Promise(resolve => { resolveOld = resolve })
const old = page.loadChannels()
page.queue.value = 'SHIPPED'
await page.loadChannels()
resolveOld({ list: [{ channelOrderId: 9 }], total: 1 })
await old
assert.equal(page.channelOrders.value.length, 0)
page.queue.value = 'ALL'
const hidden = page.loadChannels()
events.hide[0]()
resolveOld({ list: [{ channelOrderId: 9 }], total: 1 })
await hidden
assert.equal(page.channelOrders.value.length, 0)
console.log('PASS channel visibility, query failure, stale responses, preparation hints and resumed status-bar metrics')
