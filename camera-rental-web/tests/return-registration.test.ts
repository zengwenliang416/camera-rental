import { describe, expect, test } from 'bun:test'
import { returnMessage } from '../app/composables/returnMessages'
import { normalizeReturnSerial, returnSerialPattern } from '../app/utils/returnSerial'
import { hasRequiredReturnPhotos } from '../app/utils/returnValidation'

describe('customer return registration utilities', () => {
  test('normalizes the physical short serial format', () => {
    const value = normalizeReturnSerial(' a6 － 08 — 4l5h ')
    expect(value).toBe('A6-08-4L5H')
    expect(returnSerialPattern.test(value)).toBe(true)
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
