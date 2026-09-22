import { trustedRelease } from '@/models/rental/mobileWorkbench'
import { openUrl } from '@/utils/url'

import { STAFF_VERSION_CODE } from '@/config/staffVersion'

export { STAFF_VERSION } from '@/config/staffVersion'
const CHECK_INTERVAL = 30 * 60 * 1000
const REMIND_INTERVAL = 24 * 60 * 60 * 1000
const REMINDER_KEY = 'jiezuda-update-reminder'
type Release = ReturnType<typeof trustedRelease>
let pendingRequest: Promise<Release> | undefined
let available: Release | undefined
let lastAttempt = 0
let foreground = false
let prompting = false
let timer: ReturnType<typeof setTimeout> | undefined

function installedCode() {
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined')
    return Number(plus.runtime.versionCode) || STAFF_VERSION_CODE
  // #endif
  return STAFF_VERSION_CODE
}
function safeReminderPage() {
  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route || ''
  return ['pages/index/index', 'pages/user/index', 'pages-core/user/settings/index', 'pages-core/auth/login'].includes(route)
}
function fetchRelease() {
  if (!pendingRequest) {
    lastAttempt = Date.now()
    pendingRequest = new Promise<unknown>((resolve, reject) => uni.request({
      url: `https://rental.motion-cover.com/downloads/jiezuda/release.json?t=${Date.now()}`,
      header: { isToken: false },
      success: result => result.statusCode === 200 ? resolve(result.data) : reject(new Error('更新服务暂不可用')),
      fail: () => reject(new Error('网络不可用，请稍后重试')),
    })).then(trustedRelease).then((release) => {
      available = release
      return release
    }).finally(() => { pendingRequest = undefined })
  }
  return pendingRequest
}
async function remind(release: Release, automatic: boolean) {
  if (prompting || !foreground || !safeReminderPage())
    return
  if (automatic) {
    const remembered = uni.getStorageSync(REMINDER_KEY)
    if (remembered?.versionCode === release.versionCode && Number(remembered.until) > Date.now())
      return
  }
  prompting = true
  try {
    const confirmed = await new Promise<boolean>((resolve, reject) => uni.showModal({
      title: `发现新版本 ${release.version}`,
      content: `${release.notes || '优化作业体验与稳定性。'}\n下载安装包后确认覆盖安装，可保留登录和设置。`,
      confirmText: '去升级',
      cancelText: '稍后提醒',
      success: result => resolve(result.confirm),
      fail: () => reject(new Error('无法显示升级提醒，请稍后重试')),
    }))
    uni.setStorageSync(REMINDER_KEY, { versionCode: release.versionCode, until: Date.now() + REMIND_INTERVAL })
    if (confirmed && foreground)
      openUrl(release.download)
  } finally { prompting = false }
}

/** Shared by manual checks and automatic reminders; never downloads or installs silently. */
export async function checkStaffUpdate(automatic = false): Promise<'current' | 'available' | 'skipped'> {
  if (automatic && !pendingRequest && Date.now() - lastAttempt < CHECK_INTERVAL && !available)
    return 'skipped'
  const release = automatic && available && Date.now() - lastAttempt < CHECK_INTERVAL
    ? available
    : await fetchRelease()
  if (release.versionCode <= installedCode())
    return 'current'
  await remind(release, automatic)
  return 'available'
}
export function scheduleUpdateReminder() {
  foreground = true
  clearTimeout(timer)
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined' && plus.os.name === 'Android') {
    timer = setTimeout(() => {
      if (foreground)
        void checkStaffUpdate(true).catch(() => {}) // Offline must not interrupt warehouse work.
    }, 1800)
  }
  // #endif
}
export function pauseUpdateReminder() {
  foreground = false
  clearTimeout(timer)
}
