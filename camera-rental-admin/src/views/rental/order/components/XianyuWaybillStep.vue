<template>
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
    <div class="el-upload__text">{{ t('rental.xianyu.shipUploadHint') }}</div>
    <template #tip>
      <div class="el-upload__tip">{{ t('rental.xianyu.shipUploadTip') }}</div>
    </template>
  </el-upload>
  <div class="mt-12px flex flex-wrap gap-8px">
    <el-button
      v-hasPermi="['rental:xianyu:ship:ocr']"
      type="primary"
      :disabled="!hasImage"
      :loading="ocrLoading"
      @click="emit('ocr')"
    >
      {{ t('rental.xianyu.shipOcr') }}
    </el-button>
    <el-tag v-if="ocr?.extractionSource" type="info">
      {{ ocr.extractionSource }}
    </el-tag>
    <el-tag v-if="ocr?.confidence" type="success">
      {{ t('rental.xianyu.shipOcrConfidence', { value: ocr.confidence }) }}
    </el-tag>
  </div>
  <el-form class="mt-16px" label-position="top" :model="form">
    <el-form-item :label="t('rental.xianyu.waybillNo')">
      <el-input
        v-model.trim="form.waybillNo"
        clearable
        :placeholder="t('rental.xianyu.waybillNoPlaceholder')"
        @blur="emit('waybillRecognize', form.waybillNo)"
      />
    </el-form-item>
    <el-form-item :label="t('rental.xianyu.expressName')">
      <el-select
        v-model="form.expressCode"
        class="!w-1/1"
        filterable
        clearable
        :placeholder="t('rental.xianyu.expressPlaceholder')"
        @change="emit('expressChange', $event)"
      >
        <el-option
          v-for="express in expressList"
          :key="express.code"
          :label="`${express.expressName} (${express.code})`"
          :value="express.code"
        />
      </el-select>
    </el-form-item>
    <el-form-item :label="t('rental.xianyu.expressNameManual')">
      <el-input
        v-model.trim="form.expressName"
        clearable
        :placeholder="t('rental.xianyu.expressNameManualPlaceholder')"
      />
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import type { UploadFile, UploadFiles, UploadProps, UploadUserFile } from 'element-plus'
import type { XianyuExpressCompanyVO, XianyuShipmentOcrRespVO } from '@/api/rental/xianyu'
import { useI18n } from '@/hooks/web/useI18n'
import type { XianyuShipmentForm } from './xianyuShipWorkbenchTypes'

defineOptions({ name: 'XianyuWaybillStep' })

defineProps<{
  form: XianyuShipmentForm
  expressList: XianyuExpressCompanyVO[]
  ocr?: XianyuShipmentOcrRespVO
  ocrLoading: boolean
  hasImage: boolean
}>()

const uploadFiles = defineModel<UploadUserFile[]>('uploadFiles', { required: true })
const emit = defineEmits<{
  imageChange: [uploadFile: UploadFile, uploadFiles: UploadFiles]
  imageRemove: []
  imageExceed: []
  ocr: []
  expressChange: [code?: string]
  waybillRecognize: [waybillNo: string]
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
