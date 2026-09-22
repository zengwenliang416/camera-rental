import assert from 'node:assert/strict'
import fs from 'node:fs'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from '@vue/compiler-sfc'
import { createApp, nextTick, ref } from 'vue'
import { createPinia, defineStore, setActivePinia } from 'pinia'
import { createPersistedState } from 'pinia-plugin-persistedstate'

const storage = new Map([['theme-store', JSON.stringify({ theme: 'dark' })]])
function bootStore() {
  const pinia = createPinia()
  pinia.use(createPersistedState({ storage: { getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value) } }))
  createApp({}).use(pinia)
  setActivePinia(pinia)
  const source = fs.readFileSync(new URL('../src/store/theme.ts', import.meta.url), 'utf8')
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const exports = {}
  vm.runInNewContext(js, { exports, ref, require: () => ({ defineStore }) })
  return exports.useThemeStore()
}
let store = bootStore()
assert.equal(store.theme, 'dark')
const app = parse(fs.readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')).descriptor.scriptSetup.content
const js = ts.transpileModule(app, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
vm.runInNewContext(js, {
  exports: {},
  require: name => name === '@/store/theme'
    ? { useThemeStore: () => store }
    : {
        onLaunch: fn => fn(),
        onShow() {},
        onHide() {},
        onMounted() {},
        onUnmounted() {},
      },
})
assert.equal(store.theme, 'dark', 'launch must preserve saved choice')
store.toggleTheme()
await nextTick()
store = bootStore()
assert.equal(store.theme, 'light', 'light choice persists across restart')
store.toggleTheme()
await nextTick()
store = bootStore()
assert.equal(store.theme, 'dark', 'dark choice persists across restart')
assert.equal(store.themeVars.buttonPrimaryBg, '#e10600', 'brand stays red')
console.log('PASS actual Pinia hydration, app launch, light/dark switching and restart persistence')
