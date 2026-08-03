import { expect, test, type Page, type Request, type Route } from '@playwright/test'

test('mobile customer submits four fields without photo setup requests', async ({ page }) => {
  let verifyCount = 0
  let authorizeCount = 0
  let submitCount = 0
  await mockReturnApi(page, {
    onVerify: () => { verifyCount += 1 },
    onAuthorize: () => {
      authorizeCount += 1
      return uploadAuthorization(1)
    },
    onSubmit: async (request) => {
      submitCount += 1
      expect(request.postDataJSON()).toMatchObject({ attachmentIds: [] })
      await new Promise((resolve) => setTimeout(resolve, 100))
      return acceptedReceipt()
    }
  })

  await page.goto('/return')
  await expect(page.getByRole('heading', { name: '一次填完，直接登记退回。' })).toBeVisible()
  await fillRequiredFields(page)
  await page.getByRole('button', { name: '提交退回信息' }).dblclick()

  await expect(page.getByRole('heading', { name: '退回信息已登记' })).toBeVisible()
  expect(verifyCount).toBe(0)
  expect(authorizeCount).toBe(0)
  expect(submitCount).toBe(1)
})

test('selected photo uploads through verify, RustFS confirm and simple submit', async ({ page }) => {
  const events: string[] = []
  await mockReturnApi(page, {
    onVerify: (request) => {
      events.push('verify')
      expect(request.postDataJSON()).toMatchObject({
        orderNo: '',
        mobileLast4: '',
        machineCode: 'P4-01'
      })
    },
    onAuthorize: (request) => {
      events.push('authorize')
      expect(request.postDataJSON()).toMatchObject({ category: 'RETURN_PHOTO' })
      return uploadAuthorization(1)
    },
    onConfirm: () => {
      events.push('confirm')
      return confirmedPhoto(1)
    },
    onSubmit: (request) => {
      events.push('submit')
      expect(request.postDataJSON()).toMatchObject({ attachmentIds: [1] })
      return acceptedReceipt()
    }
  })
  await page.route('https://storage.test/upload/1', async (route) => {
    events.push('put')
    await route.fulfill({ status: 200, body: '' })
  })

  await page.goto('/return')
  await fillRequiredFields(page)
  await page.locator('input[type="file"]').setInputFiles(photoFile('return.jpg'))
  await expect(page.getByText('已选择 1/10')).toBeVisible()
  await page.getByRole('button', { name: '提交退回信息' }).click()

  await expect(page.getByRole('heading', { name: '退回信息已登记' })).toBeVisible()
  expect(events).toEqual(['verify', 'authorize', 'put', 'confirm', 'submit'])
})

test('failed RustFS upload can be retried before submission', async ({ page }) => {
  let uploadAttempts = 0
  let submitCount = 0
  await mockReturnApi(page, {
    onAuthorize: () => uploadAuthorization(uploadAttempts + 1),
    onConfirm: (request) => confirmedPhoto(
      (request.postDataJSON() as { attachmentId: number }).attachmentId
    ),
    onSubmit: () => {
      submitCount += 1
      return acceptedReceipt()
    }
  })
  await page.route('https://storage.test/upload/*', async (route) => {
    uploadAttempts += 1
    await route.fulfill({ status: uploadAttempts === 1 ? 500 : 200, body: '' })
  })

  await page.goto('/return')
  await fillRequiredFields(page)
  await page.locator('input[type="file"]').setInputFiles(photoFile('retry.jpg'))
  await page.getByRole('button', { name: '提交退回信息' }).click()

  await expect(page.getByRole('button', { name: '重试' })).toBeVisible()
  expect(submitCount).toBe(0)
  await page.getByRole('button', { name: '重试' }).click()
  await expect(page.getByText('上传完成')).toBeVisible()
  await page.getByRole('button', { name: '提交退回信息' }).click()
  await expect(page.getByRole('heading', { name: '退回信息已登记' })).toBeVisible()
  expect(uploadAttempts).toBe(2)
  expect(submitCount).toBe(1)
})

test('photo picker rejects a selection above ten files', async ({ page }) => {
  await mockReturnApi(page, {})
  await page.goto('/return')

  await page.locator('input[type="file"]').setInputFiles(
    Array.from({ length: 11 }, (_, index) => photoFile(`${index + 1}.jpg`))
  )

  await expect(page.getByText('照片最多选择 10 张')).toBeVisible()
  await expect(page.locator('.return-photo-grid figure')).toHaveCount(0)
})

test('order and mobile can both stay empty while machine and waybill remain required', async ({ page }) => {
  await mockReturnApi(page, {})
  await page.goto('/return')

  await page.getByRole('button', { name: '提交退回信息' }).click()
  await expect(page.getByText('请填写正确的机器编码，例如 P4-01')).toBeVisible()

  await page.getByPlaceholder('例如 P4-01').fill('P4-01')
  await page.getByRole('button', { name: '提交退回信息' }).click()
  await expect(page.getByText('请填写快递单号')).toBeVisible()
})

test('failed single submission shows the server verification error', async ({ page }) => {
  await mockReturnApi(page, { submitFailure: true })
  await page.goto('/return')
  await page.getByPlaceholder('多笔订单时填写完整订单号').fill('ORDER-MISSING')
  await page.getByPlaceholder('例如 P4-01').fill('P4-01')
  await page.getByPlaceholder('输入或粘贴物流单号').fill('SF1000000001')
  await page.getByRole('button', { name: '提交退回信息' }).click()

  await expect(page.getByText('订单信息或机器编码不匹配')).toBeVisible()
})

test('theme and locale preferences remain responsive on mobile', async ({ page }) => {
  await mockReturnApi(page, {})
  await page.addInitScript(() => {
    localStorage.setItem('return-theme', 'light')
    localStorage.setItem('return-locale', 'zh-CN')
  })
  await page.goto('/return')
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await page.getByRole('button', { name: '切换主题' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('button', { name: 'English' }).click()
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect(page.getByRole('heading', { name: 'Register the return in one step.' })).toBeVisible()
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - window.innerWidth
  )
  expect(overflow).toBeLessThanOrEqual(0)
})

test('historical token routes redirect to the fixed public entry', async ({ page }) => {
  await mockReturnApi(page, {})
  await page.goto('/return/legacy-token')
  await expect(page).toHaveURL(/\/return$/)
  await expect(page.getByRole('heading', { name: '一次填完，直接登记退回。' })).toBeVisible()
})

interface MockOptions {
  initialStatus?: 'ACCEPTED' | 'REVIEW_REQUIRED'
  submitFailure?: boolean
  onVerify?: (request: Request) => void
  onAuthorize?: (request: Request) => unknown
  onConfirm?: (request: Request) => unknown
  onSubmit?: (request: Request) => unknown | Promise<unknown>
}

async function mockReturnApi(page: Page, options: MockOptions) {
  await page.route('**/app-api/rental/return-registration/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    if (request.method() === 'GET' && path.endsWith('/session')) {
      if (!options.initialStatus) {
        await apiError(route, '退回登记链接不可用')
        return
      }
      await json(route, context(options.initialStatus))
      return
    }
    if (request.method() === 'POST' && path.endsWith('/verify')) {
      options.onVerify?.(request)
      await json(route, draftContext())
      return
    }
    if (request.method() === 'POST' && path.endsWith('/upload-authorizations')) {
      await json(route, options.onAuthorize?.(request) || uploadAuthorization(1))
      return
    }
    if (request.method() === 'POST' && path.endsWith('/attachments/confirm')) {
      await json(route, options.onConfirm?.(request) || confirmedPhoto(1))
      return
    }
    if (request.method() === 'DELETE' && path.includes('/attachments/')) {
      await json(route, true)
      return
    }
    if (request.method() === 'POST' && path.endsWith('/simple-submit')) {
      if (options.submitFailure) {
        await apiError(route, '订单信息或机器编码不匹配')
        return
      }
      await json(route, options.onSubmit
        ? await options.onSubmit(request)
        : {
            formNo: 'RR202608020001',
            status: 'REVIEW_REQUIRED',
            waybillNo: 'SF1000000001'
          })
      return
    }
    await json(route, true)
  })
}

async function fillRequiredFields(page: Page) {
  await page.getByPlaceholder('例如 P4-01').fill('p4 － 01')
  await page.getByPlaceholder('输入或粘贴物流单号').fill('SF1000000001')
}

function photoFile(name: string) {
  return {
    name,
    mimeType: 'image/jpeg',
    buffer: Buffer.from(`photo-${name}`)
  }
}

function uploadAuthorization(attachmentId: number) {
  return {
    attachmentId,
    uploadUrl: `https://storage.test/upload/${attachmentId}`,
    contentType: 'image/jpeg'
  }
}

function confirmedPhoto(attachmentId: number) {
  return {
    attachmentId,
    fileId: 100 + attachmentId,
    category: 'RETURN_PHOTO',
    name: `${attachmentId}.jpg`,
    size: 12,
    previewUrl: `https://storage.test/preview/${attachmentId}`
  }
}

function acceptedReceipt() {
  return {
    formNo: 'RR202608020001',
    status: 'ACCEPTED',
    waybillNo: 'SF1000000001',
    deliveryId: 80,
    submittedAt: '2026-08-03T10:00:00'
  }
}

function draftContext() {
  return {
    status: 'DRAFT',
    formNo: 'RR202608020001',
    orderNo: 'ORDER-001',
    assignedDeviceCount: 1,
    expiresAt: '2026-08-04T10:00:00'
  }
}

function context(status: MockOptions['initialStatus']) {
  return {
    ...draftContext(),
    status,
    receipt: {
      ...acceptedReceipt(),
      status
    }
  }
}

async function json(route: Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, data })
  })
}

async function apiError(route: Route, message: string) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 1, msg: message, data: null })
  })
}
