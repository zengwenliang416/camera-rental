import { expect, test, type Page } from '@playwright/test'

test('mobile customer can retry upload, omit packaging and submit only once', async ({ page }) => {
  let attachmentSequence = 0
  let submitCount = 0
  let failedFirstUpload = false
  const attachmentCategories = new Map<number, string>()
  await mockReturnApi(page, {
    status: 'DRAFT',
    onAuthorize: async (request) => {
      attachmentSequence += 1
      const body = request.postDataJSON() as { category: string }
      attachmentCategories.set(attachmentSequence, body.category)
      return {
        attachmentId: attachmentSequence,
        uploadUrl: `https://storage.test/upload/${attachmentSequence}`,
        contentType: 'image/jpeg'
      }
    },
    onSubmit: async () => {
      submitCount += 1
      await new Promise((resolve) => setTimeout(resolve, 100))
      return {
        formNo: 'RR202608010001',
        status: 'ACCEPTED',
        waybillNo: 'SF1000000001',
        deliveryId: 80,
        submittedAt: '2026-08-01T17:00:00'
      }
    },
    attachmentCategories
  })
  await page.route('https://storage.test/upload/*', async (route) => {
    if (!failedFirstUpload) {
      failedFirstUpload = true
      await route.fulfill({ status: 500, body: 'retry' })
      return
    }
    await route.fulfill({ status: 200, body: '' })
  })

  await page.goto('/return/test-token')
  await expect(page.locator('.order-confirm').getByText('ORDER-001', { exact: true }))
    .toBeVisible()
  await page.getByRole('button', { name: '下一步 →' }).click()
  await page.locator('select').selectOption('SHUNFENG')
  await page.locator('input[type="date"]').fill('2026-08-01')
  await page.getByPlaceholder('输入或粘贴物流单号').fill('SF1000000001')
  await page.getByRole('button', { name: '下一步 →' }).click()
  await page.getByPlaceholder('例如 A6-08-4L5H').fill('a6 － 08 — 4l5h')
  await page.getByRole('button', { name: '下一步 →' }).click()

  const photoInputs = page.locator('input[type="file"]')
  await photoInputs.nth(0).setInputFiles({
    name: 'exterior.jpg',
    mimeType: 'image/jpeg',
    buffer: Buffer.from('exterior')
  })
  await expect(page.getByRole('button', { name: '重试' })).toBeVisible()
  await page.getByRole('button', { name: '重试' }).click()
  await expect(page.locator('.photo-field').nth(0).locator('img')).toHaveCount(1)

  await photoInputs.nth(1).setInputFiles({
    name: 'serial.jpg',
    mimeType: 'image/jpeg',
    buffer: Buffer.from('serial')
  })
  await expect(page.locator('.photo-field').nth(1).locator('img')).toHaveCount(1)
  await page.getByRole('button', { name: '下一步 →' }).click()
  await expect(page.getByText('已上传 2 张，打包照片为选填')).toBeVisible()

  await page.getByRole('button', { name: '确认提交 ✓' }).dblclick()
  await expect(page.getByRole('heading', { name: '退回信息已登记' })).toBeVisible()
  expect(submitCount).toBe(1)
})

test('terminal links hide order identity', async ({ page }) => {
  await mockReturnApi(page, { status: 'EXPIRED' })
  await page.goto('/return/expired-token')
  await expect(page.getByRole('heading', { name: '链接已过期' })).toBeVisible()
  await expect(page.getByText('ORDER-001')).toHaveCount(0)
})

test('theme and locale preferences remain responsive on mobile', async ({ page }) => {
  await mockReturnApi(page, { status: 'DRAFT' })
  await page.goto('/return/draft-token')
  await expect(page.getByRole('heading', { name: '确认本次退回订单' })).toBeVisible()
  await page.getByRole('button', { name: '切换主题' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('button', { name: 'English' }).click()
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - window.innerWidth
  )
  expect(overflow).toBeLessThanOrEqual(0)
})

interface MockOptions {
  status: 'DRAFT' | 'EXPIRED' | 'REVOKED' | 'ACCEPTED'
  onAuthorize?: (request: import('@playwright/test').Request) => Promise<unknown>
  onSubmit?: () => Promise<unknown>
  attachmentCategories?: Map<number, string>
}

async function mockReturnApi(page: Page, options: MockOptions) {
  await page.route('**/app-api/rental/return-registration/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    if (request.method() === 'GET') {
      await json(route, {
        status: options.status,
        formNo: options.status === 'DRAFT' ? 'RR202608010001' : null,
        orderNo: options.status === 'DRAFT' ? 'ORDER-001' : null,
        assignedDeviceCount: options.status === 'DRAFT' ? 1 : 0,
        expiresAt: '2026-08-08T17:00:00',
        receipt: options.status === 'ACCEPTED'
          ? { formNo: 'RR202608010001', status: 'ACCEPTED', waybillNo: 'SF1000000001' }
          : null
      })
      return
    }
    if (path.endsWith('/upload-authorizations') && options.onAuthorize) {
      await json(route, await options.onAuthorize(request))
      return
    }
    if (path.endsWith('/attachments/confirm')) {
      const body = request.postDataJSON() as { attachmentId: number }
      const category = options.attachmentCategories?.get(body.attachmentId)
      await json(route, {
        attachmentId: body.attachmentId,
        fileId: 100 + body.attachmentId,
        category,
        name: `${body.attachmentId}.jpg`,
        size: 8,
        previewUrl: `data:image/jpeg;base64,${Buffer.from('photo').toString('base64')}`
      })
      return
    }
    if (path.endsWith('/submit') && options.onSubmit) {
      await json(route, await options.onSubmit())
      return
    }
    await json(route, true)
  })
}

async function json(route: import('@playwright/test').Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, data })
  })
}
