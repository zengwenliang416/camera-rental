/** Some Android PDA runtimes miss uni.onThemeChange; use the OS configuration notification. */
export function observeAndroidThemeChange(onChange: () => void): (() => void) | undefined {
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined' && plus.os.name === 'Android') {
    const invoke = plus.android.invoke as (target: PlusAndroidInstanceObject | string, method: string, ...args: unknown[]) => unknown
    const action = 'android.intent.action.CONFIGURATION_CHANGED'
    let active = false
    try {
      const activity = plus.android.runtimeMainActivity()
      const filter = plus.android.newObject('android.content.IntentFilter')
      invoke(filter, 'addAction', action)
      const receiver = plus.android.implements('io.dcloud.feature.internal.reflect.BroadcastReceiver', {
        onReceive: (_context: PlusAndroidInstanceObject, intent: PlusAndroidInstanceObject) => {
          if (active && invoke(intent, 'getAction') === action)
            onChange()
        },
      })
      const apiLevel = Number(plus.android.getAttribute('android.os.Build$VERSION', 'SDK_INT'))
      if (apiLevel >= 33)
        invoke(activity, 'registerReceiver', receiver, filter, 4) // RECEIVER_NOT_EXPORTED; OS broadcasts only.
      else
        invoke(activity, 'registerReceiver', receiver, filter)
      active = true
      return () => {
        if (!active)
          return
        active = false
        try {
          invoke(activity, 'unregisterReceiver', receiver)
        } catch {
          // Activity may already have been destroyed; late callbacks remain ignored.
        }
      }
    } catch {
      // Foreground lifecycle refresh still works; registration retries on the next show.
    }
  }
  // #endif
  return undefined
}
