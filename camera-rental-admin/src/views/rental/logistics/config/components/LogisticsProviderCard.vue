<template>
  <el-card v-loading="loading" shadow="never" class="mb-16px logistics-card">
    <template #header>
      <div class="section-header">
        <div>
          <div class="section-title">{{ t('rental.logistics.providerTitle') }}</div>
          <div class="section-description">
            {{ t('rental.logistics.providerDescription') }}
          </div>
        </div>
        <div class="text-right text-12px text-[var(--el-text-color-secondary)]">
          <div>{{ t('rental.logistics.lastVerified') }}</div>
          <div class="mt-2px font-600 text-[var(--el-text-color-primary)]">
            {{ formatNullableDate(config?.lastVerifiedAt) }}
          </div>
        </div>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="170px" class="max-w-1000px">
      <div class="grid grid-cols-1 gap-x-24px lg:grid-cols-2">
        <el-form-item :label="t('rental.logistics.enabled')">
          <div class="field-control">
            <el-switch v-model="form.enabled" />
            <div class="form-tip">{{ t('rental.logistics.enabledHint') }}</div>
          </div>
        </el-form-item>
        <el-form-item :label="t('rental.logistics.queryEnabled')">
          <div class="field-control">
            <el-switch v-model="form.queryEnabled" />
            <div class="form-tip">{{ t('rental.logistics.queryEnabledHint') }}</div>
          </div>
        </el-form-item>
        <el-form-item :label="t('rental.logistics.subscribeEnabled')">
          <div class="field-control">
            <el-switch v-model="form.subscribeEnabled" />
            <div class="form-tip">{{ t('rental.logistics.subscribeEnabledHint') }}</div>
          </div>
        </el-form-item>
        <el-form-item
          :label="t('rental.logistics.minimumQueryIntervalSeconds')"
          prop="minimumQueryIntervalSeconds"
        >
          <div class="field-control">
            <el-input-number
              v-model="form.minimumQueryIntervalSeconds"
              :min="1800"
              :max="86400"
              :step="300"
              controls-position="right"
              class="!w-full"
            />
            <div class="form-tip">
              {{
                t('rental.logistics.minimumQueryIntervalHint', {
                  minutes: Math.round(form.minimumQueryIntervalSeconds / 60)
                })
              }}
            </div>
          </div>
        </el-form-item>
        <el-form-item
          :label="t('rental.logistics.callbackBaseUrl')"
          prop="callbackBaseUrl"
          class="lg:col-span-2"
        >
          <div class="field-control">
            <el-input
              v-model="form.callbackBaseUrl"
              clearable
              placeholder="https://api.example.com"
            />
            <div class="form-tip">
              {{ t('rental.logistics.callbackBaseUrlHint') }}
              <code>/rental/webhooks/kuaidi100/tracking/...</code>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="t('rental.logistics.resultVersion')" prop="resultVersion">
          <div class="field-control">
            <el-input v-model="form.resultVersion" maxlength="16" />
            <div class="form-tip">{{ t('rental.logistics.resultVersionHint') }}</div>
          </div>
        </el-form-item>
      </div>

      <el-form-item>
        <el-button
          type="primary"
          :loading="saving"
          v-hasRole="['super_admin']"
          @click="save"
        >
          {{ t('rental.logistics.saveProvider') }}
        </el-button>
        <el-button
          :loading="verifying"
          v-hasRole="['super_admin']"
          @click="verify"
        >
          {{ t('rental.logistics.verifyProvider') }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  updateRentalLogisticsProviderConfig,
  verifyRentalLogisticsProviderConfig
} from '@/api/rental/logistics'
import type {
  RentalLogisticsProviderConfigUpdateReqVO,
  RentalLogisticsProviderConfigVO
} from '@/api/rental/logistics'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { formatNullableDate } from '@/utils/formatTime'

const props = defineProps<{
  config?: RentalLogisticsProviderConfigVO
  loading: boolean
}>()

const emit = defineEmits<{
  updated: [config: RentalLogisticsProviderConfigVO]
  refresh: []
}>()

const { t } = useI18n()
const message = useMessage()
const formRef = ref<FormInstance>()
const saving = ref(false)
const verifying = ref(false)
const form = reactive({
  providerCode: 'KUAIDI100',
  enabled: false,
  queryEnabled: false,
  subscribeEnabled: false,
  callbackBaseUrl: '',
  minimumQueryIntervalSeconds: 1800,
  resultVersion: '4'
})

const rules: FormRules = {
  minimumQueryIntervalSeconds: [
    { required: true, message: t('rental.logistics.intervalRequired'), trigger: 'change' }
  ],
  resultVersion: [
    { required: true, message: t('rental.logistics.resultVersionRequired'), trigger: 'blur' }
  ]
}

watch(
  () => props.config,
  (value) => {
    if (!value) return
    Object.assign(form, {
      providerCode: value.providerCode,
      enabled: value.enabled,
      queryEnabled: value.queryEnabled,
      subscribeEnabled: value.subscribeEnabled,
      callbackBaseUrl: value.callbackBaseUrl || '',
      minimumQueryIntervalSeconds: value.minimumQueryIntervalSeconds,
      resultVersion: value.resultVersion || '4'
    })
  },
  { immediate: true }
)

const save = async () => {
  await formRef.value?.validate()
  if (form.enabled && form.subscribeEnabled && !form.callbackBaseUrl.trim()) {
    message.warning(t('rental.logistics.providerEnablePrerequisite'))
    return
  }
  const payload: RentalLogisticsProviderConfigUpdateReqVO = {
    providerCode: form.providerCode,
    enabled: form.enabled,
    queryEnabled: form.queryEnabled,
    subscribeEnabled: form.subscribeEnabled,
    callbackSecretAction: 'KEEP',
    callbackBaseUrl: form.callbackBaseUrl.trim() || null,
    minimumQueryIntervalSeconds: form.minimumQueryIntervalSeconds,
    resultVersion: form.resultVersion.trim()
  }
  saving.value = true
  try {
    emit('updated', await updateRentalLogisticsProviderConfig(payload))
    message.success(t('rental.logistics.providerSaved'))
  } finally {
    saving.value = false
  }
}

const verify = async () => {
  verifying.value = true
  try {
    const result = await verifyRentalLogisticsProviderConfig()
    if (result.valid) {
      message.success(t('rental.logistics.providerVerified'))
    } else {
      message.warning(t('rental.logistics.providerVerifyFailed', { reason: result.reason }))
    }
    emit('refresh')
  } finally {
    verifying.value = false
  }
}
</script>

<style scoped>
.logistics-card {
  border-color: rgb(15 118 110 / 16%);
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-title {
  font-weight: 700;
}

.section-description,
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.field-control {
  width: 100%;
}

@media (width <= 720px) {
  .section-header {
    flex-direction: column;
  }
}
</style>
