export const returnSerialPattern = /^[A-Z0-9]{1,8}(?:-[A-Z0-9]{1,8}){1,4}$/

export function normalizeReturnSerial(value: string) {
  return value.toUpperCase().replace(/[‐‑‒–—―−﹘﹣－]/g, '-').replace(/\s+/g, '')
}
