import { readonly, ref } from 'vue'
// #ifdef APP-PLUS
import { AndroidScanner } from './android'
// #endif

export interface ScanResult {
  text: string
  source: 'vendor-broadcast' | 'camera' | 'manual'
  format?: string
  receivedAt: number
}

type Listener = (result: ScanResult) => void
const connected = ref(false)
export const scannerConnected = readonly(connected)

class UniScannerAdapter {
  private listeners = new Set<Listener>()
  // #ifdef APP-PLUS
  private android?: AndroidScanner
  // #endif

  async initialize() {
    // #ifdef APP-PLUS
    if (typeof plus !== 'undefined' && plus.os.name === 'Android' && !this.android) {
      const android = new AndroidScanner()
      try {
        android.initialize((text) => {
          if (import.meta.env.DEV)
            console.info('[scanner] 已接收扫码头数据')
          const result: ScanResult = { text, source: 'vendor-broadcast', receivedAt: Date.now() }
          this.listeners.forEach(listener => listener(result))
        })
        this.android = android
        connected.value = true
        console.info('[scanner] UROVO 扫码头就绪，广播监听已注册')
      } catch (error) {
        connected.value = false
        console.warn('[scanner]', error instanceof Error ? error.message : '扫码头不可用')
      }
    }
    // #endif
  }

  async start() {
    await this.initialize()
    // #ifdef APP-PLUS
    if (this.android) {
      this.android.start()
      return
    }
    // #endif
    throw new Error('扫码头不可用，请使用摄像头扫码')
  }

  stopDecode() {
    // #ifdef APP-PLUS
    this.android?.stopDecode()
    // #endif
  }

  async stop() {
    // #ifdef APP-PLUS
    this.android?.dispose()
    this.android = undefined
    // #endif
    connected.value = false
  }

  async getStatus() {
    return connected.value ? 'ready' as const : 'unavailable' as const
  }

  subscribe(listener: Listener) {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  async scanCamera() {
    const result = await uni.scanCode({ scanType: ['qrCode', 'barCode'] })
    const scan: ScanResult = {
      text: result.result,
      source: 'camera',
      format: result.scanType,
      receivedAt: Date.now(),
    }
    return scan
  }
}

export const scanner = new UniScannerAdapter()
