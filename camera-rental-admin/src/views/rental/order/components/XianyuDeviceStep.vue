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
    <el-button :disabled="!qrPayload.trim()" :loading="resolving" @click="emit('resolve')">
      {{ t('rental.xianyu.deviceQrResolve') }}
    </el-button>
  </div>
  <el-form class="mt-16px" label-position="top" :model="form">
    <el-form-item :label="t('rental.xianyu.deviceCascadeLabel')">
      <div class="w-full flex flex-wrap gap-8px">
        <el-select
          v-model="selectedCategoryCode"
          class="!w-1/3"
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
          class="!w-1/3"
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
        <el-select
          v-model="selectedDeviceNo"
          class="!w-1/3"
          clearable
          filterable
          :loading="devicesLoading"
          :disabled="!selectedModelCode"
          :placeholder="t('rental.xianyu.deviceCascadeDevicePlaceholder')"
          @change="handleDeviceChange"
        >
          <el-option
            v-for="device in deviceOptions"
            :key="device.deviceNo"
            :label="
              device.serialNumber
                ? `${device.deviceNo} / ${device.serialNumber}`
                : device.deviceNo
            "
            :value="device.deviceNo"
          />
        </el-select>
      </div>
    </el-form-item>
    <el-form-item :label="t('rental.xianyu.deviceQrPayloadLabel')">
      <el-input
        v-model.trim="qrPayload"
        clearable
        :placeholder="t('rental.xianyu.deviceQrPayloadPlaceholder')"
        @keyup.enter="emit('resolve')"
      />
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
import { computed, onMounted, ref } from 'vue'
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

defineProps<{
  form: XianyuShipmentForm
  selectedDevice?: RentalDeviceVO
  hasImage: boolean
  decoding: boolean
  resolving: boolean
}>()

const uploadFiles = defineModel<UploadUserFile[]>('uploadFiles', { required: true })
const qrPayload = defineModel<string>('qrPayload', { required: true })
const emit = defineEmits<{
  imageChange: [uploadFile: UploadFile, uploadFiles: UploadFiles]
  imageRemove: []
  imageExceed: []
  decode: []
  resolve: []
  deviceSelect: [device: RentalDeviceVO]
}>()
const { t } = useI18n()

// 大类 → 型号 → 设备 级联选择；扫码/手工输入仍然保留
const catalog = ref<RentalDeviceCategoryVO[]>([])
const selectedCategoryCode = ref('')
const selectedModelCode = ref('')
const selectedDeviceNo = ref('')
const deviceOptions = ref<RentalDeviceVO[]>([])
const devicesLoading = ref(false)

const availableModels = computed(() => {
  const category = catalog.value.find((item) => item.categoryCode === selectedCategoryCode.value)
  return (category?.models || []).filter((model) => model.enabled !== false)
})

const loadCatalog = async () => {
  try {
    catalog.value = await getRentalDeviceCatalog()
  } catch {
    catalog.value = []
  }
}

const handleCategoryChange = () => {
  selectedModelCode.value = ''
  selectedDeviceNo.value = ''
  deviceOptions.value = []
}

const handleModelChange = async () => {
  selectedDeviceNo.value = ''
  deviceOptions.value = []
  if (!selectedModelCode.value) {
    return
  }
  devicesLoading.value = true
  try {
    const page = await getRentalDevicePage({
      categoryCode: selectedCategoryCode.value,
      equipmentModelCode: selectedModelCode.value,
      pageNo: 1,
      pageSize: 100
    })
    deviceOptions.value = (page.list || []).filter((device) => device.enabled !== false)
  } finally {
    devicesLoading.value = false
  }
}

const handleDeviceChange = (deviceNo?: string) => {
  const device = deviceOptions.value.find((item) => item.deviceNo === deviceNo)
  if (device) {
    emit('deviceSelect', device)
  }
}

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
