import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { createServer } from '../../camera-rental-admin/node_modules/vite/dist/node/index.js'

const fixtureDir = dirname(fileURLToPath(import.meta.url))
const projectRoot = resolve(fixtureDir, '../..')
const adminRoot = resolve(projectRoot, 'camera-rental-admin')
const host = '127.0.0.1'
const port = 15173
const origin = `http://${host}:${port}`

process.env.VITE_BASE_URL = origin
process.env.VITE_API_URL = '/admin-api'
process.env.VITE_BASE_PATH = '/'
process.env.VITE_PORT = String(port)
process.env.VITE_OPEN = 'false'

const state = {
  mode: 'normal',
  nextRuleId: 103,
  categories: [
    {
      id: 11,
      categoryCode: 'CAMERA',
      categoryName: 'Camera',
      sortOrder: 10,
      enabled: true,
      lockVersion: 3,
      models: [
        {
          id: 101,
          categoryId: 11,
          modelCode: 'SONY-A7M4',
          modelName: 'Sony A7M4',
          deviceNoPrefix: 'A7M4',
          sortOrder: 10,
          enabled: true,
          lockVersion: 2
        },
        {
          id: 102,
          categoryId: 11,
          modelCode: 'SONY-FX3',
          modelName: 'Sony FX3',
          deviceNoPrefix: 'FX3',
          sortOrder: 20,
          enabled: true,
          lockVersion: 1
        }
      ]
    },
    {
      id: 12,
      categoryCode: 'DRONE',
      categoryName: 'Drone',
      sortOrder: 20,
      enabled: true,
      lockVersion: 1,
      models: [
        {
          id: 201,
          categoryId: 12,
          modelCode: 'DJI-MINI4-PRO',
          modelName: 'DJI Mini 4 Pro',
          deviceNoPrefix: 'MINI4',
          sortOrder: 10,
          enabled: true,
          lockVersion: 1
        }
      ]
    }
  ],
  rules: [
    {
      id: 101,
      shopId: 7,
      xianyuItemId: '5127394070109009316',
      xgjProductId: '1062409679830',
      productTitleSnapshot: 'Sony A7M4 single-model rental',
      handlingPolicy: 'CREATE_RENTAL',
      mappingMode: 'SINGLE',
      singleDeviceModelId: 101,
      enabled: true,
      ruleNote: 'Exact shop and item mapping',
      lockVersion: 4,
      skuMappings: []
    },
    {
      id: 102,
      shopId: 8,
      xianyuItemId: '1024163647751',
      xgjProductId: 'xgj-product-multi-1',
      productTitleSnapshot: 'Multi-model camera kit',
      handlingPolicy: 'CREATE_RENTAL',
      mappingMode: 'MULTI',
      enabled: true,
      ruleNote: 'Exact synchronized SKU mapping',
      lockVersion: 2,
      skuMappings: [
        {
          productSkuId: 9001,
          xgjSkuId: 'xgj-sku-a7m4',
          xianyuSkuId: 'xy-sku-a7m4',
          skuName: 'A7M4 body',
          deviceModelId: 101,
          mappingEnabled: true
        },
        {
          productSkuId: 9002,
          xgjSkuId: 'xgj-sku-fx3',
          xianyuSkuId: 'xy-sku-fx3',
          skuName: 'FX3 body',
          deviceModelId: 102,
          mappingEnabled: true
        }
      ]
    }
  ]
}

const commonResult = (data, code = 0, msg = '') => ({ code, data, msg })

const readBody = async (req) => {
  const chunks = []
  for await (const chunk of req) chunks.push(chunk)
  if (chunks.length === 0) return {}
  return JSON.parse(Buffer.concat(chunks).toString('utf8'))
}

const sendJson = (res, data, status = 200) => {
  res.statusCode = status
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  res.end(JSON.stringify(data))
}

const permissionInfo = () => ({
  user: {
    id: 90001,
    avatar: '',
    nickname: 'SpecNav Reviewer',
    deptId: 1
  },
  roles: ['super_admin'],
  permissions:
    state.mode === 'permission'
      ? ['rental:device:query']
      : [
          'rental:configuration:query',
          'rental:configuration:update',
          'rental:device:query',
          'rental:device:create',
          'rental:device:update',
          'rental:device:delete',
          'rental:device:assign'
        ],
  menus: [
    {
      id: 7001,
      parentId: 0,
      name: 'Rental Configuration',
      path: '/rental/configuration',
      component: 'rental/configuration/index',
      componentName: 'RentalConfiguration',
      icon: 'ep:setting',
      visible: true,
      keepAlive: false,
      alwaysShow: false
    },
    {
      id: 7002,
      parentId: 0,
      name: 'Rental Devices',
      path: '/rental/device',
      component: 'rental/device/index',
      componentName: 'RentalDevice',
      icon: 'ep:camera',
      visible: true,
      keepAlive: false,
      alwaysShow: false
    }
  ]
})

const delayed = async () => {
  if (state.mode === 'loading') {
    await new Promise((resolveDelay) => setTimeout(resolveDelay, 1400))
  }
}

const apiHandler = async (req, res, next) => {
  if (!req.url?.startsWith('/admin-api/')) {
    next()
    return
  }
  const url = new URL(req.url, origin)
  const path = url.pathname.slice('/admin-api'.length)

  if (path === '/__specnav/state' && req.method === 'POST') {
    const body = await readBody(req)
    state.mode = ['normal', 'loading', 'empty', 'error', 'permission'].includes(body.mode)
      ? body.mode
      : 'normal'
    sendJson(res, commonResult({ mode: state.mode }))
    return
  }

  await delayed()
  if (
    state.mode === 'error' &&
    [
      '/rental/configuration/catalog',
      '/rental/configuration/shops',
      '/rental/configuration/product-rule/page'
    ].includes(path)
  ) {
    sendJson(res, commonResult(null, 500, 'SPECNAV_FIXTURE_ERROR'), 500)
    return
  }

  if (path === '/system/auth/get-permission-info') {
    sendJson(res, commonResult(permissionInfo()))
    return
  }
  if (path === '/system/dict-data/simple-list') {
    sendJson(res, commonResult([]))
    return
  }
  if (path === '/rental/configuration/catalog' && req.method === 'GET') {
    sendJson(res, commonResult({ categories: state.mode === 'empty' ? [] : state.categories }))
    return
  }
  if (path === '/rental/configuration/shops' && req.method === 'GET') {
    sendJson(
      res,
      commonResult(
        state.mode === 'empty'
          ? []
          : [
              { id: 7, shopName: 'Xiaojiang', authorizationStatus: 'VALID' },
              { id: 8, shopName: 'Fafa', authorizationStatus: 'VALID' }
            ]
      )
    )
    return
  }
  if (path === '/rental/configuration/product-rule/page' && req.method === 'GET') {
    const rules = state.mode === 'empty' ? [] : state.rules
    sendJson(res, commonResult({ list: rules, total: rules.length }))
    return
  }
  if (path === '/rental/configuration/product-rule/get' && req.method === 'GET') {
    const id = Number(url.searchParams.get('id'))
    sendJson(res, commonResult(state.rules.find((rule) => rule.id === id)))
    return
  }
  if (path === '/rental/configuration/product-rule/synced-skus' && req.method === 'GET') {
    const itemId = url.searchParams.get('xianyuItemId')
    const rule = state.rules.find((entry) => entry.xianyuItemId === itemId)
    sendJson(
      res,
      commonResult(
        rule?.skuMappings?.length
          ? rule.skuMappings
          : [
              {
                productSkuId: 9101,
                xgjSkuId: 'xgj-sku-new-1',
                xianyuSkuId: 'xy-sku-new-1',
                skuName: 'New synchronized SKU',
                mappingEnabled: true
              }
            ]
      )
    )
    return
  }
  if (path === '/rental/configuration/product-rule/impact' && req.method === 'GET') {
    sendJson(
      res,
      commonResult({
        scannedCount: 8,
        withoutInternalOrderCount: 2,
        mutableInternalOrderCount: 3,
        protectedOrderCount: 2,
        reviewRequiredCount: 1
      })
    )
    return
  }
  if (
    ['/rental/configuration/product-rule/create', '/rental/configuration/product-rule/update'].includes(
      path
    )
  ) {
    const body = await readBody(req)
    const id = body.id ?? state.nextRuleId++
    const existingIndex = state.rules.findIndex((rule) => rule.id === id)
    const saved = {
      id,
      xgjProductId: body.xgjProductId ?? `fixture-product-${id}`,
      productTitleSnapshot: `Fixture rule ${id}`,
      lockVersion: (body.lockVersion ?? 0) + 1,
      skuMappings: body.skuMappings ?? [],
      ...body
    }
    if (existingIndex >= 0) state.rules[existingIndex] = saved
    else state.rules.push(saved)
    sendJson(
      res,
      commonResult({
        ruleId: id,
        lockVersion: saved.lockVersion,
        impact: {
          scannedCount: 8,
          withoutInternalOrderCount: 2,
          mutableInternalOrderCount: 3,
          protectedOrderCount: 2,
          reviewRequiredCount: 1
        },
        reconciliationRunId: 601
      })
    )
    return
  }
  if (path === '/rental/configuration/product-rule/status') {
    await readBody(req)
    sendJson(
      res,
      commonResult({
        ruleId: 101,
        lockVersion: 5,
        impact: {
          scannedCount: 8,
          withoutInternalOrderCount: 2,
          mutableInternalOrderCount: 3,
          protectedOrderCount: 2,
          reviewRequiredCount: 1
        },
        reconciliationRunId: 602
      })
    )
    return
  }
  if (path === '/rental/configuration/product-rule/reconciliation' && req.method === 'GET') {
    sendJson(
      res,
      commonResult({
        runId: Number(url.searchParams.get('runId')),
        productRuleId: 103,
        shopId: 7,
        xianyuItemId: 'fixture-item-new',
        triggerType: 'RULE_CHANGE',
        status: 'SUCCEEDED',
        scannedCount: 8,
        skippedCount: 1,
        createdCount: 2,
        updatedCount: 3,
        unchangedCount: 1,
        conflictCount: 1,
        failedCount: 0,
        reviewRequiredCount: 1
      })
    )
    return
  }
  if (path.startsWith('/rental/configuration/catalog/') && req.method !== 'GET') {
    const body = await readBody(req)
    if (path.endsWith('/category/create')) {
      state.categories.push({
        id: 20 + state.categories.length,
        categoryCode: body.categoryCode,
        categoryName: body.categoryName,
        sortOrder: body.sortOrder ?? 0,
        enabled: true,
        lockVersion: 0,
        models: []
      })
    }
    sendJson(res, commonResult(1))
    return
  }
  if (path === '/rental/device/catalog' && req.method === 'GET') {
    sendJson(res, commonResult(state.mode === 'empty' ? [] : state.categories))
    return
  }
  if (path === '/rental/device/page' && req.method === 'GET') {
    const list =
      state.mode === 'empty'
        ? []
        : [
            {
              id: 501,
              deviceNo: 'A7M4-001',
              serialNumber: 'SERIAL-FIXTURE-001',
              categoryCode: 'CAMERA',
              equipmentModelCode: 'SONY-A7M4',
              status: 'AVAILABLE',
              warehouseCode: 'WH-A',
              purchaseAmount: 1680000,
              enabled: true
            }
          ]
    sendJson(res, commonResult({ list, total: list.length }))
    return
  }
  if (path === '/rental/device/create' && req.method === 'POST') {
    await readBody(req)
    sendJson(res, commonResult(502))
    return
  }

  sendJson(res, commonResult(true))
}

const fixturePlugin = {
  name: 'specnav-rental-configuration-fixture',
  configureServer(server) {
    server.middlewares.use(apiHandler)
  }
}

process.chdir(adminRoot)
const server = await createServer({
  root: adminRoot,
  mode: 'env.local',
  server: {
    host,
    port,
    strictPort: true,
    open: false
  },
  plugins: [fixturePlugin]
})

await server.listen()
console.log(`SpecNav rental fixture ready at ${origin}`)

const close = async () => {
  await server.close()
  process.exit(0)
}

process.on('SIGINT', close)
process.on('SIGTERM', close)
