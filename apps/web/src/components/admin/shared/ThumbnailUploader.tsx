'use client'

/**
 * ThumbnailUploader — Cloudflare R2 upload flow.
 *
 * Step 1: User picks a file. We validate type + size client-side.
 * Step 2: POST /admin/thumbnails/upload-url to get { uploadUrl, objectKey, method }.
 * Step 3: Browser uploads the raw file bytes directly to R2 via the uploadUrl
 *         using the HTTP method the backend returned (PUT or POST).
 * Step 4: Emit the objectKey to the parent form, which sends it as
 *         `thumbnailObjectKey` in the course/program create/update request.
 *
 * The component is fully reusable — pass `ownerType` ("COURSE" | "COLLECTION" |
 * "MEDIA_ASSET") and the parent owns the value state.
 */

import { useRef, useState } from 'react'
import { ImagePlus, Loader2, X } from 'lucide-react'
import { toast } from 'sonner'
import { useRequestThumbnailUpload } from '@/hooks/useAdmin'
import type { ThumbnailUploadResponse } from '@/types'

const ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/webp']
const MAX_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB (backend allows up to 20 MB)

export interface ThumbnailUploaderProps {
  /** The R2 object key, controlled by the parent form. */
  value?: string
  /** Called when upload succeeds with the objectKey the parent should send. */
  onChange: (objectKey: string | undefined) => void
  /** Which entity this thumbnail is for. Backend uses this for the R2 path. */
  ownerType: 'COURSE' | 'COLLECTION' | 'MEDIA_ASSET' | 'FACULTY'
  /** Optional preview URL — if the parent already has a thumbnailUrl (e.g. from
   *  the API response), pass it so the user sees the current image. */
  previewUrl?: string
  label?: string
  onUploadingChange?: (uploading: boolean) => void
}

export function ThumbnailUploader({
  value,
  onChange,
  ownerType,
  previewUrl,
  label = 'Thumbnail',
  onUploadingChange,
}: ThumbnailUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [localPreview, setLocalPreview] = useState<string | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const presign = useRequestThumbnailUpload()

  const shownPreview = localPreview ?? previewUrl

  async function handleFile(file: File) {
    const normalized = normalizeThumbnailFile(file)
    if (!normalized) {
      toast.error('Please pick a PNG, JPG, or WebP image.')
      return
    }
    if (normalized.file.size > MAX_SIZE_BYTES) {
      toast.error(`Image must be under ${Math.round(MAX_SIZE_BYTES / 1024 / 1024)} MB.`)
      return
    }

    // Show local preview immediately (better UX than waiting for upload)
    const objectUrl = URL.createObjectURL(normalized.file)
    setLocalPreview(objectUrl)
    setIsUploading(true)
    onUploadingChange?.(true)

    try {
      // Step 2: ask backend for a presigned URL
      const presignRes: ThumbnailUploadResponse = await presign.mutateAsync({
        ownerType,
        filename: normalized.filename,
        contentType: normalized.contentType,
        sizeBytes: normalized.file.size,
      })

      // Step 3: upload directly to R2 with the method the backend specified
      if (!presignRes.uploadUrl || !presignRes.objectKey) {
        throw new Error('Backend did not return a valid presign response')
      }
      const method = (presignRes.method || 'PUT').toUpperCase()
      // The storage signature can include Content-Type. Use the value returned
      // by the presign endpoint instead of recalculating it in the browser.
      const signedContentType = presignRes.contentType || normalized.contentType
      const uploadRes = await fetch(presignRes.uploadUrl, {
        method,
        body: normalized.file,
        headers: { 'Content-Type': signedContentType },
        cache: 'no-store',
      })

      if (!uploadRes.ok) {
        const responseBody = await uploadRes.text().catch(() => '')
        const detail = responseBody.replace(/\s+/g, ' ').trim().slice(0, 240)
        throw new Error(
          `R2 upload failed: ${uploadRes.status} ${uploadRes.statusText}${detail ? ` — ${detail}` : ''}`,
        )
      }

      // Step 4: emit the objectKey to the parent form
      onChange(presignRes.objectKey)
      toast.success('Thumbnail uploaded.')
    } catch (err) {
      // Extract a useful message — fetch errors from R2 are often empty `{}`.
      const errorMsg = extractErrorMessage(err)

      // CORS errors produce empty TypeError "Failed to fetch" — this is the
      // most likely cause when the presign succeeds but the R2 upload fails.
      const isCorsLikely =
        err instanceof TypeError ||
        (typeof err === 'object' && err !== null && Object.keys(err).length === 0)

      if (errorMsg.startsWith('R2 upload failed')) {
        toast.error(`Upload to storage failed (${errorMsg}). Please try again.`)
      } else if (isCorsLikely) {
        toast.error(
          'Upload blocked — this is likely a CORS issue. Ask the backend dev to configure CORS on the Cloudflare R2 bucket to allow uploads from this domain.',
          { duration: 8000 },
        )
      } else {
        toast.error(errorMsg || 'Could not upload thumbnail. Please try again.')
      }

      // Revert preview on failure
      if (localPreview) URL.revokeObjectURL(localPreview)
      setLocalPreview(null)
    } finally {
      setIsUploading(false)
      onUploadingChange?.(false)
    }
  }

  function handleRemove() {
    if (localPreview) {
      URL.revokeObjectURL(localPreview)
    }
    setLocalPreview(null)
    onChange(undefined)
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div className='flex flex-col gap-1.5'>
      <label className='text-sm font-medium text-[#121c2a]'>{label}</label>
      <div className='flex items-start gap-3'>
        {/* Preview / Drop zone */}
        <div
          className='relative w-32 h-20 rounded-lg border border-dashed border-gray-300 bg-gray-50 flex items-center justify-center overflow-hidden cursor-pointer hover:border-blue-400 hover:bg-blue-50/30 transition-colors'
          onClick={() => !isUploading && inputRef.current?.click()}
          role='button'
          tabIndex={0}
          onKeyDown={(e) => {
            if ((e.key === 'Enter' || e.key === ' ') && !isUploading) {
              e.preventDefault()
              inputRef.current?.click()
            }
          }}
        >
          {shownPreview ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={shownPreview} alt='Thumbnail preview' className='w-full h-full object-cover' />
          ) : (
            <div className='flex flex-col items-center gap-1 text-gray-400'>
              <ImagePlus className='w-5 h-5' />
              <span className='text-[10px]'>PNG, JPG, WebP</span>
            </div>
          )}

          {isUploading && (
            <div className='absolute inset-0 bg-white/70 flex items-center justify-center'>
              <Loader2 className='w-5 h-5 text-blue-500 animate-spin' />
            </div>
          )}
        </div>

        {/* Hidden input + actions */}
        <div className='flex flex-col gap-1.5 pt-1'>
          <input
            ref={inputRef}
            type='file'
            accept={ALLOWED_TYPES.join(',')}
            className='hidden'
            onChange={(e) => {
              const file = e.target.files?.[0]
              if (file) handleFile(file)
            }}
          />
          <button
            type='button'
            onClick={() => inputRef.current?.click()}
            disabled={isUploading}
            className='text-xs font-medium text-blue-600 hover:text-blue-700 disabled:opacity-50'
          >
            {shownPreview ? 'Change image' : 'Upload image'}
          </button>
          {shownPreview && !isUploading && (
            <button
              type='button'
              onClick={handleRemove}
              className='inline-flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-700'
            >
              <X className='w-3 h-3' /> Remove
            </button>
          )}
          {value && (
            <span className='text-[10px] text-gray-400 truncate max-w-[180px]' title={value}>
              key: {value}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}

/** Extract a human-readable message from various error shapes. */
function extractErrorMessage(err: unknown): string {
  if (err instanceof Error) return err.message
  if (typeof err === 'string') return err
  if (err && typeof err === 'object') {
    const e = err as { message?: string; status?: number; statusText?: string }
    if (e.message) return e.message
    if (e.status) return `HTTP ${e.status}${e.statusText ? ` ${e.statusText}` : ''}`
  }
  return 'Unknown error'
}

function normalizeThumbnailFile(file: File): {
  file: File
  filename: string
  contentType: string
} | null {
  const originalName = file.name.trim()
  const extension = originalName.split('.').pop()?.toLowerCase() ?? ''
  const contentTypeByExtension: Record<string, string> = {
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    webp: 'image/webp',
  }
  const contentType = contentTypeByExtension[extension]
  if (!contentType) return null

  const baseName = originalName
    .slice(0, Math.max(0, originalName.length - extension.length - 1))
    .normalize('NFKD')
    .replace(/[^a-zA-Z0-9_-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase() || 'thumbnail'
  const filename = `${baseName}.${extension}`
  return {
    file: file.type === contentType && file.name === filename
      ? file
      : new File([file], filename, { type: contentType, lastModified: file.lastModified }),
    filename,
    contentType,
  }
}
