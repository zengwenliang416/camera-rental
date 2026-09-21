<template>
  <view class="header px-40rpx pb-40rpx pt-100rpx">
    <view class="text-56rpx text-[#111] font-extrabold leading-tight">
      租赁仓务
    </view>
    <view class="mt-8rpx text-28rpx text-[#111] font-semibold">
      员工手持作业端
    </view>
    <view class="mt-28rpx border-t-4 border-[#111] pt-24rpx text-24rpx text-[#6b6b6b]">
      {{ title }}
    </view>
  </view>
</template>

<script lang="ts" setup>
const title = import.meta.env.VITE_APP_TITLE // 应用标题
</script>
