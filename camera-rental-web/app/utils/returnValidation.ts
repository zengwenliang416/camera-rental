import type { UploadedPhoto } from '../types/return-registration'

export function hasRequiredReturnPhotos(photos: UploadedPhoto[]) {
  const categories = new Set(photos.map((photo) => photo.category))
  return categories.has('DEVICE_EXTERIOR') && categories.has('SERIAL_LABEL')
}
