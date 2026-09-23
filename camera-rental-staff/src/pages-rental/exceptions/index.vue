<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content">
      <staff-header title="操作记录" @scanner="goScan" />
      <view class="hero">
        <view>
          <view class="muted">
            待处理
          </view>
          <view class="num accent">
            {{ openItems.length }}
          </view>
          <view class="muted">
            条
          </view>
        </view>
        <view class="rule" />
        <view>
          <view class="muted">
            今日已标记
          </view>
          <view class="num">
            {{ resolvedToday.length }}
          </view>
          <view class="muted">
            条
          </view>
        </view>
      </view>

      <view class="tabs">
        <view
          v-for="tab in tabs"
          :key="tab.key"
          class="tab"
          :class="{ on: current === tab.key }"
          @click="current = tab.key"
        >
          {{ tab.label }}
          <text class="count">
            ({{ countOf(tab.key) }})
          </text>
        </view>
      </view>

      <view class="section-head">
        <text>待处理异常</text>
        <text class="muted">
          {{ filteredOpen.length }} 条
        </text>
      </view>
      <view v-if="!filteredOpen.length" class="empty">
        当前没有未处理异常。扫码失败、型号不符和发货失败会记录在这里。
      </view>
      <view v-for="item in filteredOpen" :key="item.id" class="card accent" @click="openItem(item)">
        <view class="card-title">
          {{ item.title }}
        </view>
        <view class="muted">
          {{ item.detail }}
        </view>
        <view class="meta">
          {{ formatTime(item.createdAt) }} | {{ item.source }}
        </view>
        <view class="actions">
          <wd-button size="small" type="primary" @click.stop="openItem(item)">
            {{ item.itemId || item.rentalOrderId ? '返回相关业务' : item.kind === 'scan' ? '重新查询' : '查看说明' }}
          </wd-button>
          <wd-button size="small" variant="plain" @click.stop="exceptions.resolve(item.id)">
            标记已查看
          </wd-button>
        </view>
      </view>

      <view class="section-head">
        <text>已标记记录</text>
        <text class="muted">
          {{ resolvedItems.length }} 条
        </text>
      </view>
      <view v-for="item in resolvedItems" :key="item.id" class="card">
        <view class="card-title">
          {{ item.title }}
        </view>
        <view class="muted">
          {{ item.detail }}
        </view>
        <view class="meta">
          {{ formatTime(item.createdAt) }} | {{ item.source }}
        </view>
      </view>

      <view class="note">
        这里只保留本次登录的操作失败记录。标记仅表示已查看，不会修改订单、设备或物流状态。
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import StaffHeader from '@/components/rental/staff-header.vue'
import { useStaffExceptionStore } from '@/store/staffException'
import type { StaffException, StaffExceptionKind } from '@/store/staffException'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const exceptions = useStaffExceptionStore()
const { openItems, resolvedToday, items } = storeToRefs(exceptions)
const current = ref<'all' | StaffExceptionKind>('all')
const tabs = [
  { key: 'all' as const, label: '全部' },
  { key: 'scan' as const, label: '扫码' },
  { key: 'order' as const, label: '订单' },
  { key: 'network' as const, label: '网络' },
]
const resolvedItems = computed(() => items.value.filter(item => item.status === 'resolved' && (current.value === 'all' || item.kind === current.value)))
const filteredOpen = computed(() => current.value === 'all' ? openItems.value : openItems.value.filter(item => item.kind === current.value))

function countOf(key: 'all' | StaffExceptionKind) {
  if (key === 'all')
    return openItems.value.length
  return openItems.value.filter(item => item.kind === key).length
}

function formatTime(ts: number) {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function goScan() {
  uni.switchTab({ url: '/pages-rental/device-scan/index' })
}

function openItem(item: StaffException) {
  if (item.itemId)
    uni.navigateTo({ url: `/pages-rental/orders/candidates?itemId=${item.itemId}` })
  else if (item.rentalOrderId)
    uni.navigateTo({ url: `/pages-rental/orders/detail?id=${item.rentalOrderId}` })
  else if (item.kind === 'scan')
    goScan()
  else uni.showModal({ title: item.title, content: `${item.detail}\n请返回原作业页面刷新并核对结果，不要重复提交。`, showCancel: false })
}
</script>

<style scoped>
.page {
  padding-top: var(--staff-status-bar-height, 0px);
  box-sizing: border-box;
  min-height: 100vh;
  background: var(--staff-surface);
}
.content {
  height: calc(100vh - var(--staff-status-bar-height, 0px));
  padding: 12rpx 28rpx 160rpx;
  box-sizing: border-box;
}
.hero {
  display: flex;
  align-items: flex-end;
  gap: 40rpx;
  padding: 24rpx 0;
}
.rule {
  width: 2rpx;
  height: 80rpx;
  background: var(--staff-border);
}
.muted {
  color: var(--staff-muted);
  font-size: 22rpx;
}
.num {
  font-size: 72rpx;
  font-weight: 800;
  line-height: 1;
}
.accent {
  color: var(--staff-accent-text);
}
.tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 8rpx 0 20rpx;
  border: 2rpx solid var(--staff-ink);
}
.tab {
  padding: 16rpx 0;
  text-align: center;
  font-size: 24rpx;
}
.tab.on {
  color: #fff;
  background: var(--staff-accent, #e10600);
}
.count {
  font-size: 20rpx;
}
.section-head {
  display: flex;
  justify-content: space-between;
  margin: 24rpx 0 12rpx;
  font-weight: 800;
}
.card {
  padding: 20rpx;
  margin-bottom: 12rpx;
  border: 2rpx solid var(--staff-border);
  border-left: 8rpx solid var(--staff-info);
}
.card.accent {
  border-left-color: var(--staff-accent-text);
}
.card-title {
  font-size: 30rpx;
  font-weight: 800;
}
.meta {
  margin-top: 8rpx;
  color: var(--staff-muted);
  font-size: 22rpx;
}
.actions {
  display: flex;
  gap: 12rpx;
  margin-top: 16rpx;
}
.empty,
.note {
  padding: 20rpx 0;
  color: var(--staff-muted);
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
