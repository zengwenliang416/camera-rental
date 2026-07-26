<template>
  <s-layout navbar="normal" title="租赁订单">
    <view class="rental-page">
      <view class="summary-card">
        <view class="summary-title">我的租赁订单</view>
        <view class="summary-desc">这里先展示租赁订单状态骨架；真实数据接入 `/app-api/rental/**` 后替换。</view>
        <button class="primary-btn" @tap="goProducts">继续选器材</button>
      </view>

      <view class="tabs">
        <view
          v-for="tab in tabs"
          :key="tab"
          class="tab"
          :class="{ active: state.tab === tab }"
          @tap="state.tab = tab"
        >
          {{ tab }}
        </view>
      </view>

      <view class="order-list">
        <view v-for="item in filteredOrders" :key="item.id" class="order-card">
          <view class="order-head">
            <view>
              <view class="order-id">{{ item.id }}</view>
              <view class="order-title">{{ item.title }}</view>
            </view>
            <view class="status">{{ item.status }}</view>
          </view>
          <view class="order-meta">租期：{{ item.dateRange }}</view>
          <view class="order-meta">物流：{{ item.logistics }}</view>
          <view class="order-foot">
            <view class="amount">{{ formatCent(item.amount) }}</view>
            <button class="ghost-btn" @tap="showDetail(item)">查看详情</button>
            <button class="primary-btn small" @tap="goLogistics">物流进度</button>
          </view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import sheep from '@/sheep';
  import { formatCent, rentalOrders } from '@/sheep/helper/rentalDemo';

  const tabs = ['全部', '待支付', '待发货', '租用中'];
  const state = reactive({
    tab: '全部',
  });

  const filteredOrders = computed(() => {
    if (state.tab === '全部') return rentalOrders;
    return rentalOrders.filter((item) => item.status === state.tab);
  });

  const goProducts = () => {
    sheep.$router.go('/pages/rental/products');
  };

  const goLogistics = () => {
    sheep.$router.go('/pages/rental/logistics');
  };

  const showDetail = (item) => {
    uni.showModal({
      title: item.id,
      content: '订单详情页待 App API 接入后补齐；当前先验证列表、状态和物流入口。',
      showCancel: false,
    });
  };
</script>

<style lang="scss" scoped>
  .rental-page {
    min-height: 100vh;
    padding: 24rpx 24rpx 120rpx;
    background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  }

  .summary-card,
  .order-card {
    border: 1rpx solid rgba(15, 23, 42, 0.08);
    border-radius: 28rpx;
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.06);
  }

  .summary-card {
    padding: 32rpx;
  }

  .summary-title {
    color: #111827;
    font-size: 40rpx;
    font-weight: 800;
  }

  .summary-desc,
  .order-meta {
    margin-top: 12rpx;
    color: #64748b;
    font-size: 24rpx;
    line-height: 1.55;
  }

  .tabs {
    display: flex;
    gap: 14rpx;
    margin-top: 24rpx;
    overflow-x: auto;
  }

  .tab {
    flex: 0 0 auto;
    padding: 14rpx 24rpx;
    border-radius: 999rpx;
    background: #ffffff;
    color: #64748b;
    font-size: 26rpx;
  }

  .tab.active {
    background: #111827;
    color: #ffffff;
  }

  .order-list {
    display: flex;
    flex-direction: column;
    gap: 20rpx;
    margin-top: 24rpx;
  }

  .order-card {
    padding: 28rpx;
  }

  .order-head,
  .order-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18rpx;
  }

  .order-id {
    color: #94a3b8;
    font-size: 22rpx;
  }

  .order-title {
    margin-top: 8rpx;
    color: #111827;
    font-size: 30rpx;
    font-weight: 800;
  }

  .status {
    flex: 0 0 auto;
    color: #ea580c;
    font-size: 24rpx;
    font-weight: 800;
  }

  .order-foot {
    margin-top: 24rpx;
  }

  .amount {
    color: #f97316;
    font-size: 32rpx;
    font-weight: 900;
  }

  .primary-btn,
  .ghost-btn {
    height: 70rpx;
    border-radius: 999rpx;
    font-size: 24rpx;
    font-weight: 800;
    line-height: 70rpx;
  }

  .primary-btn {
    margin-top: 24rpx;
    color: #ffffff;
    background: #111827;
  }

  .primary-btn.small,
  .ghost-btn {
    margin: 0;
    padding: 0 22rpx;
  }

  .ghost-btn {
    color: #334155;
    background: #f1f5f9;
  }
</style>
