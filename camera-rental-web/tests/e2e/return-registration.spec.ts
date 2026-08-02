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

  await page.goto('/return')
  await page.getByPlaceholder('请输入完整闲鱼订单号').fill('ORDER-001')
  await page.getByPlaceholder('4 位数字').fill('8000')
  await page.getByRole('button', { name: '验证并继续' }).click()
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

test('failed verification uses one enumeration-resistant error', async ({ page }) => {
  await mockReturnApi(page, { status: 'DRAFT', verifyFailure: true })
  await page.goto('/return')
  await page.getByPlaceholder('请输入完整闲鱼订单号').fill('ORDER-404')
  await page.getByPlaceholder('4 位数字').fill('9999')
  await page.getByRole('button', { name: '验证并继续' }).click()
  await expect(page.getByText('订单号或手机号后四位不匹配')).toBeVisible()
  await expect(page.getByText('ORDER-001')).toHaveCount(0)
})

test('theme and locale preferences remain responsive on mobile', async ({ page }) => {
  await mockReturnApi(page, { status: 'DRAFT', initialSession: true })
  await page.goto('/return')
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

test('historical token routes redirect to the fixed public entry', async ({ page }) => {
  await mockReturnApi(page, { status: 'DRAFT' })
  await page.goto('/return/legacy-token')
  await expect(page).toHaveURL(/\/return$/)
  await expect(page.getByRole('heading', { name: '验证您的租赁订单' })).toBeVisible()
})

interface MockOptions {
  status: 'DRAFT' | 'EXPIRED' | 'REVOKED' | 'ACCEPTED'
  initialSession?: boolean
  verifyFailure?: boolean
  onAuthorize?: (request: import('@playwright/test').Request) => Promise<unknown>
  onSubmit?: () => Promise<unknown>
  attachmentCategories?: Map<number, string>
}

async function mockReturnApi(page: Page, options: MockOptions) {
  let sessionActive = options.initialSession ?? false
  await page.route('**/app-api/rental/return-registration/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    if (request.method() === 'GET' && path.endsWith('/session')) {
      if (!sessionActive) {
        await apiError(route, '退回登记链接不可用')
        return
      }
      await json(route, context(options.status))
      return
    }
    if (request.method() === 'POST' && path.endsWith('/verify')) {
      if (options.verifyFailure) {
        await apiError(route, '订单号或手机号后四位不匹配')
        return
      }
      sessionActive = true
      await json(route, context(options.status))
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

function context(status: MockOptions['status']) {
  return {
    status,
    formNo: status === 'DRAFT' ? 'RR202608010001' : null,
    orderNo: status === 'DRAFT' ? 'ORDER-001' : null,
    assignedDeviceCount: status === 'DRAFT' ? 1 : 0,
    expiresAt: '2026-08-08T17:00:00',
    receipt: status === 'ACCEPTED'
      ? { formNo: 'RR202608010001', status: 'ACCEPTED', waybillNo: 'SF1000000001' }
      : null
  }
}

async function json(route: import('@playwright/test').Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, data })
  })
}

async function apiError(route: import('@playwright/test').Route, message: string) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 1, msg: message, data: null })
  })
}
