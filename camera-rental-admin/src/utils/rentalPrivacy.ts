const MIN_VISIBLE_EDGE = 3

export const maskChannelIdentifier = (value?: string | number | null) => {
  const text = String(value ?? '').trim()
  if (!text) {
    return '-'
  }
  if (text.length <= MIN_VISIBLE_EDGE * 2) {
    return `${text.slice(0, 1)}***${text.slice(-1)}`
  }
  return `${text.slice(0, MIN_VISIBLE_EDGE)}***${text.slice(-MIN_VISIBLE_EDGE)}`
}

export const maskSensitiveText = (value?: string | null) => {
  if (!value) {
    return '-'
  }
  return value
    .replace(/(收货地址|寄回地址|发货地址|详细地址|地址)([:：\s]*)([^#，,;；\r\n]+)/g, '$1$2***')
    .replace(/(收件人|姓名|联系人)([:：\s]*)([^#，,;；\r\n]+)/g, '$1$2***')
    .replace(
      /(^|\D)(1[3-9]\d{9})(?=\D|$)/g,
      (_, prefix, text) => `${prefix}${text.slice(0, 3)}****${text.slice(-4)}`
    )
    .replace(
      /(^|\D)(\d{10,})(?=\D|$)/g,
      (_, prefix, text) => `${prefix}${text.slice(0, 3)}***${text.slice(-3)}`
    )
}
