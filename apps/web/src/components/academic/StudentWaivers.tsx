'use client'
import { useRef, useState } from 'react'
import { useCreateWaiver, useWaiverCourses, useWaivers } from '@/services/prerequisite.service'
import { WaiverHistory } from './WaiverHistory'
import { toast } from 'sonner'
export function StudentWaivers({ initialTemplateId = '' }: { initialTemplateId?: string }) {
 const courses = useWaiverCourses()
 const [page, setPage] = useState(0)
 const history = useWaivers(false, 'PENDING', page)
 const create = useCreateWaiver()
 const [templateId, setTemplateId] = useState(initialTemplateId)
 const [reason, setReason] = useState('')
 const [file, setFile] = useState<File>()
 const [error, setError] = useState('')
 const input = useRef<HTMLInputElement>(null)
 async function submit(event: React.FormEvent) {
  event.preventDefault(); setError('')
  if (file && (file.size > 10485760 || file.size < 1 || !['application/pdf', 'image/jpeg', 'image/png'].includes(file.type))) { setError('Use a PDF, JPEG or PNG file up to 10 MB.'); return }
  try { await create.mutateAsync({ courseTemplateId: templateId, reason: reason.trim(), file }); setReason(''); setFile(undefined); if (input.current) input.current.value = ''; setPage(0); toast.success('Your waiver request has been submitted.') }
  catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to submit request. Try again.') }
 }
 return <div className="mx-auto flex max-w-4xl flex-col gap-6">
  <div><h1 className="font-heading text-2xl text-[#1c3873]">Credit waivers</h1><p className="mt-2 max-w-prose text-sm text-slate-600">Request recognition of prior learning for a course. Approval awards its credits and satisfies course prerequisites without marking lessons or exams completed.</p></div>
  <form onSubmit={submit} className="grid gap-4 rounded-xl bg-white p-5 sm:p-6">
   <h2 className="font-heading text-xl text-[#1c3873]">Request a waiver</h2>
   {courses.isError ? <p role="alert" className="text-sm text-red-700">Unable to load courses. <button type="button" onClick={() => courses.refetch()} className="underline">Retry</button></p> : <label className="grid gap-1.5 text-sm font-medium">Course<select required disabled={courses.isLoading} value={templateId} onChange={e => setTemplateId(e.target.value)} className="h-11 w-full min-w-0 rounded-md border bg-white px-3"><option value="">{courses.isLoading ? 'Loading courses…' : 'Select the course to receive credit for'}</option>{courses.data?.map(c => <option key={c.courseTemplateId} value={c.courseTemplateId}>{c.title} · {c.credits} credits</option>)}</select></label>}
   <label className="grid gap-1.5 text-sm font-medium">Reason<textarea required maxLength={5000} value={reason} onChange={e => setReason(e.target.value)} rows={4} placeholder="Describe your prior study or experience in this subject." className="w-full rounded-md border p-3" /></label>
   <label className="grid gap-1.5 text-sm font-medium">Supporting file (optional)<input ref={input} type="file" accept="application/pdf,image/jpeg,image/png" onChange={e => setFile(e.target.files?.[0])} className="w-full min-w-0 rounded-md border p-2" /><span className="font-normal text-slate-600">PDF, JPEG or PNG, up to 10 MB. Only you and administrators can access this file.</span></label>
   {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
   <button disabled={create.isPending || !courses.data?.length} className="justify-self-start rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{create.isPending ? 'Submitting…' : 'Submit waiver request'}</button>
  </form>
  <section className="min-w-0 rounded-xl bg-white p-5 sm:p-6"><h2 className="font-heading text-xl text-[#1c3873]">Your requests</h2>
   {history.isLoading ? <p role="status" className="mt-4">Loading requests…</p> : history.isError ? <p role="alert" className="mt-4 text-red-700">Unable to load requests. <button onClick={() => history.refetch()} className="underline">Retry</button></p> : !history.data?.length ? <p className="mt-4 text-sm text-slate-600">No waiver requests on this page.</p> : <WaiverHistory items={history.data} />}
   <div className="mt-4 flex gap-3"><button disabled={page === 0 || history.isFetching} onClick={() => setPage(page - 1)} className="rounded-lg border px-4 py-2 text-sm disabled:opacity-40">Previous</button><button disabled={(history.data?.length ?? 0) < 50 || history.isFetching} onClick={() => setPage(page + 1)} className="rounded-lg border px-4 py-2 text-sm disabled:opacity-40">Next</button></div>
  </section>
 </div>
}
