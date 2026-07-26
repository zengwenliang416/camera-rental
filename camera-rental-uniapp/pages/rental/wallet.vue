<template>
  <s-layout navbar="normal" title="押金与钱包">
    <view class="rental-page">
      <view class="wallet-card">
        <view class="label">Deposit Wallet</view>
        <view class="amount">¥0.00</view>
        <view class="desc">押金冻结、退款和赔偿都必须由后端支付/订单状态驱动。</view>
      </view>
      <view class="panel">
        <view class="row">
          <view>
            <view class="row-title">待冻结押金</view>
            <view class="row-desc">下单支付前由后端返回实际金额</view>
          </view>
          <view class="row-value">¥0.00</view>
        </view>
        <view class="row">
          <view>
            <view class="row-title">待退押金</view>
            <view class="row-desc">回仓检测完成后进入退款流程</view>
          </view>
          <view class="row-value">¥0.00</view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<style lang="scss" scoped>
  .rental-page {
    min-height: 100vh;
    padding: 24rpx 24rpx 120rpx;
    background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  }

  .wallet-card {
    padding: 36rpx;
    border-radius: 32rpx;
    color: #ffffff;
    background: linear-gradient(135deg, #111827 0%, #334155 58%, #f97316 100%);
    box-shadow: 0 22rpx 54rpx rgba(15, 23, 42, 0.18);
  }

  .label {
    font-size: 22rpx;
    letter-spacing: 4rpx;
    opacity: 0.72;
    text-transform: uppercase;
  }

  .amount {
    margin-top: 18rpx;
    font-size: 56rpx;
    font-weight: 900;
  }

  .desc {
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.55;
    opacity: 0.82;
  }

  .panel {
    margin-top: 24rpx;
    overflow: hidden;
    border-radius: 28rpx;
    background: #ffffff;
    box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.06);
  }

  .row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
    padding: 30rpx;
    border-bottom: 1rpx solid #eef2f7;
  }

  .row:last-child {
    border-bottom: 0;
  }

  .row-title {
    color: #111827;
    font-size: 30rpx;
    font-weight: 800;
  }

  .row-desc {
    margin-top: 8rpx;
    color: #64748b;
    font-size: 24rpx;
  }

  .row-value {
    color: #f97316;
    font-size: 28rpx;
    font-weight: 900;
  }
</style>
