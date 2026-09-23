<template>
  <view class="card" :class="tone" @click="$emit('click')">
    <view class="kicker">
      <view class="kicker-left">
        <view class="i-carbon-time clock" />
        <text>{{ kicker }}</text>
      </view>
      <view v-if="tag" class="tag" :class="tone">
        {{ tag }}
      </view>
    </view>
    <view class="order">
      订单 {{ maskOrderId(orderNo) }}
    </view>
    <view class="body">
      <view class="thumb">
        <view class="i-carbon-camera" />
      </view>
      <view class="info">
        <view class="goods">
          {{ title }}
        </view>
        <view class="meta">
          <text>{{ subtitle }}</text>
          <text v-if="routeLabel" class="sep">
            {{ routeLabel }}
          </text>
        </view>
        <view v-if="source" class="source">
          来源 {{ source }}
        </view>
      </view>
      <wd-button v-if="action" size="small" type="primary" @click.stop="$emit('action')">
        {{ action }}
      </wd-button>
      <view v-else-if="!readonly" class="arrow">
        ›
      </view>
    </view>
    <view v-if="showRecipient" class="recipient">
      <view>收件人：{{ receiverName || '未提供' }}</view>
      <view>电话：{{ receiverMobile || '未提供' }}</view>
      <view>地址：{{ receiverAddress || '未提供' }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { maskOrderId } from '@/utils/staffScan'

withDefaults(defineProps<{
  kicker: string
  tag?: string
  tone?: 'danger' | 'info' | 'accent'
  orderNo: string
  title: string
  subtitle?: string
  routeLabel?: string
  source?: string
  action?: string
  receiverName?: string
  receiverMobile?: string
  receiverAddress?: string
  readonly?: boolean
  showRecipient?: boolean
}>(), {
  tag: '',
  tone: 'accent',
  subtitle: '',
  routeLabel: '',
  source: '',
  action: '',
})

defineEmits<{ click: [], action: [] }>()
</script>

<style scoped>
.recipient {
  margin-top: 20rpx;
  padding-top: 16rpx;
  border-top: 1px solid var(--staff-border);
  font-size: 26rpx;
  line-height: 1.7;
  overflow-wrap: anywhere;
  color: var(--staff-ink);
}
.card {
  padding: 24rpx;
  background: var(--staff-surface);
  border-left: 8rpx solid var(--staff-accent, #e10600);
}
.card.info {
  border-left-color: var(--staff-info);
}
.kicker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  color: var(--staff-accent-text);
  font-size: 26rpx;
  font-weight: 800;
}
.card.info .kicker {
  color: var(--staff-info);
}
.kicker-left {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.clock {
  font-size: 28rpx;
}
.tag {
  padding: 4rpx 12rpx;
  font-size: 20rpx;
  font-weight: 700;
  background: var(--staff-accent-soft, #fff1f0);
}
.tag.info {
  color: var(--staff-info);
  background: var(--staff-info-soft);
}
.order {
  margin-top: 8rpx;
  color: var(--staff-muted);
  font-size: 22rpx;
}
.body {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}
.thumb {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  color: var(--staff-ink);
  background: var(--staff-soft);
  font-size: 40rpx;
}
.info {
  flex: 1;
  min-width: 0;
}
.goods {
  font-size: 32rpx;
  font-weight: 800;
  line-height: 1.25;
}
.meta,
.source {
  margin-top: 6rpx;
  color: var(--staff-muted);
  font-size: 22rpx;
}
.sep {
  margin-left: 12rpx;
}
.arrow {
  color: var(--staff-muted);
  font-size: 40rpx;
}
</style>
