import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildDeviceNoPreview,
  findModel,
  getModelsForCategory,
  isModelInCategory,
  normalizeDeviceNoSuffix
} from '../src/views/rental/device/deviceCatalogModel.ts'

const catalog = [
  {
    id: 1,
    categoryCode: 'DJI',
    categoryName: '大疆',
    models: [
      { id: 11, modelCode: '360', modelName: '360', deviceNoPrefix: '360' },
      { id: 12, modelCode: 'NANO', modelName: 'NANO', deviceNoPrefix: 'NANO' },
      { id: 13, modelCode: 'P4P', modelName: 'P4P', deviceNoPrefix: 'P4P' }
    ]
  },
  {
    id: 2,
    categoryCode: 'INSTA360',
    categoryName: '影石',
    models: [{ id: 21, modelCode: 'ACE', modelName: 'ACE', deviceNoPrefix: 'ACE' }]
  },
  {
    id: 3,
    categoryCode: 'STAND',
    categoryName: '支架',
    models: [{ id: 31, modelCode: '支架', modelName: '支架', deviceNoPrefix: '支架' }]
  }
]

test('category selection exposes only backend-provided models', () => {
  assert.deepEqual(
    getModelsForCategory(catalog, 'DJI').map((model) => model.modelCode),
    ['360', 'NANO', 'P4P']
  )
  assert.deepEqual(
    getModelsForCategory(catalog, 'STAND').map((model) => model.modelCode),
    ['支架']
  )
  assert.deepEqual(getModelsForCategory(catalog, 'UNKNOWN'), [])
})

test('model membership follows the selected category', () => {
  assert.equal(isModelInCategory(catalog, 'DJI', 'P4P'), true)
  assert.equal(isModelInCategory(catalog, 'INSTA360', 'P4P'), false)
})

test('device number preview combines the backend prefix and administrator suffix', () => {
  assert.equal(findModel(catalog, 'DJI', 'P4P')?.deviceNoPrefix, 'P4P')
  assert.equal(normalizeDeviceNoSuffix('1'), '01')
  assert.equal(normalizeDeviceNoSuffix('99'), '99')
  assert.equal(normalizeDeviceNoSuffix('100'), '100')
  assert.equal(normalizeDeviceNoSuffix('999'), '999')
  assert.equal(normalizeDeviceNoSuffix('0'), '')
  assert.equal(normalizeDeviceNoSuffix('001'), '')
  assert.equal(normalizeDeviceNoSuffix('1000'), '')
  assert.equal(buildDeviceNoPreview('P4P', '2'), 'P4P-02')
  assert.equal(buildDeviceNoPreview('P4P', '100'), 'P4P-100')
  assert.equal(buildDeviceNoPreview('支架', '08'), '支架-08')
})
