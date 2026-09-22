<template>
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      title="应用设置"
      left-arrow
      placeholder
      safe-area-inset-top
      fixed
      @click-left="handleBack"
    />

    <!-- Logo 区域 -->
    <view class="flex flex-col items-center py-60rpx">
      <wd-img class="mb-24rpx" src="/static/brand/jiezuda-logo.png" width="188rpx" height="150rpx" mode="aspectFit" />
      <text class="staff-text-ink text-40rpx font-medium">捷租达</text>
    </view>

    <!-- 设置列表 -->
    <view class="mx-24rpx">
      <wd-cell-group custom-class="cell-group" border>
        <wd-cell
          title="当前版本"
          :value="`v${version}`"
          is-link
          @click="handleShowVersion"
        >
          <template #icon>
            <wd-icon name="exclamation-circle" size="20px" color="#1890ff" class="mr-16rpx" />
          </template>
        </wd-cell>
        <wd-cell
          title="字典缓存"
          :value="storageSize"
          is-link
          @click="handleClearCache"
        >
          <template #icon>
            <wd-icon name="delete" size="20px" color="#faad14" class="mr-16rpx" />
          </template>
        </wd-cell>
        <StaffThemeSetting />
        <wd-cell title="检查更新" :value="checkingUpdate ? '正在检查…' : updateLabel" is-link @click="checkUpdate" />
      </wd-cell-group>
    </view>

    <!-- 底部协议和版权 -->
    <view class="mt-80rpx flex flex-col items-center">
      <view class="mb-40rpx flex items-center text-26rpx">
        <text class="staff-text-info" @click="handleGoAgreement">《用户协议》</text>
        <text class="staff-text-muted">与</text>
        <text class="staff-text-info" @click="handleGoPrivacy">《隐私协议》</text>
      </view>
      <text class="staff-text-muted mb-10rpx text-24rpx">
        捷租达 · 员工作业端
      </text>
      <text class="staff-text-muted text-24rpx">
        设备租赁与仓务管理
      </text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { useDialog } from '@wot-ui/ui/components/wd-dialog'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { onMounted, ref } from 'vue'
import { navigateBackPlus } from '@/utils'
import { useDictStore } from '@/store/dict'
import { checkStaffUpdate, STAFF_VERSION } from '@/services/staffUpdate'
import StaffThemeSetting from '@/components/staff-theme-setting.vue'

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const dialog = useDialog()
const version = ref(STAFF_VERSION) // 当前版本号
const storageSize = ref('') // 本地缓存大小

/** 返回上一页 */
function handleBack() {
  navigateBackPlus('/pages/user/index')
}

/** 获取应用版本号 */
function getAppVersion() {
  // #ifdef APP-PLUS
  const appInfo = uni.getSystemInfoSync()
  version.value = appInfo.appVersion || STAFF_VERSION
  // #endif
}

/** 获取本地缓存大小 */
function getStorageSize() {
  storageSize.value = useDictStore().isLoaded ? '已加载' : '未加载'
}

/** 显示版本信息 */
function handleShowVersion() {
  toast.info(`当前版本：v${version.value}`)
}

const checkingUpdate = ref(false)
const updateLabel = ref('点击检查新版本')
async function checkUpdate() {
  if (checkingUpdate.value)
    return
  checkingUpdate.value = true
  try {
    const result = await checkStaffUpdate()
    updateLabel.value = result === 'current' ? '已是最新版本' : '发现新版本，可再次检查升级'
    if (result === 'current')
      toast.success('当前已是最新版本')
  } catch (error) {
    if (error instanceof Error) {
      updateLabel.value = '检查失败，点击重试'
      toast.warning(error.message)
    }
  } finally { checkingUpdate.value = false }
}

/** 清除缓存 */
async function handleClearCache() {
  try {
    await dialog.confirm({
      title: '提示',
      msg: '仅清除字典缓存，保留登录、租户和操作数据，确定继续吗？',
    })
  } catch {
    return
  }

  try {
    useDictStore().clearDictCache()
    getStorageSize()
    toast.success('缓存清除成功')
  } catch {
    toast.error('缓存清除失败')
  }
}

/** 跳转到用户协议 */
function handleGoAgreement() {
  uni.navigateTo({ url: '/pages-core/user/settings/agreement/index' })
}

/** 跳转到隐私协议 */
function handleGoPrivacy() {
  uni.navigateTo({ url: '/pages-core/user/settings/privacy/index' })
}

/** 初始化 */
onMounted(() => {
  getStorageSize()
  getAppVersion()
})
</script>

<style lang="scss" scoped>
:deep(.cell-group) {
  border-radius: 12rpx;
  overflow: hidden;
  box-shadow: 0 3rpx 8rpx rgba(24, 144, 255, 0.06);
}
</style>
