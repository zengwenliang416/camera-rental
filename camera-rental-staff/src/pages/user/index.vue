<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content">
      <view class="title">
        我的
      </view>

      <view class="card">
        <view class="user">
          <view class="avatar">
            <view class="i-carbon-user" />
          </view>
          <view>
            <view class="name">
              {{ userInfo.nickname || userInfo.username || '员工' }}
            </view>
            <view class="muted">
              账号 {{ userInfo.username || '-' }}
            </view>
          </view>
        </view>
        <view class="perm">
          <text class="muted">
            权限范围
          </text>
          <text class="strong">
            {{ permissionText }}
          </text>
        </view>
      </view>

      <view class="card row" @click="goScan">
        <view class="i-carbon-barcode ico" />
        <view class="flex-1">
          <view class="strong">
            扫码头
          </view>
          <view class="muted">
            {{ scannerConnected ? '侧键扫描已就绪，点击进入设备查询' : '扫码头不可用，可使用摄像头' }}
          </view>
        </view>
        <view class="muted">
          {{ scannerConnected ? '已连接' : '未连接' }} ›
        </view>
      </view>

      <view class="list">
        <view class="item" @click="toggleTheme">
          <text>主题</text>
          <text class="muted">
            {{ theme === 'dark' ? '深色' : '浅色' }} ›
          </text>
        </view>
        <view class="item" @click="handleGoSettings">
          <text>网络诊断 / 关于仓务 App</text>
          <text class="muted">
            ›
          </text>
        </view>
        <view class="item" @click="handleGoSecurity">
          <text>登录有效</text>
          <text class="muted">
            账号安全 ›
          </text>
        </view>
      </view>

      <wd-button type="primary" block @click="handleLogout">
        退出登录
      </wd-button>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useDialog } from '@wot-ui/ui/components/wd-dialog'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref } from 'vue'
import { getUserProfile } from '@/api/system/user/profile'
import type { UserProfileVO } from '@/api/system/user/profile'
import { LOGIN_PAGE } from '@/router/config'
import { scannerConnected } from '@/services/scanner'
import { useUserStore } from '@/store'
import { useThemeStore } from '@/store/theme'
import { useTokenStore } from '@/store/token'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const userStore = useUserStore()
const tokenStore = useTokenStore()
const themeStore = useThemeStore()
const toast = useToast()
const dialog = useDialog()
const { userInfo, permissions } = storeToRefs(userStore)
const { theme } = storeToRefs(themeStore)
const userProfile = ref<UserProfileVO | null>(null)
const permissionText = computed(() => {
  const labels: string[] = []
  if (permissions.value.includes('rental:xianyu:ship'))
    labels.push('发货')
  if (permissions.value.includes('rental:device:assign'))
    labels.push('设备分配')
  if (permissions.value.includes('rental:device:query') || permissions.value.includes('rental:schedule:query'))
    labels.push('回仓检测')
  return labels.length ? labels.join(' · ') : '仓务作业'
})

onMounted(async () => {
  try {
    userProfile.value = await getUserProfile()
  } catch {
    userProfile.value = null
  }
  await userStore.fetchUserInfo()
})

function toggleTheme() {
  themeStore.toggleTheme()
  toast.success(themeStore.theme === 'dark' ? '已切换深色' : '已切换浅色')
}

function goScan() {
  uni.switchTab({ url: '/pages-rental/device-scan/index' })
}

function handleGoSecurity() {
  uni.navigateTo({ url: '/pages-core/user/security/index' })
}

function handleGoSettings() {
  uni.navigateTo({ url: '/pages-core/user/settings/index' })
}

async function handleLogout() {
  try {
    await dialog.confirm({ title: '提示', msg: '确定要退出登录吗？' })
  } catch {
    return
  }
  await tokenStore.logout()
  toast.success('退出登录成功')
  setTimeout(() => {
    uni.reLaunch({ url: LOGIN_PAGE })
  }, 500)
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #fff;
}
.content {
  height: 100vh;
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 28rpx 160rpx;
  box-sizing: border-box;
}
.title {
  font-size: 56rpx;
  font-weight: 800;
}
.card {
  padding: 24rpx 0;
  border-bottom: 2rpx solid #eee;
}
.user {
  display: flex;
  gap: 20rpx;
  align-items: center;
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  border: 2rpx solid #111;
  font-size: 48rpx;
}
.name {
  font-size: 36rpx;
  font-weight: 800;
}
.muted {
  color: #6b6b6b;
  font-size: 24rpx;
}
.strong {
  font-weight: 800;
}
.perm {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
}
.row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.ico {
  font-size: 44rpx;
}
.list {
  margin: 12rpx 0 32rpx;
}
.item {
  display: flex;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 2rpx solid #eee;
  font-size: 28rpx;
}
</style>
