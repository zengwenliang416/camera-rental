'use strict';

const scenarios = {
  'return-success-idempotent': {
    scenario_data: {
      start_url: 'http://127.0.0.1:4190/return'
    },
    scenario: async function ({ page, assertion, data }) {
      let submitCount = 0;
      let submittedBody = null;

      await page.route('**/app-api/rental/return-registration/**', async (route) => {
        const request = route.request();
        const pathname = new URL(request.url()).pathname;
        if (request.method() === 'GET' && pathname.endsWith('/session')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 1,
              msg: '退回登记链接不可用',
              data: null
            })
          });
          return;
        }
        if (request.method() === 'POST' && pathname.endsWith('/simple-submit')) {
          submitCount += 1;
          submittedBody = request.postDataJSON();
          await new Promise((resolve) => setTimeout(resolve, 120));
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 0,
              data: {
                formNo: 'RR202608300001',
                status: 'ACCEPTED',
                waybillNo: 'SF1000000001',
                deliveryId: 80,
                submittedAt: '2026-08-30T10:00:00'
              }
            })
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 0, data: true })
        });
      });

      await page.goto(data.start_url);
      const heading = page.getByRole('heading', {
        name: '一次填完，直接登记退回。'
      });
      await heading.waitFor({ state: 'visible' });
      assertion.equal(
        'vc01-fixed-entry-visible',
        await heading.isVisible(),
        true
      );

      await page.getByPlaceholder('例如 P4-01').fill('p4 － 01');
      await page
        .getByPlaceholder('输入或粘贴物流单号')
        .fill('SF1000000001');
      await page.getByRole('button', { name: '提交退回信息' }).dblclick();

      const success = page.getByRole('heading', { name: '退回信息已登记' });
      await success.waitFor({ state: 'visible' });
      assertion.equal(
        'vc01-receipt-accepted',
        await success.isVisible(),
        true
      );
      assertion.equal('vc01-submit-count', submitCount, 1);
      assertion.equal(
        'vc01-normalized-machine-code',
        submittedBody && submittedBody.machineCode,
        'P4-01'
      );
    }
  },

  'return-review-and-security': {
    scenario_data: {
      start_url: 'http://127.0.0.1:4190/return'
    },
    scenario: async function ({ page, assertion, data }) {
      let submitCount = 0;
      let unsafeWriteCount = 0;

      await page.route('**/app-api/rental/return-registration/**', async (route) => {
        const request = route.request();
        const pathname = new URL(request.url()).pathname;
        if (request.method() === 'GET' && pathname.endsWith('/session')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 1,
              msg: '退回登记链接不可用',
              data: null
            })
          });
          return;
        }
        if (request.method() === 'POST' && pathname.endsWith('/simple-submit')) {
          submitCount += 1;
          const body = request.postDataJSON();
          if (body.machineCode === 'UNKNOWN-01') {
            await route.fulfill({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                code: 0,
                data: {
                  formNo: 'RR202608300002',
                  status: 'REVIEW_REQUIRED',
                  waybillNo: body.waybillNo,
                  submittedAt: '2026-08-30T10:05:00'
                }
              })
            });
            return;
          }
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 1,
              msg: '订单信息或机器编码不匹配',
              data: null
            })
          });
          return;
        }
        if (
          pathname.includes('/device')
          || pathname.includes('/assignment')
          || pathname.includes('/delivery')
        ) {
          unsafeWriteCount += 1;
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 0, data: true })
        });
      });

      await page.goto(data.start_url);
      await page.getByPlaceholder('例如 P4-01').fill('UNKNOWN-01');
      await page
        .getByPlaceholder('输入或粘贴物流单号')
        .fill('SF1000000002');
      await page.getByRole('button', { name: '提交退回信息' }).click();
      const reviewHeading = page.getByRole('heading', {
        name: '登记已提交，等待人工核对'
      });
      await reviewHeading.waitFor({ state: 'visible' });
      assertion.equal(
        'vc02-review-required-visible',
        await reviewHeading.isVisible(),
        true
      );
      assertion.equal('vc02-no-authoritative-side-effect', unsafeWriteCount, 0);

      await page.goto(data.start_url);
      await page
        .getByPlaceholder('多笔订单时填写完整订单号')
        .fill('ORDER-MISSING');
      await page.getByPlaceholder('例如 P4-01').fill('P4-01');
      await page
        .getByPlaceholder('输入或粘贴物流单号')
        .fill('SF1000000003');
      await page.getByRole('button', { name: '提交退回信息' }).click();
      const unifiedError = page.getByText('订单信息或机器编码不匹配');
      await unifiedError.waitFor({ state: 'visible' });
      assertion.equal(
        'vc02-unified-error-visible',
        await unifiedError.isVisible(),
        true
      );
      assertion.equal('vc02-submit-count', submitCount, 2);
    }
  },

  'return-private-upload': {
    scenario_data: {
      start_url: 'http://127.0.0.1:4190/return'
    },
    scenario: async function ({ page, assertion, data }) {
      const events = [];
      let submitAttachments = null;

      await page.route('**/app-api/rental/return-registration/**', async (route) => {
        const request = route.request();
        const pathname = new URL(request.url()).pathname;
        if (request.method() === 'GET' && pathname.endsWith('/session')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 1,
              msg: '退回登记链接不可用',
              data: null
            })
          });
          return;
        }
        if (request.method() === 'POST' && pathname.endsWith('/verify')) {
          events.push('verify');
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 0,
              data: {
                status: 'DRAFT',
                formNo: 'RR202608300003',
                orderNo: 'ORDER-001',
                assignedDeviceCount: 1,
                expiresAt: '2026-08-31T10:00:00'
              }
            })
          });
          return;
        }
        if (
          request.method() === 'POST'
          && pathname.endsWith('/upload-authorizations')
        ) {
          events.push('authorize');
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 0,
              data: {
                attachmentId: 1,
                uploadUrl: 'https://storage.test/upload/1',
                contentType: 'image/jpeg'
              }
            })
          });
          return;
        }
        if (
          request.method() === 'POST'
          && pathname.endsWith('/attachments/confirm')
        ) {
          events.push('confirm');
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 0,
              data: {
                attachmentId: 1,
                fileId: 101,
                category: 'RETURN_PHOTO',
                name: 'return.jpg',
                size: 12,
                previewUrl: 'https://storage.test/preview/1'
              }
            })
          });
          return;
        }
        if (request.method() === 'POST' && pathname.endsWith('/simple-submit')) {
          events.push('submit');
          submitAttachments = request.postDataJSON().attachmentIds;
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 0,
              data: {
                formNo: 'RR202608300003',
                status: 'ACCEPTED',
                waybillNo: 'SF1000000004',
                deliveryId: 81,
                submittedAt: '2026-08-30T10:10:00'
              }
            })
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 0, data: true })
        });
      });
      await page.route('https://storage.test/upload/1', async (route) => {
        events.push('put');
        await route.fulfill({ status: 200, body: '' });
      });
      await page.route('https://storage.test/preview/1', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'image/jpeg',
          body: ''
        });
      });

      await page.goto(data.start_url);
      await page.getByPlaceholder('例如 P4-01').fill('P4-01');
      await page
        .getByPlaceholder('输入或粘贴物流单号')
        .fill('SF1000000004');
      await page.locator('input[type="file"]').evaluate((input) => {
        const transfer = new DataTransfer();
        transfer.items.add(new File(
          ['specnav-return-photo'],
          'return.jpg',
          { type: 'image/jpeg' }
        ));
        input.files = transfer.files;
        input.dispatchEvent(new Event('change', { bubbles: true }));
      });
      await page.getByRole('button', { name: '提交退回信息' }).click();
      const success = page.getByRole('heading', { name: '退回信息已登记' });
      await success.waitFor({ state: 'visible' });

      assertion.equal(
        'vc03-private-upload-order',
        events,
        ['verify', 'authorize', 'put', 'confirm', 'submit']
      );
      assertion.equal(
        'vc03-confirmed-file-bound',
        submitAttachments,
        [1]
      );
      assertion.equal(
        'vc03-upload-success-visible',
        await success.isVisible(),
        true
      );
    }
  }
};

module.exports = { scenarios };
