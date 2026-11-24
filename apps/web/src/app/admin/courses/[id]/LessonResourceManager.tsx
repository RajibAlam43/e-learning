'use client'

import { useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Download, FileImage, FileText, Loader2, Pencil, Trash2, Upload, Video, X } from 'lucide-react'
import { toast } from 'sonner'
import { useAdminLesson, useCreateLessonResource, useCreateMediaAsset, useDeleteLessonResource, useLessonResourceDownload, useLessonResourceUpload, useRequestLessonVideoUpload, useUpdateLessonResource, useUpdateMediaAsset } from '@/hooks/useAdmin'
import { QUERY_KEYS } from '@/constants/queryKeys'
import type { AdminLessonDetail, AdminLessonResource, UpdateLessonResourceRequest } from '@/types'
import { getYouTubeVideoId, getYouTubeWatchUrl } from '@/lib/video-embed'

const DOCUMENT_MAX_BYTES = 50 * 1024 * 1024
const VIDEO_MAX_BYTES = 2 * 1024 * 1024 * 1024
const DOCUMENT_TYPES = ['application/pdf', 'image/png', 'image/jpeg', 'image/webp']

export function LessonResourceManager({ lesson, refetch, onClose }: { lesson: AdminLessonDetail; refetch?: () => Promise<unknown>; onClose?: () => void }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const queryClient = useQueryClient()
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState('')
  const [youtubeUrl, setYoutubeUrl] = useState(() => lesson.mediaAsset?.provider === 'YOUTUBE' ? getYouTubeWatchUrl(lesson.mediaAsset.providerAssetId ?? lesson.mediaAsset.playbackId ?? lesson.mediaAsset.fileUrl) : '')
  const [edit, setEdit] = useState<AdminLessonResource>()
  const upload = useLessonResourceUpload()
  const videoUpload = useRequestLessonVideoUpload()
  const create = useCreateLessonResource()
  const update = useUpdateLessonResource()
  const createMediaAsset = useCreateMediaAsset()
  const updateMediaAsset = useUpdateMediaAsset()
  const remove = useDeleteLessonResource()
  const download = useLessonResourceDownload()
  const resources = lesson.resources ?? []
  const isYouTubePreview = lesson.isFree === true && lesson.lessonType === 'VIDEO'

  async function saveYouTubeVideo() {
    setError('')
    const sourceId = getYouTubeVideoId(youtubeUrl)
    if (!sourceId) return setError('Enter a valid YouTube URL or 11-character video ID.')
    setBusy(true)
    try {
      const mediaData = { provider: 'YOUTUBE' as const, assetType: 'VIDEO' as const, providerAssetId: sourceId, playbackId: sourceId, playbackPolicy: 'PUBLIC' as const, preferredPlaybackMode: 'IFRAME', title: lesson.title ?? 'Lesson video', titleEn: lesson.titleEn ?? undefined }
      if (lesson.mediaAsset?.mediaAssetId) await updateMediaAsset.mutateAsync({ id: lesson.mediaAsset.mediaAssetId, data: mediaData })
      else await createMediaAsset.mutateAsync({ lessonId: lesson.lessonId!, ...mediaData })
      await Promise.all([refetch?.(), queryClient.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() })])
      toast.success('YouTube video attached')
    } catch (cause) {
      setError(errorMessage(cause))
      toast.error(errorMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  async function uploadBytes(url: string, method: string, file: File) {
    await new Promise<void>((resolve, reject) => {
      const request = new XMLHttpRequest()
      request.open(method, url)
      request.setRequestHeader('Content-Type', file.type)
      request.upload.onprogress = event => { if (event.lengthComputable) setProgress(Math.round(event.loaded / event.total * 100)) }
      request.onload = () => request.status >= 200 && request.status < 300 ? resolve() : reject(new Error('Upload failed.'))
      request.onerror = () => reject(new Error('Upload failed.'))
      request.send(file)
    })
  }

  async function addFile(file: File) {
    setError('')
    const isVideo = file.type.startsWith('video/')
    if (!isVideo && !DOCUMENT_TYPES.includes(file.type)) return setError('Choose a video, PDF, PNG, JPEG, or WebP file.')
    if (file.size > (isVideo ? VIDEO_MAX_BYTES : DOCUMENT_MAX_BYTES)) return setError(isVideo ? 'Videos must be 2 GB or smaller.' : 'Documents must be 50 MB or smaller.')
    setBusy(true); setProgress(0)
    let closeAfterUpload = false
    try {
      if (isVideo) {
        const signed = await videoUpload.mutateAsync({ lessonId: lesson.lessonId!, data: { filename: file.name, contentType: file.type, sizeBytes: file.size } })
        if (!signed.uploadUrl) throw new Error('Upload URL was not returned.')
        await uploadBytes(signed.uploadUrl, signed.method ?? 'PUT', file)
        await Promise.all([refetch?.(), queryClient.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() })])
        toast.success('Video uploaded. Processing has started.')
        closeAfterUpload = true
      } else {
        const signed = await upload.mutateAsync({ lessonId: lesson.lessonId!, data: { filename: file.name, contentType: file.type, sizeBytes: file.size } })
        if (!signed.uploadUrl || !signed.objectKey) throw new Error('Upload URL was not returned.')
        await uploadBytes(signed.uploadUrl, signed.method ?? 'PUT', file)
        await create.mutateAsync({ lessonId: lesson.lessonId!, data: { title: file.name.replace(/\.[^.]+$/, ''), resourceType: file.type === 'application/pdf' ? 'PDF' : 'IMAGE', purpose: 'SUPPLEMENTARY', mimeType: file.type, objectKey: signed.objectKey, position: resources.length + 1 } })
        await refetch?.()
        toast.success('Resource uploaded')
      }
    } catch (cause) { setError(errorMessage(cause)); toast.error(errorMessage(cause)) }
    finally { setBusy(false); setProgress(0); if (inputRef.current) inputRef.current.value = '' }
    if (closeAfterUpload) onClose?.()
  }

  async function openDownload(id: string) {
    try { const result = await download.mutateAsync(id); if (!result.downloadUrl) throw new Error('Download link unavailable.'); window.open(result.downloadUrl, '_blank', 'noopener,noreferrer') }
    catch (cause) { setError(errorMessage(cause)); toast.error(errorMessage(cause)) }
  }

  async function saveResource(value: UpdateLessonResourceRequest) {
    if (!edit?.resourceId) return
    try { await update.mutateAsync({ id: edit.resourceId, data: value }); await refetch?.(); toast.success('Resource updated'); setEdit(undefined) }
    catch (cause) { setError(errorMessage(cause)); toast.error(errorMessage(cause)) }
  }

  return <section className="mt-6 rounded-xl border border-slate-200 bg-slate-50 p-4">
    <div className="flex flex-wrap items-center justify-between gap-3"><div><h3 className="font-semibold text-[#1c3873]">Lesson content</h3><p className="text-xs text-slate-500">Attach the primary video/PDF or supplementary downloads.</p></div>{!isYouTubePreview && <><button type="button" disabled={busy} onClick={() => inputRef.current?.click()} className="inline-flex items-center gap-2 rounded-lg bg-[#1c3873] px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"><Upload className="h-4 w-4" />{busy ? `Uploading ${progress}%` : 'Upload file'}</button><input ref={inputRef} type="file" accept="video/*,.pdf,image/png,image/jpeg,image/webp" className="hidden" onChange={event => { const file = event.target.files?.[0]; if (file) void addFile(file) }} /></>}</div>
    {isYouTubePreview && <div className="mt-4 grid gap-2 sm:grid-cols-[1fr_auto]"><label className="grid gap-1 text-sm font-medium text-slate-700">YouTube video URL<input type="url" value={youtubeUrl} onChange={event => setYoutubeUrl(event.target.value)} placeholder="https://www.youtube.com/watch?v=..." className="h-10 rounded-lg border border-slate-200 bg-white px-3 outline-none focus:border-[#1c3873] focus:ring-2 focus:ring-[#1c3873]/10" /></label><button type="button" disabled={busy} onClick={() => void saveYouTubeVideo()} className="self-end rounded-lg bg-[#1c3873] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Saving...' : 'Save video'}</button></div>}
    {busy && <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200"><div className="h-full bg-[#1c3873] transition-all" style={{ width: `${progress}%` }} /></div>}
    {error && <div role="alert" className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{error}</div>}
    {lesson.mediaAsset && <div className="mt-4 flex items-center gap-3 rounded-lg border bg-white p-3"><Video className="h-5 w-5 text-violet-600" /><div className="min-w-0"><p className="truncate text-sm font-medium">{lesson.mediaAsset.title ?? 'Video asset'}</p><p className="text-xs text-slate-500">{lesson.mediaAsset.status ?? 'Attached'}{lesson.mediaAsset.playbackId ? ` · ${lesson.mediaAsset.playbackId}` : ''}</p></div></div>}
    {resources.length === 0 ? <p className="mt-4 text-sm text-slate-500">No downloadable resources attached.</p> : <ul className="mt-4 divide-y rounded-lg border bg-white">{resources.map(resource => <li key={resource.resourceId} className="flex items-center justify-between gap-3 p-3"><div className="flex min-w-0 items-center gap-3">{resource.resourceType === 'PDF' ? <FileText className="h-5 w-5 shrink-0 text-red-600" /> : <FileImage className="h-5 w-5 shrink-0 text-blue-600" />}<div className="min-w-0"><p className="truncate text-sm font-medium">{resource.title ?? 'Untitled resource'}</p><p className="text-xs text-slate-500">{resource.purpose === 'PRIMARY_CONTENT' ? 'Primary content' : 'Supplementary'}{resource.mimeType ? ` · ${resource.mimeType}` : ''}</p></div></div><div className="flex items-center gap-1"><button type="button" onClick={() => resource.resourceId && setEdit(resource)} className="rounded-md p-2 text-slate-600 hover:bg-slate-100" aria-label="Edit resource"><Pencil className="h-4 w-4" /></button><button type="button" disabled={download.isPending} onClick={() => resource.resourceId && void openDownload(resource.resourceId)} className="rounded-md p-2 text-slate-600 hover:bg-slate-100 disabled:opacity-50" aria-label="Download resource"><Download className="h-4 w-4" /></button><button type="button" disabled={remove.isPending} onClick={() => { if (resource.resourceId && window.confirm('Delete this resource?')) remove.mutate(resource.resourceId, { onSuccess: () => { toast.success('Resource deleted'); refetch?.() }, onError: cause => toast.error(errorMessage(cause)) }) }} className="rounded-md p-2 text-red-600 hover:bg-red-50 disabled:opacity-50" aria-label="Delete resource"><Trash2 className="h-4 w-4" /></button></div></li>)}</ul>}
    {edit && <ResourceEdit resource={edit} busy={update.isPending} onClose={() => setEdit(undefined)} onSave={saveResource} />}
  </section>
}

function ResourceEdit({ resource, busy, onClose, onSave }: { resource: AdminLessonResource; busy: boolean; onClose: () => void; onSave: (value: UpdateLessonResourceRequest) => void }) {
  const [form, setForm] = useState<UpdateLessonResourceRequest>({ title: resource.title, titleEn: resource.titleEn, resourceType: resource.resourceType, purpose: resource.purpose, mimeType: resource.mimeType, position: resource.position })
  return <div className="fixed inset-0 z-[60] grid place-items-center bg-black/45 p-4"><form onSubmit={event => { event.preventDefault(); onSave({ ...form, title: form.title?.trim() }) }} className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"><div className="mb-5 flex items-center justify-between"><h3 className="font-heading text-lg font-semibold text-[#1c3873]">Edit resource</h3><button type="button" onClick={onClose} aria-label="Close"><X className="h-5 w-5" /></button></div><div className="grid gap-3"><label className="grid gap-1 text-sm font-medium">Title<input required value={form.title ?? ''} onChange={event => setForm({ ...form, title: event.target.value })} className="h-10 rounded-lg border px-3" /></label><label className="grid gap-1 text-sm font-medium">Title (English)<input value={form.titleEn ?? ''} onChange={event => setForm({ ...form, titleEn: event.target.value })} className="h-10 rounded-lg border px-3" /></label><label className="grid gap-1 text-sm font-medium">Purpose<select value={form.purpose ?? 'SUPPLEMENTARY'} onChange={event => setForm({ ...form, purpose: event.target.value as UpdateLessonResourceRequest['purpose'] })} className="h-10 rounded-lg border px-3"><option value="PRIMARY_CONTENT">Primary content</option><option value="SUPPLEMENTARY">Supplementary</option></select></label><label className="grid gap-1 text-sm font-medium">Position<input type="number" min={1} value={String(form.position ?? 1)} onChange={event => setForm({ ...form, position: Number(event.target.value) || 1 })} className="h-10 rounded-lg border px-3" /></label></div><div className="mt-5 flex justify-end gap-2"><button type="button" onClick={onClose} className="rounded-lg border px-4 py-2 text-sm">Cancel</button><button disabled={busy} className="rounded-lg bg-[#1c3873] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Saving...' : 'Save'}</button></div></form></div>
}

export function LessonResourceLauncher({ lessonId, onClose }: { lessonId: string; onClose?: () => void }) { const query = useAdminLesson(lessonId, true); if (query.isLoading) return <div className="mt-6 flex items-center gap-2 text-sm text-slate-500"><Loader2 className="h-4 w-4 animate-spin" />Loading lesson content...</div>; if (query.isError || !query.data) return <p role="alert" className="mt-6 text-sm text-red-700">Unable to load lesson content.</p>; return <LessonResourceManager lesson={query.data} refetch={() => query.refetch()} onClose={onClose} /> }
function errorMessage(cause: unknown) { if (cause instanceof Error) return cause.message; if (cause && typeof cause === 'object' && 'message' in cause && typeof cause.message === 'string') return cause.message; return 'The request could not be completed.' }
