<template>
  <view class="staff-header">
    <view class="row">
      <view class="titles">
        <wd-img v-if="brand" src="/static/brand/jiezuda-logo.png" width="100rpx" height="80rpx" mode="aspectFit" />
        <view class="title">
          {{ title }}
        </view>
        <view v-if="warehouse" class="warehouse">
          <view class="i-carbon-location pin" />
          <text>{{ warehouse }}</text>
        </view>
      </view>
      <view class="scanner" @click="$emit('scanner')">
        <view class="i-carbon-barcode scan-ico" />
        <view>
          <view class="scan-line">
            <view class="dot" :class="connected ? 'on' : 'off'" />
            <text>{{ connected ? '扫码头已连接' : '扫码头不可用' }}</text>
            <text class="chev">
              ›
            </text>
          </view>
          <view class="scan-sub">
            {{ connected ? '作业页支持侧键扫码' : '点此进入扫码作业' }}
          </view>
        </view>
      </view>
    </view>
    <view class="rule" />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { scannerConnected } from '@/services/scanner'

const props = withDefaults(defineProps<{
  title: string
  brand?: boolean
  warehouse?: string
  connected?: boolean
}>(), {
  warehouse: '',
  connected: undefined,
})
defineEmits<{ scanner: [] }>()

const connected = computed(() => props.connected ?? scannerConnected.value)
</script>

<style scoped>
.staff-header {
  padding: 12rpx 0 0;
}
.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
}
.title {
  color: #111;
  font-size: 48rpx;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -1rpx;
}
.warehouse {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-top: 8rpx;
  color: #111;
  font-size: 24rpx;
  font-weight: 600;
}
.pin {
  font-size: 24rpx;
}
.scanner {
  display: flex;
  max-width: 300rpx;
  gap: 10rpx;
  padding-top: 8rpx;
}
.scan-ico {
  margin-top: 4rpx;
  font-size: 32rpx;
}
.scan-line {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  font-weight: 700;
}
.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.dot.on {
  background: #16a34a;
}
.dot.off {
  background: #d1d5db;
}
.chev {
  color: #999;
}
.scan-sub {
  margin-top: 4rpx;
  color: #6b6b6b;
  font-size: 20rpx;
}
.rule {
  margin-top: 20rpx;
  border-bottom: 6rpx solid #111;
}
</style>
