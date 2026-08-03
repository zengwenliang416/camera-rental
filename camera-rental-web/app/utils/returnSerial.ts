export const returnSerialPattern = /^(?=.{4,64}$)[A-Z0-9]+(?:-[A-Z0-9]+)*-\d{2}$/

export function normalizeReturnSerial(value: string) {
  return value.toUpperCase().replace(/[‐‑‒–—―−﹘﹣－]/g, '-').replace(/\s+/g, '')
}
