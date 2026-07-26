<template>
  <s-layout navbar="normal" title="物流进度">
    <view class="rental-page">
      <view class="input-card">
        <view class="title">物流与设备流转</view>
        <view class="desc">客户端只展示物流状态；设备 SN 绑定、发货和回仓必须由员工端/后端校验。</view>
        <input v-model="state.waybillNo" class="input" placeholder="输入或粘贴运单号" />
        <button class="primary-btn" @tap="track">查询物流</button>
      </view>

      <view class="timeline-card">
        <view v-for="(item, index) in timeline" :key="item.title" class="timeline-row">
          <view class="dot" :class="{ active: index < 2 }"></view>
          <view>
            <view class="step-title">{{ item.title }}</view>
            <view class="step-desc">{{ item.desc }}</view>
          </view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';

  const state = reactive({
    waybillNo: 'SF5113560342626',
  });

  const timeline = [
    { title: '仓库拣货', desc: '员工扫码确认具体设备 SN 和配件清单。' },
    { title: '已交快递', desc: '运单号录入后，后端记录发货事件并通知客户。' },
    { title: '客户签收', desc: '签收次日可作为默认计租开始，需要后端规则确认。' },
    { title: '客户发回', desc: '客户提交回寄运单，仓库等待回仓。' },
    { title: '回仓检测', desc: '检测完成后释放设备占用周期。' },
  ];

  const track = () => {
    uni.showToast({
      title: state.waybillNo ? '物流查询接口待接入' : '请先输入运单号',
      icon: 'none',
    });
  };
</script>

<style lang="scss" scoped>
  .rental-page {
    min-height: 100vh;
    padding: 24rpx 24rpx 120rpx;
    background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%);
  }

  .input-card,
  .timeline-card {
    border: 1rpx solid rgba(15, 23, 42, 0.08);
    border-radius: 28rpx;
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.06);
  }

  .input-card {
    padding: 32rpx;
  }

  .title {
    color: #111827;
    font-size: 40rpx;
    font-weight: 800;
  }

  .desc {
    margin-top: 12rpx;
    color: #64748b;
    font-size: 24rpx;
    line-height: 1.55;
  }

  .input {
    margin-top: 24rpx;
    height: 76rpx;
    padding: 0 24rpx;
    border-radius: 20rpx;
    background: #f1f5f9;
    color: #111827;
    font-size: 28rpx;
  }

  .primary-btn {
    margin-top: 24rpx;
    height: 76rpx;
    border-radius: 999rpx;
    color: #ffffff;
    background: #111827;
    font-size: 28rpx;
    font-weight: 800;
    line-height: 76rpx;
  }

  .timeline-card {
    margin-top: 24rpx;
    padding: 30rpx;
  }

  .timeline-row {
    display: flex;
    gap: 22rpx;
    padding-bottom: 28rpx;
  }

  .timeline-row:last-child {
    padding-bottom: 0;
  }

  .dot {
    width: 22rpx;
    height: 22rpx;
    margin-top: 8rpx;
    border-radius: 50%;
    background: #cbd5e1;
  }

  .dot.active {
    background: #f97316;
  }

  .step-title {
    color: #111827;
    font-size: 30rpx;
    font-weight: 800;
  }

  .step-desc {
    margin-top: 8rpx;
    color: #64748b;
    font-size: 24rpx;
    line-height: 1.55;
  }
</style>
