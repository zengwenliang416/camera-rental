<template>
  <view class="banner" @click="$emit('click')">
    <view class="i-carbon-scan icon" />
    <view class="copy">
      <view class="title">
        {{ title }}
      </view>
      <view class="sub">
        {{ subtitle }}
      </view>
    </view>
    <view class="chev">
      ›
    </view>
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  subtitle?: string
}>(), {
  title: '扫码识别',
  subtitle: '设备码、运单码均可识别',
})

defineEmits<{ click: [] }>()
</script>

<style scoped>
.banner {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  color: #fff;
  background: var(--staff-accent, #e10600);
}
.icon {
  flex-shrink: 0;
  padding-right: 20rpx;
  border-right: 2rpx solid rgba(255, 255, 255, 0.35);
  font-size: 52rpx;
}
.copy {
  flex: 1;
}
.title {
  font-size: 34rpx;
  font-weight: 800;
}
.sub {
  margin-top: 6rpx;
  font-size: 22rpx;
  opacity: 0.9;
}
.chev {
  font-size: 44rpx;
  font-weight: 300;
}
</style>
