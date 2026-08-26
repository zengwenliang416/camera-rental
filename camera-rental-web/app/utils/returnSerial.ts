export const returnSerialPattern =
  /^(?=.{4,64}$)(?:[A-Z0-9]+(?:-[A-Z0-9]+)*|支架)-(?:0[1-9]|[1-9][0-9]|[1-9][0-9]{2})$/

export function normalizeReturnSerial(value: string) {
  return value.toUpperCase().replace(/[‐‑‒–—―−﹘﹣－]/g, '-').replace(/\s+/g, '')
}
