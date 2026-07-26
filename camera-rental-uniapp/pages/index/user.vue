<!-- 个人中心：支持装修 -->
<template>
  <s-layout
    title="我的"
    tabbar="/pages/index/user"
    navbar="custom"
    :bgStyle="template.page"
    :navbarStyle="template.navigationBar"
    onShareAppMessage
  >
    <s-block
      v-for="(item, index) in template.components"
      :key="index"
      :styles="item.property.style"
    >
      <s-block-item :type="item.id" :data="item.property" :styles="item.property.style" />
    </s-block>
    <view v-if="isFallbackUser" class="rental-user">
      <view class="profile-card">
        <view>
          <view class="profile-label">Rental Account</view>
          <view class="profile-title">本地客户中心</view>
          <view class="profile-desc">当前为未登录联调模式，可浏览入口和验证路由。</view>
        </view>
        <button class="login-btn" @tap="showLogin">登录</button>
      </view>

      <view class="menu-panel">
        <view class="menu-row" @tap="go('/pages/rental/orders')">
          <view>
            <view class="menu-title">我的租赁订单</view>
            <view class="menu-desc">查看待支付、待发货、租用中和售后</view>
          </view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-row" @tap="go('/pages/rental/address')">
          <view>
            <view class="menu-title">收货地址</view>
            <view class="menu-desc">维护发货和归还地址</view>
          </view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-row" @tap="go('/pages/rental/favorites')">
          <view>
            <view class="menu-title">收藏器材</view>
            <view class="menu-desc">收藏常租相机和镜头</view>
          </view>
          <view class="menu-arrow">›</view>
        </view>
        <view class="menu-row" @tap="go('/pages/rental/wallet')">
          <view>
            <view class="menu-title">押金与钱包</view>
            <view class="menu-desc">押金冻结、退款和余额记录</view>
          </view>
          <view class="menu-arrow">›</view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed } from 'vue';
  import { onShow, onPageScroll, onPullDownRefresh } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';

  // 隐藏原生tabBar
  uni.hideTabBar({
    fail: () => {},
  });

  const template = computed(() => sheep.$store('app').template.user);
  const isFallbackUser = computed(() => !template.value?.components?.length);

  const go = (url) => {
    sheep.$router.go(url);
  };

  const showLogin = () => {
    showAuthModal('accountLogin');
  };

  onShow(() => {
    sheep.$store('user').updateUserData();
  });

  onPullDownRefresh(() => {
    sheep.$store('user').updateUserData();
    setTimeout(function () {
      uni.stopPullDownRefresh();
    }, 800);
  });

  onPageScroll(() => {});
</script>

<style lang="scss" scoped>
  .rental-user {
    min-height: 100vh;
    padding: 28rpx 24rpx 140rpx;
    background:
      radial-gradient(circle at 82% 0%, rgba(255, 122, 69, 0.18), transparent 34%),
      linear-gradient(180deg, #f7fbff 0%, #f6f7fb 48%, #ffffff 100%);
  }

  .profile-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24rpx;
    padding: 34rpx;
    border-radius: 32rpx;
    color: #fff;
    background: linear-gradient(135deg, #111827 0%, #334155 58%, #ff7a45 100%);
    box-shadow: 0 22rpx 54rpx rgba(15, 23, 42, 0.18);
  }

  .profile-label {
    font-size: 22rpx;
    letter-spacing: 4rpx;
    opacity: 0.72;
    text-transform: uppercase;
  }

  .profile-title {
    margin-top: 12rpx;
    font-size: 40rpx;
    font-weight: 700;
  }

  .profile-desc {
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.55;
    opacity: 0.82;
  }

  .login-btn {
    flex: 0 0 auto;
    margin: 0;
    padding: 0 30rpx;
    height: 66rpx;
    border-radius: 999rpx;
    color: #1f2937;
    font-size: 26rpx;
    font-weight: 700;
    line-height: 66rpx;
    background: #fff;
  }

  .menu-panel {
    margin-top: 28rpx;
    overflow: hidden;
    border: 1rpx solid rgba(15, 23, 42, 0.08);
    border-radius: 28rpx;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 18rpx 38rpx rgba(15, 23, 42, 0.06);
  }

  .menu-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
    padding: 28rpx;
    border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  }

  .menu-row:last-child {
    border-bottom: 0;
  }

  .menu-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #1f2937;
  }

  .menu-desc {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: #6b7280;
  }

  .menu-arrow {
    color: #ff7a45;
    font-size: 42rpx;
    font-weight: 700;
  }
</style>
