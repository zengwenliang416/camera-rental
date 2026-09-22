<script setup lang="ts">
import { onHide, onLaunch, onShow } from '@dcloudio/uni-app'
import { onMounted, onUnmounted } from 'vue'
import { navigateToInterceptor } from '@/router/interceptor'
import { useDictStore, useTokenStore } from '@/store'
import { tabbarStore } from '@/tabbar/store'
import { scanner } from '@/services/scanner'
import { useThemeStore } from '@/store/theme'
import { pauseUpdateReminder, scheduleUpdateReminder } from '@/services/staffUpdate'

onLaunch(() => {
  const theme = useThemeStore()
  theme.startThemeListener()
  theme.setThemeVars({ buttonPrimaryBg: '#e10600', buttonPrimaryBgActive: '#b80500', buttonPrimaryColor: '#e10600', buttonPrimaryPlainBorder: '#e10600' })
})
onShow((options) => {
  scheduleUpdateReminder()
  useThemeStore().startThemeListener()
  void scanner.initialize()

  // 微信环境可能清理本地缓存，导致登录态仍在但字典缓存丢失，这里做一次非阻塞补偿加载
  // 对应 https://t.zsxq.com/boU4A 帖子
  const tokenStore = useTokenStore()
  const dictStore = useDictStore()
  if (tokenStore.updateNowTime().hasLogin && !dictStore.isLoaded) {
    void dictStore.loadDictCacheWithRetry()
  }

  // 处理直接进入页面路由的情况：如h5直接输入路由、微信小程序分享后进入等
  // https://github.com/unibest-tech/unibest/issues/192
  if (options?.path) {
    navigateToInterceptor.invoke({ url: `/${options.path}`, query: options.query })
  } else {
    navigateToInterceptor.invoke({ url: '/' })
  }
  tabbarStore.syncCurIdxByCurrentPageAsync()
})
onUnmounted(() => useThemeStore().stopThemeListener())

onHide(() => {
  pauseUpdateReminder()
  void scanner.stop()
})

// #ifdef H5
function syncTabbarWhenPageVisible() {
  if (document.visibilityState === 'visible') {
    tabbarStore.syncCurIdxByCurrentPageAsync()
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', syncTabbarWhenPageVisible)
  window.addEventListener('pageshow', syncTabbarWhenPageVisible)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', syncTabbarWhenPageVisible)
  window.removeEventListener('pageshow', syncTabbarWhenPageVisible)
})
// #endif
</script>

<style lang="scss">

</style>
