<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useThemeStore } from '@/store'
import FgTabbar from '@/tabbar/index.vue'
import { isPageTabbar, tabbarStore } from './tabbar/store'
import { currRoute } from './utils'

const themeStore = useThemeStore()
themeStore.setThemeVars({
  buttonPrimaryBg: '#e10600',
  buttonPrimaryBgActive: '#b80500',
  buttonPrimaryColor: '#e10600',
  buttonPrimaryPlainBorder: '#e10600',
})

const providerThemeVars = computed(() => ({
  ...themeStore.themeVars,
  buttonPrimaryColor: themeStore.theme === 'dark' ? '#ff827d' : '#e10600',
  buttonPrimaryPlainBorder: themeStore.theme === 'dark' ? '#ff827d' : '#e10600',
}))

let navigationThemeTimer: ReturnType<typeof setTimeout> | undefined
onUnmounted(() => clearTimeout(navigationThemeTimer))

function syncSystemTheme() {
  const dark = themeStore.theme === 'dark'
  const background = dark ? '#181a1f' : '#ffffff'
  uni.setNavigationBarColor({ frontColor: dark ? '#ffffff' : '#000000', backgroundColor: background })
  // #ifdef APP-PLUS
  plus.navigator.setStatusBarStyle(dark ? 'light' : 'dark')
  plus.navigator.setStatusBarBackground(background)
  plus.webview.currentWebview().setStyle({ background })
  if (plus.os.name === 'Android') {
    clearTimeout(navigationThemeTimer)
    // Apply after DCloud finishes updating status-bar flags for this page.
    navigationThemeTimer = setTimeout(() => {
      try {
        const activity = plus.android.runtimeMainActivity()
        const window = plus.android.invoke(activity, 'getWindow')
        const color = plus.android.invoke('android.graphics.Color', 'parseColor', background)
        plus.android.invoke(window, 'setNavigationBarColor', color)
        if (Number.parseInt(plus.os.version || '0', 10) >= 11) {
          const controller = plus.android.invoke(window, 'getInsetsController')
          // Apply both bar appearances after page navigation resets native defaults.
          // Native.js is variadic; the SDK types only declare one Java argument.
          const invokeAppearance = plus.android.invoke as (target: PlusAndroidInstanceObject, method: string, appearance: number, mask: number) => unknown
          invokeAppearance(controller, 'setSystemBarsAppearance', dark ? 0 : 24, 24)
        } else {
          const decor = plus.android.invoke(window, 'getDecorView')
          const flags = Number(plus.android.invoke(decor, 'getSystemUiVisibility'))
          plus.android.invoke(decor, 'setSystemUiVisibility', dark ? flags & ~0x2010 : flags | 0x2010)
        }
      } catch {
        // Older Android versions retain the system navigation bar appearance.
      }
    }, 100)
  }
  // #endif
}
watch([() => themeStore.theme, () => themeStore.systemTheme], syncSystemTheme)

const isCurrentPageTabbar = ref(true)
onShow(() => {
  syncSystemTheme()
  tabbarStore.syncCurIdxByCurrentPageAsync()
  const { path } = currRoute()
  // “蜡笔小开心”提到本地是 '/pages/index/index'，线上是 '/' 导致线上 tabbar 不见了
  // 所以这里需要判断一下，如果是 '/' 就当做首页，也要显示 tabbar
  if (path === '/') {
    isCurrentPageTabbar.value = true
  } else {
    isCurrentPageTabbar.value = isPageTabbar(path)
  }
})

const helloKuRoot = ref('Hello AppKuVue')

const exposeRef = ref('this is form app.Ku.vue')

defineExpose({
  exposeRef,
})
</script>

<template>
  <wd-config-provider custom-class="staff-theme" :theme-vars="providerThemeVars" :theme="themeStore.theme">
    <!-- 这个先隐藏了，知道这样用就行 -->
    <view class="hidden text-center">
      {{ helloKuRoot }}，这里可以配置全局的东西
    </view>

    <KuRootView />

    <FgTabbar v-if="isCurrentPageTabbar" />
    <wd-toast />
    <wd-dialog />
  </wd-config-provider>
</template>
