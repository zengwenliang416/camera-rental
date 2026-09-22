<script setup lang="ts">
/* eslint-disable brace-style */ // 原因：unibest 官方维护的代码，尽量不要大概，避免难以合并
// i-carbon-code
import type { CustomTabBarItem } from './types'
import { customTabbarEnable, needHideNativeTabbar, tabbarCacheEnable } from './config'
import { tabbarList, tabbarStore } from './store'

// #ifdef MP-WEIXIN
// 将自定义节点设置成虚拟的（去掉自定义组件包裹层），更加接近Vue组件的表现，能更好的使用flex属性
defineOptions({
  virtualHost: true,
})
// #endif

/**
 * 中间的鼓包tabbarItem的点击事件
 */
function handleClick(index: number) {
  const item = tabbarList[index]
  if (!item || !tabbarStore.isTabbarItemVisible(index)) {
    return
  }
  // 当前高亮和真实页面都已经是目标 tab 时，不重复跳转
  if (index === tabbarStore.curIdx && tabbarStore.isCurrentRouteTabbarItem(index)) {
    return
  }
  const url = item.pagePath
  const prevIdx = tabbarStore.curIdx
  const syncTabbarAfterNavigation = () => {
    tabbarStore.syncCurIdxByCurrentPageAsync()
  }
  const restoreTabbarWhenNavigationFailed = () => {
    tabbarStore.setCurIdx(prevIdx)
  }
  tabbarStore.setCurIdx(index)
  if (tabbarCacheEnable) {
    uni.switchTab({
      url,
      success: syncTabbarAfterNavigation,
      fail: restoreTabbarWhenNavigationFailed,
    })
  }
  else {
    uni.navigateTo({
      url,
      success: syncTabbarAfterNavigation,
      fail: restoreTabbarWhenNavigationFailed,
    })
  }
}
// #ifndef MP-WEIXIN || MP-ALIPAY
// 因为有了 custom:true， 微信里面不需要多余的hide操作
onLoad(() => {
  // 解决原生 tabBar 未隐藏导致有2个 tabBar 的问题
  needHideNativeTabbar
  && uni.hideTabBar({
    fail(err) {
      console.log('hideTabBar fail: ', err)
    },
    success(res) {
      // console.log('hideTabBar success: ', res)
    },
  })
})
// #endif

// #ifdef MP-ALIPAY
onMounted(() => {
  // 解决支付宝自定义tabbar 未隐藏导致有2个 tabBar 的问题; 注意支付宝很特别，需要在 onMounted 钩子调用
  customTabbarEnable // 另外，支付宝里面，只要是 customTabbar 都需要隐藏
  && uni.hideTabBar({
    fail(err) {
      console.log('hideTabBar fail: ', err)
    },
    success(res) {
      // console.log('hideTabBar success: ', res)
    },
  })
})
// #endif
const activeColor = 'var(--staff-accent-text)'
const inactiveColor = 'var(--staff-muted)'
function getColorByIndex(index: number) {
  return tabbarStore.curIdx === index ? activeColor : inactiveColor
}

function getImageByIndex(index: number, item: CustomTabBarItem) {
  if (!item.iconActive) {
    console.warn('image 模式下，需要配置 iconActive (高亮时的图片），否则无法切换高亮图片')
    return item.icon
  }
  return tabbarStore.curIdx === index ? item.iconActive : item.icon
}
</script>

<template>
  <view v-if="customTabbarEnable" class="h-50px pb-safe">
    <view class="border-and-fixed" @touchmove.stop.prevent>
      <view class="h-50px flex items-center">
        <template v-for="(item, index) in tabbarList" :key="index">
          <view
            v-if="tabbarStore.isTabbarItemVisible(index)"
            class="flex flex-1 flex-col items-center justify-center"
            :style="{ color: getColorByIndex(index) }"
            @click="handleClick(index)"
          >
            <view v-if="item.isBulge" class="relative flex flex-col items-center">
              <view class="staff-scan-bulge">
                <view :class="item.icon" class="text-22px text-white" />
              </view>
              <view class="mt-22px text-12px">
                {{ item.text }}
              </view>
            </view>
            <view v-else class="relative px-3 text-center">
              <template v-if="item.iconType === 'uiLib'">
                <!-- TODO: 以下内容请根据选择的UI库自行替换 -->
                <!-- 如：<wd-icon name="home" /> (https://wot-ui.cn/component/icon.html) -->
                <!-- 如：<uv-icon name="home" /> (https://www.uvui.cn/components/icon.html) -->
                <!-- 如：<sar-icon name="image" /> (https://sard.wzt.zone/sard-uniapp-docs/components/icon)(sar没有home图标^_^) -->
                <wd-icon :name="item.icon" size="20" />
              </template>
              <template v-if="item.iconType === 'unocss' || item.iconType === 'iconfont'">
                <view :class="item.icon" class="text-20px" />
              </template>
              <template v-if="item.iconType === 'image'">
                <image :src="getImageByIndex(index, item)" mode="scaleToFill" class="h-20px w-20px" />
              </template>
              <view class="mt-2px text-12px">
                {{ item.text }}
              </view>
              <!-- 角标显示 -->
              <view v-if="item.badge">
                <template v-if="item.badge === 'dot'">
                  <view class="absolute right-0 top-0 h-2 w-2 rounded-full bg-#f56c6c" />
                </template>
                <template v-else>
                  <view class="absolute top-0 box-border h-5 min-w-5 center rounded-full bg-#f56c6c px-1 text-center text-xs text-white -right-3">
                    {{ item.badge > 99 ? '99+' : item.badge }}
                  </view>
                </template>
              </view>
            </view>
          </view>
        </template>
      </view>

      <view class="pb-safe" />
    </view>
  </view>
</template>

<style scoped lang="scss">
.border-and-fixed {
  background: var(--staff-surface);
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;

  border-top: 1px solid var(--staff-border);
  box-sizing: border-box;
}
.staff-scan-bulge {
  position: absolute;
  top: -36px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 4px solid var(--staff-surface);
  border-radius: 50%;
  background: var(--staff-accent, #e10600);
  box-shadow: 0 8px 16px rgba(225, 6, 0, 0.28);
}
</style>
