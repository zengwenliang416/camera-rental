<template>
  <Dialog
    v-model="dialogVisible"
    :title="t('rental.device.modelCreateTitle')"
    width="520px"
    :fullscreen="false"
  >
    <el-form
      ref="formRef"
      v-loading="saving"
      :model="formData"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item :label="t('rental.device.category')" prop="categoryId">
        <el-select v-model="formData.categoryId" class="!w-100%" filterable>
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.categoryName"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('rental.device.modelCode')" prop="modelCode">
        <el-input
          v-model="formData.modelCode"
          maxlength="64"
          :placeholder="t('rental.device.modelCodePlaceholder')"
          @blur="fillDefaultsFromModelCode"
        />
      </el-form-item>
      <el-form-item :label="t('rental.device.modelName')" prop="modelName">
        <el-input
          v-model="formData.modelName"
          maxlength="128"
          :placeholder="t('rental.device.modelNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.device.deviceNoPrefix')" prop="deviceNoPrefix">
        <el-input
          v-model="formData.deviceNoPrefix"
          maxlength="64"
          :placeholder="t('rental.device.deviceNoPrefixPlaceholder')"
        />
        <div class="text-12px text-[var(--el-text-color-secondary)]">
          {{ t('rental.device.deviceNoPrefixHint') }}
        </div>
      </el-form-item>
      <el-form-item :label="t('rental.device.sortOrder')" prop="sortOrder">
        <el-input-number
          v-model="formData.sortOrder"
          :min="0"
          :max="10000"
          controls-position="right"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="submit">
        {{ t('common.ok') }}
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import {
  createRentalDeviceModel,
  type RentalDeviceCategoryVO
} from '@/api/rental/device'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'

const props = defineProps<{
  categories: RentalDeviceCategoryVO[]
}>()
const emit = defineEmits<{
  success: [result: { id: number; categoryId: number; modelCode: string }]
}>()
const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  categoryId: undefined as number | undefined,
  modelCode: '',
  modelName: '',
  deviceNoPrefix: '',
  sortOrder: 100
})
const tokenPattern = /^(?:[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*|支架)$/
const rules = computed<FormRules>(() => ({
  categoryId: [
    { required: true, message: t('rental.device.categoryRequired'), trigger: 'change' }
  ],
  modelCode: [
    { required: true, message: t('rental.device.modelCodeRequired'), trigger: 'blur' },
    {
      pattern: tokenPattern,
      message: t('rental.device.modelCodeFormat'),
      trigger: 'blur'
    }
  ],
  modelName: [
    { required: true, message: t('rental.device.modelNameRequired'), trigger: 'blur' }
  ],
  deviceNoPrefix: [
    { required: true, message: t('rental.device.deviceNoPrefixRequired'), trigger: 'blur' },
    {
      pattern: tokenPattern,
      message: t('rental.device.deviceNoPrefixFormat'),
      trigger: 'blur'
    }
  ]
}))

const open = (categoryId?: number) => {
  Object.assign(formData, {
    categoryId: categoryId ?? props.categories[0]?.id,
    modelCode: '',
    modelName: '',
    deviceNoPrefix: '',
    sortOrder: 100
  })
  formRef.value?.resetFields()
  dialogVisible.value = true
}
defineExpose({ open })

const fillDefaultsFromModelCode = () => {
  const modelCode = formData.modelCode.trim().toUpperCase()
  formData.modelCode = modelCode
  if (!formData.modelName) formData.modelName = modelCode
  if (!formData.deviceNoPrefix) formData.deviceNoPrefix = modelCode
}

const submit = async () => {
  await formRef.value?.validate()
  const categoryId = formData.categoryId!
  const modelCode = formData.modelCode.trim().toUpperCase()
  saving.value = true
  try {
    const id = await createRentalDeviceModel({
      categoryId,
      modelCode,
      modelName: formData.modelName.trim(),
      deviceNoPrefix: formData.deviceNoPrefix.trim().toUpperCase(),
      sortOrder: formData.sortOrder
    })
    dialogVisible.value = false
    message.success(t('rental.device.modelCreateSuccess'))
    emit('success', { id, categoryId, modelCode })
  } finally {
    saving.value = false
  }
}
</script>
