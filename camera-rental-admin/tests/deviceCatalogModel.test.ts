import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildDeviceNoHint,
  findModel,
  getModelsForCategory,
  isModelInCategory
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

test('device number hint uses the backend-provided prefix', () => {
  assert.equal(findModel(catalog, 'DJI', 'P4P')?.deviceNoPrefix, 'P4P')
  assert.equal(buildDeviceNoHint('P4P'), 'P4P-01 ~ P4P-99')
  assert.equal(buildDeviceNoHint('支架'), '支架-01 ~ 支架-99')
})
