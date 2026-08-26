<template>
  <Dialog
    v-model="dialogVisible"
    :title="t('rental.device.categoryCreateTitle')"
    width="480px"
    :fullscreen="false"
  >
    <el-form
      ref="formRef"
      v-loading="saving"
      :model="formData"
      :rules="rules"
      label-width="110px"
    >
      <el-form-item :label="t('rental.device.categoryCode')" prop="categoryCode">
        <el-input
          v-model="formData.categoryCode"
          maxlength="32"
          :placeholder="t('rental.device.categoryCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.device.categoryName')" prop="categoryName">
        <el-input
          v-model="formData.categoryName"
          maxlength="64"
          :placeholder="t('rental.device.categoryNamePlaceholder')"
        />
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
import { createRentalDeviceCategory } from '@/api/rental/device'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'

const emit = defineEmits<{
  success: [result: { id: number; categoryCode: string }]
}>()
const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  categoryCode: '',
  categoryName: '',
  sortOrder: 100
})
const rules = computed<FormRules>(() => ({
  categoryCode: [
    { required: true, message: t('rental.device.categoryCodeRequired'), trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9]+(?:[_-][A-Za-z0-9]+)*$/,
      message: t('rental.device.categoryCodeFormat'),
      trigger: 'blur'
    }
  ],
  categoryName: [
    { required: true, message: t('rental.device.categoryNameRequired'), trigger: 'blur' }
  ]
}))

const open = () => {
  Object.assign(formData, {
    categoryCode: '',
    categoryName: '',
    sortOrder: 100
  })
  formRef.value?.resetFields()
  dialogVisible.value = true
}
defineExpose({ open })

const submit = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const categoryCode = formData.categoryCode.trim().toUpperCase()
    const id = await createRentalDeviceCategory({
      categoryCode,
      categoryName: formData.categoryName.trim(),
      sortOrder: formData.sortOrder
    })
    dialogVisible.value = false
    message.success(t('rental.device.categoryCreateSuccess'))
    emit('success', { id, categoryCode })
  } finally {
    saving.value = false
  }
}
</script>
