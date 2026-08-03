import type {
  PhotoCategory,
  ReturnContext,
  ReturnDraft,
  ReturnReceipt,
  UploadedPhoto
} from '~/types/return-registration'

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

export function useReturnRegistration() {
  const config = useRuntimeConfig()
  const base = `${config.public.apiBase}/rental/return-registration`

  async function request<T>(path = '', options: Parameters<typeof $fetch>[1] = {}) {
    const result = await $fetch<ApiResult<T>>(`${base}${path}`, {
      credentials: 'include',
      ...options
    })
    if (result.code !== 0) throw new Error(result.msg || '请求失败')
    return result.data
  }

  const verify = (orderNo: string, mobileLast4: string, machineCode: string) =>
    request<ReturnContext>('/verify', {
      method: 'POST',
      body: { orderNo, mobileLast4, machineCode }
    })

  const loadContext = () => request<ReturnContext>('/session')

  const simpleSubmit = (form: {
    orderNo: string
    mobileLast4: string
    machineCode: string
    waybillNo: string
    attachmentIds?: number[]
  }) =>
    request<ReturnReceipt>('/simple-submit', {
      method: 'POST',
      body: form
    })

  async function upload(
    file: File,
    category: PhotoCategory,
    onProgress: (progress: number) => void
  ): Promise<UploadedPhoto> {
    const auth = await request<{
      attachmentId: number
      uploadUrl: string
      contentType: string
    }>('/upload-authorizations', {
      method: 'POST',
      body: { category, name: file.name, contentType: file.type }
    })
    try {
      await uploadObject(auth.uploadUrl, auth.contentType, file, onProgress)
      return await request<UploadedPhoto>('/attachments/confirm', {
        method: 'POST',
        body: { attachmentId: auth.attachmentId }
      })
    } catch (error) {
      await request(`/attachments/${auth.attachmentId}`, { method: 'DELETE' }).catch(() => undefined)
      throw error
    }
  }

  function uploadObject(
    url: string,
    contentType: string,
    file: File,
    onProgress: (progress: number) => void
  ) {
    return new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', url)
      xhr.setRequestHeader('Content-Type', contentType)
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          onProgress(Math.round((event.loaded / event.total) * 100))
        }
      }
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          onProgress(100)
          resolve()
        } else {
          reject(new Error(`图片上传失败 (${xhr.status})`))
        }
      }
      xhr.onerror = () => reject(new Error('图片上传失败'))
      xhr.send(file)
    })
  }

  const removePhoto = (attachmentId: number) =>
    request(`/attachments/${attachmentId}`, { method: 'DELETE' })

  const submit = (context: ReturnContext, draft: ReturnDraft, idempotencyKey: string) =>
    request<ReturnReceipt>('/submit', {
      method: 'POST',
      body: {
        orderNo: context.orderNo,
        carrierCode: draft.carrierCode,
        carrierName: draft.carrierName,
        waybillNo: draft.waybillNo,
        shippedDate: draft.shippedDate,
        serials: draft.serials,
        attachmentIds: draft.photos.map((photo) => photo.attachmentId),
        issueDescription: draft.issueDescription || undefined,
        idempotencyKey
      }
    })

  return { verify, loadContext, simpleSubmit, upload, removePhoto, submit }
}
