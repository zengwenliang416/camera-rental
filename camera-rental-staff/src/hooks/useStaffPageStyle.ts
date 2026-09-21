import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'

/** 使用设备实际状态栏高度，避免 Android 自定义导航与系统时间重叠。 */
export function useStaffPageStyle() {
  const style = ref<Record<string, string>>({})
  function update() {
    const { statusBarHeight } = uni.getSystemInfoSync()
    style.value = { '--staff-status-bar-height': `${statusBarHeight || 0}px` }
  }
  update()
  onShow(update)
  return style
}
