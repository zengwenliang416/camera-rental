<script setup lang="ts">
defineProps<{ step: number }>()
const { t } = useReturnPreferences()
const labels = computed(() => [
  t('progressOrder'),
  t('progressLogistics'),
  t('progressDevices'),
  t('progressPhotos'),
  t('progressSubmit')
])
</script>

<template>
  <ol class="return-progress" aria-label="登记步骤">
    <li v-for="(label, index) in labels" :key="label" :class="{ active: step === index, done: step > index }">
      <span>{{ step > index ? '✓' : String(index + 1).padStart(2, '0') }}</span>
      <strong>{{ label }}</strong>
    </li>
  </ol>
</template>
