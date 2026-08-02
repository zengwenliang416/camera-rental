<script setup lang="ts">
import type {
  PhotoCategory,
  PhotoUploadTask,
  RegistrationStatus,
  ReturnContext,
  ReturnDraft,
  ReturnReceipt,
  UploadedPhoto
} from '~/types/return-registration'
import { normalizeReturnSerial, returnSerialPattern } from '~/utils/returnSerial'
import { hasRequiredReturnPhotos } from '~/utils/returnValidation'

const api = useReturnRegistration()
const preferences = useReturnPreferences()
const { locale, theme, t } = preferences
const step = ref(0)
const loading = ref(true)
const verifying = ref(false)
const submitting = ref(false)
const error = ref('')
const context = ref<ReturnContext>()
const receipt = ref<ReturnReceipt>()
const verification = reactive({
  orderNo: '',
  mobileLast4: ''
})
const uploadTasks = reactive<PhotoUploadTask[]>([])
const draft = reactive<ReturnDraft>({
  carrierCode: '',
  carrierName: '',
  waybillNo: '',
  shippedDate: '',
  serials: [''],
  issueDescription: '',
  photos: []
})
const carriers = [
  { code: 'SHUNFENG', zh: '顺丰速运', en: 'SF Express' },
  { code: 'JD', zh: '京东物流', en: 'JD Logistics' },
  { code: 'ZHONGTONG', zh: '中通快递', en: 'ZTO Express' },
  { code: 'YUANTONG', zh: '圆通速递', en: 'YTO Express' },
  { code: 'SHENTONG', zh: '申通快递', en: 'STO Express' },
  { code: 'YUNDA', zh: '韵达快递', en: 'Yunda Express' },
  { code: 'DEPPON', zh: '德邦快递', en: 'Deppon Express' },
  { code: 'OTHER', zh: '其他', en: 'Other' }
]

useHead(() => ({ title: `${t('service')} · 捷租达` }))

onMounted(async () => {
  preferences.initialize()
  try {
    context.value = await api.loadContext()
    receipt.value = context.value.receipt
    restoreDraft()
  } catch {
    context.value = undefined
  } finally {
    loading.value = false
  }
})

function restoreDraft() {
  if (!context.value?.formNo || context.value.status !== 'DRAFT') return
  const saved = sessionStorage.getItem(`return-draft:${context.value.formNo}`)
  if (saved) Object.assign(draft, JSON.parse(saved))
}

async function verifyOrder() {
  error.value = ''
  const orderNo = verification.orderNo.trim()
  const mobileLast4 = verification.mobileLast4.replace(/\D/g, '')
  if (!orderNo || !/^\d{4}$/.test(mobileLast4)) {
    error.value = t('verificationInputError')
    return
  }
  verifying.value = true
  try {
    context.value = await api.verify(orderNo, mobileLast4)
    receipt.value = context.value.receipt
    restoreDraft()
  } catch {
    error.value = t('verificationFailed')
  } finally {
    verifying.value = false
  }
}

watch(
  draft,
  (value) => {
    if (import.meta.client && context.value?.status === 'DRAFT') {
      sessionStorage.setItem(`return-draft:${context.value.formNo}`, JSON.stringify(value))
    }
  },
  { deep: true }
)

const photos = (category: PhotoCategory) =>
  draft.photos.filter((photo) => photo.category === category)
const tasks = (category: PhotoCategory) =>
  uploadTasks.filter((task) => task.category === category)
const isUploadBusy = (category: PhotoCategory) =>
  tasks(category).some((task) => task.status === 'UPLOADING')

function selectCarrier() {
  draft.carrierName = carriers.find((carrier) => carrier.code === draft.carrierCode)?.zh || ''
}

function validateCurrent() {
  error.value = ''
  if (step.value === 1 && (!draft.carrierCode || !draft.waybillNo.trim() || !draft.shippedDate)) {
    error.value = t('completeLogistics')
  }
  if (step.value === 2) {
    draft.serials = draft.serials.map(normalizeReturnSerial)
    if (
      draft.serials.some((serial) => !returnSerialPattern.test(serial)) ||
      new Set(draft.serials).size !== draft.serials.length
    ) {
      error.value = t('invalidSerial')
    }
  }
  if (step.value === 3 && !hasRequiredReturnPhotos(draft.photos)) {
    error.value = t('missingPhotos')
  }
  return !error.value
}

function next() {
  if (!validateCurrent()) return
  step.value = Math.min(4, step.value + 1)
  scrollTo({ top: 0, behavior: 'smooth' })
}

function addSerial() {
  if (draft.serials.length < 8) draft.serials.push('')
}

async function uploadFiles(category: PhotoCategory, files: File[]) {
  error.value = ''
  for (const file of files) {
    const task = reactive<PhotoUploadTask>({
      id: crypto.randomUUID(),
      category,
      file,
      progress: 0,
      status: 'UPLOADING'
    })
    uploadTasks.push(task)
    await runUpload(task)
  }
}

async function runUpload(task: PhotoUploadTask) {
  task.status = 'UPLOADING'
  task.progress = 0
  task.error = undefined
  try {
    if (task.file.size > 15 * 1024 * 1024) {
      throw new Error(`${task.file.name} ${t('oversized')}`)
    }
    const photo = await api.upload(task.file, task.category, (value) => {
      task.progress = value
    })
    draft.photos.push(photo)
    uploadTasks.splice(uploadTasks.findIndex((value) => value.id === task.id), 1)
  } catch (cause) {
    task.status = 'FAILED'
    task.error = cause instanceof Error ? cause.message : t('uploadFailed')
    error.value = task.error
  }
}

async function removePhoto(photo: UploadedPhoto) {
  try {
    await api.removePhoto(photo.attachmentId)
    draft.photos = draft.photos.filter((item) => item.attachmentId !== photo.attachmentId)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : t('deleteFailed')
  }
}

async function submit() {
  if (!context.value || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    const keyName = `return-submit-key:${context.value.formNo}`
    let key = sessionStorage.getItem(keyName)
    if (!key) {
      key = crypto.randomUUID()
      sessionStorage.setItem(keyName, key)
    }
    receipt.value = await api.submit(context.value, draft, key)
    context.value.status = receipt.value.status
    sessionStorage.removeItem(`return-draft:${context.value.formNo}`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : t('submitFailed')
  } finally {
    submitting.value = false
  }
}

const status = computed<RegistrationStatus | 'INVALID'>(() => {
  if (!context.value) return 'INVALID'
  return context.value.status
})
</script>

<template>
  <main class="return-page">
    <header class="return-topbar">
      <a href="/" class="return-brand"><b>J</b><span><strong>捷租达</strong><small>{{ t('service') }}</small></span></a>
      <div class="return-tools">
        <button type="button" :aria-label="t('theme')" @click="preferences.toggleTheme()">
          {{ theme === 'light' ? '◐' : '☀' }}
        </button>
        <button type="button" @click="preferences.toggleLocale()">{{ t('locale') }}</button>
        <a href="tel:" class="return-help">{{ t('help') }}</a>
      </div>
    </header>

    <section v-if="loading" class="return-loading" aria-live="polite">
      <span />
      <p>{{ t('loading') }}</p>
    </section>

    <section v-else-if="!context" class="verification-panel">
      <p class="return-kicker">ORDER VERIFICATION</p>
      <h1>{{ t('verificationTitle') }}</h1>
      <p>{{ t('verificationBody') }}</p>
      <form @submit.prevent="verifyOrder">
        <label>
          <span>{{ t('xianyuOrderNo') }}</span>
          <input
            v-model="verification.orderNo"
            maxlength="128"
            autocomplete="off"
            :placeholder="t('xianyuOrderPlaceholder')"
          >
        </label>
        <label>
          <span>{{ t('mobileLast4') }}</span>
          <input
            v-model="verification.mobileLast4"
            maxlength="4"
            inputmode="numeric"
            autocomplete="off"
            :placeholder="t('mobileLast4Placeholder')"
          >
        </label>
        <p v-if="error" class="return-error" role="alert">{{ error }}</p>
        <button type="submit" :disabled="verifying">
          {{ verifying ? t('verifying') : t('verifyAndContinue') }}
        </button>
      </form>
      <div class="verification-privacy">
        <b>{{ t('verificationPrivacyTitle') }}</b>
        <span>{{ t('verificationPrivacyBody') }}</span>
      </div>
    </section>

    <ReturnStatusPanel
      v-else-if="status !== 'DRAFT'"
      :status="status"
      :receipt="receipt"
    />

    <div v-else-if="context" class="return-layout">
      <aside class="return-aside">
        <p class="return-kicker">{{ t('kicker') }}</p>
        <h1>{{ t('introTitle') }}</h1>
        <p>{{ t('introBody') }}</p>
        <div class="return-ticket">
          <small>{{ t('registration') }}</small>
          <strong>{{ context.orderNo }}</strong>
          <span>{{ context.assignedDeviceCount }} {{ t('assigned') }}</span>
        </div>
        <ReturnProgress :step="step" />
        <div class="return-privacy"><b>{{ t('privacyTitle') }}</b><span>{{ t('privacyBody') }}</span></div>
      </aside>

      <section class="return-form-shell">
        <div class="mobile-step">
          <span>{{ locale === 'zh-CN' ? '第 ' : '' }}{{ step + 1 }} {{ t('stepCount') }}</span>
          <i><b :style="{ width: `${(step + 1) * 20}%` }" /></i>
        </div>

        <div v-if="step === 0" class="return-step">
          <header><span>01</span><div><p>ORDER</p><h2>{{ t('orderTitle') }}</h2></div></header>
          <div class="order-confirm">
            <small>{{ t('orderNo') }}</small><strong>{{ context.orderNo }}</strong>
            <span v-if="context.rentalStart">{{ t('rentalPeriod') }} {{ context.rentalStart }} - {{ context.rentalEnd }}</span>
          </div>
          <p class="field-note">{{ t('orderNote') }}</p>
        </div>

        <div v-else-if="step === 1" class="return-step">
          <header><span>02</span><div><p>LOGISTICS</p><h2>{{ t('logisticsTitle') }}</h2></div></header>
          <div class="field-grid">
            <label><span>{{ t('carrier') }} <em>{{ t('required') }}</em></span><select v-model="draft.carrierCode" @change="selectCarrier">
              <option value="">{{ t('chooseCarrier') }}</option>
              <option v-for="carrier in carriers" :key="carrier.code" :value="carrier.code">
                {{ locale === 'zh-CN' ? carrier.zh : carrier.en }}
              </option>
            </select></label>
            <label><span>{{ t('shippedDate') }} <em>{{ t('required') }}</em></span><input v-model="draft.shippedDate" type="date"></label>
          </div>
          <label><span>{{ t('waybill') }} <em>{{ t('required') }}</em></span><input v-model.trim="draft.waybillNo" autocomplete="off" :placeholder="t('waybillPlaceholder')"></label>
          <p class="field-note">{{ t('logisticsNote') }}</p>
        </div>

        <div v-else-if="step === 2" class="return-step">
          <header><span>03</span><div><p>DEVICES</p><h2>{{ t('deviceTitle') }}</h2></div></header>
          <div class="serial-list">
            <div v-for="(_, index) in draft.serials" :key="index">
              <b>{{ String(index + 1).padStart(2, '0') }}</b>
              <input v-model="draft.serials[index]" maxlength="48" :placeholder="t('serialPlaceholder')" @blur="draft.serials[index] = normalizeReturnSerial(draft.serials[index] || '')">
              <button v-if="draft.serials.length > 1" type="button" @click="draft.serials.splice(index, 1)">×</button>
            </div>
          </div>
          <button class="add-serial" type="button" :disabled="draft.serials.length >= 8" @click="addSerial">{{ t('addDevice') }}</button>
          <div class="serial-sample"><code>A6-08-4L5H</code><span><b>{{ t('serialHintTitle') }}</b>{{ t('serialHint') }}</span></div>
        </div>

        <div v-else-if="step === 3" class="return-step">
          <header><span>04</span><div><p>PHOTOS</p><h2>{{ t('photoTitle') }}</h2></div></header>
          <ReturnPhotoField category="DEVICE_EXTERIOR" :title="t('exterior')" :hint="t('exteriorHint')" required :photos="photos('DEVICE_EXTERIOR')" :tasks="tasks('DEVICE_EXTERIOR')" :busy="isUploadBusy('DEVICE_EXTERIOR')" @upload="uploadFiles('DEVICE_EXTERIOR', $event)" @remove="removePhoto" @retry="runUpload" />
          <ReturnPhotoField category="SERIAL_LABEL" :title="t('label')" :hint="t('labelHint')" required :photos="photos('SERIAL_LABEL')" :tasks="tasks('SERIAL_LABEL')" :busy="isUploadBusy('SERIAL_LABEL')" @upload="uploadFiles('SERIAL_LABEL', $event)" @remove="removePhoto" @retry="runUpload" />
          <ReturnPhotoField category="PACKAGING" :title="t('packaging')" :hint="t('packagingHint')" :photos="photos('PACKAGING')" :tasks="tasks('PACKAGING')" :busy="isUploadBusy('PACKAGING')" @upload="uploadFiles('PACKAGING', $event)" @remove="removePhoto" @retry="runUpload" />
          <ReturnPhotoField category="DAMAGE_DETAIL" :title="t('damagePhotos')" :hint="t('damagePhotosHint')" :photos="photos('DAMAGE_DETAIL')" :tasks="tasks('DAMAGE_DETAIL')" :busy="isUploadBusy('DAMAGE_DETAIL')" @upload="uploadFiles('DAMAGE_DETAIL', $event)" @remove="removePhoto" @retry="runUpload" />
          <label><span>{{ t('issue') }} <i>{{ t('optional') }}</i></span><textarea v-model="draft.issueDescription" maxlength="1000" rows="4" :placeholder="t('issuePlaceholder')"></textarea></label>
        </div>

        <div v-else class="return-step">
          <header><span>05</span><div><p>REVIEW</p><h2>{{ t('reviewTitle') }}</h2></div></header>
          <dl class="review-list">
            <div><dt>{{ t('orderNo') }}</dt><dd>{{ context.orderNo }}</dd></div>
            <div><dt>{{ t('returnLogistics') }}</dt><dd>{{ draft.carrierName }} · {{ draft.waybillNo }}<br>{{ draft.shippedDate }}</dd></div>
            <div><dt>{{ t('deviceSerials') }}</dt><dd><code v-for="serial in draft.serials" :key="serial">{{ serial }}</code></dd></div>
            <div><dt>{{ t('returnPhotos') }}</dt><dd>{{ t('uploaded') }} {{ draft.photos.length }} {{ t('photosOptional') }}</dd></div>
            <div><dt>{{ t('issueSummary') }}</dt><dd>{{ draft.issueDescription || t('noIssue') }}</dd></div>
          </dl>
        </div>

        <p v-if="error" class="return-error" role="alert">{{ error }}</p>
        <footer class="return-actions">
          <button v-if="step > 0" type="button" class="secondary" @click="step--">{{ t('previous') }}</button>
          <button v-if="step < 4" type="button" class="primary" @click="next">{{ t('next') }}</button>
          <button v-else type="button" class="primary" :disabled="submitting" @click="submit">
            {{ submitting ? t('submitting') : t('submit') }}
          </button>
        </footer>
      </section>
    </div>
  </main>
</template>
