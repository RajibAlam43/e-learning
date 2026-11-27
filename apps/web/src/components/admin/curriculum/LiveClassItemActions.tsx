'use client'
import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Trash2, X } from 'lucide-react'
import { toast } from 'sonner'
import { apiClient } from '@/lib/api-client'
import type { AdminSectionItem, AdminLiveClassItemResponse, CreateLiveClassItemRequest } from '@/types'
import { curriculumError, useLiveClassItem, useLiveClassItemMutation } from '@/services/curriculum-items.service'

const input = 'w-full min-w-0 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1c3873]'

export function LiveClassItemActions({ item }: { item: AdminSectionItem }) {
  const { id: courseId } = useParams<{ id: string }>()
  const slotId = item.liveClass?.liveClassItemId ?? item.itemId ?? ''
  const [open, setOpen] = useState(false)
  const query = useLiveClassItem(courseId, slotId, open && !!slotId)
  const change = useLiveClassItemMutation(courseId, slotId)
  const cache = useQueryClient()
  const cancel = useMutation({ mutationFn: () => apiClient.delete(`/live-classes/${item.liveClass?.liveClassId}`),
    onSuccess: () => cache.invalidateQueries({ queryKey: ['admin'] }) })
  const title = item.liveClass?.title ?? 'live class'
  const protectedHistory = item.liveClass?.status === 'LIVE' || item.liveClass?.status === 'COMPLETED'
  async function remove() {
    if (!window.confirm(`Remove “${title}” from the curriculum? Cohorts sharing this curriculum are affected. Scheduled meetings must be cancelled first. Meeting history is retained.`)) return
    try { await change.mutateAsync(undefined); toast.success('Live class removed from curriculum') }
    catch (cause) { toast.error(curriculumError(cause)) }
  }
  async function cancelMeeting() {
    if (!window.confirm(`Cancel the scheduled meeting “${title}”?`)) return
    try { await cancel.mutateAsync(); toast.success('Meeting cancelled. You can now remove its curriculum item.') }
    catch (cause) { toast.error(curriculumError(cause)) }
  }
  return <>
    <button type="button" onClick={() => setOpen(true)} className="rounded-md px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-100" aria-label={`Edit live class ${title}`}>Edit</button>
    {item.liveClass?.status === 'SCHEDULED' && <button type="button" disabled={cancel.isPending || change.isPending} onClick={cancelMeeting} className="rounded-md px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-50">{cancel.isPending ? 'Cancelling…' : 'Cancel meeting'}</button>}
    <button type="button" disabled={!slotId || protectedHistory || change.isPending || cancel.isPending} title={protectedHistory ? 'Ongoing and completed meeting history is protected' : 'Remove live class from curriculum'} onClick={remove} aria-label={`Remove live class ${title}`} className="rounded-md p-1.5 text-red-600 hover:bg-red-50 disabled:opacity-40"><Trash2 className="h-4 w-4" /></button>
    {open && <div role="dialog" aria-modal="true" aria-labelledby="edit-live-item-title" className="fixed inset-0 z-50 grid place-items-center bg-black/45 p-4"><div className="max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-2xl bg-white p-5 sm:p-6">
      <div className="mb-4 flex items-center justify-between gap-3"><h2 id="edit-live-item-title" className="font-heading text-xl text-[#1c3873]">Edit live class item</h2><button type="button" aria-label="Close" disabled={change.isPending} onClick={() => setOpen(false)} className="rounded-lg p-2 hover:bg-slate-100"><X className="h-5 w-5" /></button></div>
      {query.isLoading ? <p role="status">Loading live class…</p> : query.isError || !query.data ? <p role="alert">Unable to load the live class. <button className="underline" onClick={() => query.refetch()}>Retry</button></p> : <LiveItemForm key={slotId} item={query.data} saving={change.isPending} save={async data => { await change.mutateAsync(data); setOpen(false); toast.success('Live class item updated') }} />}
    </div></div>}
  </>
}

function LiveItemForm({ item, saving, save }: { item: AdminLiveClassItemResponse; saving: boolean; save: (data: CreateLiveClassItemRequest) => Promise<void> }) {
  const [form, setForm] = useState<CreateLiveClassItemRequest>({ sectionId: item.sectionId ?? '', position: item.position, title: item.title ?? '', titleEn: item.titleEn ?? '', description: item.description ?? '', descriptionEn: item.descriptionEn ?? '', expectedDurationMinutes: item.expectedDurationMinutes ?? undefined, isMandatory: item.isMandatory ?? true })
  const [error, setError] = useState('')
  async function submit(event: React.FormEvent) { event.preventDefault(); setError(''); try { await save({ ...form, title: form.title.trim() }) } catch (cause) { setError(curriculumError(cause)) } }
  return <form onSubmit={submit} className="grid min-w-0 gap-4"><p className="text-sm text-slate-600">Curriculum details are shared by cohorts. Use Schedule to change meeting times. Use the move controls when another item occupies the desired position.</p>
    <label className="grid gap-1 text-sm font-medium">Title (Bangla)<input required className={input} value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /></label>
    <label className="grid gap-1 text-sm font-medium">Title (English)<input className={input} value={form.titleEn ?? ''} onChange={e => setForm({ ...form, titleEn: e.target.value })} /></label>
    <div className="grid gap-4 sm:grid-cols-2"><label className="grid min-w-0 gap-1 text-sm font-medium">Position<input required min={1} type="number" className={input} value={form.position ?? ''} onChange={e => setForm({ ...form, position: Number(e.target.value) })} /></label><label className="grid min-w-0 gap-1 text-sm font-medium">Expected duration (minutes)<input min={1} type="number" className={input} value={form.expectedDurationMinutes ?? ''} onChange={e => setForm({ ...form, expectedDurationMinutes: e.target.value ? Number(e.target.value) : undefined })} /></label></div>
    <label className="grid gap-1 text-sm font-medium">Description (Bangla)<textarea rows={2} className={input} value={form.description ?? ''} onChange={e => setForm({ ...form, description: e.target.value })} /></label>
    <label className="grid gap-1 text-sm font-medium">Description (English)<textarea rows={2} className={input} value={form.descriptionEn ?? ''} onChange={e => setForm({ ...form, descriptionEn: e.target.value })} /></label>
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.isMandatory ?? true} onChange={e => setForm({ ...form, isMandatory: e.target.checked })} />Mandatory item</label>
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}<button disabled={saving || !form.title.trim()} className="rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{saving ? 'Saving…' : 'Save live class'}</button>
  </form>
}
