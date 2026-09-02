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
import type { UploadFile, UploadFiles, UploadProps, UploadUserFile } from 'element-plus'
import type { RentalDeviceVO } from '@/api/rental/device'
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
}>()
const { t } = useI18n()

const handleChange: UploadProps['onChange'] = (uploadFile, files) => {
  emit('imageChange', uploadFile, files)
}
const handleRemove: UploadProps['onRemove'] = () => {
  emit('imageRemove')
}
const handleExceed: UploadProps['onExceed'] = () => {
  emit('imageExceed')
}
</script>
