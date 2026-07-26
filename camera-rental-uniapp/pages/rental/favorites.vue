<template>
  <s-layout navbar="normal" title="收藏器材">
    <view class="rental-page">
      <view class="header-card">
        <view class="title">收藏器材</view>
        <view class="desc">先放本地样例，后续接会员收藏和租赁商品 App API。</view>
      </view>
      <view v-for="item in rentalFavorites" :key="item.id" class="favorite-card">
        <view>
          <view class="category">{{ item.category }}</view>
          <view class="name">{{ item.title }}</view>
          <view class="desc">{{ item.subtitle }}</view>
        </view>
        <button class="primary-btn" @tap="goSchedule(item.id)">选租期</button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import sheep from '@/sheep';
  import { rentalFavorites } from '@/sheep/helper/rentalDemo';

  const goSchedule = (productId) => {
    sheep.$router.go('/pages/rental/schedule', { productId });
  };
</script>

<style lang="scss" scoped>
  .rental-page {
    min-height: 100vh;
    padding: 24rpx 24rpx 120rpx;
    background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%);
  }

  .header-card,
  .favorite-card {
    margin-bottom: 22rpx;
    padding: 30rpx;
    border-radius: 28rpx;
    background: #ffffff;
    box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.06);
  }

  .title {
    color: #111827;
    font-size: 38rpx;
    font-weight: 800;
  }

  .category,
  .desc {
    color: #64748b;
    font-size: 24rpx;
    line-height: 1.55;
  }

  .name {
    margin-top: 8rpx;
    color: #111827;
    font-size: 32rpx;
    font-weight: 800;
  }

  .primary-btn {
    margin-top: 22rpx;
    height: 72rpx;
    border-radius: 999rpx;
    color: #ffffff;
    background: #111827;
    font-size: 26rpx;
    font-weight: 800;
    line-height: 72rpx;
  }
</style>
