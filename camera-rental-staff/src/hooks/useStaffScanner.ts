import { onHide, onShow, onUnload } from '@dcloudio/uni-app'
import { scanner, scannerConnected } from '@/services/scanner'
import type { ScanResult } from '@/services/scanner'

/** 仅向可见作业页投递；扫描只查询/填入，履约仍需人工确认。 */
export function useStaffScanner(onScan: (result: ScanResult) => void | Promise<void>) {
  let unsubscribe: (() => void) | undefined
  let active = false
  let busy = false
  let lastText = ''
  let lastTime = 0

  async function accept(result: ScanResult) {
    if (!active)
      return
    if (busy) {
      uni.showToast({ title: '正在处理上一条，请稍后重扫', icon: 'none' })
      return
    }
    if (result.text === lastText && result.receivedAt - lastTime < 700)
      return
    busy = true
    lastText = result.text
    lastTime = result.receivedAt
    try {
      await onScan(result)
    } catch (error) {
      lastText = ''
      uni.showToast({ title: error instanceof Error ? error.message : '扫码处理失败，请重试', icon: 'none' })
    } finally {
      busy = false
    }
  }

  onShow(() => {
    active = true
    lastText = ''
    unsubscribe?.()
    unsubscribe = scanner.subscribe((result) => {
      void accept(result)
    })
    void scanner.initialize()
  })
  function release() {
    active = false
    unsubscribe?.()
    unsubscribe = undefined
    scanner.stopDecode()
  }
  onHide(release)
  onUnload(release)

  async function scan() {
    try {
      if (scannerConnected.value)
        await scanner.start()
      else
        await accept(await scanner.scanCamera())
    } catch (error) {
      if (error && typeof error === 'object' && 'errMsg' in error && String(error.errMsg).includes('cancel'))
        return
      uni.showToast({ title: error instanceof Error ? error.message : '扫码失败，可改用手工输入', icon: 'none' })
    }
  }

  return { scan, scannerConnected }
}
