import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { after, before, test } from 'node:test'
import vue from '@vitejs/plugin-vue'
import { createSSRApp, h, type Component } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createServer, type ViteDevServer } from 'vite'

const root = new URL('..', import.meta.url).pathname
const read = (path: string) => readFile(new URL(`../${path}`, import.meta.url), 'utf8')
let server: ViteDevServer

const passthroughStub: Component = {
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h('div', attrs, [slots.default?.(), slots.footer?.()])
  }
}

const emptyStub: Component = {
  inheritAttrs: false,
  setup(_props, { attrs }) {
    return () => h('div', attrs)
  }
}

before(async () => {
  server = await createServer({
    configFile: false,
    root,
    appType: 'custom',
    server: { middlewareMode: true },
    optimizeDeps: { noDiscovery: true },
    resolve: { alias: { '@': `${root}/src` } },
    plugins: [
      {
        name: 'configuration-test-stubs',
        enforce: 'pre',
        resolveId(id) {
          if (id.endsWith('/src/hooks/web/useI18n') || id === '@/hooks/web/useI18n') {
            return '\0configuration-i18n'
          }
          if (id.endsWith('/src/hooks/web/useMessage') || id === '@/hooks/web/useMessage') {
            return '\0configuration-message'
          }
        },
        load(id) {
          if (id === '\0configuration-i18n') {
            return `export const useI18n = () => ({
              t: (key, params) => params ? key + JSON.stringify(params) : key
            })`
          }
          if (id === '\0configuration-message') {
            return `export const useMessage = () => ({
              success: () => undefined,
              warning: () => undefined,
              error: () => undefined
            })`
          }
        }
      },
      vue()
    ]
  })
})

after(async () => {
  await server.close()
})

const renderComponent = async (
  path: string,
  props: Record<string, unknown>,
  componentNames: string[]
) => {
  const module = await server.ssrLoadModule(path)
  const app = createSSRApp({ render: () => h(module.default, props) })
  for (const name of componentNames) {
    if (name === 'el-table' || name === 'el-table-column') continue
    app.component(name, passthroughStub)
  }
  app.component('ElTable', emptyStub)
  app.component('ElTableColumn', emptyStub)
  app.component('Icon', passthroughStub)
  app.directive('loading', () => undefined)
  return renderToString(app)
}

test('identifier summary renders explicit missing markers instead of hiding fields', async () => {
  const html = await renderComponent(
    '/src/views/rental/configuration/components/ChannelIdentifierSummary.vue',
    {
      scope: 'product',
      xgjProductId: 'xgj-product-1',
      xianyuItemId: undefined
    },
    ['el-button']
  )
  assert.match(html, /xgj-product-1/)
  assert.match(html, /<code class="is-empty"[^>]*>—<\/code>/)
  assert.match(html, /rental\.configuration\.xianyuItemId/)
})

test('narrow SKU structure renders both exact identifiers and mapping state', async () => {
  const html = await renderComponent(
    '/src/views/rental/configuration/components/ChannelSkuMappingTable.vue',
    {
      modelValue: [
        {
          productSkuId: 81,
          xgjSkuId: '537044127563786',
          xianyuSkuId: '991122334455',
          skuName: '标准套装',
          deviceModelId: undefined
        }
      ],
      models: [],
      editable: false,
      loading: false
    },
    [
      'el-alert',
      'el-table',
      'el-table-column',
      'el-select',
      'el-option',
      'el-tag',
      'el-empty',
      'el-button'
    ]
  )
  assert.match(html, /class="mobile-sku-list"/)
  assert.match(html, /537044127563786/)
  assert.match(html, /991122334455/)
  assert.match(html, /rental\.configuration\.waitingMapping/)
})

test('reconciliation result renders status, counters, retry warning and viewport width', async () => {
  const html = await renderComponent(
    '/src/views/rental/configuration/components/RuleReconciliationResultDialog.vue',
    {
      modelValue: true,
      loadError: true,
      run: {
        runId: 1,
        productRuleId: 2,
        shopId: 3,
        xianyuItemId: '5127394070109009316',
        triggerType: 'RULE_CHANGE',
        status: 'COMPLETED_WITH_ERRORS',
        scannedCount: 8,
        skippedCount: 1,
        createdCount: 2,
        updatedCount: 3,
        unchangedCount: 1,
        conflictCount: 4,
        failedCount: 5,
        reviewRequiredCount: 6
      }
    },
    ['el-dialog', 'el-alert', 'el-button']
  )
  assert.match(html, /width="min\(720px, calc\(100vw - 24px\)\)"/)
  assert.match(html, /reconciliationStatus\.COMPLETED_WITH_ERRORS/)
  assert.match(html, /reconciliationLoadError/)
  for (const count of ['8', '1', '2', '3', '4', '5', '6']) {
    assert.match(html, new RegExp(`<strong[^>]*>${count}</strong>`))
  }
})

test('rental device keeps catalog selection but removes catalog quick-create controls', async () => {
  const source = await read('src/views/rental/device/index.vue')
  assert.match(source, /getRentalDeviceCatalog/)
  assert.match(source, /createRentalDevice/)
  assert.doesNotMatch(source, /DeviceCategoryCreateDialog/)
  assert.doesNotMatch(source, /DeviceModelCreateDialog/)
})

test('configuration layouts keep explicit narrow-screen rules', async () => {
  const ruleTable = await read(
    'src/views/rental/configuration/components/ChannelProductRuleTable.vue'
  )
  const skuTable = await read(
    'src/views/rental/configuration/components/ChannelSkuMappingTable.vue'
  )
  assert.match(ruleTable, /@media \(width <= 720px\)/)
  assert.match(ruleTable, /\.mobile-rule-card__skus/)
  assert.match(skuTable, /@media \(width <= 720px\)/)
  assert.match(skuTable, /\.mobile-sku-list/)
})
