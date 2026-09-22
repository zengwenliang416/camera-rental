import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'
import * as vue from 'vue'
import { createPinia, defineStore, setActivePinia } from 'pinia'
import { createPersistedState } from 'pinia-plugin-persistedstate'

const storage = new Map([['theme-store', JSON.stringify({ theme: 'dark' })]])
const listeners = new Set()
let osTheme = 'light'
const uni = {
  getSystemInfoSync: () => ({ osTheme, theme: 'light' }),
  onThemeChange: fn => listeners.add(fn),
  offThemeChange: fn => listeners.delete(fn),
}
function load(file, mocks, expose = '', globals = {}) {
  let source = fs.readFileSync(new URL(file, import.meta.url), 'utf8')
  if (file.endsWith('.vue'))
    source = parse(source).descriptor.scriptSetup.content
  if (expose)
    source += `\nexport { ${expose} }`
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const exports = {}
  vm.runInNewContext(js, { exports, uni, document: { removeEventListener() {} }, window: { removeEventListener() {} }, defineProps() {}, require: name => mocks[name], ...globals })
  return exports
}
function bootStore() {
  const pinia = createPinia()
  pinia.use(createPersistedState({ storage: { getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value) } }))
  vue.createApp({}).use(pinia)
  setActivePinia(pinia)
  return load('../src/store/theme.ts', { 'pinia': { defineStore }, vue, '@/services/systemTheme': { observeAndroidThemeChange: () => undefined } }).useThemeStore()
}
function systemChanges(value) {
  osTheme = value
  for (const listener of listeners)
    listener({ theme: value })
}
let store = bootStore()
const appHooks = { launch: [], show: [], unmount: [] }
load('../src/App.vue', {
  '@dcloudio/uni-app': { onLaunch: fn => appHooks.launch.push(fn), onShow: fn => appHooks.show.push(fn), onHide() {} },
  'vue': { onMounted() {}, onUnmounted: fn => appHooks.unmount.push(fn) },
  '@/store/theme': { useThemeStore: () => store },
  '@/store': { useTokenStore: () => ({ updateNowTime: () => ({ hasLogin: false }) }), useDictStore: () => ({}) },
  '@/services/scanner': { scanner: { initialize() {} } },
  '@/tabbar/store': { tabbarStore: { syncCurIdxByCurrentPageAsync() {} } },
  '@/router/interceptor': { navigateToInterceptor: { invoke() {} } },
})
appHooks.launch.forEach(fn => fn())
assert.equal(store.themeMode, 'system', 'legacy manual cache upgrades to requested system default')
assert.equal(store.theme, 'light', 'launch reads phone rather than last rendered theme')
store.startThemeListener()
assert.equal(listeners.size, 1, 'subscription is idempotent')
systemChanges('dark')
assert.equal(store.theme, 'dark')
systemChanges('light')
assert.equal(store.theme, 'light')
store.setThemeMode('dark')
systemChanges('light')
assert.equal(store.theme, 'dark', 'manual dark ignores system light')
store.setThemeMode('light')
systemChanges('dark')
assert.equal(store.theme, 'light', 'manual light ignores system dark')
store.setThemeMode('system')
assert.equal(store.theme, 'dark', 'return to system reads current phone theme immediately')
osTheme = 'light' // missed notification while backgrounded
appHooks.show.forEach(fn => fn({}))
assert.equal(store.theme, 'light', 'foreground refresh catches missed OS changes')
store.setThemeMode('dark')
await vue.nextTick()
const saved = JSON.parse(storage.get('theme-store'))
assert.equal(saved.themeMode, 'dark')
assert.equal(saved.theme, undefined, 'derived appearance is not persisted')
assert.equal(saved.systemTheme, undefined, 'OS state is not persisted')
appHooks.unmount.forEach(fn => fn())
assert.equal(listeners.size, 0, 'teardown removes the same callback')
store = bootStore()
store.startThemeListener()
assert.equal(store.theme, 'dark', 'manual override persists across restarts')
store.setThemeMode('system')
await vue.nextTick()
store.stopThemeListener()
osTheme = 'dark'
store = bootStore()
store.startThemeListener()
assert.equal(store.themeMode, 'system')
assert.equal(store.theme, 'dark', 'system preference reads fresh OS appearance on restart')
assert.equal(store.themeVars.buttonPrimaryBg, '#e10600')
systemChanges('unknown')
assert.equal(store.theme, 'dark', 'unsupported events cannot corrupt the theme')
const picker = load('../src/components/staff-theme-setting.vue', { vue, '@/store/theme': { useThemeStore: () => store } }, 'selectMode')
picker.selectMode({ index: 1 })
assert.equal(store.themeMode, 'light')
picker.selectMode({ index: 2 })
assert.equal(store.themeMode, 'dark')
picker.selectMode({ index: 0 })
assert.equal(store.themeMode, 'system')
picker.selectMode({ index: -1 })
assert.equal(store.themeMode, 'system')
store.stopThemeListener()
assert.equal(listeners.size, 0)
console.log('PASS legacy migration, live OS changes, manual overrides, foreground resync, restart persistence, listener cleanup and shared picker')

let receiver
let callbacks = 0
let registered = 0
let unregistered = 0
let sdk = 32
const platform = { os: { name: 'Android' }, android: {
  runtimeMainActivity: () => ({}),
  newObject: () => ({}),
  getAttribute: () => sdk,
  implements: (_name, implementation) => implementation,
  invoke: (target, method, ...args) => {
    if (method === 'registerReceiver') {
      receiver = args[0]
      registered++
      if (sdk >= 33)
        assert.equal(args[2], 4)
    }
    if (method === 'unregisterReceiver')
      unregistered++
    if (method === 'getAction')
      return target.action
  },
} }
const native = load('../src/services/systemTheme.ts', {}, '', { plus: platform })
const releaseListener = native.observeAndroidThemeChange(() => callbacks++)
receiver.onReceive({}, { action: 'unrelated' })
assert.equal(callbacks, 0)
receiver.onReceive({}, { action: 'android.intent.action.CONFIGURATION_CHANGED' })
assert.equal(callbacks, 1)
releaseListener()
releaseListener()
receiver.onReceive({}, { action: 'android.intent.action.CONFIGURATION_CHANGED' })
assert.equal(callbacks, 1)
assert.equal(unregistered, 1)
sdk = 34
native.observeAndroidThemeChange(() => {})()
assert.equal(registered, 2)
assert.equal(unregistered, 2)
console.log('PASS Android config notification, protected registration flags, cleanup and late callback isolation')
