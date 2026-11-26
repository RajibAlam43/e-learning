'use client'
import { useState } from 'react'
import { programErrorMessage } from '@/services/program-curriculum.service'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import { toast } from 'sonner'
import { useAdminCollection, useAdminCourses, usePublishCollection, useUnpublishCollection, useUpdateCollection } from '@/hooks/useAdmin'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { ProgramCurriculumEditor } from '@/components/admin/program/ProgramCurriculumEditor'
import type { AdminCollectionDetail, UpdateCollectionRequest } from '@/types'
export default function ProgramPage() {
 const { id } = useParams<{ id: string }>()
 const query = useAdminCollection(id)
 const courses = useAdminCourses()
 const publish = usePublishCollection()
 const unpublish = useUnpublishCollection()
 const [error, setError] = useState('')
 async function toggle() { setError(''); try { if (query.data?.status === 'PUBLISHED') await unpublish.mutateAsync(id); else await publish.mutateAsync(id); await query.refetch() } catch (cause) { setError(programErrorMessage(cause, 'Unable to change publication status.')) } }
 if (query.isLoading || courses.isLoading) return <p role="status" className="p-6">Loading program…</p>
 if (query.isError || courses.isError || !query.data) return <div role="alert" className="p-6">Unable to load the program. <button onClick={() => { void query.refetch(); void courses.refetch() }} className="underline">Retry</button></div>
 const item = query.data
 return <div className="mx-auto flex max-w-5xl flex-col gap-6"><Link href="/admin/programs" className="text-sm text-[#1c3873] underline underline-offset-4">Back to programs</Link><AdminPageHeader title={item.title ?? 'Program'} description={`${item.collectionType} · ${item.status}`} action={<button onClick={toggle} disabled={publish.isPending || unpublish.isPending} className="rounded-lg border bg-white px-4 py-2.5 text-sm text-[#1c3873] disabled:opacity-60">{item.status === 'PUBLISHED' ? 'Unpublish program' : 'Publish program'}</button>} />{error && <p role="alert" className="text-sm text-red-700">{error}</p>}<ProgramDetailsForm key={item.collectionId} item={item} /><ProgramCurriculumEditor key={`${id}-${item.programRequirements?.requiredCredits == null ? 'legacy' : 'credit'}`} program={item} courses={courses.data ?? []} /></div>
}
function ProgramDetailsForm({ item }: { item: AdminCollectionDetail }) {
 const update = useUpdateCollection()
 const [form, setForm] = useState<UpdateCollectionRequest>({ title: item.title, titleEn: item.titleEn ?? '', slug: item.slug, collectionType: item.collectionType, priceBdt: item.priceBdt, description: item.description ?? '', descriptionEn: item.descriptionEn ?? '' })
 const [error, setError] = useState('')
 async function submit(event: React.FormEvent) { event.preventDefault(); setError(''); try { await update.mutateAsync({ id: item.collectionId!, data: form }); toast.success('Program details saved.') } catch (cause) { setError(programErrorMessage(cause, 'Unable to save details.')) } }
 return <form onSubmit={submit} className="rounded-xl bg-white p-5 sm:p-6"><h2 className="font-heading text-xl text-[#1c3873]">Program details</h2><div className="mt-4 grid gap-4 sm:grid-cols-2">{(['title', 'titleEn', 'slug'] as const).map(key => <label key={key} className="grid gap-1.5 text-sm font-medium">{key === 'titleEn' ? 'Title (English)' : key === 'slug' ? 'Slug' : 'Title'}<input required={key !== 'titleEn'} value={form[key] ?? ''} onChange={e => setForm({ ...form, [key]: e.target.value })} className="h-11 min-w-0 rounded-md border px-3" /></label>)}<label className="grid gap-1.5 text-sm font-medium">Type<select value={form.collectionType} onChange={e => setForm({ ...form, collectionType: e.target.value as UpdateCollectionRequest['collectionType'] })} className="h-11 rounded-md border bg-white px-3">{['PACK', 'TRACK', 'DIPLOMA', 'DEGREE'].map(type => <option key={type}>{type}</option>)}</select></label><label className="grid gap-1.5 text-sm font-medium">Price (BDT)<input required min={0} step="0.01" type="number" value={form.priceBdt ?? 0} onChange={e => setForm({ ...form, priceBdt: Number(e.target.value) })} className="h-11 min-w-0 rounded-md border px-3" /></label></div><label className="mt-4 grid gap-1.5 text-sm font-medium">Description<textarea rows={3} value={form.description ?? ''} onChange={e => setForm({ ...form, description: e.target.value })} className="rounded-md border p-3" /></label>{error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}<button disabled={update.isPending} className="mt-4 rounded-lg border px-4 py-2.5 text-sm text-[#1c3873] disabled:opacity-60">{update.isPending ? 'Saving…' : 'Save program details'}</button></form>
}
