<script setup lang="ts">
import type {
  RegistrationStatus,
  ReturnContext,
  ReturnMethod,
  ReturnReceipt
} from '~/types/return-registration'
import { normalizeReturnSerial, returnSerialPattern } from '~/utils/returnSerial'

type SelectedPhotoStatus = 'PENDING' | 'UPLOADING' | 'UPLOADED' | 'FAILED'

interface SelectedPhoto {
  id: string
  file: File
  previewUrl: string
  progress: number
  status: SelectedPhotoStatus
  attachmentId?: number
  error?: string
}

const MAX_PHOTO_COUNT = 5
const MAX_PHOTO_SIZE = 15 * 1024 * 1024
const ALLOWED_PHOTO_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const api = useReturnRegistration()
const preferences = useReturnPreferences()
const { theme, t } = preferences
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const context = ref<ReturnContext>()
const receipt = ref<ReturnReceipt>()
const photos = reactive<SelectedPhoto[]>([])
const form = reactive({
  orderNo: '',
  senderMobile: '',
  machineCode: '',
  waybillNo: '',
  errandPlatformName: '',
  returnMethod: 'EXPRESS' as ReturnMethod
})

const methodOptions = computed(() => [
  { value: 'EXPRESS' as const, label: t('returnMethodExpress') },
  { value: 'ERRAND' as const, label: t('returnMethodErrand') },
  { value: 'SELF_DELIVERY' as const, label: t('returnMethodSelf') }
])

useHead(() => ({ title: `${t('service')} · 捷租达` }))

onMounted(async () => {
  preferences.initialize()
  try {
    context.value = await api.loadContext()
    receipt.value = context.value.receipt
  } catch {
    context.value = undefined
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  for (const photo of photos) URL.revokeObjectURL(photo.previewUrl)
})

function validate() {
  error.value = ''
  const orderNo = form.orderNo.trim()
  const senderMobile = form.senderMobile.replace(/\D/g, '')
  const machineCode = normalizeReturnSerial(form.machineCode)
  const waybillNo = form.waybillNo.trim()
  const errandPlatformName = form.errandPlatformName.trim()

  if (!/^1\d{10}$/.test(senderMobile)) {
    error.value = t('senderMobileRequired')
    return false
  }
  if (!returnSerialPattern.test(machineCode)) {
    error.value = t('machineCodeRequired')
    return false
  }
  if (form.returnMethod === 'EXPRESS' && !waybillNo) {
    error.value = t('waybillRequired')
    return false
  }
  if (form.returnMethod === 'ERRAND' && !errandPlatformName) {
    error.value = t('errandPlatformRequired')
    return false
  }

  form.orderNo = orderNo
  form.senderMobile = senderMobile
  form.machineCode = machineCode
  form.waybillNo = form.returnMethod === 'EXPRESS' ? waybillNo : ''
  form.errandPlatformName = form.returnMethod === 'ERRAND' ? errandPlatformName : ''
  return true
}

function selectPhotos(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  error.value = ''

  if (photos.length + files.length > MAX_PHOTO_COUNT) {
    error.value = t('photoLimit')
    return
  }
  for (const file of files) {
    if (!ALLOWED_PHOTO_TYPES.has(file.type)) {
      error.value = t('photoTypeInvalid')
      continue
    }
    if (file.size > MAX_PHOTO_SIZE) {
      error.value = `${file.name} ${t('oversized')}`
      continue
    }
    photos.push({
      id: crypto.randomUUID(),
      file,
      previewUrl: URL.createObjectURL(file),
      progress: 0,
      status: 'PENDING'
    })
  }
}

async function uploadPhoto(photo: SelectedPhoto) {
  photo.status = 'UPLOADING'
  photo.progress = 0
  photo.error = undefined
  try {
    const uploaded = await api.upload(photo.file, 'RETURN_PHOTO', (progress) => {
      photo.progress = progress
    })
    photo.attachmentId = uploaded.attachmentId
    photo.status = 'UPLOADED'
  } catch (cause) {
    photo.status = 'FAILED'
    photo.error = cause instanceof Error ? cause.message : t('uploadFailed')
    throw cause
  }
}

async function retryPhoto(photo: SelectedPhoto) {
  if (submitting.value || photo.status === 'UPLOADING') return
  error.value = ''
  try {
    await uploadPhoto(photo)
  } catch {
    error.value = photo.error || t('uploadFailed')
  }
}

async function removePhoto(photo: SelectedPhoto) {
  if (submitting.value || photo.status === 'UPLOADING') return
  error.value = ''
  try {
    if (photo.attachmentId) await api.removePhoto(photo.attachmentId)
    URL.revokeObjectURL(photo.previewUrl)
    photos.splice(photos.findIndex((item) => item.id === photo.id), 1)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : t('deleteFailed')
  }
}

async function submit() {
  if (submitting.value || !validate()) return
  submitting.value = true
  error.value = ''
  try {
    if (photos.length) {
      await api.verify(form.orderNo, form.machineCode)
      for (const photo of photos) {
        if (photo.status !== 'UPLOADED') await uploadPhoto(photo)
      }
    }
    receipt.value = await api.simpleSubmit({
      ...form,
      attachmentIds: photos
        .map((photo) => photo.attachmentId)
        .filter((attachmentId): attachmentId is number => attachmentId !== undefined)
    })
    context.value = undefined
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : t('submitFailed')
  } finally {
    submitting.value = false
  }
}

const status = computed<RegistrationStatus | undefined>(() =>
  receipt.value?.status || (context.value?.status === 'DRAFT' ? undefined : context.value?.status)
)
</script>

<template>
  <main class="return-page">
    <header class="return-topbar">
      <a href="/" class="return-brand">
        <img src="/images/jiezuda-logo.png" alt="捷租达">
        <span><strong>捷租达</strong><small>{{ t('service') }}</small></span>
      </a>
      <div class="return-tools">
        <button type="button" :aria-label="t('theme')" @click="preferences.toggleTheme()">
          {{ theme === 'light' ? '◐' : '☀' }}
        </button>
        <button type="button" @click="preferences.toggleLocale()">{{ t('locale') }}</button>
      </div>
    </header>

    <section v-if="loading" class="return-loading" aria-live="polite">
      <span />
      <p>{{ t('loading') }}</p>
    </section>

    <ReturnStatusPanel
      v-else-if="status"
      :status="status"
      :receipt="receipt || context?.receipt"
    />

    <section v-else class="return-card">
      <div class="return-intro">
        <p class="return-kicker">RETURN CHECK-IN</p>
        <p>{{ t('simpleReturnBody') }}</p>
      </div>

      <form class="return-form" @submit.prevent="submit">
        <label>
          <span>{{ t('xianyuOrderNo') }}</span>
          <input
            v-model="form.orderNo"
            maxlength="128"
            autocomplete="off"
            :placeholder="t('xianyuOrderPlaceholder')"
          >
        </label>

        <label>
          <span>{{ t('senderMobile') }} <b>{{ t('required') }}</b></span>
          <input
            v-model="form.senderMobile"
            maxlength="11"
            inputmode="tel"
            autocomplete="tel"
            :placeholder="t('senderMobilePlaceholder')"
          >
        </label>

        <label>
          <span>{{ t('machineCode') }} <b>{{ t('required') }}</b></span>
          <input
            v-model="form.machineCode"
            maxlength="128"
            autocomplete="off"
            :placeholder="t('serialPlaceholder')"
          >
        </label>

        <fieldset class="return-methods">
          <legend>{{ t('returnMethod') }}</legend>
          <div class="return-method-options">
            <label v-for="option in methodOptions" :key="option.value" class="return-method-option">
              <input
                v-model="form.returnMethod"
                type="radio"
                name="returnMethod"
                :value="option.value"
                :disabled="submitting"
              >
              <span>{{ option.label }}</span>
            </label>
          </div>
          <p v-if="form.returnMethod === 'SELF_DELIVERY'" class="return-method-hint">
            {{ t('selfDeliveryHint') }}
          </p>
        </fieldset>

        <label v-if="form.returnMethod === 'EXPRESS'">
          <span>{{ t('waybill') }} <b>{{ t('required') }}</b></span>
          <input
            v-model="form.waybillNo"
            maxlength="128"
            autocomplete="off"
            :placeholder="t('waybillPlaceholder')"
          >
        </label>

        <label v-else-if="form.returnMethod === 'ERRAND'">
          <span>{{ t('errandPlatform') }} <b>{{ t('required') }}</b></span>
          <input
            v-model="form.errandPlatformName"
            maxlength="128"
            autocomplete="organization"
            :placeholder="t('errandPlatformPlaceholder')"
          >
        </label>

        <fieldset class="return-photos">
          <legend>
            <span>
              <strong>{{ t('optionalPhotos') }}</strong>
              <small>{{ t('optionalPhotosHint') }}</small>
            </span>
            <b>{{ t('photoCount') }} {{ photos.length }}/{{ MAX_PHOTO_COUNT }}</b>
          </legend>

          <div v-if="photos.length" class="return-photo-grid">
            <figure v-for="photo in photos" :key="photo.id">
              <img :src="photo.previewUrl" :alt="photo.file.name">
              <button
                type="button"
                :aria-label="t('remove')"
                :disabled="submitting || photo.status === 'UPLOADING'"
                @click="removePhoto(photo)"
              >
                ×
              </button>
              <figcaption>
                <progress
                  v-if="photo.status === 'UPLOADING'"
                  :value="photo.progress"
                  max="100"
                />
                <span v-if="photo.status === 'PENDING'">{{ t('photoReady') }}</span>
                <span v-else-if="photo.status === 'UPLOADING'">
                  {{ t('uploading') }} {{ photo.progress }}%
                </span>
                <span v-else-if="photo.status === 'UPLOADED'">{{ t('photoUploaded') }}</span>
                <button
                  v-else
                  type="button"
                  :disabled="submitting"
                  @click="retryPhoto(photo)"
                >
                  {{ t('retry') }}
                </button>
              </figcaption>
            </figure>
          </div>

          <label
            class="return-photo-picker"
            :aria-disabled="submitting || photos.length >= MAX_PHOTO_COUNT"
          >
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              :disabled="submitting || photos.length >= MAX_PHOTO_COUNT"
              @change="selectPhotos"
            >
            {{ t('upload') }}
          </label>
        </fieldset>

        <p v-if="error" class="return-error" role="alert">{{ error }}</p>
        <button class="return-submit" type="submit" :disabled="submitting">
          {{ submitting && photos.length ? t('photosUploading') : submitting ? t('submitting') : t('simpleSubmit') }}
        </button>
      </form>

      <div class="return-note">
        <b>{{ t('simplePrivacyTitle') }}</b>
        <span>{{ t('simplePrivacyBody') }}</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
.return-page {
  --ink: #102a20;
  --muted: #667068;
  --paper: #fffdf8;
  --line: #d8d7ce;
  --accent: #0e5b42;
  min-height: 100svh;
  padding: 18px;
  color: var(--ink);
  background:
    radial-gradient(circle at 12% 8%, rgb(236 194 105 / 28%), transparent 30rem),
    radial-gradient(circle at 90% 88%, rgb(29 111 82 / 18%), transparent 28rem),
    #f0eee7;
}

:global(html[data-theme='dark']) .return-page {
  --ink: #edf5ee;
  --muted: #aab8ae;
  --paper: #13241c;
  --line: #385044;
  --accent: #dba84d;
  background:
    radial-gradient(circle at 12% 8%, rgb(219 168 77 / 18%), transparent 30rem),
    radial-gradient(circle at 90% 88%, rgb(39 133 98 / 22%), transparent 28rem),
    #09130f;
}

.return-topbar {
  width: min(100%, 880px);
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.return-brand {
  display: flex;
  gap: 12px;
  align-items: center;
  color: inherit;
  text-decoration: none;
}

.return-brand > img {
  width: 64px;
  height: 52px;
  display: block;
  object-fit: contain;
  border-radius: 12px;
}

.return-brand span {
  display: grid;
}

.return-brand strong {
  font-size: 19px;
}

.return-brand small {
  color: var(--muted);
}

.return-tools {
  display: flex;
  gap: 8px;
}

.return-tools button {
  min-width: 46px;
  height: 46px;
  padding: 0 15px;
  border: 1px solid var(--line);
  border-radius: 14px;
  color: var(--ink);
  background: var(--paper);
  font: inherit;
}

.return-card,
.return-loading {
  width: min(100%, 720px);
  margin: 0 auto;
  border: 1px solid rgb(120 119 110 / 24%);
  border-radius: 30px;
  background: var(--paper);
  box-shadow: 0 24px 70px rgb(36 48 40 / 13%);
}

.return-card {
  padding: clamp(24px, 6vw, 52px);
}

.return-kicker {
  margin: 0 0 10px;
  color: var(--accent);
  font: 800 12px/1.2 ui-monospace, monospace;
  letter-spacing: .16em;
}

.return-intro h1 {
  max-width: 11em;
  margin: 0;
  font: 800 clamp(36px, 8vw, 60px)/.98 Georgia, 'Songti SC', serif;
  letter-spacing: -.045em;
}

.return-intro > p:last-child {
  max-width: 35em;
  margin: 20px 0 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.75;
}

.return-form {
  margin-top: 34px;
  display: grid;
  gap: 20px;
}

.return-form label {
  display: grid;
  gap: 9px;
  font-weight: 700;
}

.return-form label span {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.return-form label b {
  color: var(--accent);
  font-size: 12px;
}

.return-form input {
  width: 100%;
  height: 58px;
  box-sizing: border-box;
  padding: 0 17px;
  border: 1px solid var(--line);
  border-radius: 16px;
  color: var(--ink);
  background: transparent;
  font: 500 18px/1 ui-monospace, 'SFMono-Regular', monospace;
  outline: none;
}

.return-form input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 14%, transparent);
}

.return-methods {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.return-methods legend {
  font-weight: 700;
}

.return-method-options {
  margin-top: 9px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.return-method-option {
  position: relative;
  display: block;
}

.return-method-option input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  cursor: pointer;
}

.return-method-option span {
  min-height: 52px;
  display: grid;
  place-items: center;
  padding: 0 8px;
  border: 1px solid var(--line);
  border-radius: 16px;
  cursor: pointer;
  font-weight: 700;
  font-size: 15px;
  text-align: center;
}

.return-method-option input:checked + span {
  border-color: var(--accent);
  background: color-mix(in srgb, var(--accent) 10%, transparent);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 14%, transparent);
}

.return-method-option input:focus-visible + span {
  outline: 2px solid var(--accent);
}

.return-method-option input:disabled + span {
  opacity: .6;
  cursor: not-allowed;
}

.return-method-hint {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.6;
}

.return-photos {
  min-width: 0;
  margin: 4px 0 0;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 20px;
}

.return-photos legend {
  width: 100%;
  padding: 0;
  display: flex;
  gap: 18px;
  align-items: start;
  justify-content: space-between;
}

.return-photos legend span {
  display: grid;
  gap: 5px;
}

.return-photos legend small {
  color: var(--muted);
  font-weight: 500;
  line-height: 1.5;
}

.return-photos legend b {
  flex: 0 0 auto;
  color: var(--accent);
  font-size: 12px;
}

.return-photo-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.return-photo-grid figure {
  position: relative;
  aspect-ratio: 1;
  margin: 0;
  overflow: hidden;
  border-radius: 14px;
  background: color-mix(in srgb, var(--ink) 8%, transparent);
}

.return-photo-grid img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.return-photo-grid > figure > button {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: rgb(0 0 0 / 65%);
  font-size: 18px;
}

.return-photo-grid figcaption {
  position: absolute;
  inset: auto 0 0;
  min-height: 28px;
  padding: 5px 7px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(transparent, rgb(0 0 0 / 78%));
  font-size: 10px;
  text-align: center;
}

.return-photo-grid progress {
  width: 100%;
  height: 4px;
}

.return-photo-grid figcaption button {
  border: 0;
  color: #fff;
  background: transparent;
  font: inherit;
  text-decoration: underline;
}

.return-photo-picker {
  min-height: 50px;
  margin-top: 14px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--accent);
  border-radius: 15px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 7%, transparent);
  cursor: pointer;
}

.return-photo-picker[aria-disabled='true'] {
  opacity: .5;
  cursor: not-allowed;
}

.return-photo-picker input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.return-submit {
  min-height: 60px;
  border: 0;
  border-radius: 18px;
  color: #fff;
  background: #0e5b42;
  font: 800 19px/1.2 inherit;
}

.return-submit:disabled {
  opacity: .65;
}

.return-error {
  margin: 0;
  padding: 14px 16px;
  border-radius: 14px;
  color: #a4342c;
  background: #fff0ec;
  line-height: 1.5;
}

.return-note {
  margin-top: 26px;
  padding: 18px;
  display: grid;
  gap: 7px;
  border-radius: 18px;
  color: var(--muted);
  background: color-mix(in srgb, var(--ink) 5%, transparent);
  line-height: 1.6;
}

.return-note b {
  color: var(--ink);
}

.return-loading {
  min-height: 360px;
  display: grid;
  place-content: center;
  justify-items: center;
  color: var(--muted);
}

.return-loading span {
  width: 34px;
  height: 34px;
  border: 3px solid var(--line);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin .8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 560px) {
  .return-page {
    padding: 12px;
  }

  .return-topbar {
    margin-bottom: 12px;
  }

  .return-brand small {
    display: none;
  }

  .return-tools button {
    height: 44px;
    padding: 0 12px;
  }

  .return-card {
    padding: 26px 20px;
    border-radius: 24px;
  }

  .return-intro h1 {
    font-size: 40px;
  }

  .return-form {
    margin-top: 28px;
    gap: 17px;
  }

  .return-photos legend {
    display: grid;
    gap: 8px;
  }

  .return-photo-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
