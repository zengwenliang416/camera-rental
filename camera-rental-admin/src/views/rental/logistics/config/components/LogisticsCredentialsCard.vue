<template>
  <el-card shadow="never" class="logistics-card">
    <template #header>
      <div class="section-header">
        <div>
          <div class="section-title">{{ t('rental.logistics.credentialsTitle') }}</div>
          <div class="section-description">
            {{ t('rental.logistics.credentialsDescription') }}
          </div>
        </div>
        <el-button
          type="primary"
          v-hasPermi="['rental:logistics:config:update']"
          @click="openDialog()"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          {{ t('rental.logistics.addCredential') }}
        </el-button>
      </div>
    </template>

    <el-table :data="config?.credentials || []">
      <el-table-column
        prop="credentialName"
        :label="t('rental.logistics.credentialName')"
        min-width="160"
      />
      <el-table-column :label="t('rental.logistics.credentialEnabled')" width="110">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{
              row.enabled ? t('rental.logistics.enabledState') : t('rental.logistics.disabledState')
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.logistics.customerCode')" min-width="170">
        <template #default="{ row }">
          <span v-if="row.customerCodeConfigured">{{ row.maskedCustomerCode || '********' }}</span>
          <el-tag v-else type="danger" size="small">
            {{ t('rental.logistics.notConfigured') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.logistics.apiKey')" min-width="170">
        <template #default="{ row }">
          <span v-if="row.apiKeyConfigured">{{ row.maskedApiKey || '********' }}</span>
          <el-tag v-else type="danger" size="small">
            {{ t('rental.logistics.notConfigured') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="sortOrder"
        :label="t('rental.logistics.sortOrder')"
        width="100"
        align="center"
      />
      <el-table-column :label="t('rental.logistics.configStatus')" width="150">
        <template #default="{ row }">
          <el-tag :type="logisticsStatusTagType(row.configStatus)">
            {{ translatedStatus(row.configStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.logistics.lastVerified')" width="180">
        <template #default="{ row }">
          {{ formatNullableDate(row.lastVerifiedAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('table.action')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            v-hasPermi="['rental:logistics:config:verify']"
            :loading="verifyingId === row.id"
            @click="verify(row.id)"
          >
            {{ t('rental.logistics.verify') }}
          </el-button>
          <el-button
            link
            type="primary"
            v-hasPermi="['rental:logistics:config:update']"
            @click="openDialog(row)"
          >
            {{ t('action.edit') }}
          </el-button>
          <el-button
            link
            type="danger"
            v-hasPermi="['rental:logistics:config:update']"
            @click="remove(row)"
          >
            {{ t('action.del') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="py-24px text-[var(--el-text-color-secondary)]">
          {{ t('rental.logistics.credentialsEmpty') }}
        </div>
      </template>
    </el-table>
  </el-card>

  <el-dialog
    v-model="dialogVisible"
    :title="form.id ? t('rental.logistics.editCredential') : t('rental.logistics.addCredential')"
    width="640px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="145px">
      <el-form-item :label="t('rental.logistics.credentialName')" prop="credentialName">
        <el-input
          v-model="form.credentialName"
          maxlength="64"
          :placeholder="t('rental.logistics.credentialNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('rental.logistics.credentialEnabled')">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item :label="t('rental.logistics.sortOrder')" prop="sortOrder">
        <el-input-number v-model="form.sortOrder" :min="0" :max="10000" controls-position="right" />
        <span class="ml-10px text-12px text-[var(--el-text-color-secondary)]">
          {{ t('rental.logistics.sortOrderHint') }}
        </span>
      </el-form-item>

      <el-divider content-position="left">{{ t('rental.logistics.customerCode') }}</el-divider>
      <LogisticsSecretEditor
        v-model:action="form.customerCodeAction"
        v-model:value="form.customerCode"
        :configured="editing?.customerCodeConfigured || false"
        :masked="editing?.maskedCustomerCode || undefined"
        :allow-keep="Boolean(form.id)"
        :label="t('rental.logistics.customerCode')"
      />

      <el-divider content-position="left">{{ t('rental.logistics.apiKey') }}</el-divider>
      <LogisticsSecretEditor
        v-model:action="form.apiKeyAction"
        v-model:value="form.apiKey"
        :configured="editing?.apiKeyConfigured || false"
        :masked="editing?.maskedApiKey || undefined"
        :allow-keep="Boolean(form.id)"
        :label="t('rental.logistics.apiKey')"
      />
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="save">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  deleteRentalLogisticsProviderCredential,
  saveRentalLogisticsProviderCredential,
  verifyRentalLogisticsProviderCredential
} from '@/api/rental/logistics'
import type {
  RentalLogisticsProviderConfigVO,
  RentalLogisticsProviderCredentialSaveReqVO,
  RentalLogisticsProviderCredentialVO,
  RentalLogisticsSecretAction
} from '@/api/rental/logistics'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { formatNullableDate } from '@/utils/formatTime'
import LogisticsSecretEditor from './LogisticsSecretEditor.vue'
import { logisticsStatusKey, logisticsStatusTagType } from '../logisticsConfigModel'

defineProps<{ config?: RentalLogisticsProviderConfigVO }>()
const emit = defineEmits<{ refresh: [] }>()
const { t } = useI18n()
const message = useMessage()
const formRef = ref<FormInstance>()
const saving = ref(false)
const verifyingId = ref<number>()
const dialogVisible = ref(false)
const editing = ref<RentalLogisticsProviderCredentialVO>()
const form = reactive({
  id: undefined as number | undefined,
  providerCode: 'KUAIDI100',
  credentialName: '',
  enabled: false,
  sortOrder: 100,
  customerCodeAction: 'REPLACE' as RentalLogisticsSecretAction,
  customerCode: '',
  apiKeyAction: 'REPLACE' as RentalLogisticsSecretAction,
  apiKey: ''
})
const rules: FormRules = {
  credentialName: [
    { required: true, message: t('rental.logistics.credentialNameRequired'), trigger: 'blur' }
  ],
  sortOrder: [
    { required: true, message: t('rental.logistics.sortOrderRequired'), trigger: 'change' }
  ]
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    providerCode: 'KUAIDI100',
    credentialName: '',
    enabled: false,
    sortOrder: 100,
    customerCodeAction: 'REPLACE',
    customerCode: '',
    apiKeyAction: 'REPLACE',
    apiKey: ''
  })
}

const openDialog = (credential?: RentalLogisticsProviderCredentialVO) => {
  editing.value = credential
  resetForm()
  if (credential) {
    Object.assign(form, {
      id: credential.id,
      providerCode: credential.providerCode,
      credentialName: credential.credentialName,
      enabled: credential.enabled,
      sortOrder: credential.sortOrder,
      customerCodeAction: 'KEEP',
      apiKeyAction: 'KEEP'
    })
  }
  dialogVisible.value = true
}

const secretExists = (
  configured: boolean,
  action: RentalLogisticsSecretAction,
  replacement: string
) => {
  if (action === 'CLEAR') return false
  if (action === 'REPLACE') return Boolean(replacement.trim())
  return configured
}

const save = async () => {
  await formRef.value?.validate()
  const customerCodeValid = secretExists(
    Boolean(editing.value?.customerCodeConfigured),
    form.customerCodeAction,
    form.customerCode
  )
  const apiKeyValid = secretExists(
    Boolean(editing.value?.apiKeyConfigured),
    form.apiKeyAction,
    form.apiKey
  )
  if (!customerCodeValid || !apiKeyValid) {
    message.warning(t('rental.logistics.credentialSecretsRequired'))
    return
  }
  const payload: RentalLogisticsProviderCredentialSaveReqVO = {
    id: form.id,
    providerCode: form.providerCode,
    credentialName: form.credentialName.trim(),
    enabled: form.enabled,
    sortOrder: form.sortOrder,
    customerCodeAction: form.customerCodeAction,
    customerCode: form.customerCodeAction === 'REPLACE' ? form.customerCode.trim() : undefined,
    apiKeyAction: form.apiKeyAction,
    apiKey: form.apiKeyAction === 'REPLACE' ? form.apiKey.trim() : undefined
  }
  saving.value = true
  try {
    await saveRentalLogisticsProviderCredential(payload)
    dialogVisible.value = false
    message.success(t('rental.logistics.credentialSaved'))
    emit('refresh')
  } finally {
    saving.value = false
  }
}

const remove = async (credential: RentalLogisticsProviderCredentialVO) => {
  await message.delConfirm(
    t('rental.logistics.deleteCredentialConfirm', { name: credential.credentialName })
  )
  await deleteRentalLogisticsProviderCredential(credential.id)
  message.success(t('rental.logistics.credentialDeleted'))
  emit('refresh')
}

const verify = async (id: number) => {
  verifyingId.value = id
  try {
    const result = await verifyRentalLogisticsProviderCredential(id)
    if (result.valid) {
      message.success(t('rental.logistics.credentialVerified'))
    } else {
      message.warning(t('rental.logistics.credentialVerifyFailed', { reason: result.reason }))
    }
    emit('refresh')
  } finally {
    verifyingId.value = undefined
  }
}

const translatedStatus = (status: string) => {
  const key = logisticsStatusKey(status)
  return key === status ? status : t(key)
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

.section-description {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (width <= 720px) {
  .section-header {
    flex-direction: column;
  }
}
</style>
