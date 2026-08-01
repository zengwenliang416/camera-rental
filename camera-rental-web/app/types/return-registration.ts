export type RegistrationStatus =
  | 'DRAFT'
  | 'REVIEW_REQUIRED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'REVOKED'

export interface ReturnReceipt {
  formNo: string
  status: RegistrationStatus
  waybillNo?: string
  deliveryId?: number
  submittedAt?: string
}

export interface ReturnContext {
  status: RegistrationStatus
  formNo: string
  orderNo: string
  sourceType?: string
  rentalStart?: string
  rentalEnd?: string
  assignedDeviceCount: number
  expiresAt: string
  receipt?: ReturnReceipt
}

export type PhotoCategory =
  | 'DEVICE_EXTERIOR'
  | 'SERIAL_LABEL'
  | 'PACKAGING'
  | 'DAMAGE_DETAIL'

export interface UploadedPhoto {
  attachmentId: number
  fileId: number
  category: PhotoCategory
  name: string
  size: number
  previewUrl: string
}

export interface ReturnDraft {
  carrierCode: string
  carrierName: string
  waybillNo: string
  shippedDate: string
  serials: string[]
  issueDescription: string
  photos: UploadedPhoto[]
}

export interface PhotoUploadTask {
  id: string
  category: PhotoCategory
  file: File
  progress: number
  status: 'UPLOADING' | 'FAILED'
  error?: string
}
