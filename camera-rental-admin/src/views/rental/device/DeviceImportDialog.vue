<template>
  <el-dialog
    v-model="visible"
    :title="tr('title')"
    width="min(1060px, calc(100vw - 24px))"
    class="device-import-dialog"
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    :show-close="!busy"
    destroy-on-close
    @closed="reset"
  >
    <div class="import-body">
      <div class="mobile-step"
        >{{ step + 1 }} / 3 · {{ [tr('uploadStep'), tr('reviewStep'), tr('printStep')][step] }}</div
      >
      <el-steps :active="step" simple class="mb-16px import-steps">
        <el-step :title="tr('uploadStep')" /><el-step :title="tr('reviewStep')" /><el-step
          :title="tr('printStep')"
        />
      </el-steps>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="mb-16px"
      />
      <template v-if="step === 0">
        <el-form label-position="left" label-width="110px">
          <el-form-item :label="tr('mode')">
            <el-radio-group v-model="mode" :disabled="busy">
              <el-radio value="REPRINT">{{ tr('reprint') }}</el-radio>
              <el-radio value="CREATE" :disabled="busy || !canCreate">{{ tr('create') }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <el-upload
          drag
          multiple
          accept=".txt"
          :auto-upload="false"
          :show-file-list="false"
          :disabled="busy"
          :on-change="addFile"
          :limit="MAX_IMPORT_FILES"
          :on-exceed="fileLimit"
          ref="uploadRef"
        >
          <Icon icon="ep:upload-filled" :size="30" />
          <div class="el-upload__text"
            >{{ tr('drop') }} <em>{{ tr('choose') }}</em></div
          >
          <template #tip
            ><div class="el-upload__tip">{{ tr('limits') }}</div></template
          >
        </el-upload>
        <div class="file-heading"
          ><strong>{{ tr('files', { count: files.length }) }}</strong>
          <el-button link type="primary" @click="downloadExample">{{
            tr('sample')
          }}</el-button></div
        >
        <el-table :data="files" border max-height="320">
          <el-table-column :label="tr('file')" min-width="180">
            <template #default="{ row }"
              ><div>{{ row.name }}</div>
              <div
                v-for="issue in row.parsed.issues"
                :key="`${issue.lineNumber}-${issue.code}`"
                class="parse-error"
              >
                {{ tr('line', { line: issue.lineNumber }) }} · {{ tr(`parse.${issue.code}`) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="tr('category')" min-width="140"
            ><template #default="{ row }">
              <el-select
                v-model="row.categoryCode"
                :disabled="busy"
                filterable
                :placeholder="tr('selectCategory')"
                @change="row.modelCode = ''"
              >
                <el-option
                  v-for="category in catalog"
                  :key="category.id"
                  :label="category.categoryName"
                  :value="category.categoryCode"
                />
              </el-select> </template
          ></el-table-column>
          <el-table-column :label="tr('model')" min-width="135"
            ><template #default="{ row }">
              <el-select
                v-model="row.modelCode"
                :disabled="busy || !row.categoryCode"
                filterable
                :placeholder="tr('selectModel')"
              >
                <el-option
                  v-for="model in models(row.categoryCode)"
                  :key="model.id"
                  :label="model.modelName"
                  :value="model.modelCode"
                />
              </el-select> </template
          ></el-table-column>
          <el-table-column :label="tr('singleColumn')" min-width="130"
            ><template #default="{ row }">
              <el-select v-model="row.singleColumn" :disabled="busy" @change="reparse(row)">
                <el-option :label="tr('serial')" value="serial" /><el-option
                  :label="tr('deviceNo')"
                  value="device"
                />
              </el-select> </template
          ></el-table-column>
          <el-table-column :label="tr('count')" width="85"
            ><template #default="{ row }">{{ row.parsed.rows.length }}</template></el-table-column
          >
          <el-table-column :label="tr('action')" width="90"
            ><template #default="{ row }">
              <el-button link type="danger" :disabled="busy" @click="removeFile(row.uid)">{{
                tr('remove')
              }}</el-button>
            </template></el-table-column
          >
          <template #empty>{{ tr('empty') }}</template>
        </el-table>
        <el-alert
          v-if="files.length && !configured"
          class="mt-12px"
          type="warning"
          show-icon
          :closable="false"
          :title="tr('configureHint')"
        />
        <el-collapse class="mt-12px"
          ><el-collapse-item :title="tr('format')" name="format">
            <p>{{ tr('formatHelp') }}</p
            ><pre>{{ sampleText }}</pre>
          </el-collapse-item></el-collapse
        >
      </template>
      <template v-else-if="step === 1 && preview">
        <el-alert
          :title="tr('summary', counts)"
          :type="preview.canSubmit ? 'info' : 'warning'"
          show-icon
          :closable="false"
          class="mb-16px"
        />
        <el-table :data="preview.rows" border max-height="430">
          <el-table-column :label="tr('source')" min-width="160"
            ><template #default="{ row }"
              >{{ row.fileName }}:{{ row.lineNumber }}</template
            ></el-table-column
          >
          <el-table-column prop="deviceNo" :label="tr('deviceNo')" min-width="120" />
          <el-table-column prop="serialNumber" :label="tr('serial')" min-width="145" />
          <el-table-column prop="equipmentModelCode" :label="tr('model')" width="95" />
          <el-table-column :label="tr('status')" min-width="210"
            ><template #default="{ row }">
              <el-tag
                :type="
                  row.status === 'MATCHED'
                    ? 'success'
                    : ['CONFLICT', 'MISSING'].includes(row.status)
                      ? 'danger'
                      : 'info'
                "
                >{{ tr(`statusLabels.${row.status}`) }}</el-tag
              >
              <span class="row-reason">{{ tr(`reasons.${row.reason}`) }}</span>
            </template></el-table-column
          >
        </el-table>
        <p class="help">{{ mode === 'CREATE' ? tr('commitHint') : tr('reprintHint') }}</p>
      </template>
      <template v-else-if="step === 2">
        <el-alert
          :title="tr('ready', { count: deviceIds.length })"
          type="success"
          :closable="false"
          show-icon
          class="mb-16px"
        />
        <el-form label-position="top" class="label-form">
          <el-form-item :label="tr('shop')" required
            ><el-input v-model="shopName" :disabled="busy" maxlength="24" @input="invalidateLabel"
          /></el-form-item>
          <el-form-item :label="tr('phone')" required
            ><el-input v-model="phone" :disabled="busy" maxlength="32" @input="invalidateLabel"
          /></el-form-item>
        </el-form>
        <p class="help">{{ tr('printHint') }}</p>
        <el-button :loading="busy" :disabled="!labelValid" @click="loadLabel">{{
          tr('previewLabel')
        }}</el-button>
        <div v-if="labelImage" class="label-preview"
          ><el-image :src="labelImage" fit="contain" :alt="tr('previewLabel')" />
          <p>{{ tr('size') }}</p></div
        >
        <el-alert class="mt-16px" type="info" :closable="false" :title="tr('zipHint')" />
        <p v-if="downloaded" role="status">{{ tr('downloaded') }}</p>
      </template>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <span>{{
          step === 2
            ? tr('labelCount', { count: deviceIds.length })
            : tr('total', { files: files.length, count: total })
        }}</span>
        <div>
          <el-button :disabled="busy" @click="visible = false">{{
            step === 2 ? tr('close') : tr('cancel')
          }}</el-button>
          <el-button v-if="step === 1" :disabled="busy" @click="backToConfiguration">{{
            tr('back')
          }}</el-button>
          <el-button
            v-if="step === 0"
            type="primary"
            :loading="busy"
            :disabled="!configured || total > MAX_IMPORT_ROWS"
            @click="review"
            >{{ tr('next') }}</el-button
          >
          <el-button
            v-if="step === 1"
            type="primary"
            :loading="busy"
            :disabled="!preview?.canSubmit"
            @click="confirm"
          >
            {{ mode === 'CREATE' ? tr('commit', { count: counts.newCount }) : tr('continue') }}
          </el-button>
          <el-button
            v-if="step === 2"
            type="primary"
            :loading="busy"
            :disabled="!labelValid || !labelImage"
            @click="downloadZip"
            >{{ tr('download') }}</el-button
          >
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import type { UploadFile, UploadInstance } from 'element-plus'
import type { RentalDeviceCategoryVO } from '@/api/rental/device'
import {
  commitDeviceImport,
  downloadDeviceLabels,
  previewDeviceImport,
  previewDeviceLabels,
  type DeviceImportPreview,
  type DeviceLabelRequest
} from '@/api/rental/deviceImport'
import { useI18n } from '@/hooks/web/useI18n'
import { useLocaleStore } from '@/store/modules/locale'
import { checkPermi } from '@/utils/permission'
import download from '@/utils/download'
import {
  decodeDeviceFile,
  parseDeviceText,
  MAX_IMPORT_FILES,
  MAX_IMPORT_ROWS,
  MAX_FILE_BYTES,
  type SingleColumn
} from './deviceImportModel'

const props = defineProps<{ catalog: RentalDeviceCategoryVO[] }>()
const emit = defineEmits<{ success: [] }>()
const { t } = useI18n()
const tr = (key: string, values?: Record<string, unknown>) =>
  values ? t(`rental.deviceImport.${key}`, values) : t(`rental.deviceImport.${key}`)
const localeStore = useLocaleStore()
const canCreate = computed(() => checkPermi(['rental:device:create']))
const visible = ref(false),
  step = ref(0),
  busy = ref(false),
  error = ref('')
const mode = ref<'REPRINT' | 'CREATE'>('REPRINT')
interface DeviceFile {
  uid: number
  name: string
  text: string
  categoryCode: string
  modelCode: string
  singleColumn: SingleColumn
  parsed: ReturnType<typeof parseDeviceText>
}
const files = ref<DeviceFile[]>([]),
  uploadRef = ref<UploadInstance>()
const preview = ref<DeviceImportPreview>(),
  deviceIds = ref<number[]>([])
const shopName = ref(''),
  phone = ref(''),
  labelImage = ref(''),
  downloaded = ref(false)
let generation = 0
const models = (category: string) =>
  props.catalog.find((item) => item.categoryCode === category)?.models || []
const total = computed(() => files.value.reduce((sum, file) => sum + file.parsed.rows.length, 0))
const configured = computed(
  () =>
    files.value.length > 0 &&
    files.value.every(
      (file) =>
        file.categoryCode &&
        models(file.categoryCode).some((m) => m.modelCode === file.modelCode) &&
        !file.parsed.issues.length
    )
)
const counts = computed(() => ({
  matched: preview.value?.rows.filter((row) => row.status === 'MATCHED').length || 0,
  newCount: preview.value?.rows.filter((row) => row.status === 'NEW').length || 0,
  duplicates: preview.value?.rows.filter((row) => row.status === 'DUPLICATE').length || 0,
  conflicts:
    preview.value?.rows.filter((row) => ['CONFLICT', 'MISSING'].includes(row.status)).length || 0
}))
const labelValid = computed(
  () => !!shopName.value.trim() && /^[0-9+() -]{3,32}$/.test(phone.value.trim())
)
const sampleText = `device_no,serial_number
A5-01,DEMO000001
A5-02,DEMO000002
`
const labelRequest = (): DeviceLabelRequest => ({
  deviceIds: deviceIds.value,
  shopName: shopName.value.trim(),
  phone: phone.value.trim(),
  locale: localeStore.getCurrentLocale.lang === 'en' ? 'en' : 'zh-CN'
})
function reset() {
  generation++
  step.value = 0
  files.value = []
  preview.value = undefined
  deviceIds.value = []
  error.value = ''
  labelImage.value = ''
  busy.value = false
  downloaded.value = false
  mode.value = 'REPRINT'
  shopName.value = ''
  phone.value = ''
}
function open() {
  reset()
  visible.value = true
}
function openForDevices(ids: number[]) {
  reset()
  deviceIds.value = [...new Set(ids)]
  step.value = 2
  visible.value = true
}
defineExpose({ open, openForDevices })
onBeforeUnmount(() => {
  generation++
})
function reparse(file: DeviceFile) {
  file.parsed = parseDeviceText(file.text, file.singleColumn)
}
function removeFile(uid: number) {
  files.value = files.value.filter((file) => file.uid !== uid)
  error.value = ''
}
function fileLimit() {
  error.value = tr('fileLimit')
}
async function addFile(upload: UploadFile) {
  if (!upload.raw) return
  uploadRef.value?.clearFiles()
  if (files.value.length >= MAX_IMPORT_FILES) {
    fileLimit()
    return
  }
  if (
    !/\.txt$/i.test(upload.name) ||
    upload.raw.size > MAX_FILE_BYTES ||
    upload.name.length > 128
  ) {
    error.value = tr('fileInvalid')
    return
  }
  const token = generation
  // Add a placeholder immediately so concurrent file reads count toward the limit.
  const item: DeviceFile = {
    uid: upload.uid,
    name: upload.name,
    text: '',
    categoryCode: '',
    modelCode: '',
    singleColumn: 'serial',
    parsed: { rows: [], issues: [{ lineNumber: 1, code: 'READING' }] }
  }
  files.value.push(item)
  try {
    const text = decodeDeviceFile(await upload.raw.arrayBuffer())
    if (token !== generation) return
    const file = files.value.find((file) => file.uid === upload.uid)
    if (!file) return
    file.text = text
    reparse(file)
    const stem = upload.name.replace(/\.txt$/i, '').toUpperCase()
    const candidates = props.catalog
      .flatMap((category) => category.models.map((model) => ({ category, model })))
      .filter(
        ({ model }) =>
          stem.split(/[\s_.-]+/).includes(model.modelCode.toUpperCase()) ||
          stem === model.modelCode.toUpperCase()
      )
    if (candidates.length === 1) {
      file.categoryCode = candidates[0].category.categoryCode
      file.modelCode = candidates[0].model.modelCode
    }
    if (total.value > MAX_IMPORT_ROWS) error.value = tr('parse.LIMIT')
  } catch {
    if (token !== generation) return
    const file = files.value.find((file) => file.uid === upload.uid)
    if (file) file.parsed.issues = [{ lineNumber: 1, code: 'ENCODING' }]
  }
}
async function run(action: (isCurrent: () => boolean) => Promise<void>) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  const token = generation
  try {
    await action(() => token === generation && visible.value)
  } catch {
    if (token === generation) error.value = tr('requestFailed')
  } finally {
    if (token === generation) busy.value = false
  }
}
async function review() {
  if (!configured.value || total.value > MAX_IMPORT_ROWS) return
  await run(async (isCurrent) => {
    const result = await previewDeviceImport({
      mode: mode.value,
      rows: files.value.flatMap((file) =>
        file.parsed.rows.map((row) => ({
          ...row,
          fileName: file.name,
          categoryCode: file.categoryCode,
          equipmentModelCode: file.modelCode
        }))
      )
    })
    if (!isCurrent()) return
    preview.value = result
    step.value = 1
  })
}
function backToConfiguration() {
  step.value = 0
  preview.value = undefined
  error.value = ''
}
async function confirm() {
  if (!preview.value?.canSubmit) return
  await run(async (isCurrent) => {
    const current = preview.value!
    const ids =
      mode.value === 'CREATE'
        ? await commitDeviceImport(current.batchId)
        : [
            ...new Set(
              current.rows.filter((row) => row.status === 'MATCHED').map((row) => row.deviceId!)
            )
          ]
    if (!isCurrent()) return
    deviceIds.value = ids
    if (mode.value === 'CREATE') emit('success')
    step.value = 2
  })
}
function invalidateLabel() {
  labelImage.value = ''
  downloaded.value = false
}
async function loadLabel() {
  await run(async (isCurrent) => {
    labelImage.value = ''
    const result = await previewDeviceLabels(labelRequest())
    if (!isCurrent()) return
    labelImage.value = result.imageDataUrl
  })
}
async function downloadZip() {
  await run(async (isCurrent) => {
    const blob = await downloadDeviceLabels(labelRequest())
    if (!isCurrent()) return
    download.zip(blob, 'device-labels.zip')
    downloaded.value = true
  })
}
function downloadExample() {
  const url = URL.createObjectURL(
    new Blob(['\ufeff' + sampleText], { type: 'text/plain;charset=utf-8' })
  )
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'device-import-example.txt'
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
</script>

<style scoped lang="scss">
.import-body {
  max-height: calc(100dvh - 220px);
  overflow-y: auto;
}
.mobile-step {
  display: none;
}
@media (max-width: 640px) {
  .import-steps {
    display: none;
  }
  .mobile-step {
    display: block;
    margin-bottom: 16px;
    color: var(--el-text-color-primary);
    font-weight: 500;
  }
}
.file-heading,
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.file-heading {
  margin: 16px 0 8px;
}
.dialog-footer > span,
.help,
.row-reason {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.row-reason {
  display: block;
  margin-top: 4px;
}
.help {
  margin: 12px 0;
}
.parse-error {
  color: var(--el-color-danger);
  font-size: 12px;
}
.label-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.label-preview {
  padding: 24px 0;
  text-align: center;
}
.label-preview .el-image {
  width: min(100%, 472px);
  border: 1px solid var(--el-border-color);
}
pre {
  white-space: pre-wrap;
  font-family: monospace;
}
@media (max-width: 640px) {
  .dialog-footer {
    align-items: flex-start;
    flex-direction: column;
  }
  .dialog-footer > div {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .label-form {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
