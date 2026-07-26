<!-- 首页，支持店铺装修 -->
<template>
  <view v-if="template">
    <s-layout
      title="首页"
      navbar="custom"
      tabbar="/pages/index/index"
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
      <view v-if="isFallbackHome" class="rental-home">
        <view class="hero-card">
          <view class="eyebrow">Camera Rental</view>
          <view class="hero-title">相机租赁客户端</view>
          <view class="hero-desc">本地联调模式已启动，后端接口指向 127.0.0.1:48080。</view>
        </view>
        <view class="quick-grid">
          <view class="quick-card" hover-class="quick-card--hover" @tap="handleQuickAction('products')">
            <view class="quick-title">器材浏览</view>
            <view class="quick-desc">相机、镜头、无人机和配件</view>
          </view>
          <view class="quick-card" hover-class="quick-card--hover" @tap="handleQuickAction('schedule')">
            <view class="quick-title">租期选择</view>
            <view class="quick-desc">按后端排期校验可租状态</view>
          </view>
          <view class="quick-card" hover-class="quick-card--hover" @tap="handleQuickAction('orders')">
            <view class="quick-title">订单支付</view>
            <view class="quick-desc">金额和库存以后端为准</view>
          </view>
          <view class="quick-card" hover-class="quick-card--hover" @tap="handleQuickAction('logistics')">
            <view class="quick-title">物流进度</view>
            <view class="quick-desc">发货、归还、检测全流程</view>
          </view>
        </view>
      </view>
    </s-layout>
  </view>
</template>

<script setup>
  import { computed } from 'vue';
  import { onLoad, onShow, onPageScroll, onPullDownRefresh } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import $share from '@/sheep/platform/share';
  // 隐藏原生tabBar
  uni.hideTabBar({
    fail: () => {},
  });

  const template = computed(() => sheep.$store('app').template?.home);
  const isFallbackHome = computed(() => !template.value?.components?.length);

  const handleQuickAction = (action) => {
    const routeMap = {
      products: '/pages/rental/products',
      schedule: '/pages/rental/schedule',
      orders: '/pages/rental/orders',
      logistics: '/pages/rental/logistics',
    };
    if (routeMap[action]) {
      sheep.$router.go(routeMap[action]);
      return;
    }
  };
  // 在此处拦截改变一下首页轮播图 此处先写死后期复活 放到启动函数里
  // (async function() {
  // console.log('原代码首页定制化数据',template)
  // let {
  // 	data
  // } = await index2Api.decorate();
  // console.log('首页导航配置化过高无法兼容',JSON.parse(data[1].value))
  // 改变首页底部数据 但是没有通过数组id获取商品数据接口
  // let {
  // 	data: datas
  // } = await index2Api.spids();
  // template.value.data[9].data.goodsIds = datas.list.map(item => item.id);
  // template.value.data[0].data.list = JSON.parse(data[0].value).map(item => {
  // 	return {
  // 		src: item.picUrl,
  // 		url: item.url,
  // 		title: item.name,
  // 		type: "image"
  // 	}
  // })
  // }())

  onLoad((options) => {
    // #ifdef MP
    // 小程序识别二维码
    if (options.scene) {
      const sceneParams = decodeURIComponent(options.scene).split('=');
      console.log('sceneParams=>', sceneParams);
      options[sceneParams[0]] = sceneParams[1];
    }
    // #endif

    // 预览模板
    if (options.templateId) {
      sheep.$store('app').init(options.templateId);
    }

    // 解析分享信息
    if (options.spm) {
      $share.decryptSpm(options.spm);
    }

    // 进入指定页面(完整页面路径)
    if (options.page) {
      sheep.$router.go(decodeURIComponent(options.page));
    }
  });

  onShow(async() => {
    // #ifdef APP-PLUS
    // ios首次授权网络，需要重新加载一次应用初始化
    // 可能需要考虑上uni.onNetworkStatusChange，uni.offNetworkStatusChange组合拳以及主动主动唤起权限申请
    // 一开始放app.vue，感觉负载太重，搬到这里来了。
    // 如果你的首页不是这个页面，需要把代码搬过去。
    if (sheep.$platform.os === 'ios') {
      if (await sheep.$platform.checkNetwork()) {
        await sheep.$store('app').init();
      }
    }
    // #endif
  });

  // 下拉刷新
  onPullDownRefresh(() => {
    sheep.$store('app').init();
    setTimeout(function () {
      uni.stopPullDownRefresh();
    }, 800);
  });

  onPageScroll(() => {});
</script>

<style lang="scss" scoped>
  .rental-home {
    min-height: 100vh;
    padding: 28rpx 24rpx 140rpx;
    background:
      radial-gradient(circle at 20% 0%, rgba(255, 130, 83, 0.22), transparent 34%),
      linear-gradient(180deg, #fff7ef 0%, #f5f7fb 46%, #ffffff 100%);
  }

  .hero-card {
    border-radius: 32rpx;
    padding: 40rpx 34rpx;
    color: #fff;
    background: linear-gradient(135deg, #1f2937 0%, #3b342c 48%, #ff7a45 100%);
    box-shadow: 0 24rpx 60rpx rgba(31, 41, 55, 0.18);
  }

  .eyebrow {
    font-size: 22rpx;
    letter-spacing: 4rpx;
    opacity: 0.78;
    text-transform: uppercase;
  }

  .hero-title {
    margin-top: 16rpx;
    font-size: 44rpx;
    font-weight: 700;
    line-height: 1.2;
  }

  .hero-desc {
    margin-top: 18rpx;
    max-width: 560rpx;
    font-size: 26rpx;
    line-height: 1.7;
    opacity: 0.86;
  }

  .quick-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20rpx;
    margin-top: 28rpx;
  }

  .quick-card {
    min-height: 150rpx;
    padding: 28rpx 24rpx;
    border: 1rpx solid rgba(31, 41, 55, 0.08);
    border-radius: 26rpx;
    background: rgba(255, 255, 255, 0.88);
    box-shadow: 0 16rpx 34rpx rgba(15, 23, 42, 0.06);
  }

  .quick-card--hover {
    transform: translateY(2rpx);
    opacity: 0.82;
  }

  .quick-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #1f2937;
  }

  .quick-desc {
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.45;
    color: #6b7280;
  }
</style>
