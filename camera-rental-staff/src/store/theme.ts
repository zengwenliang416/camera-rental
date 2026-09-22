import type { ConfigProviderThemeVars } from '@wot-ui/ui/components/wd-config-provider/types'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { observeAndroidThemeChange } from '@/services/systemTheme'

export type ThemeMode = 'system' | 'light' | 'dark'
type Theme = Exclude<ThemeMode, 'system'>
const isTheme = (value: unknown): value is Theme => value === 'light' || value === 'dark'

export const useThemeStore = defineStore('theme-store', () => {
  // Legacy theme values are refreshed on launch; new installs and upgrades default to system.
  const themeMode = ref<ThemeMode>('system')
  const theme = ref<Theme>('light')
  const systemTheme = ref<Theme>('light')
  const themeVars = ref<ConfigProviderThemeVars>({
    buttonPrimaryBg: '#e10600',
    buttonPrimaryBgActive: '#b80500',
    buttonPrimaryColor: '#e10600',
    buttonPrimaryPlainBorder: '#e10600',
  })
  const modeLabel = computed(() => themeMode.value === 'system'
    ? `跟随系统 · ${theme.value === 'dark' ? '深色' : '浅色'}`
    : themeMode.value === 'dark' ? '深色模式' : '浅色模式')
  let listening = false
  let stopPlatformListener: (() => void) | undefined

  function applySystemTheme(value: unknown) {
    if (isTheme(value))
      systemTheme.value = value
    theme.value = themeMode.value === 'system' ? systemTheme.value : themeMode.value
  }
  function refreshSystemTheme() {
    try {
      const info = uni.getSystemInfoSync()
      // osTheme describes the phone, whereas theme can describe the host app.
      applySystemTheme(isTheme(info.osTheme) ? info.osTheme : info.theme)
    } catch {
      applySystemTheme(undefined)
    }
  }
  function handleThemeChange(event: { theme: string }) {
    if (isTheme(event.theme))
      applySystemTheme(event.theme)
  }
  function startThemeListener() {
    refreshSystemTheme()
    if (!stopPlatformListener)
      stopPlatformListener = observeAndroidThemeChange(refreshSystemTheme)
    if (!listening && typeof uni.onThemeChange === 'function') {
      uni.onThemeChange(handleThemeChange)
      listening = true
    }
  }
  function stopThemeListener() {
    stopPlatformListener?.()
    stopPlatformListener = undefined
    if (listening && typeof uni.offThemeChange === 'function')
      uni.offThemeChange(handleThemeChange)
    listening = false
  }
  function setThemeMode(mode: ThemeMode) {
    if (mode !== 'system' && !isTheme(mode))
      return
    themeMode.value = mode
    refreshSystemTheme()
  }
  function setThemeVars(partialVars: Partial<ConfigProviderThemeVars>) {
    themeVars.value = { ...themeVars.value, ...partialVars }
  }
  return { theme, themeMode, systemTheme, modeLabel, themeVars, setThemeMode, setThemeVars, refreshSystemTheme, startThemeListener, stopThemeListener }
}, {
  // Persist preference, not the last observed OS appearance.
  persist: { paths: ['themeMode', 'themeVars'] },
})
