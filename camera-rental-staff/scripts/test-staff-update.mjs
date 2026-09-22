import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'

const source = ts.transpileModule(fs.readFileSync('src/services/staffUpdate.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS },
}).outputText
const release = { package: 'com.motioncover.rental.staff', version: '1.0.7', versionCode: 107,
  download: 'https://rental.motion-cover.com/downloads/jiezuda/jiezuda-1.0.7.apk' }
function boot() {
  let now = 1_000_000_000, route = 'pages/index/index', response = release
  let requestCount = 0, modalCount = 0, confirmation = false, offline = false
  let pending, modalFail = false
  const storage = new Map(), opened = [], timers = new Map()
  const exports = {}
  vm.runInNewContext(source, {
    exports, Date: { now: () => now },
    plus: { runtime: { versionCode: '106' }, os: { name: 'Android' } },
    getCurrentPages: () => [{ route }],
    setTimeout: fn => { timers.set(1, fn); return 1 }, clearTimeout: id => timers.delete(id),
    uni: {
      request: options => { requestCount++; pending = options },
      getStorageSync: key => storage.get(key), setStorageSync: (key, value) => storage.set(key, value),
      showModal: options => { modalCount++; modalFail ? options.fail() : options.success({ confirm: confirmation }) },
    },
    require: name => ({
      '@/config/staffVersion': { STAFF_VERSION_CODE: 106, STAFF_VERSION: '1.0.6' },
      '@/utils/url': { openUrl: url => opened.push(url) },
      '@/models/rental/mobileWorkbench': { trustedRelease: value => {
        // URL and metadata validation is separately covered by test-mobile-workbench.mjs.
        if (value.invalid) throw new Error('invalid release')
        return value
      } },
    })[name],
  })
  const settle = () => { assert.equal(pending.header.isToken, false); offline ? pending.fail() : pending.success({ statusCode: 200, data: response }) }
  return { api: exports, settle, opened, storage, timers,
    advance: value => { now += value }, route: value => { route = value }, response: value => { response = value },
    confirm: () => { confirmation = true }, offline: () => { offline = true }, modalFail: () => { modalFail = true },
    counts: () => [requestCount, modalCount] }
}
{
  const h = boot(); h.api.scheduleUpdateReminder()
  const first = h.api.checkStaffUpdate(true), second = h.api.checkStaffUpdate(true)
  assert.deepEqual(h.counts(), [1, 0], 'single flight shares network request')
  h.settle(); await Promise.all([first, second])
  assert.deepEqual(h.counts(), [1, 1]); assert.equal(h.opened.length, 0, 'never download without confirmation')
  await h.api.checkStaffUpdate(true)
  assert.deepEqual(h.counts(), [1, 1], 'cancel snoozes cached version')
  h.advance(31 * 60_000)
  const again = h.api.checkStaffUpdate(true); h.settle(); await again
  assert.deepEqual(h.counts(), [2, 1], 'snooze persists after network refresh')
  h.response({ ...release, version: '1.0.8', versionCode: 108 })
  h.advance(31 * 60_000)
  const newer = h.api.checkStaffUpdate(true); h.settle(); await newer
  assert.deepEqual(h.counts(), [3, 2], 'new version bypasses old snooze')
  h.confirm()
  const manual = h.api.checkStaffUpdate(); h.settle(); await manual
  assert.equal(h.opened.length, 1, 'manual check bypasses snooze and confirmation opens package')
}
{
  const h = boot(); h.api.scheduleUpdateReminder(); h.route('pages-rental/xianyu-ship/index')
  const work = h.api.checkStaffUpdate(true); h.settle(); await work
  assert.deepEqual(h.counts(), [1, 0], 'warehouse work is not interrupted')
  h.route('pages/index/index'); await h.api.checkStaffUpdate(true)
  assert.deepEqual(h.counts(), [1, 1], 'cached reminder shown after returning home')
}
{
  const h = boot(); h.api.scheduleUpdateReminder()
  const work = h.api.checkStaffUpdate(true); h.api.pauseUpdateReminder(); h.settle(); await work
  assert.deepEqual(h.counts(), [1, 0], 'background response cannot prompt')
  assert.equal(h.timers.size, 0)
}
{
  const h = boot(); h.api.scheduleUpdateReminder(); h.response({ ...release, versionCode: 106 })
  const work = h.api.checkStaffUpdate(true); h.settle(); assert.equal(await work, 'current')
  assert.deepEqual(h.counts(), [1, 0])
}
{
  const h = boot(); h.api.scheduleUpdateReminder(); h.offline()
  const work = h.api.checkStaffUpdate(true); h.settle(); await assert.rejects(work)
  assert.equal(await h.api.checkStaffUpdate(true), 'skipped', 'offline attempts are throttled')
}
{
  const h = boot(); h.api.scheduleUpdateReminder(); h.modalFail()
  const work = h.api.checkStaffUpdate(true); h.settle(); await assert.rejects(work)
  assert.equal(h.storage.size, 0, 'failed prompt must not suppress tomorrow reminder')
}
console.log('staff update: single flight, snooze, new release, manual check, foreground and work-page guards passed')
