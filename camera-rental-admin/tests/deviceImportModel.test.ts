import assert from 'node:assert/strict'
import test from 'node:test'
import { decodeDeviceFile, parseDeviceText } from '../src/views/rental/device/deviceImportModel.ts'

test('preserves leading zeros and maps reversed English headers', () => {
  assert.deepEqual(parseDeviceText('serial_number,device_no\n00001234,A5-01').rows, [
    { lineNumber: 2, deviceNo: 'A5-01', serialNumber: '00001234' }
  ])
})
test('supports tabs, Chinese comma, whitespace and optional empty serial', () => {
  for (const separator of ['\t', '，', ' ']) {
    assert.equal(parseDeviceText(`A5-01${separator}DEMO1234`).rows[0].serialNumber, 'DEMO1234')
  }
  assert.equal(parseDeviceText('设备编号,序列号\nA5-01,').rows[0].serialNumber, '')
})
test('single column interpretation is explicit, not a number heuristic', () => {
  assert.equal(parseDeviceText('00123456').rows[0].serialNumber, '00123456')
  assert.equal(parseDeviceText('A5-01', 'device').rows[0].deviceNo, 'A5-01')
  assert.equal(parseDeviceText('设备编号\nA5-01', 'serial').rows[0].deviceNo, 'A5-01')
})
test('does not silently accept malformed rows or duplicate/unknown headers', () => {
  assert.equal(parseDeviceText('设备编号,型号\nA5-01,A5').issues[0].code, 'HEADER')
  assert.equal(parseDeviceText('设备编号,设备编号\nA5-01,A5-02').issues[0].code, 'HEADER')
  assert.equal(parseDeviceText('A5-01,"oops').issues[0].code, 'COLUMNS')
  assert.equal(parseDeviceText('A5-01,"foo"extra').issues[0].code, 'COLUMNS')
  assert.equal(parseDeviceText('A5-01,SN1234,extra').issues[0].lineNumber, 1)
})
test('handles BOM, blank lines, CRLF and quoted CSV without losing source line', () => {
  assert.deepEqual(parseDeviceText('\ufeff设备编号,序列号\r\n\r\n"A5-01","00001234"').rows, [
    { lineNumber: 3, deviceNo: 'A5-01', serialNumber: '00001234' }
  ])
})
test('rejects empty/oversized batches and binary files', () => {
  assert.equal(parseDeviceText(' \n').issues[0].code, 'EMPTY')
  assert.equal(
    parseDeviceText(Array.from({ length: 201 }, (_, i) => `DEMO${i}`).join('\n')).issues[0].code,
    'LIMIT'
  )
  assert.throws(() => decodeDeviceFile(new Uint8Array([0, 1, 2]).buffer))
})
test('decodes UTF8 and BOM-marked UTF16LE', () => {
  const text = '设备编号,序列号\nA5-01,00001234'
  assert.equal(decodeDeviceFile(new TextEncoder().encode(text).buffer), text)
  const bytes = Buffer.concat([Buffer.from([0xff, 0xfe]), Buffer.from(text, 'utf16le')])
  assert.equal(
    decodeDeviceFile(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)),
    text
  )
})
