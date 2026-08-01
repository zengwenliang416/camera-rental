<script setup lang="ts">
import type { PhotoCategory, PhotoUploadTask, UploadedPhoto } from '~/types/return-registration'

const props = defineProps<{
  category: PhotoCategory
  title: string
  hint: string
  required?: boolean
  photos: UploadedPhoto[]
  tasks: PhotoUploadTask[]
  busy?: boolean
}>()
const { t } = useReturnPreferences()
const emit = defineEmits<{
  upload: [files: File[]]
  remove: [photo: UploadedPhoto]
  retry: [task: PhotoUploadTask]
}>()

function select(event: Event) {
  const input = event.target as HTMLInputElement
  emit('upload', Array.from(input.files || []).slice(0, 6 - props.photos.length))
  input.value = ''
}
</script>

<template>
  <article class="photo-field">
    <header>
      <div><strong>{{ title }}</strong><small>{{ hint }}</small></div>
      <span :class="required ? 'required' : 'optional'">{{ required ? t('mustPhoto') : t('optionalPhoto') }}</span>
    </header>
    <div v-if="photos.length" class="photo-grid">
      <figure v-for="photo in photos" :key="photo.attachmentId">
        <img :src="photo.previewUrl" :alt="`${title}照片`">
        <button type="button" :aria-label="t('remove')" @click="emit('remove', photo)">×</button>
      </figure>
    </div>
    <div v-if="tasks.length" class="upload-list">
      <div v-for="task in tasks" :key="task.id">
        <span>{{ task.file.name }}</span>
        <progress v-if="task.status === 'UPLOADING'" :value="task.progress" max="100" />
        <button v-else type="button" @click="emit('retry', task)">{{ t('retry') }}</button>
        <small>{{ task.status === 'UPLOADING' ? `${t('uploading')} ${task.progress}%` : task.error }}</small>
      </div>
    </div>
    <label class="upload-control" :aria-disabled="busy || photos.length >= 6">
      <input
        type="file"
        accept="image/jpeg,image/png,image/webp"
        capture="environment"
        multiple
        :disabled="busy || photos.length >= 6"
        @change="select"
      >
      {{ busy ? `${t('uploading')}…` : t('upload') }}
    </label>
  </article>
</template>
