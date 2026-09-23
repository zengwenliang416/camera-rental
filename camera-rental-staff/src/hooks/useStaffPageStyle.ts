import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'

/** Keep the system inset stable when a resumed Android WebView briefly reports zero. */
export function useStaffPageStyle() {
  const style = ref<Record<string, string>>({})
  let lastHeight = 0
  function update() {
    const info = uni.getSystemInfoSync()
    const heights = [info.statusBarHeight, info.safeAreaInsets?.top]
    // #ifdef APP-PLUS
    if (typeof plus !== 'undefined') {
      try {
        heights.push(plus.navigator.getStatusbarHeight())
      } catch { /* Some runtimes do not expose native metrics yet. */ }
    }
    // #endif
    const measured = Math.max(0, ...heights.filter((value): value is number => typeof value === 'number' && Number.isFinite(value) && value >= 0))
    if (measured > 0)
      lastHeight = measured
    style.value = { '--staff-status-bar-height': `${measured || lastHeight}px` }
  }
  update()
  onShow(update)
  return style
}
