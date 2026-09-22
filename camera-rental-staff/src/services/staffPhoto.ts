import { authorizePhoto, confirmPhoto } from '@/api/rental/warehouse'

async function readPhoto(path: string): Promise<ArrayBuffer> {
  if (typeof uni.getFileSystemManager === 'function')
    return uni.getFileSystemManager().readFileSync(path) as ArrayBuffer
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined') {
    return new Promise((resolve, reject) => {
      plus.io.resolveLocalFileSystemURL(path, (entry) => {
        ;(entry as unknown as PlusIoFileEntry).file((file) => {
          const reader = new plus.io.FileReader()
          reader.onloadend = (event) => {
            const value = String((event.target as unknown as { result?: string }).result || '')
            if (!value.includes(','))
              reject(new Error('照片读取失败'))
            else resolve(uni.base64ToArrayBuffer(value.split(',')[1]))
          }
          reader.onerror = () => reject(new Error('照片读取失败'))
          reader.readAsDataURL(file)
        }, () => reject(new Error('照片读取失败')))
      }, () => reject(new Error('照片不存在')))
    })
  }
  // #endif
  const response = await fetch(path)
  return response.arrayBuffer()
}
export async function uploadInspectionPhoto(deviceId: number, assignmentId: number) {
  const chosen = await uni.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['camera', 'album'] })
  const data = await readPhoto(chosen.tempFilePaths[0])
  if (!data.byteLength || data.byteLength > 5 * 1024 * 1024)
    throw new Error('照片不能超过 5 MB，请压缩后重试')
  const bytes = new Uint8Array(data)
  const contentType = bytes[0] === 0xFF && bytes[1] === 0xD8
    ? 'image/jpeg'
    : bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E && bytes[3] === 0x47 ? 'image/png' : ''
  if (!contentType)
    throw new Error('仅支持 JPEG 或 PNG 照片')
  const authorization = await authorizePhoto(deviceId, assignmentId, contentType)
  await new Promise<void>((resolve, reject) => uni.request({
    url: authorization.uploadUrl,
    method: 'PUT',
    data,
    header: { 'Content-Type': contentType, 'isToken': false },
    success: result => result.statusCode >= 200 && result.statusCode < 300 ? resolve() : reject(new Error('照片上传失败，请重试')),
    fail: () => reject(new Error('照片上传失败，请检查网络')),
  }))
  return confirmPhoto(authorization.id)
}
