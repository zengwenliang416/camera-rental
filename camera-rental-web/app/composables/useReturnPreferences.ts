import type { ReturnLocale, ReturnMessageKey } from './returnMessages'
import { returnMessage } from './returnMessages'

export type ReturnTheme = 'light' | 'dark'

export function useReturnPreferences() {
  const locale = useState<ReturnLocale>('return-locale', () => 'zh-CN')
  const theme = useState<ReturnTheme>('return-theme', () => 'light')
  const t = (key: ReturnMessageKey) => returnMessage(locale.value, key)

  function applyPreferences() {
    if (!import.meta.client) return
    document.documentElement.dataset.theme = theme.value
    document.documentElement.lang = locale.value
  }

  function initialize() {
    if (!import.meta.client) return
    const savedLocale = localStorage.getItem('return-locale')
    const savedTheme = localStorage.getItem('return-theme')
    if (savedLocale === 'zh-CN' || savedLocale === 'en') locale.value = savedLocale
    if (savedTheme === 'light' || savedTheme === 'dark') {
      theme.value = savedTheme
    } else if (matchMedia('(prefers-color-scheme: dark)').matches) {
      theme.value = 'dark'
    }
    applyPreferences()
  }

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('return-theme', theme.value)
    applyPreferences()
  }

  function toggleLocale() {
    locale.value = locale.value === 'zh-CN' ? 'en' : 'zh-CN'
    localStorage.setItem('return-locale', locale.value)
    applyPreferences()
  }

  return { locale, theme, t, initialize, toggleTheme, toggleLocale }
}
