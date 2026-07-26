<template>
  <s-layout navbar="normal" title="租赁地址">
    <view class="rental-page">
      <view class="header-card">
        <view class="title">发货与归还地址</view>
        <view class="desc">地址会用于发货、归还和押金风控；保存时需要会员登录和后端校验。</view>
        <button class="primary-btn" @tap="showTodo">新增地址</button>
      </view>
      <view class="address-card" v-for="item in rentalAddresses" :key="item.id">
        <view class="name-row">
          <view class="name">{{ item.name }} {{ item.mobile }}</view>
          <view v-if="item.isDefault" class="badge">默认</view>
        </view>
        <view class="detail">{{ item.detail }}</view>
        <button class="ghost-btn" @tap="showTodo">编辑地址</button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { rentalAddresses } from '@/sheep/helper/rentalDemo';

  const showTodo = () => {
    uni.showToast({ title: '地址保存接口待接入', icon: 'none' });
  };
</script>

<style lang="scss" scoped>
  .rental-page {
    min-height: 100vh;
    padding: 24rpx 24rpx 120rpx;
    background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  }

  .header-card,
  .address-card {
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

  .desc,
  .detail {
    margin-top: 12rpx;
    color: #64748b;
    font-size: 24rpx;
    line-height: 1.55;
  }

  .name-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
  }

  .name {
    color: #111827;
    font-size: 30rpx;
    font-weight: 800;
  }

  .badge {
    padding: 8rpx 16rpx;
    border-radius: 999rpx;
    background: #fff1e8;
    color: #ea580c;
    font-size: 22rpx;
  }

  .primary-btn,
  .ghost-btn {
    margin-top: 24rpx;
    height: 72rpx;
    border-radius: 999rpx;
    font-size: 26rpx;
    font-weight: 800;
    line-height: 72rpx;
  }

  .primary-btn {
    color: #ffffff;
    background: #111827;
  }

  .ghost-btn {
    color: #334155;
    background: #f1f5f9;
  }
</style>
