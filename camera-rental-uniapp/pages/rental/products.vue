<template>
  <s-layout navbar="normal" title="租赁器材">
    <view class="rental-page">
      <view class="search-card">
        <view class="search-title">选择要租的器材</view>
        <view class="search-desc">当前为本地演示数据；真实库存、排期和金额以后端 App API 为准。</view>
        <input
          v-model="state.keyword"
          class="search-input"
          placeholder="搜索相机、镜头、无人机"
          confirm-type="search"
        />
      </view>

      <scroll-view class="category-scroll" scroll-x>
        <view class="category-list">
          <view
            v-for="item in categories"
            :key="item"
            class="category-chip"
            :class="{ active: state.category === item }"
            @tap="state.category = item"
          >
            {{ item }}
          </view>
        </view>
      </scroll-view>

      <view class="product-list">
        <view v-for="item in filteredProducts" :key="item.id" class="product-card">
          <view class="product-top">
            <view>
              <view class="product-category">{{ item.category }}</view>
              <view class="product-title">{{ item.title }}</view>
              <view class="product-subtitle">{{ item.subtitle }}</view>
            </view>
            <view class="status-pill">{{ item.status }}</view>
          </view>
          <view class="tag-row">
            <text v-for="tag in item.tags" :key="tag" class="tag">{{ tag }}</text>
          </view>
          <view class="meta-row">
            <view>
              <view class="meta-label">日租金</view>
              <view class="price">{{ formatCent(item.rentPerDay) }}/天</view>
            </view>
            <view>
              <view class="meta-label">押金</view>
              <view class="meta-value">{{ formatCent(item.deposit) }}</view>
            </view>
            <view>
              <view class="meta-label">库存</view>
              <view class="meta-value">{{ item.stockText }}</view>
            </view>
          </view>
          <view class="action-row">
            <button class="ghost-btn" @tap="goSchedule(item.id)">查看排期</button>
            <button class="primary-btn" @tap="goSchedule(item.id)">选租期</button>
          </view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import sheep from '@/sheep';
  import { rentalProducts, formatCent } from '@/sheep/helper/rentalDemo';

  const state = reactive({
    keyword: '',
    category: '全部',
  });

  const categories = ['全部', '相机', '镜头', '无人机', '配件'];

  const filteredProducts = computed(() => {
    const keyword = state.keyword.trim().toLowerCase();
    return rentalProducts.filter((item) => {
      const matchCategory = state.category === '全部' || item.category === state.category;
      const text = `${item.title} ${item.subtitle} ${item.category}`.toLowerCase();
      return matchCategory && (!keyword || text.includes(keyword));
    });
  });

  const goSchedule = (productId) => {
    sheep.$router.go('/pages/rental/schedule', { productId });
  };
</script>

<style lang="scss" scoped>
  .rental-page {
    min-height: 100vh;
    padding: 24rpx 24rpx 120rpx;
    background: linear-gradient(180deg, #fff7ed 0%, #f8fafc 42%, #ffffff 100%);
  }

  .search-card,
  .product-card {
    border: 1rpx solid rgba(15, 23, 42, 0.08);
    border-radius: 28rpx;
    background: rgba(255, 255, 255, 0.92);
    box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.06);
  }

  .search-card {
    padding: 32rpx;
  }

  .search-title {
    color: #111827;
    font-size: 40rpx;
    font-weight: 800;
  }

  .search-desc {
    margin-top: 12rpx;
    color: #64748b;
    font-size: 24rpx;
    line-height: 1.55;
  }

  .search-input {
    margin-top: 24rpx;
    height: 76rpx;
    padding: 0 24rpx;
    border-radius: 20rpx;
    background: #f1f5f9;
    color: #111827;
    font-size: 28rpx;
  }

  .category-scroll {
    margin: 24rpx -24rpx 0;
    white-space: nowrap;
  }

  .category-list {
    display: flex;
    gap: 16rpx;
    padding: 0 24rpx;
  }

  .category-chip {
    padding: 16rpx 26rpx;
    border-radius: 999rpx;
    background: #ffffff;
    color: #64748b;
    font-size: 26rpx;
  }

  .category-chip.active {
    background: #111827;
    color: #ffffff;
  }

  .product-list {
    display: flex;
    flex-direction: column;
    gap: 22rpx;
    margin-top: 24rpx;
  }

  .product-card {
    padding: 28rpx;
  }

  .product-top,
  .meta-row,
  .action-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
  }

  .product-category,
  .meta-label {
    color: #94a3b8;
    font-size: 22rpx;
  }

  .product-title {
    margin-top: 8rpx;
    color: #111827;
    font-size: 34rpx;
    font-weight: 800;
  }

  .product-subtitle {
    margin-top: 10rpx;
    color: #64748b;
    font-size: 24rpx;
    line-height: 1.5;
  }

  .status-pill {
    flex: 0 0 auto;
    padding: 10rpx 18rpx;
    border-radius: 999rpx;
    background: #fff1e8;
    color: #ea580c;
    font-size: 22rpx;
    font-weight: 700;
  }

  .tag-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 20rpx;
  }

  .tag {
    padding: 8rpx 14rpx;
    border-radius: 999rpx;
    background: #f8fafc;
    color: #475569;
    font-size: 22rpx;
  }

  .meta-row {
    margin-top: 24rpx;
    padding-top: 22rpx;
    border-top: 1rpx solid #eef2f7;
  }

  .price {
    margin-top: 6rpx;
    color: #f97316;
    font-size: 32rpx;
    font-weight: 800;
  }

  .meta-value {
    margin-top: 6rpx;
    color: #111827;
    font-size: 26rpx;
    font-weight: 700;
  }

  .action-row {
    margin-top: 24rpx;
  }

  .ghost-btn,
  .primary-btn {
    flex: 1;
    height: 72rpx;
    border-radius: 999rpx;
    font-size: 26rpx;
    font-weight: 700;
    line-height: 72rpx;
  }

  .ghost-btn {
    color: #334155;
    background: #f1f5f9;
  }

  .primary-btn {
    color: #ffffff;
    background: #111827;
  }
</style>
