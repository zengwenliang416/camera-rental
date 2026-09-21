/** UROVO 系统 SDK 的 Native.js 边界；厂家类名只在适配层出现。 */
function invoke(target: PlusAndroidInstanceObject | string, method: string, ...args: unknown[]): unknown {
  // Native.js 支持可变参数，当前 @dcloudio/types 只声明了一个参数。
  const call = plus.android.invoke as (target: PlusAndroidInstanceObject | string, method: string, ...args: unknown[]) => unknown
  return call(target, method, ...args)
}

export class AndroidScanner {
  private manager?: PlusAndroidInstanceObject
  private activity?: PlusAndroidInstanceObject
  private receiver?: PlusAndroidInstanceObject
  private originalMode?: number
  private openedHere = false

  initialize(onResult: (text: string) => void) {
    if (this.receiver)
      return
    try {
      this.manager = plus.android.newObject('android.device.ScanManager')
      this.activity = plus.android.runtimeMainActivity()
      if (!this.manager || !this.activity)
        throw new Error('此设备未提供扫码头接口')
      if (invoke(this.manager, 'getScannerState') !== true) {
        this.openedHere = invoke(this.manager, 'openScanner') === true
        if (!this.openedHere)
          throw new Error('扫码头未能开启')
      }
      const mode = invoke(this.manager, 'getOutputMode')
      if (mode !== 0 && mode !== 1)
        throw new Error('无法读取扫码头输出模式')
      this.originalMode = mode
      const propertyClass = 'android.device.scanner.configuration.PropertyID'
      const actionId = plus.android.getAttribute(propertyClass, 'WEDGE_INTENT_ACTION_NAME')
      const dataId = plus.android.getAttribute(propertyClass, 'WEDGE_INTENT_DATA_STRING_TAG')
      const config = invoke(this.manager, 'getParameterString', [actionId, dataId]) as string[] | null
      const action = config?.[0] || 'android.intent.ACTION_DECODE_DATA'
      const dataKey = config?.[1] || 'barcode_string'
      const filter = plus.android.newObject('android.content.IntentFilter')
      invoke(filter, 'addAction', action)
      const receiver = plus.android.implements('io.dcloud.feature.internal.reflect.BroadcastReceiver', {
        onReceive: (_context: PlusAndroidInstanceObject, intent: PlusAndroidInstanceObject) => {
          if (!this.receiver || invoke(intent, 'getAction') !== action)
            return
          const value = invoke(intent, 'getStringExtra', dataKey)
          if (typeof value === 'string' && value.trim())
            onResult(value.trim())
        },
      })
      const apiLevel = Number(plus.android.getAttribute('android.os.Build$VERSION', 'SDK_INT'))
      if (apiLevel >= 33)
        invoke(this.activity, 'registerReceiver', receiver, filter, 2) // RECEIVER_EXPORTED：厂家进程广播
      else
        invoke(this.activity, 'registerReceiver', receiver, filter)
      this.receiver = receiver
      if (mode !== 0 && invoke(this.manager, 'switchOutputMode', 0) !== true)
        throw new Error('无法切换扫码头到广播模式')
      if (invoke(this.manager, 'getScannerState') !== true || invoke(this.manager, 'getOutputMode') !== 0)
        throw new Error('扫码头尚未就绪')
    } catch (error) {
      this.dispose()
      throw error
    }
  }

  start() {
    if (!this.manager || !this.receiver || invoke(this.manager, 'startDecode') !== true)
      throw new Error('扫码头启动失败，请使用侧键重试或摄像头扫码')
  }

  stopDecode() {
    if (this.manager)
      invoke(this.manager, 'stopDecode')
  }

  dispose() {
    const receiver = this.receiver
    this.receiver = undefined
    const cleanup = (action: () => void) => {
      try {
        action()
      } catch {
        console.warn('[scanner] 扫码头资源释放失败')
      }
    }
    if (receiver && this.activity)
      cleanup(() => { invoke(this.activity, 'unregisterReceiver', receiver) })
    const manager = this.manager
    if (manager) {
      cleanup(() => this.stopDecode())
      if (this.originalMode !== undefined)
        cleanup(() => { invoke(manager, 'switchOutputMode', this.originalMode) })
      if (this.openedHere)
        cleanup(() => { invoke(manager, 'closeScanner') })
    }
    this.manager = undefined
    this.activity = undefined
    this.originalMode = undefined
    this.openedHere = false
  }
}
