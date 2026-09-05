<template>
  <el-alert
    class="mb-12px"
    type="info"
    :closable="false"
    :title="t('rental.xianyu.deviceQrHint')"
  />
  <el-upload
    v-model:file-list="uploadFiles"
    accept="image/*"
    :auto-upload="false"
    :limit="1"
    :on-change="handleChange"
    :on-exceed="handleExceed"
    :on-remove="handleRemove"
    drag
  >
    <Icon icon="ep:upload-filled" class="mb-8px text-32px text-[var(--el-color-primary)]" />
    <div class="el-upload__text">{{ t('rental.xianyu.deviceQrUploadHint') }}</div>
    <template #tip>
      <div class="el-upload__tip">{{ t('rental.xianyu.deviceQrUploadTip') }}</div>
    </template>
  </el-upload>
  <div class="mt-12px flex flex-wrap gap-8px">
    <el-button type="primary" :disabled="!hasImage" :loading="decoding" @click="emit('decode')">
      {{ t('rental.xianyu.deviceQrDecode') }}
    </el-button>
  </div>
  <el-form class="mt-16px" label-position="top" :model="form">
    <el-form-item :label="t('rental.xianyu.deviceSearchLabel')">
      <el-select
        v-model="selectedDeviceNo"
        class="w-full"
        clearable
        filterable
        remote
        remote-show-suffix
        :debounce="0"
        :remote-method="handleDeviceSearch"
        :loading="devicesLoading"
        :placeholder="t('rental.xianyu.deviceCascadeDevicePlaceholder')"
        @visible-change="handleDeviceDropdown"
        @change="handleDeviceChange"
        @clear="handleDeviceClear"
      >
        <el-option
          v-for="device in visibleDeviceOptions"
          :key="device.id"
          :label="deviceLabel(device)"
          :value="device.deviceNo"
          :disabled="device.enabled === false"
        />
        <template #footer>
          <el-button v-if="devicesError" link type="primary" @click="loadDevices(nextPage)">
            {{ t('rental.common.retry') }}
          </el-button>
          <el-button
            v-else-if="deviceOptions.length < deviceTotal"
            link
            type="primary"
            :loading="devicesLoading"
            @click="loadDevices(nextPage)"
          >
            {{ t('rental.xianyu.deviceSearchMore') }}
          </el-button>
        </template>
      </el-select>
      <el-text v-if="devicesError" type="danger">{{ t('rental.common.loadError') }}</el-text>
    </el-form-item>
    <el-form-item :label="t('rental.xianyu.deviceCascadeLabel')">
      <el-space class="w-full" wrap>
        <el-select
          v-model="selectedCategoryCode"
          class="!w-240px"
          clearable
          :placeholder="t('rental.xianyu.deviceCascadeCategoryPlaceholder')"
          @change="handleCategoryChange"
        >
          <el-option
            v-for="category in catalog"
            :key="category.categoryCode"
            :label="category.categoryName"
            :value="category.categoryCode"
          />
        </el-select>
        <el-select
          v-model="selectedModelCode"
          class="!w-240px"
          clearable
          :disabled="!selectedCategoryCode"
          :placeholder="t('rental.xianyu.deviceCascadeModelPlaceholder')"
          @change="handleModelChange"
        >
          <el-option
            v-for="model in availableModels"
            :key="model.modelCode"
            :label="model.modelName"
            :value="model.modelCode"
          />
        </el-select>
        <el-button :disabled="!selectedCategoryCode && !selectedModelCode" @click="clearFilters">
          {{ t('rental.xianyu.deviceSearchClearFilters') }}
        </el-button>
        <el-button v-if="catalogError" link type="primary" @click="loadCatalog">
          {{ t('rental.common.retry') }}
        </el-button>
      </el-space>
      <el-text v-if="catalogError" type="danger">{{ t('rental.common.loadError') }}</el-text>
    </el-form-item>
    <el-form-item :label="t('rental.device.deviceNo')">
      <el-input
        v-model.trim="form.deviceNo"
        clearable
        autofocus
        :placeholder="t('rental.xianyu.deviceScanPlaceholder')"
      />
    </el-form-item>
  </el-form>
  <el-descriptions v-if="selectedDevice" class="mt-12px" :column="3" border>
    <el-descriptions-item :label="t('rental.device.deviceNo')">
      {{ selectedDevice.deviceNo }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.device.serialNumber')">
      {{ selectedDevice.serialNumber || '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('common.status')">
      {{ selectedDevice.status ? t(getRentalLabelKey('device', selectedDevice.status)) : '-' }}
    </el-descriptions-item>
  </el-descriptions>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { UploadFile, UploadFiles, UploadProps, UploadUserFile } from 'element-plus'
import {
  getRentalDeviceCatalog,
  getRentalDevicePage,
  type RentalDeviceCategoryVO,
  type RentalDeviceVO
} from '@/api/rental/device'
import { useI18n } from '@/hooks/web/useI18n'
import type { XianyuShipmentForm } from './xianyuShipWorkbenchTypes'
import { getRentalLabelKey } from '@/utils/rentalLabels'

defineOptions({ name: 'XianyuDeviceStep' })

const props = defineProps<{
  form: XianyuShipmentForm
  selectedDevice?: RentalDeviceVO
  hasImage: boolean
  decoding: boolean
}>()

const uploadFiles = defineModel<UploadUserFile[]>('uploadFiles', { required: true })
const emit = defineEmits<{
  imageChange: [uploadFile: UploadFile, uploadFiles: UploadFiles]
  imageRemove: []
  imageExceed: []
  decode: []
  deviceSelect: [device: RentalDeviceVO]
  deviceClear: []
}>()
const { t } = useI18n()

const catalog = ref<RentalDeviceCategoryVO[]>([])
const catalogError = ref(false)
const selectedCategoryCode = ref('')
const selectedModelCode = ref('')
const selectedDeviceNo = ref('')
const deviceOptions = ref<RentalDeviceVO[]>([])
const devicesLoading = ref(false)
const devicesError = ref(false)
const deviceTotal = ref(0)
const nextPage = ref(1)
const keyword = ref('')
let requestVersion = 0
let searchTimer: ReturnType<typeof setTimeout> | undefined

const availableModels = computed(() => {
  const category = catalog.value.find((item) => item.categoryCode === selectedCategoryCode.value)
  return (category?.models || []).filter(
    (model) => model.enabled !== false || model.modelCode === selectedModelCode.value
  )
})

// 保留当前设备的选项，避免翻页或扫码回填后只显示编号。
const visibleDeviceOptions = computed(() => {
  const selected = props.selectedDevice
  return selected && !deviceOptions.value.some((device) => device.id === selected.id)
    ? [selected, ...deviceOptions.value]
    : deviceOptions.value
})

const deviceLabel = (device: RentalDeviceVO) => {
  const category = catalog.value.find((item) => item.categoryCode === device.categoryCode)
  const model = category?.models.find((item) => item.modelCode === device.equipmentModelCode)
  return [
    device.deviceNo,
    device.serialNumber,
    category?.categoryName || device.categoryCode,
    model?.modelName || device.equipmentModelCode,
    device.status ? t(getRentalLabelKey('device', device.status)) : ''
  ]
    .filter(Boolean)
    .join(' / ')
}

const loadCatalog = async () => {
  catalogError.value = false
  try {
    catalog.value = await getRentalDeviceCatalog()
  } catch {
    catalogError.value = true
  }
}

const invalidateSearch = () => {
  clearTimeout(searchTimer)
  requestVersion += 1
  devicesLoading.value = false
}

const loadDevices = async (pageNo = 1) => {
  if (devicesLoading.value) return
  const version = ++requestVersion
  devicesLoading.value = true
  devicesError.value = false
  try {
    const page = await getRentalDevicePage({
      categoryCode: selectedCategoryCode.value || undefined,
      equipmentModelCode: selectedModelCode.value || undefined,
      keyword: keyword.value || undefined,
      enabled: true,
      pageNo,
      pageSize: 20
    })
    if (version !== requestVersion) return
    const options = pageNo === 1 ? [] : deviceOptions.value
    deviceOptions.value = [
      ...new Map([...options, ...(page.list || [])].map((device) => [device.id, device])).values()
    ]
    deviceTotal.value = page.total
    nextPage.value = pageNo + 1
  } catch {
    if (version === requestVersion) devicesError.value = true
  } finally {
    if (version === requestVersion) devicesLoading.value = false
  }
}

const resetSearchResults = () => {
  invalidateSearch()
  deviceOptions.value = []
  deviceTotal.value = 0
  nextPage.value = 1
  devicesError.value = false
}

const handleDeviceSearch = (query: string) => {
  resetSearchResults()
  keyword.value = query.trim()
  searchTimer = setTimeout(() => void loadDevices(), 300)
}

const handleDeviceDropdown = (visible: boolean) => {
  if (!visible) return
  resetSearchResults()
  keyword.value = ''
  void loadDevices()
}

const clearSelectedDevice = () => {
  selectedDeviceNo.value = ''
  emit('deviceClear')
}

const handleCategoryChange = () => {
  selectedModelCode.value = ''
  handleModelChange()
}

const handleModelChange = () => {
  clearSelectedDevice()
  keyword.value = ''
  resetSearchResults()
  void loadDevices()
}

const clearFilters = () => {
  selectedCategoryCode.value = ''
  handleCategoryChange()
}

const handleDeviceClear = () => {
  clearSelectedDevice()
  handleDeviceSearch('')
}

const handleDeviceChange = (deviceNo?: string) => {
  const device = visibleDeviceOptions.value.find((item) => item.deviceNo === deviceNo)
  if (device) {
    resetSearchResults()
    emit('deviceSelect', device)
  }
}

watch(
  () => props.selectedDevice,
  (device) => {
    selectedDeviceNo.value = device?.deviceNo || ''
    if (device) {
      resetSearchResults()
      selectedCategoryCode.value = device.categoryCode || ''
      selectedModelCode.value = device.equipmentModelCode
    }
  },
  { immediate: true }
)

onBeforeUnmount(invalidateSearch)

const handleChange: UploadProps['onChange'] = (uploadFile, files) => {
  emit('imageChange', uploadFile, files)
}
const handleRemove: UploadProps['onRemove'] = () => {
  emit('imageRemove')
}
const handleExceed: UploadProps['onExceed'] = () => {
  emit('imageExceed')
}

onMounted(loadCatalog)
</script>
