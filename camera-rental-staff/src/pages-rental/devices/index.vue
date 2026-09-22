<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="设备列表" left-arrow safe-area-inset-top placeholder @click-left="back" />
    <scroll-view scroll-y class="workbench-content" @scrolltolower="more">
      <view class="workbench-card">
        <wd-input v-model="keyword" placeholder="设备编号 / 序列号" clearable @confirm="load(true)" />
        <wd-input v-model="model" placeholder="型号编码（选填，精确匹配）" clearable @confirm="load(true)" />
        <view class="workbench-actions">
          <wd-button :variant="enabledOnly ? 'base' : 'plain'" size="small" @click="enabledOnly = !enabledOnly; load(true)">
            {{ enabledOnly ? '仅启用设备' : '全部设备' }}
          </wd-button><wd-button size="small" :loading="loading" @click="load(true)">
            查询
          </wd-button>
        </view>
      </view>
      <view class="workbench-note">
        {{ loading ? '正在加载…' : `共 ${total} 台设备` }} · 可按侧键扫描定位设备
      </view>
      <view v-if="error" class="workbench-error">
        {{ error }}<wd-button variant="plain" size="small" @click="load(true)">
          重试
        </wd-button>
      </view>
      <view v-if="!error && !loading && !rows.length" class="workbench-note">
        没有匹配的设备，请检查编号或型号
      </view>
      <view v-for="device in rows" :key="device.id" class="workbench-card">
        <view class="workbench-row">
          <text class="workbench-title">{{ device.deviceNo }}</text><text>{{ deviceStatusLabel(device.status) }}</text>
        </view>
        <view class="workbench-note">
          型号 {{ device.equipmentModelCode }} · {{ device.enabled ? '已启用' : '已停用' }}
        </view>
        <view class="workbench-note">
          仓位 {{ device.warehouseCode || '未登记' }}
        </view>
        <view class="workbench-actions">
          <wd-button variant="plain" size="small" @click="openDevice(device.deviceNo)">
            详情
          </wd-button><wd-button v-if="hasAccessByCodes(['rental:schedule:query'])" variant="plain" size="small" @click="openSchedule(device.deviceNo)">
            查看排期
          </wd-button>
        </view>
      </view>
      <wd-button v-if="rows.length < total" variant="plain" block :loading="loading" @click="more">
        加载更多
      </wd-button>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import {
  onLoad,
  onUnload,
} from '@dcloudio/uni-app'
import { ref } from 'vue'
import {
  getRentalDevices,
  lookupRentalDevice,
} from '@/api/rental/device'
import type { RentalDevice } from '@/api/rental/device'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import { useAccess } from '@/hooks/useAccess'
import {
  deviceStatusLabel,
  staffError,
} from '@/models/rental/staffOperations'
import { setTabParams } from '@/utils/url'

definePage({
  style: {
    navigationStyle: 'custom',
  },
})
const staffPageStyle = useStaffPageStyle()
const {
  hasAccessByCodes,
} = useAccess()
const keyword = ref('')
const model = ref('')
const enabledOnly = ref(false)
const rows = ref<RentalDevice[]>([])
const total = ref(0)
const error = ref('')
const loading = ref(false)
let page = 1
let generation = 0
async function load(reset = true) {
  if (!reset && loading.value)
    return
  const version = ++generation
  if (!hasAccessByCodes(['rental:device:query'])) {
    loading.value = false
    rows.value = []
    error.value = '当前账号没有设备查询权限'
    return
  }
  loading.value = true
  error.value = ''
  const next = reset ? 1 : page + 1
  if (reset) {
    rows.value = []
    total.value = 0
  }
  try {
    const data = await getRentalDevices({
      pageNo: next,
      pageSize: 20,
      keyword: keyword.value.trim() || undefined,
      equipmentModelCode: model.value.trim() || undefined,
      enabled: enabledOnly.value ? true : undefined,
    })
    if (version !== generation)
      return
    rows.value = reset ? data.list || [] : [...rows.value, ...(data.list || [])]
    total.value = data.total
    page = next
  } catch (e) {
    if (version === generation)
      error.value = staffError(e, '设备列表加载失败')
  } finally {
    if (version === generation)
      loading.value = false
  }
}
function more() {
  if (rows.value.length < total.value)
    void load(false)
}
function back() {
  uni.navigateBack({
    fail: () => uni.switchTab({
      url: '/pages/index/index',
    }),
  })
}
function openDevice(deviceNo: string) {
  setTabParams({
    deviceNo,
  })
  uni.switchTab({
    url: '/pages-rental/device-scan/index',
  })
}
function openSchedule(deviceNo: string) {
  uni.navigateTo({
    url: `/pages-rental/schedule/index?deviceNo=${encodeURIComponent(deviceNo)}`,
  })
}
useStaffScanner(async (scan) => {
  const device = await lookupRentalDevice(scan.text)
  openDevice(device.deviceNo)
})
onLoad(() => {
  void load()
})
onUnload(() => {
  generation++
})
</script>
