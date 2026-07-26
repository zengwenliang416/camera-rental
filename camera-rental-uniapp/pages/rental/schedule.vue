<template>
  <s-layout navbar="normal" title="租期选择">
    <view class="rental-page">
      <view class="hero-card">
        <view class="hero-label">Selected Gear</view>
        <view class="hero-title">{{ product.title }}</view>
        <view class="hero-desc">{{ product.subtitle }}</view>
      </view>

      <view class="form-card">
        <view class="section-title">选择租用日期</view>
        <view class="date-grid">
          <picker mode="date" :value="state.startDate" @change="state.startDate = $event.detail.value">
            <view class="date-box">
              <view class="label">计租开始</view>
              <view class="date-value">{{ state.startDate }}</view>
            </view>
          </picker>
          <picker mode="date" :value="state.endDate" @change="state.endDate = $event.detail.value">
            <view class="date-box">
              <view class="label">计租结束</view>
              <view class="date-value">{{ state.endDate }}</view>
            </view>
          </picker>
        </view>
        <view class="notice">发货、回仓和检测会形成设备占用周期；最终冲突检查必须由后端事务完成。</view>
        <button class="primary-btn full" @tap="checkAvailability">预检查可租状态</button>
      </view>

      <view v-if="state.checked" class="result-card">
        <view class="result-title">本地预检查通过</view>
        <view class="result-desc">预计租金 {{ estimatedAmount }}，押金 {{ formatCent(product.deposit) }}。</view>
        <view class="result-desc">这只是客户端展示，提交订单时后端会重新校验排期、金额和重复提交。</view>
        <button class="primary-btn full" @tap="goOrder">生成预订单</button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import dayjs from 'dayjs';
  import sheep from '@/sheep';
  import { createDefaultRentalDates, formatCent, getRentalProduct } from '@/sheep/helper/rentalDemo';

  const dates = createDefaultRentalDates();
  const state = reactive({
    productId: 'p4p',
    startDate: dates.startDate,
    endDate: dates.endDate,
    checked: false,
  });

  const product = computed(() => getRentalProduct(state.productId));
  const rentalDays = computed(() => {
    const start = dayjs(state.startDate);
    const end = dayjs(state.endDate);
    const diff = end.diff(start, 'day') + 1;
    return Math.max(diff, 1);
  });
  const estimatedAmount = computed(() => formatCent(rentalDays.value * product.value.rentPerDay));

  const checkAvailability = () => {
    if (dayjs(state.endDate).isBefore(dayjs(state.startDate))) {
      uni.showToast({ title: '结束日期不能早于开始日期', icon: 'none' });
      return;
    }
    state.checked = true;
    uni.showToast({ title: '预检查通过', icon: 'none' });
  };

  const goOrder = () => {
    sheep.$router.go('/pages/rental/orders', {
      productId: state.productId,
      startDate: state.startDate,
      endDate: state.endDate,
    });
  };

  onLoad((options) => {
    if (options?.productId) {
      state.productId = options.productId;
    }
  });
</script>

<style lang="scss" scoped>
  .rental-page {
    min-height: 100vh;
    padding: 24rpx 24rpx 120rpx;
    background:
      radial-gradient(circle at 80% 0%, rgba(251, 146, 60, 0.2), transparent 36%),
      linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  }

  .hero-card,
  .form-card,
  .result-card {
    border-radius: 30rpx;
    padding: 30rpx;
    box-shadow: 0 18rpx 42rpx rgba(15, 23, 42, 0.07);
  }

  .hero-card {
    color: #ffffff;
    background: linear-gradient(135deg, #0f172a 0%, #334155 58%, #f97316 100%);
  }

  .hero-label {
    font-size: 22rpx;
    letter-spacing: 4rpx;
    opacity: 0.72;
    text-transform: uppercase;
  }

  .hero-title {
    margin-top: 14rpx;
    font-size: 42rpx;
    font-weight: 800;
  }

  .hero-desc,
  .notice,
  .result-desc {
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.6;
  }

  .hero-desc {
    opacity: 0.82;
  }

  .form-card,
  .result-card {
    margin-top: 24rpx;
    background: rgba(255, 255, 255, 0.94);
  }

  .section-title,
  .result-title {
    color: #111827;
    font-size: 32rpx;
    font-weight: 800;
  }

  .date-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18rpx;
    margin-top: 22rpx;
  }

  .date-box {
    padding: 24rpx;
    border-radius: 22rpx;
    background: #f8fafc;
  }

  .label {
    color: #94a3b8;
    font-size: 22rpx;
  }

  .date-value {
    margin-top: 10rpx;
    color: #111827;
    font-size: 28rpx;
    font-weight: 800;
  }

  .notice,
  .result-desc {
    color: #64748b;
  }

  .primary-btn {
    height: 76rpx;
    border-radius: 999rpx;
    color: #ffffff;
    background: #111827;
    font-size: 28rpx;
    font-weight: 800;
    line-height: 76rpx;
  }

  .full {
    margin-top: 26rpx;
  }
</style>
