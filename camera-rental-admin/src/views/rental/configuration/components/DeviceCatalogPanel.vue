<template>
  <section class="catalog-panel">
    <div class="panel-heading">
      <div>
        <h2>{{ t('rental.configuration.catalogTitle') }}</h2>
        <p>{{ t('rental.configuration.catalogHint') }}</p>
      </div>
      <el-button v-if="canUpdate" type="primary" @click="openCategoryDialog()">
        <Icon icon="ep:plus" class="mr-5px" />
        {{ t('rental.configuration.newCategory') }}
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="7" animated />
    <el-empty
      v-else-if="catalog.length === 0"
      :description="t('rental.configuration.catalogEmpty')"
    />
    <div v-else class="catalog-grid">
      <section class="catalog-card catalog-card--categories">
        <header>
          <strong>{{ t('rental.configuration.categories') }}</strong>
          <el-tag effect="plain">{{ catalog.length }}</el-tag>
        </header>
        <div class="category-list">
          <button
            v-for="category in catalog"
            :key="category.id"
            type="button"
            class="category-row"
            :class="{ 'is-active': category.id === selectedCategoryId }"
            @click="selectedCategoryId = category.id"
          >
            <span>
              <b>{{ category.categoryCode }}</b>
              <small>{{ category.categoryName }}</small>
            </span>
            <el-tag :type="category.enabled === false ? 'info' : 'success'" effect="plain">
              {{
                category.enabled === false
                  ? t('rental.configuration.disabled')
                  : t('rental.configuration.enabled')
              }}
            </el-tag>
          </button>
        </div>
      </section>

      <section class="catalog-card catalog-card--models">
        <header>
          <div>
            <strong>
              {{
                selectedCategory
                  ? t('rental.configuration.categoryModels', {
                      name: selectedCategory.categoryName
                    })
                  : t('rental.configuration.models')
              }}
            </strong>
            <small v-if="selectedCategory">{{ selectedCategory.categoryCode }}</small>
          </div>
          <div class="catalog-card__actions" v-if="canUpdate && selectedCategory">
            <el-button link type="primary" @click="openCategoryDialog(selectedCategory)">
              {{ t('action.edit') }}
            </el-button>
            <el-button link type="primary" @click="openModelDialog()">
              <Icon icon="ep:plus" class="mr-3px" />
              {{ t('rental.configuration.newModel') }}
            </el-button>
          </div>
        </header>

        <el-table :data="selectedCategory?.models ?? []">
          <el-table-column
            prop="modelName"
            :label="t('rental.configuration.modelName')"
            min-width="150"
          />
          <el-table-column
            prop="modelCode"
            :label="t('rental.configuration.modelCode')"
            min-width="140"
          >
            <template #default="{ row }">
              <code class="catalog-code">{{ row.modelCode }}</code>
            </template>
          </el-table-column>
          <el-table-column
            prop="deviceNoPrefix"
            :label="t('rental.configuration.prefix')"
            min-width="140"
          >
            <template #default="{ row }">
              <code class="catalog-code">{{ row.deviceNoPrefix }}</code>
            </template>
          </el-table-column>
          <el-table-column
            prop="sortOrder"
            :label="t('rental.configuration.sortOrder')"
            width="90"
          />
          <el-table-column :label="t('rental.configuration.status')" width="105">
            <template #default="{ row }">
              <el-tag :type="row.enabled === false ? 'info' : 'success'" effect="plain">
                {{
                  row.enabled === false
                    ? t('rental.configuration.disabled')
                    : t('rental.configuration.enabled')
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canUpdate" :label="t('table.action')" width="170" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openModelDialog(row)">
                {{ t('action.edit') }}
              </el-button>
              <el-button
                link
                :type="row.enabled === false ? 'success' : 'warning'"
                @click="emit('change-model-status', row)"
              >
                {{
                  row.enabled === false
                    ? t('rental.configuration.enable')
                    : t('rental.configuration.disable')
                }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="py-20px text-[var(--el-text-color-secondary)]">
              {{ t('rental.configuration.modelEmpty') }}
            </div>
          </template>
        </el-table>

        <div v-if="canUpdate && selectedCategory" class="category-status">
          <span>{{ t('rental.configuration.categoryStatusHint') }}</span>
          <el-button
            link
            :type="selectedCategory.enabled === false ? 'success' : 'warning'"
            @click="emit('change-category-status', selectedCategory)"
          >
            {{
              selectedCategory.enabled === false
                ? t('rental.configuration.enableCategory')
                : t('rental.configuration.disableCategory')
            }}
          </el-button>
        </div>
      </section>
    </div>

    <el-dialog
      v-model="categoryDialogVisible"
      :title="
        categoryForm.id
          ? t('rental.configuration.editCategory')
          : t('rental.configuration.newCategory')
      "
      width="min(480px, calc(100vw - 24px))"
      destroy-on-close
    >
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-position="top"
      >
        <el-form-item :label="t('rental.configuration.categoryCode')" prop="categoryCode">
          <el-input v-model="categoryForm.categoryCode" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('rental.configuration.categoryName')" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('rental.configuration.sortOrder')" prop="sortOrder">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitCategory">
          {{ t('common.ok') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="modelDialogVisible"
      :title="
        modelForm.id ? t('rental.configuration.editModel') : t('rental.configuration.newModel')
      "
      width="min(520px, calc(100vw - 24px))"
      destroy-on-close
    >
      <el-form ref="modelFormRef" :model="modelForm" :rules="modelRules" label-position="top">
        <el-form-item :label="t('rental.configuration.category')" prop="categoryId">
          <el-select v-model="modelForm.categoryId" class="!w-100%" filterable>
            <el-option
              v-for="category in enabledCategories"
              :key="category.id"
              :label="`${category.categoryName} (${category.categoryCode})`"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('rental.configuration.modelCode')" prop="modelCode">
          <el-input v-model="modelForm.modelCode" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('rental.configuration.modelName')" prop="modelName">
          <el-input v-model="modelForm.modelName" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('rental.configuration.prefix')" prop="deviceNoPrefix">
          <el-input v-model="modelForm.deviceNoPrefix" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('rental.configuration.sortOrder')" prop="sortOrder">
          <el-input-number v-model="modelForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitModel">
          {{ t('common.ok') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { RentalDeviceCategoryVO, RentalDeviceModelVO } from '@/api/rental/catalog'
import type {
  RentalDeviceCategoryCreateReqVO,
  RentalDeviceCategoryUpdateReqVO,
  RentalDeviceModelCreateReqVO,
  RentalDeviceModelUpdateReqVO
} from '@/api/rental/configuration'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'

const props = withDefaults(
  defineProps<{
    catalog: RentalDeviceCategoryVO[]
    loading?: boolean
    saving?: boolean
    canUpdate?: boolean
  }>(),
  {
    loading: false,
    saving: false,
    canUpdate: false
  }
)

const emit = defineEmits<{
  'save-category': [
    value: RentalDeviceCategoryCreateReqVO | RentalDeviceCategoryUpdateReqVO,
    done: () => void
  ]
  'save-model': [
    value: RentalDeviceModelCreateReqVO | RentalDeviceModelUpdateReqVO,
    done: () => void
  ]
  'change-category-status': [value: RentalDeviceCategoryVO]
  'change-model-status': [value: RentalDeviceModelVO]
}>()

const { t } = useI18n()
const message = useMessage()
const selectedCategoryId = ref<number>()
const categoryDialogVisible = ref(false)
const modelDialogVisible = ref(false)
const categoryFormRef = ref<FormInstance>()
const modelFormRef = ref<FormInstance>()

const categoryForm = reactive({
  id: undefined as number | undefined,
  categoryCode: '',
  categoryName: '',
  sortOrder: 0,
  lockVersion: undefined as number | undefined
})

const modelForm = reactive({
  id: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  modelCode: '',
  modelName: '',
  deviceNoPrefix: '',
  sortOrder: 0,
  lockVersion: undefined as number | undefined
})

const categoryRules = computed<FormRules>(() => ({
  categoryCode: [
    { required: true, message: t('rental.configuration.categoryCodeRequired'), trigger: 'blur' },
    {
      pattern: /^[A-Z0-9_-]+$/,
      message: t('rental.configuration.categoryCodeFormat'),
      trigger: 'blur'
    }
  ],
  categoryName: [
    { required: true, message: t('rental.configuration.categoryNameRequired'), trigger: 'blur' }
  ]
}))

const modelRules = computed<FormRules>(() => ({
  categoryId: [
    { required: true, message: t('rental.configuration.categoryRequired'), trigger: 'change' }
  ],
  modelCode: [
    { required: true, message: t('rental.configuration.modelCodeRequired'), trigger: 'blur' }
  ],
  modelName: [
    { required: true, message: t('rental.configuration.modelNameRequired'), trigger: 'blur' }
  ],
  deviceNoPrefix: [
    { required: true, message: t('rental.configuration.prefixRequired'), trigger: 'blur' }
  ]
}))

const selectedCategory = computed(() =>
  props.catalog.find((category) => category.id === selectedCategoryId.value)
)
const enabledCategories = computed(() =>
  props.catalog.filter((category) => category.enabled !== false)
)

watch(
  () => props.catalog,
  (catalog) => {
    if (!catalog.some((category) => category.id === selectedCategoryId.value)) {
      selectedCategoryId.value = catalog[0]?.id
    }
  },
  { immediate: true }
)

const openCategoryDialog = (category?: RentalDeviceCategoryVO) => {
  categoryForm.id = category?.id
  categoryForm.categoryCode = category?.categoryCode ?? ''
  categoryForm.categoryName = category?.categoryName ?? ''
  categoryForm.sortOrder = category?.sortOrder ?? 0
  categoryForm.lockVersion = category?.lockVersion
  categoryDialogVisible.value = true
}

const openModelDialog = (model?: RentalDeviceModelVO) => {
  modelForm.id = model?.id
  modelForm.categoryId = model?.categoryId ?? selectedCategory.value?.id
  modelForm.modelCode = model?.modelCode ?? ''
  modelForm.modelName = model?.modelName ?? ''
  modelForm.deviceNoPrefix = model?.deviceNoPrefix ?? ''
  modelForm.sortOrder = model?.sortOrder ?? 0
  modelForm.lockVersion = model?.lockVersion
  modelDialogVisible.value = true
}

const submitCategory = async () => {
  if (!(await categoryFormRef.value?.validate())) return
  const close = () => {
    categoryDialogVisible.value = false
  }
  if (categoryForm.id !== undefined) {
    if (categoryForm.lockVersion === undefined) {
      message.error(t('rental.configuration.versionMissing'))
      return
    }
    emit(
      'save-category',
      {
        id: categoryForm.id,
        categoryCode: categoryForm.categoryCode.trim().toUpperCase(),
        categoryName: categoryForm.categoryName.trim(),
        sortOrder: categoryForm.sortOrder,
        lockVersion: categoryForm.lockVersion
      },
      close
    )
    return
  }
  emit(
    'save-category',
    {
      categoryCode: categoryForm.categoryCode.trim().toUpperCase(),
      categoryName: categoryForm.categoryName.trim(),
      sortOrder: categoryForm.sortOrder
    },
    close
  )
}

const submitModel = async () => {
  if (!(await modelFormRef.value?.validate()) || modelForm.categoryId === undefined) return
  const close = () => {
    modelDialogVisible.value = false
  }
  if (modelForm.id !== undefined) {
    if (modelForm.lockVersion === undefined) {
      message.error(t('rental.configuration.versionMissing'))
      return
    }
    emit(
      'save-model',
      {
        id: modelForm.id,
        categoryId: modelForm.categoryId,
        modelCode: modelForm.modelCode.trim(),
        modelName: modelForm.modelName.trim(),
        deviceNoPrefix: modelForm.deviceNoPrefix.trim(),
        sortOrder: modelForm.sortOrder,
        lockVersion: modelForm.lockVersion
      },
      close
    )
    return
  }
  emit(
    'save-model',
    {
      categoryId: modelForm.categoryId,
      modelCode: modelForm.modelCode.trim(),
      modelName: modelForm.modelName.trim(),
      deviceNoPrefix: modelForm.deviceNoPrefix.trim(),
      sortOrder: modelForm.sortOrder
    },
    close
  )
}
</script>

<style scoped>
.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-heading h2 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.panel-heading p {
  margin: 5px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.catalog-grid {
  display: grid;
  grid-template-columns: minmax(230px, 0.34fr) minmax(0, 1fr);
  gap: 14px;
}

.catalog-card {
  min-width: 0;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.catalog-card > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.catalog-card > header strong,
.catalog-card > header small {
  display: block;
}

.catalog-card > header small {
  margin-top: 2px;
  font-family: SFMono-Regular, Consolas, monospace;
  color: var(--el-text-color-secondary);
}

.category-list {
  display: grid;
  gap: 6px;
  padding: 10px;
}

.category-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 12px;
  color: var(--el-text-color-primary);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
}

.category-row:hover {
  background: var(--el-fill-color-light);
}

.category-row.is-active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}

.category-row b,
.category-row small {
  display: block;
}

.category-row b,
.catalog-code {
  font-family: SFMono-Regular, Consolas, monospace;
}

.category-row small {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
}

.catalog-card__actions {
  display: flex;
  align-items: center;
}

.category-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  border-top: 1px solid var(--el-border-color-lighter);
}

@media (width <= 860px) {
  .catalog-grid {
    grid-template-columns: 1fr;
  }
}

@media (width <= 560px) {
  .panel-heading {
    flex-direction: column;
  }

  .panel-heading .el-button {
    width: 100%;
  }

  .catalog-card > header {
    align-items: flex-start;
    flex-direction: column;
    padding-block: 12px;
  }

  .category-status {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
