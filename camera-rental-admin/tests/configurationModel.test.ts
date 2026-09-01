import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildImpactPreviewKey,
  buildProductRuleSaveRequest,
  buildRuleScopeKey,
  hasMissingSkuMappings,
  isConfigurationVersionConflict,
  isImpactPreviewFresh,
  isTerminalReconciliationStatus,
  normalizeExternalIdentifier,
  normalizeHandlingPolicy,
  recoverConfigurationVersionConflict,
  type RentalChannelProductRuleDraft
} from '../src/views/rental/configuration/configurationModel.ts'

const longItemId = '5127394070109009316'

const createDraft = (
  overrides: Partial<RentalChannelProductRuleDraft> = {}
): RentalChannelProductRuleDraft => ({
  shopId: 7,
  xianyuItemId: longItemId,
  handlingPolicy: 'CREATE_RENTAL',
  mappingMode: 'SINGLE',
  singleDeviceModelId: 31,
  enabled: true,
  ruleNote: '',
  skuMappings: [],
  synchronizedProductSkuIds: [],
  ...overrides
})

test('external identifiers remain trimmed strings without numeric conversion', () => {
  const normalized = normalizeExternalIdentifier(` ${longItemId} `)
  assert.equal(normalized, longItemId)
  assert.equal(typeof normalized, 'string')
  assert.notEqual(normalized, String(Number(longItemId)))
})

test('single-model request excludes SKU mappings and preserves lock version', () => {
  const request = buildProductRuleSaveRequest(
    createDraft({ id: 10, lockVersion: 4, ruleNote: ' exact ' })
  )
  assert.equal(request.xianyuItemId, longItemId)
  assert.equal(request.mappingMode, 'SINGLE')
  assert.equal(request.singleDeviceModelId, 31)
  assert.deepEqual(request.skuMappings, [])
  assert.equal(request.lockVersion, 4)
  assert.equal(request.ruleNote, 'exact')
})

test('multi-SKU request contains only synchronized rows with a selected model', () => {
  const request = buildProductRuleSaveRequest(
    createDraft({
      mappingMode: 'MULTI',
      singleDeviceModelId: undefined,
      skuMappings: [
        {
          productSkuId: 81,
          xgjSkuId: '537044127563786',
          xianyuSkuId: '991122334455',
          deviceModelId: 41,
          mappingEnabled: true
        },
        {
          productSkuId: 82,
          xgjSkuId: '537044127563787',
          deviceModelId: undefined,
          mappingEnabled: true
        }
      ],
      synchronizedProductSkuIds: [81, 82]
    })
  )
  assert.equal(request.mappingMode, 'MULTI')
  assert.deepEqual(request.skuMappings, [{ productSkuId: 81, deviceModelId: 41, enabled: true }])
})

test('multi-SKU request rejects forged and cross-product SKU rows', () => {
  const request = buildProductRuleSaveRequest(
    createDraft({
      mappingMode: 'MULTI',
      singleDeviceModelId: undefined,
      skuMappings: [
        {
          productSkuId: 81,
          xgjSkuId: 'owned-sku',
          deviceModelId: 41,
          mappingEnabled: true
        },
        {
          productSkuId: 999,
          xgjSkuId: 'forged-sku',
          deviceModelId: 42,
          mappingEnabled: true
        }
      ],
      synchronizedProductSkuIds: [81]
    })
  )
  assert.deepEqual(request.skuMappings, [{ productSkuId: 81, deviceModelId: 41, enabled: true }])
})

test('updates fail closed when the backend version token is missing', () => {
  assert.throws(
    () => buildProductRuleSaveRequest(createDraft({ id: 10, lockVersion: undefined })),
    /lockVersion is required/
  )
})

test('CONFIG_SKIPPED clears model configuration deterministically', () => {
  const request = buildProductRuleSaveRequest(
    createDraft({ handlingPolicy: normalizeHandlingPolicy('CONFIG_SKIPPED') })
  )
  assert.equal(request.handlingPolicy, 'CONFIG_SKIPPED')
  assert.equal(request.mappingMode, 'NONE')
  assert.equal(request.singleDeviceModelId, undefined)
  assert.deepEqual(request.skuMappings, [])
})

test('impact preview becomes stale after any persisted field changes', () => {
  const draft = createDraft({ id: 10, lockVersion: 2 })
  const key = buildImpactPreviewKey(draft)
  assert.equal(isImpactPreviewFresh(draft, key), true)
  assert.equal(isImpactPreviewFresh({ ...draft, ruleNote: 'changed' }, key), false)
  assert.equal(isImpactPreviewFresh({ ...draft, lockVersion: 3 }, key), false)
})

test('missing SKU mapping includes empty synchronized lists and unmapped rows', () => {
  assert.equal(hasMissingSkuMappings([]), true)
  assert.equal(
    hasMissingSkuMappings([
      { productSkuId: 1, xgjSkuId: 'sku-1', deviceModelId: 11, mappingEnabled: true },
      { productSkuId: 2, xgjSkuId: 'sku-2' }
    ]),
    true
  )
  assert.equal(
    hasMissingSkuMappings([
      { productSkuId: 1, xgjSkuId: 'sku-1', deviceModelId: 11, mappingEnabled: true }
    ]),
    false
  )
})

test('scope keys preserve exact identifiers and separate shops and products', () => {
  assert.equal(buildRuleScopeKey(7, ` ${longItemId} `), `7:${longItemId}`)
  assert.notEqual(buildRuleScopeKey(7, longItemId), buildRuleScopeKey(8, longItemId))
  assert.notEqual(buildRuleScopeKey(7, longItemId), buildRuleScopeKey(7, 'other-item'))
  assert.equal(buildRuleScopeKey(undefined, longItemId), '')
})

test('reconciliation terminal states and optimistic-lock conflicts are explicit', () => {
  for (const status of ['SUCCEEDED', 'COMPLETED_WITH_ERRORS', 'FAILED']) {
    assert.equal(isTerminalReconciliationStatus(status), true)
  }
  for (const status of [undefined, 'PENDING', 'RUNNING']) {
    assert.equal(isTerminalReconciliationStatus(status), false)
  }
  assert.equal(isConfigurationVersionConflict({ code: 1_040_002_026 }), true)
  assert.equal(
    isConfigurationVersionConflict({ response: { data: { code: 1_040_002_026 } } }),
    true
  )
  assert.equal(
    isConfigurationVersionConflict({ message: '租赁配置已被其他管理员修改，请刷新' }),
    true
  )
  assert.equal(
    isConfigurationVersionConflict(
      Object.assign(new Error('租赁配置已被其他管理员修改，请刷新后重试'), {
        code: 1_040_002_026
      })
    ),
    true
  )
  assert.equal(isConfigurationVersionConflict(new Error('network failed')), false)
})

test('optimistic-lock recovery closes stale editor before loading authoritative state', async () => {
  const transitions: string[] = []
  let editorOpen = true
  const recovered = await recoverConfigurationVersionConflict(
    Object.assign(new Error('租赁配置已被其他管理员修改，请刷新后重试'), {
      code: 1_040_002_026
    }),
    () => {
      editorOpen = false
      transitions.push('closed')
    },
    async () => {
      assert.equal(editorOpen, false)
      transitions.push('reloaded')
    }
  )

  assert.equal(recovered, true)
  assert.equal(editorOpen, false)
  assert.deepEqual(transitions, ['closed', 'reloaded'])
})
