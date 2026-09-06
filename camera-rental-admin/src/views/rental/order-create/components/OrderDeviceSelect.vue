<template>
  <div class="order-device-select">
    <el-select
      ref="selectRef"
      v-model="selectedIds"
      class="w-full"
      clearable
      collapse-tags
      collapse-tags-tooltip
      filterable
      multiple
      remote
      remote-show-suffix
      :reserve-keyword="false"
      :disabled="disabled"
      :loading="loading"
      :placeholder="
        disabled
          ? t('rental.orderCreate.itemDevicePeriodFirst')
          : t('rental.orderCreate.itemDevicePlaceholder')
      "
      :remote-method="handleSearch"
      @change="handleSelectionChange"
      @visible-change="handleVisibleChange"
    >
      <el-option
        v-for="device in visibleOptions"
        :key="device.id"
        :disabled="!isSelectable(device)"
        :label="deviceLabel(device)"
        :value="device.id"
      />
      <template #footer>
        <el-button v-if="loadError" link type="primary" @click="loadDevices(nextPage)">
          {{ t('rental.common.retry') }}
        </el-button>
        <el-button
          v-else-if="options.length < total"
          link
          type="primary"
          :loading="loading"
          @click="loadDevices(nextPage)"
        >
          {{ t('rental.orderCreate.itemDeviceMore') }}
        </el-button>
      </template>
    </el-select>
    <el-text v-if="loadError" class="mt-4px block" type="danger">
      {{ t('rental.orderCreate.itemDeviceLoadError') }}
    </el-text>
    <el-text v-else-if="modelValue.length" class="mt-4px block" type="info">
      {{
        t('rental.orderCreate.itemDeviceSelected', {
          count: modelValue.length,
          modelCode: selectedModelCode
        })
      }}
    </el-text>
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import type { SelectInstance } from 'element-plus'
import { getRentalDevicePage, type RentalDeviceVO } from '@/api/rental/device'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { getRentalLabelKey } from '@/utils/rentalLabels'

defineOptions({ name: 'RentalOrderDeviceSelect' })

const props = withDefaults(
  defineProps<{
    modelValue: RentalDeviceVO[]
    disabled?: boolean
    excludedDeviceIds?: number[]
  }>(),
  {
    disabled: false,
    excludedDeviceIds: () => []
  }
)

const emit = defineEmits<{
  'update:modelValue': [devices: RentalDeviceVO[]]
}>()

const { t } = useI18n()
const message = useMessage()
const selectRef = ref<SelectInstance>()
const selectedIds = ref<number[]>([])
const options = ref<RentalDeviceVO[]>([])
const loading = ref(false)
const loadError = ref(false)
const total = ref(0)
const nextPage = ref(1)
const keyword = ref('')
let requestVersion = 0

const selectedModelCode = computed(() => props.modelValue[0]?.equipmentModelCode || '')
const excludedDeviceIdSet = computed(() => new Set(props.excludedDeviceIds))
const visibleOptions = computed(() => [
  ...new Map([...props.modelValue, ...options.value].map((device) => [device.id, device])).values()
])

const deviceLabel = (device: RentalDeviceVO) =>
  [
    device.deviceNo,
    device.serialNumber,
    device.equipmentModelCode,
    device.status ? t(getRentalLabelKey('device', device.status)) : ''
  ]
    .filter(Boolean)
    .join(' / ')

const isSelectable = (device: RentalDeviceVO) => {
  const selectedHere = selectedIds.value.includes(device.id)
  return (
    device.enabled !== false &&
    device.status === 'AVAILABLE' &&
    (!selectedModelCode.value || device.equipmentModelCode === selectedModelCode.value) &&
    (selectedHere || !excludedDeviceIdSet.value.has(device.id))
  )
}

const invalidateRequest = () => {
  requestVersion += 1
  loading.value = false
}

const loadDevices = async (pageNo = 1) => {
  if (loading.value || props.disabled) return
  const version = ++requestVersion
  loading.value = true
  loadError.value = false
  try {
    const page = await getRentalDevicePage({
      pageNo,
      pageSize: 20,
      keyword: keyword.value || undefined,
      equipmentModelCode: selectedModelCode.value || undefined,
      enabled: true
    })
    if (version !== requestVersion) return
    const previous = pageNo === 1 ? [] : options.value
    options.value = [
      ...new Map([...previous, ...(page.list || [])].map((device) => [device.id, device])).values()
    ]
    total.value = page.total
    nextPage.value = pageNo + 1
  } catch {
    if (version === requestVersion) loadError.value = true
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

const resetOptions = () => {
  invalidateRequest()
  options.value = []
  total.value = 0
  nextPage.value = 1
  loadError.value = false
}

const handleSearch = (query: string) => {
  keyword.value = query.trim()
  resetOptions()
  void loadDevices()
}

const handleVisibleChange = (visible: boolean) => {
  if (visible && options.value.length === 0) {
    void loadDevices()
  }
}

const handleSelectionChange = (ids: number[]) => {
  const devices = ids
    .map((id) => visibleOptions.value.find((device) => device.id === id))
    .filter((device): device is RentalDeviceVO => Boolean(device))
  const modelCode = devices[0]?.equipmentModelCode
  const sameModelDevices = modelCode
    ? devices.filter((device) => device.equipmentModelCode === modelCode)
    : devices
  if (sameModelDevices.length !== devices.length) {
    message.warning(t('rental.orderCreate.itemDeviceSameModel'))
  }
  selectedIds.value = sameModelDevices.map((device) => device.id)
  emit('update:modelValue', sameModelDevices)
  keyword.value = ''
  void nextTick(() => {
    selectRef.value?.blur()
    resetOptions()
  })
}

watch(
  () => props.modelValue.map((device) => device.id),
  (ids) => {
    selectedIds.value = ids
  },
  { immediate: true }
)

watch(
  () => props.disabled,
  (disabled) => {
    if (disabled) resetOptions()
  }
)

onBeforeUnmount(invalidateRequest)
</script>

<style scoped>
.order-device-select {
  width: min(520px, 100%);
}
</style>
