'use strict'

module.exports = {
  scenarios: {
    'rental-configuration-admin-flow': {
      scenario: async function ({ page, assertion }) {
      const origin = 'http://127.0.0.1:15173'
      const cache = (value) =>
        JSON.stringify({
          c: Date.now(),
          e: 253402300799999,
          v: JSON.stringify(value)
        })
      await page.addInitScript(
        ({ tokenValue, tenantValue }) => {
          localStorage.setItem('ACCESS_TOKEN', tokenValue)
          localStorage.setItem('tenantId', tenantValue)
        },
        {
          tokenValue: cache('specnav-local-token'),
          tenantValue: cache(1)
        }
      )
      await page.goto(`${origin}/rental/configuration`)
      await page.getByRole('heading', { name: '租赁配置' }).waitFor()

      const results = []
      results.push(await page.getByText('Sony A7M4 single-model rental').first().isVisible())
      results.push(await page.getByText('5127394070109009316').first().isVisible())
      results.push(await page.getByText('1062409679830').first().isVisible())

      await page.getByRole('tab', { name: '设备目录' }).click()
      results.push(await page.getByText('SONY-A7M4', { exact: true }).isVisible())
      await page.getByRole('button', { name: '新增大类' }).click()
      await page.getByLabel('大类编码').fill('LENS')
      await page.getByLabel('大类名称').fill('Lens')
      await page.getByRole('dialog', { name: '新增大类' }).getByRole('button', { name: '确定' }).click()
      await page.getByText('LENS', { exact: true }).last().waitFor()
      results.push(await page.getByText('LENS', { exact: true }).last().isVisible())

      await page.getByRole('tab', { name: '渠道商品规则' }).click()
      await page.getByRole('button', { name: '新增规则' }).click()
      const drawer = page.getByRole('dialog', { name: '新增规则' })
      await drawer.locator('.el-select').first().click()
      await page.getByText('Xiaojiang (#7)').last().click()
      await drawer.getByLabel('闲鱼商品 ID').fill('fixture-item-new')
      await drawer.locator('.el-select').nth(1).click()
      await page.getByText('Sony A7M4').last().click()
      await drawer.getByRole('button', { name: '预览影响并保存' }).click()
      const impact = page.getByRole('dialog', { name: '确认规则变更影响' })
      await impact.waitFor()
      results.push(await impact.getByText('履约受保护').isVisible())
      results.push(await impact.getByText('2', { exact: true }).last().isVisible())
      await impact.getByRole('button', { name: '确认保存并重评' }).click()
      const reconciliation = page.getByRole('dialog', { name: '订单重评结果' })
      await reconciliation.waitFor()
      results.push(await reconciliation.getByText('重评完成').isVisible())
      await reconciliation.getByRole('button', { name: '关闭', exact: true }).click()

      const multiRow = page.getByText('Multi-model camera kit').first()
      await multiRow.waitFor()
      const row = multiRow.locator('xpath=ancestor::tr')
      await row.locator('td').first().click()
      await page.getByText('xgj-sku-a7m4').first().waitFor()
      results.push(await page.getByText('xy-sku-fx3').first().isVisible())

      await page.getByRole('tab', { name: '闲鱼备注规范' }).click()
      results.push(await page.getByText('发货8.31/收货9.1/发回9.6').isVisible())
      results.push(await page.getByText('换机', { exact: true }).isVisible())
      results.push(await page.getByText('早退', { exact: true }).isVisible())

      assertion.ok('CASE-001-admin-configuration-ASSERT', results.every(Boolean))
    },
    },

    'rental-device-shared-catalog': {
      scenario: async function ({ page, assertion }) {
      const origin = 'http://127.0.0.1:15173'
      const cache = (value) =>
        JSON.stringify({
          c: Date.now(),
          e: 253402300799999,
          v: JSON.stringify(value)
        })
      await page.addInitScript(
        ({ tokenValue, tenantValue }) => {
          localStorage.setItem('ACCESS_TOKEN', tokenValue)
          localStorage.setItem('tenantId', tenantValue)
        },
        {
          tokenValue: cache('specnav-local-token'),
          tenantValue: cache(1)
        }
      )
      await page.goto(`${origin}/rental/device`)
      await page.getByText('A7M4-001').waitFor()

      const results = []
      results.push(await page.getByText('SONY-A7M4', { exact: true }).first().isVisible())
      results.push((await page.getByRole('button', { name: '新增大类' }).count()) === 0)
      results.push((await page.getByRole('button', { name: '新增型号' }).count()) === 0)
      await page.getByRole('button', { name: '新增' }).click()
      const dialog = page.getByRole('dialog', { name: '新建设备' })
      await dialog.waitFor()
      results.push(await dialog.getByLabel('设备大类').isVisible())
      results.push(await dialog.getByLabel('设备型号').isVisible())
      results.push((await dialog.getByText('新增大类').count()) === 0)
      results.push((await dialog.getByText('新增型号').count()) === 0)

      assertion.ok('CASE-009-rental-device-catalog-ASSERT', results.every(Boolean))
    },
    },

    'rental-configuration-sensory-states': {
      scenario: async function ({ page, assertion }) {
      const origin = 'http://127.0.0.1:15173'
      const cache = (value) =>
        JSON.stringify({
          c: Date.now(),
          e: 253402300799999,
          v: JSON.stringify(value)
        })
      await page.addInitScript(
        ({ tokenValue, tenantValue }) => {
          localStorage.setItem('ACCESS_TOKEN', tokenValue)
          localStorage.setItem('tenantId', tenantValue)
        },
        {
          tokenValue: cache('specnav-local-token'),
          tenantValue: cache(1)
        }
      )
      const setMode = async (mode) => {
        await page.evaluate(async (nextMode) => {
          await fetch('/admin-api/__specnav/state', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode: nextMode })
          })
        }, mode)
      }
      const setCache = async (key, value) => {
        await page.evaluate(
          ({ cacheKey, cacheValue }) => {
            localStorage.setItem(
              cacheKey,
              JSON.stringify({
                c: Date.now(),
                e: 253402300799999,
                v: JSON.stringify(cacheValue)
              })
            )
          },
          { cacheKey: key, cacheValue: value }
        )
      }

      const results = []
      await page.goto(`${origin}/rental/configuration`)
      await page.getByRole('heading', { name: '租赁配置' }).waitFor()
      results.push(await page.getByRole('heading', { name: '租赁配置' }).isVisible())

      await setCache('isDark', true)
      await page.reload()
      await page.getByRole('heading', { name: '租赁配置' }).waitFor()
      results.push(await page.locator('html.dark').isVisible())

      await setCache('lang', 'en')
      await page.reload()
      await page.getByRole('heading', { name: 'Rental Configuration' }).waitFor()
      results.push(await page.getByRole('tab', { name: 'Channel Product Rules' }).isVisible())

      await page.setViewportSize({ width: 390, height: 844 })
      await page.reload()
      const mobileRules = page.locator('.mobile-rules')
      await mobileRules.getByText('Sony A7M4 single-model rental', { exact: true }).waitFor()
      results.push(await mobileRules.isVisible())
      results.push(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= document.documentElement.clientWidth
        )
      )

      await setMode('loading')
      await page.reload({ waitUntil: 'domcontentloaded' })
      const loadingSkeleton = page.locator('.el-skeleton')
      await loadingSkeleton.waitFor()
      results.push(await loadingSkeleton.isVisible())
      await page.getByRole('heading', { name: 'Rental Configuration' }).waitFor()

      await setMode('error')
      await page.reload()
      await page.getByText('Rental configuration failed to load').waitFor()
      results.push(await page.getByText('Rental configuration failed to load').isVisible())

      await setMode('empty')
      await page.reload()
      await page.getByRole('tab', { name: 'Device Catalog' }).click()
      await page.getByText('No device catalog').waitFor()
      results.push(await page.getByText('No device catalog').isVisible())

      await setMode('permission')
      await page.reload()
      const permissionMessage = page.getByText(
        "Sorry, you don't have permission to access this page."
      )
      await permissionMessage.waitFor()
      results.push(await permissionMessage.isVisible())

      await setMode('normal')
      await setCache('lang', 'zh-CN')
      await setCache('isDark', false)
      await page.setViewportSize({ width: 1280, height: 900 })
      await page.reload()
      await page.getByRole('heading', { name: '租赁配置' }).waitFor()

      assertion.ok('CASE-010-theme-locale-states-ASSERT', results.every(Boolean))
    }
    }
  }
}
