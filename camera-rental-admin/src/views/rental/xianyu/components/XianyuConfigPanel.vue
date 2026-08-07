<template>
  <el-card class="mb-16px" shadow="never">
    <template #header>
      <div class="flex items-center justify-between gap-12px">
        <div>
          <div class="font-600">{{ t('rental.xianyu.configTitle') }}</div>
          <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">
            {{ t('rental.xianyu.configSubtitle') }}
          </div>
        </div>
        <div class="flex items-center gap-8px">
          <el-tag :type="getRentalTagType('integration', config?.status)">
            {{ rentalLabel('integration', config?.status) }}
          </el-tag>
          <el-button :loading="loading" @click="reload">
            <Icon icon="ep:refresh" class="mr-5px" />{{ t('common.refresh') }}
          </el-button>
        </div>
      </div>
    </template>

    <el-alert
      class="mb-16px"
      type="info"
      :closable="false"
      :title="t('rental.xianyu.secretHint')"
    />

    <el-form
      ref="formRef"
      v-loading="loading"
      :model="form"
      :rules="rules"
      label-width="150px"
      class="max-w-980px"
    >
      <el-divider content-position="left">{{ t('rental.xianyu.connectionSettings') }}</el-divider>
      <el-form-item :label="t('rental.xianyu.enabled')" prop="enabled">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.baseUrl')" prop="baseUrl">
        <el-input v-model="form.baseUrl" placeholder="https://open.goofish.pro" />
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.appKey')" prop="appKey">
        <el-input
          v-model="form.appKey"
          clearable
          :placeholder="replacementPlaceholder(config?.appKeyMasked)"
        />
        <div class="form-tip">{{ t('rental.xianyu.credentialKeepHint') }}</div>
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.appSecret')" prop="appSecret">
        <el-input
          v-model="form.appSecret"
          type="password"
          show-password
          clearable
          autocomplete="new-password"
          :placeholder="
            config?.appSecretConfigured
              ? t('rental.xianyu.secretConfiguredPlaceholder')
              : t('rental.xianyu.secretMissingPlaceholder')
          "
        />
        <div class="form-tip">{{ t('rental.xianyu.credentialKeepHint') }}</div>
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.webhookBaseUrl')" prop="webhookBaseUrl">
        <el-input
          v-model="form.webhookBaseUrl"
          clearable
          placeholder="https://rental.example.com/admin-api/rental/xianyu/webhook"
        />
      </el-form-item>

      <el-divider content-position="left">{{ t('rental.xianyu.operationSettings') }}</el-divider>
      <el-form-item :label="t('rental.xianyu.writeEnabled')">
        <el-switch v-model="form.writeEnabled" :disabled="!form.enabled" />
        <span class="ml-10px text-12px text-[var(--el-color-danger)]">
          {{ t('rental.xianyu.writeRiskHint') }}
        </span>
      </el-form-item>
      <el-form-item :label="t('rental.xianyu.jobEnabled')">
        <el-switch v-model="form.jobEnabled" :disabled="!form.enabled" />
        <span class="ml-10px text-12px text-[var(--el-text-color-secondary)]">
          {{ t('rental.xianyu.jobCronHint') }}
        </span>
      </el-form-item>

      <el-divider content-position="left">{{ t('rental.xianyu.syncSettings') }}</el-divider>
      <div class="grid grid-cols-1 gap-x-20px md:grid-cols-2">
        <el-form-item :label="t('rental.xianyu.lookbackDays')" prop="lookbackDays">
          <el-input-number v-model="form.lookbackDays" :min="1" :max="180" />
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.overlapMinutes')" prop="overlapMinutes">
          <el-input-number v-model="form.overlapMinutes" :min="0" :max="1440" />
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.maxPagesPerShop')" prop="maxPagesPerShop">
          <el-input-number v-model="form.maxPagesPerShop" :min="1" :max="100" />
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.pageSize')" prop="pageSize">
          <el-input-number v-model="form.pageSize" :min="1" :max="100" />
        </el-form-item>
        <el-form-item
          :label="t('rental.xianyu.pushRetryStaleSeconds')"
          prop="pushRetryStaleSeconds"
        >
          <el-input-number v-model="form.pushRetryStaleSeconds" :min="30" :max="86400" />
        </el-form-item>
        <el-form-item :label="t('rental.xianyu.pushRetryBatchSize')" prop="pushRetryBatchSize">
          <el-input-number v-model="form.pushRetryBatchSize" :min="1" :max="1000" />
        </el-form-item>
      </div>

      <el-form-item>
        <el-button
          type="primary"
          :loading="saving"
          @click="save"
          v-hasRole="['super_admin']"
        >
          {{ t('common.save') }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getXianyuConfig,
  updateXianyuConfig,
  type XianyuConfigUpdateReqVO,
  type XianyuConfigVO
} from '@/api/rental/xianyu'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { getRentalLabelKey, getRentalTagType } from '@/utils/rentalLabels'

const emit = defineEmits<{ loadError: [] }>()
const { t } = useI18n()
const message = useMessage()
const formRef = ref<FormInstance>()
const config = ref<XianyuConfigVO>()
const loading = ref(false)
const saving = ref(false)
const form = reactive<XianyuConfigUpdateReqVO>({
  enabled: false,
  baseUrl: 'https://open.goofish.pro',
  appKey: '',
  appSecret: '',
  webhookBaseUrl: '',
  writeEnabled: false,
  jobEnabled: false,
  lookbackDays: 7,
  overlapMinutes: 10,
  maxPagesPerShop: 20,
  pageSize: 50,
  pushRetryStaleSeconds: 120,
  pushRetryBatchSize: 100
})
const rules: FormRules = {
  baseUrl: [{ required: true, message: t('rental.xianyu.baseUrlRequired'), trigger: 'blur' }]
}

watch(
  () => form.enabled,
  (enabled) => {
    if (!enabled) {
      form.writeEnabled = false
      form.jobEnabled = false
    }
  }
)

const rentalLabel = (group: 'integration', value?: string | null) =>
  t(getRentalLabelKey(group, value), { code: value ?? '' })

const replacementPlaceholder = (masked?: string) =>
  masked
    ? t('rental.xianyu.keyConfiguredPlaceholder', { masked })
    : t('rental.xianyu.keyMissingPlaceholder')

const applyConfig = (value: XianyuConfigVO) => {
  config.value = value
  Object.assign(form, {
    enabled: value.enabled,
    baseUrl: value.baseUrl || 'https://open.goofish.pro',
    appKey: '',
    appSecret: '',
    webhookBaseUrl: value.webhookBaseUrl || '',
    writeEnabled: value.writeEnabled,
    jobEnabled: value.jobEnabled,
    lookbackDays: value.lookbackDays,
    overlapMinutes: value.overlapMinutes,
    maxPagesPerShop: value.maxPagesPerShop,
    pageSize: value.pageSize,
    pushRetryStaleSeconds: value.pushRetryStaleSeconds,
    pushRetryBatchSize: value.pushRetryBatchSize
  })
}

const reload = async () => {
  loading.value = true
  try {
    applyConfig(await getXianyuConfig())
  } catch {
    emit('loadError')
  } finally {
    loading.value = false
  }
}

const save = async () => {
  await formRef.value?.validate()
  if (form.writeEnabled && !config.value?.writeEnabled) {
    await message.confirm(t('rental.xianyu.enableWriteConfirm'))
  }
  saving.value = true
  try {
    await updateXianyuConfig({ ...form })
    form.appKey = ''
    form.appSecret = ''
    message.success(t('rental.xianyu.configSaved'))
    await reload()
  } finally {
    saving.value = false
  }
}

defineExpose({ reload })
</script>

<style scoped>
.form-tip {
  width: 100%;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
