'use strict';

function returnRegistrationMockBootstrap(scenarioMode) {
    const stateKey = `specnav:return-registration:${scenarioMode}`;
    const initialState = {
      submitCount: 0,
      submittedBody: null,
      unsafeWriteCount: 0,
      events: [],
      submitAttachments: null
    };
    const storedState = window.sessionStorage.getItem(stateKey);
    const state = storedState ? JSON.parse(storedState) : initialState;
    window.__specnavReturnState = state;

    const persist = () => {
      window.sessionStorage.setItem(stateKey, JSON.stringify(state));
    };
    const jsonResponse = (body) => new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
    const requestBody = async (input, init) => {
      if (typeof init?.body === 'string') return JSON.parse(init.body);
      if (input instanceof Request) {
        const text = await input.clone().text();
        return text ? JSON.parse(text) : {};
      }
      return {};
    };
    const nativeFetch = window.fetch.bind(window);

    window.fetch = async (input, init = {}) => {
      const rawUrl = typeof input === 'string' || input instanceof URL
        ? input.toString()
        : input.url;
      const url = new URL(rawUrl, window.location.origin);
      const method = (
        init.method
        || (input instanceof Request ? input.method : 'GET')
      ).toUpperCase();
      const pathname = url.pathname;
      const isReturnApi = pathname.includes(
        '/app-api/rental/return-registration/'
      );

      if (!isReturnApi) return nativeFetch(input, init);
      if (method === 'GET' && pathname.endsWith('/session')) {
        return jsonResponse({
          code: 1,
          msg: '退回登记链接不可用',
          data: null
        });
      }

      const body = await requestBody(input, init);
      if (method === 'POST' && pathname.endsWith('/simple-submit')) {
        state.submitCount += 1;
        state.submittedBody = body;
        persist();

        if (scenarioMode === 'success') {
          await new Promise((resolve) => setTimeout(resolve, 120));
          return jsonResponse({
            code: 0,
            data: {
              formNo: 'RR202608300001',
              status: 'ACCEPTED',
              waybillNo: 'SF1000000001',
              deliveryId: 80,
              submittedAt: '2026-08-30T10:00:00'
            }
          });
        }
        if (scenarioMode === 'review') {
          if (body.machineCode === 'UNKNOWN-01') {
            return jsonResponse({
              code: 0,
              data: {
                formNo: 'RR202608300002',
                status: 'REVIEW_REQUIRED',
                waybillNo: body.waybillNo,
                submittedAt: '2026-08-30T10:05:00'
              }
            });
          }
          return jsonResponse({
            code: 1,
            msg: '订单信息或机器编码不匹配',
            data: null
          });
        }

        state.events.push('submit');
        state.submitAttachments = body.attachmentIds;
        persist();
        return jsonResponse({
          code: 0,
          data: {
            formNo: 'RR202608300003',
            status: 'ACCEPTED',
            waybillNo: 'SF1000000004',
            deliveryId: 81,
            submittedAt: '2026-08-30T10:10:00'
          }
        });
      }

      if (scenarioMode === 'upload') {
        if (method === 'POST' && pathname.endsWith('/verify')) {
          state.events.push('verify');
          persist();
          return jsonResponse({
            code: 0,
            data: {
              status: 'DRAFT',
              formNo: 'RR202608300003',
              orderNo: 'ORDER-001',
              assignedDeviceCount: 1,
              expiresAt: '2026-08-31T10:00:00'
            }
          });
        }
        if (method === 'POST' && pathname.endsWith('/upload-authorizations')) {
          state.events.push('authorize');
          persist();
          return jsonResponse({
            code: 0,
            data: {
              attachmentId: 1,
              uploadUrl: 'https://storage.test/upload/1',
              contentType: 'image/jpeg'
            }
          });
        }
        if (method === 'POST' && pathname.endsWith('/attachments/confirm')) {
          state.events.push('confirm');
          persist();
          return jsonResponse({
            code: 0,
            data: {
              attachmentId: 1,
              fileId: 101,
              category: 'RETURN_PHOTO',
              name: 'return.jpg',
              size: 12,
              previewUrl: 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=='
            }
          });
        }
      }

      if (
        pathname.includes('/device')
        || pathname.includes('/assignment')
        || pathname.includes('/delivery')
      ) {
        state.unsafeWriteCount += 1;
        persist();
      }
      return jsonResponse({ code: 0, data: true });
    };

    if (scenarioMode === 'upload') {
      window.XMLHttpRequest = class SpecNavUploadRequest {
        constructor() {
          this.method = '';
          this.url = '';
          this.status = 0;
          this.readyState = 0;
          this.onload = null;
          this.onerror = null;
          this.upload = { onprogress: null };
        }

        open(method, url) {
          this.method = method;
          this.url = new URL(url, window.location.origin).toString();
          this.readyState = 1;
        }

        setRequestHeader() {}

        send(body) {
          if (
            this.method.toUpperCase() !== 'PUT'
            || this.url !== 'https://storage.test/upload/1'
            || !(body instanceof Blob)
          ) {
            this.status = 500;
            this.readyState = 4;
            queueMicrotask(() => this.onerror?.(new Event('error')));
            return;
          }
          state.events.push('put');
          persist();
          setTimeout(() => {
            this.upload.onprogress?.({
              lengthComputable: true,
              loaded: body.size,
              total: body.size
            });
            this.status = 200;
            this.readyState = 4;
            this.onload?.(new Event('load'));
          }, 0);
        }
      };
    }
}

function createReturnRegistrationMockScript(mode) {
  return `(${returnRegistrationMockBootstrap.toString()})(${JSON.stringify(mode)});`;
}

const scenarios = {
  'return-success-idempotent': {
    scenario_data: {
      start_url: 'http://127.0.0.1:4190/return',
      mock_script: createReturnRegistrationMockScript('success')
    },
    scenario: async function ({ page, assertion, data }) {
      await page.addInitScript({ content: data.mock_script });

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
      const state = await page.evaluate(() => window.__specnavReturnState);
      assertion.equal('vc01-submit-count', state.submitCount, 1);
      assertion.equal(
        'vc01-normalized-machine-code',
        state.submittedBody && state.submittedBody.machineCode,
        'P4-01'
      );
    }
  },

  'return-review-and-security': {
    scenario_data: {
      start_url: 'http://127.0.0.1:4190/return',
      mock_script: createReturnRegistrationMockScript('review')
    },
    scenario: async function ({ page, assertion, data }) {
      await page.addInitScript({ content: data.mock_script });

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
      const state = await page.evaluate(() => window.__specnavReturnState);
      assertion.equal(
        'vc02-no-authoritative-side-effect',
        state.unsafeWriteCount,
        0
      );
      assertion.equal('vc02-submit-count', state.submitCount, 2);
    }
  },

  'return-private-upload': {
    scenario_data: {
      start_url: 'http://127.0.0.1:4190/return',
      mock_script: createReturnRegistrationMockScript('upload')
    },
    scenario: async function ({ page, assertion, data }) {
      await page.addInitScript({ content: data.mock_script });

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
      const state = await page.evaluate(() => window.__specnavReturnState);

      assertion.equal(
        'vc03-private-upload-order',
        state.events,
        ['verify', 'authorize', 'put', 'confirm', 'submit']
      );
      assertion.equal(
        'vc03-confirmed-file-bound',
        state.submitAttachments,
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
