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

  test('requires a model prefix and canonical 01-999 sequence', () => {
    expect(returnSerialPattern.test('P4P-01')).toBe(true)
    expect(returnSerialPattern.test('DJI-P4P-09')).toBe(true)
    expect(returnSerialPattern.test('P4P-100')).toBe(true)
    expect(returnSerialPattern.test('支架-999')).toBe(true)
    expect(returnSerialPattern.test('P4-1')).toBe(false)
    expect(returnSerialPattern.test('P4-001')).toBe(false)
    expect(returnSerialPattern.test('P4-1000')).toBe(false)
    expect(returnSerialPattern.test('A6-08-4L5H')).toBe(false)
  })

  test('accepts every current return model prefix and the explicit stand code', () => {
    const prefixes = [
      '360', 'NANO', 'A5', 'A6', 'P3', 'P4', 'P4P',
      'ACE', 'X5', 'GT', 'G3',
      'X300P', 'X200U', 'X300U',
      'XT5', 'XT50', 'XS20', 'X100VI',
      'R50', 'G12', 'G7X2',
      'GR3X', 'GR4',
      '支架'
    ]

    for (const prefix of prefixes) {
      expect(returnSerialPattern.test(`${prefix}-01`)).toBe(true)
    }
    expect(returnSerialPattern.test('相机-01')).toBe(false)
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

  test('describes the required sender and return-method-specific fields', () => {
    expect(returnMessage('zh-CN', 'simpleReturnBody')).toContain('订单号可不填')
    expect(returnMessage('zh-CN', 'simpleReturnBody')).toContain('手机号机器编码必填')
    expect(returnMessage('zh-CN', 'simpleReturnBody')).toContain('快递单号')
    expect(returnMessage('zh-CN', 'simpleReturnBody')).toContain('跑腿平台名称')
    expect(returnMessage('zh-CN', 'simpleReturnBody')).toContain('本人送回')
    expect(returnMessage('zh-CN', 'selfDeliveryHint')).toContain('本人送回！')
  })

  test('labels all three return methods in both locales', () => {
    expect(returnMessage('zh-CN', 'returnMethodExpress')).toBe('快递寄回')
    expect(returnMessage('zh-CN', 'returnMethodSelf')).toBe('本人送回')
    expect(returnMessage('zh-CN', 'returnMethodErrand')).toBe('跑腿送回')
    expect(returnMessage('en', 'returnMethodExpress')).toBe('Express')
    expect(returnMessage('en', 'returnMethodSelf')).toBe('Self drop-off')
    expect(returnMessage('en', 'returnMethodErrand')).toBe('Errand courier')
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
