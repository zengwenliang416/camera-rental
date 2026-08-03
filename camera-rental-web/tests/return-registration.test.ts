import { describe, expect, test } from 'bun:test'
import { returnMessage } from '../app/composables/returnMessages'
import { normalizeReturnSerial, returnSerialPattern } from '../app/utils/returnSerial'
import { hasRequiredReturnPhotos } from '../app/utils/returnValidation'

describe('customer return registration utilities', () => {
  test('normalizes the physical short serial format', () => {
    const value = normalizeReturnSerial(' p4 － 01 ')
    expect(value).toBe('P4-01')
    expect(returnSerialPattern.test(value)).toBe(true)
  })

  test('requires a model prefix and two-digit sequence', () => {
    expect(returnSerialPattern.test('P4P-01')).toBe(true)
    expect(returnSerialPattern.test('DJI-P4P-09')).toBe(true)
    expect(returnSerialPattern.test('P4-1')).toBe(false)
    expect(returnSerialPattern.test('P4-001')).toBe(false)
    expect(returnSerialPattern.test('A6-08-4L5H')).toBe(false)
  })

  test('allows submission without an optional packaging photo', () => {
    expect(hasRequiredReturnPhotos([
      photo(1, 'DEVICE_EXTERIOR'),
      photo(2, 'SERIAL_LABEL')
    ])).toBe(true)
  })

  test('provides both supported locales', () => {
    expect(returnMessage('zh-CN', 'submit')).toContain('确认')
    expect(returnMessage('en', 'submit')).toContain('Confirm')
  })

  test('explains that an unregistered machine code is saved for manual review', () => {
    expect(returnMessage('zh-CN', 'simpleReturnBody')).toContain('无需预先入库')
    expect(returnMessage('zh-CN', 'simpleReturnBody')).toContain('人工复核')
  })
})

function photo(id: number, category: 'DEVICE_EXTERIOR' | 'SERIAL_LABEL') {
  return {
    attachmentId: id,
    fileId: id,
    category,
    name: `${id}.jpg`,
    size: 1,
    previewUrl: `/preview/${id}`
  }
}
