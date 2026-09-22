<template>
  <view v-if="list" class="theme-row" @click="visible = true">
    <text>外观主题</text>
    <text class="theme-value">{{ themeStore.modeLabel }} ›</text>
  </view>
  <wd-cell v-else title="外观主题" :value="themeStore.modeLabel" is-link @click="visible = true" />
  <wd-action-sheet v-model="visible" title="外观主题" :actions="actions" cancel-text="取消" @select="selectMode" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useThemeStore } from '@/store/theme'
import type { ThemeMode } from '@/store/theme'

defineProps<{ list?: boolean }>()
const themeStore = useThemeStore()
const visible = ref(false)
const modes: ThemeMode[] = ['system', 'light', 'dark']
const actions = computed(() => [
  { name: '跟随系统', description: '手机切换深浅色时，捷租达同步切换' },
  { name: '浅色模式', description: '始终使用浅色' },
  { name: '深色模式', description: '始终使用深色' },
].map((action, index) => ({ ...action, name: `${action.name}${themeStore.themeMode === modes[index] ? ' ✓' : ''}` })))
function selectMode({ index }: { index: number }) {
  const mode = modes[index]
  if (mode)
    themeStore.setThemeMode(mode)
  visible.value = false
}
</script>

<style scoped>
.theme-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  padding: 28rpx 0;
  border-bottom: 2rpx solid var(--staff-border);
  font-size: 28rpx;
}
.theme-value {
  color: var(--staff-muted);
  font-size: 24rpx;
  text-align: right;
}
</style>
