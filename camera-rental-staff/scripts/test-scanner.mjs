import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import ts from 'typescript'

// Mock 只替代 Native.js 和页面生命周期；加载实际适配器/消费逻辑。
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'staff-scanner-test-'))
function compile(source, target, replacements = []) {
  let code = ts.transpileModule(fs.readFileSync(new URL(source, import.meta.url), 'utf8'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 },
  }).outputText
  for (const [from, to] of replacements)
    code = code.replace(from, to)
  fs.writeFileSync(path.join(dir, target), code)
}

try {
  compile('../src/services/scanner/android.ts', 'android.mjs')
  const { AndroidScanner } = await import(pathToFileURL(path.join(dir, 'android.mjs')))
  const native = { mode: 1, opened: false, receiver: undefined, registrations: 0, closes: 0, failSwitch: false }
  const manager = {
    getScannerState: () => native.opened,
    openScanner: () => (native.opened = true),
    closeScanner: () => {
      native.closes++
      native.opened = false
      return true
    },
    getOutputMode: () => native.mode,
    switchOutputMode: (mode) => {
      if (native.failSwitch && mode === 0)
        return false
      native.mode = mode
      return true
    },
    getParameterString: () => ['vendor.custom.action', 'custom_data'],
    startDecode: () => true,
    stopDecode: () => true,
  }
  const activity = {
    registerReceiver: (receiver, _filter, flag) => {
      assert.equal(flag, 2)
      native.receiver = receiver
      native.registrations++
    },
    unregisterReceiver: () => { native.receiver = undefined },
  }
  globalThis.plus = { android: {
    newObject: name => name.includes('ScanManager') ? manager : { addAction() {} },
    runtimeMainActivity: () => activity,
    getAttribute: (_target, name) => name === 'SDK_INT' ? 33 : 1,
    implements: (_name, receiver) => receiver,
    invoke: (target, name, ...args) => target[name](...args),
  } }
  const adapter = new AndroidScanner()
  const results = []
  adapter.initialize(value => results.push(value))
  adapter.initialize(value => results.push(value))
  assert.equal(native.registrations, 1, '重复初始化不能重复注册')
  assert.equal(native.mode, 0)
  const receiver = native.receiver
  const intent = (action, text) => ({
    getAction: () => action,
    getStringExtra: (key) => {
      assert.equal(key, 'custom_data')
      return text
    },
  })
  receiver.onReceive(null, intent('unrelated', 'ignored'))
  receiver.onReceive(null, intent('vendor.custom.action', '  TEST-01  '))
  assert.deepEqual(results, ['TEST-01'], '使用设备配置的 Action/key，忽略其他广播')
  adapter.dispose()
  receiver.onReceive(null, intent('vendor.custom.action', 'late'))
  assert.deepEqual(results, ['TEST-01'], '释放后的晚到广播不投递')
  assert.equal(native.mode, 1, '退出恢复键盘模式')
  assert.equal(native.closes, 1, '只关闭本适配器开启的扫描引擎')
  native.failSwitch = true
  assert.throws(() => adapter.initialize(() => {}), /广播模式/)
  assert.equal(native.receiver, undefined, '初始化失败回滚监听')
  assert.equal(native.mode, 1)
  native.failSwitch = false
  native.opened = true
  const closes = native.closes
  adapter.initialize(() => {})
  adapter.dispose()
  assert.equal(native.opened, true, '保留原本开启的扫描引擎')
  assert.equal(native.closes, closes)
  console.log('PASS native: 自定义协议、幂等注册、晚到广播、模式恢复、失败回滚、引擎所有权')

  fs.writeFileSync(path.join(dir, 'mocks.mjs'), `
export const hooks = { show: [], hide: [], unload: [] }
export const onShow = cb => hooks.show.push(cb)
export const onHide = cb => hooks.hide.push(cb)
export const onUnload = cb => hooks.unload.push(cb)
export const listeners = new Set()
export const scannerConnected = { value: true }
export const scanner = {
  initialize: async () => {}, stopDecode: () => {}, start: async () => {},
  subscribe: cb => { listeners.add(cb); return () => listeners.delete(cb) },
  scanCamera: async () => ({ text: 'camera', source: 'camera', receivedAt: 4000 })
}
`)
  compile('../src/hooks/useStaffScanner.ts', 'hook.mjs', [
    ['\'@dcloudio/uni-app\'', '\'./mocks.mjs\''],
    ['\'@/services/scanner\'', '\'./mocks.mjs\''],
  ])
  const { useStaffScanner } = await import(pathToFileURL(path.join(dir, 'hook.mjs')))
  const { hooks, listeners, scannerConnected } = await import(pathToFileURL(path.join(dir, 'mocks.mjs')))
  const toasts = []
  globalThis.uni = { showToast: value => toasts.push(value.title) }
  const seen = []
  let finish
  const page = useStaffScanner(async (result) => {
    seen.push(result.text)
    if (result.text === 'first')
      await new Promise((resolve) => { finish = resolve })
    if (result.text === 'fail')
      throw new Error('network')
  })
  const emit = (text, receivedAt) => listeners.forEach(cb => cb({ text, receivedAt, source: 'vendor-broadcast' }))
  const flush = () => new Promise(resolve => setImmediate(resolve))
  hooks.show.forEach(cb => cb())
  hooks.show.forEach(cb => cb())
  assert.equal(listeners.size, 1)
  emit('first', 1000)
  emit('busy', 1100)
  assert.deepEqual(seen, ['first'])
  assert.ok(toasts.some(text => text.includes('上一条')))
  finish()
  await flush()
  emit('first', 1200)
  assert.deepEqual(seen, ['first'], '短时间重复码去重')
  emit('fail', 2000)
  await flush()
  emit('fail', 2100)
  await flush()
  assert.equal(seen.filter(text => text === 'fail').length, 2, '失败可立即重试')
  scannerConnected.value = false
  await page.scan()
  assert.equal(seen.at(-1), 'camera')
  hooks.hide.forEach(cb => cb())
  emit('hidden', 5000)
  assert.equal(listeners.size, 0)
  assert.equal(seen.at(-1), 'camera', '隐藏页不消费')
  hooks.unload.forEach(cb => cb())
  console.log('PASS lifecycle: 单页订阅、忙碌提示、重复码抑制、失败重试、相机回退、隐藏页隔离')
} finally {
  fs.rmSync(dir, { recursive: true, force: true })
  delete globalThis.plus
  delete globalThis.uni
}
