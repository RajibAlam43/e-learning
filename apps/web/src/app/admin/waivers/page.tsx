'use client'
import { useRef, useState } from 'react'
import { useReviewWaiver, useWaivers, type Waiver } from '@/services/prerequisite.service'
import { useSemesters } from '@/services/academic.service'
import { WaiverHistory } from '@/components/academic/WaiverHistory'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { toast } from 'sonner'
export default function WaiversPage() {
 const [status, setStatus] = useState('PENDING')
 const [page, setPage] = useState(0)
 const query = useWaivers(true, status, page)
 const review = useReviewWaiver()
 const semesters = useSemesters()
 const [selected, setSelected] = useState<Waiver>()
 const [reason, setReason] = useState('')
 const [decision, setDecision] = useState<'APPROVED' | 'REJECTED'>('APPROVED')
 const [semesterId, setSemesterId] = useState('')
 const [error, setError] = useState('')
 const form = useRef<HTMLFormElement>(null)
 function select(item: Waiver) { setSelected(item); setReason(''); setError(''); setDecision('APPROVED'); setSemesterId(''); requestAnimationFrame(() => { form.current?.scrollIntoView({ block: 'start', behavior: 'smooth' }); form.current?.querySelector('select')?.focus() }) }
 async function submit(event: React.FormEvent) {
  event.preventDefault(); if (!selected?.id || !selected.currentCredits) return; setError('')
  try { await review.mutateAsync({ id: selected.id, decision, reason: reason.trim(), expectedCredits: selected.currentCredits, semesterId: semesterId || undefined }); setSelected(undefined); toast.success(decision === 'APPROVED' ? 'Waiver approved. Course credits are on the student’s record.' : 'Waiver rejected.') }
  catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to review request.'); void query.refetch() }
 }
 return <div className="mx-auto flex max-w-5xl flex-col gap-6">
  <AdminPageHeader title="Credit waivers" description="Review prior learning and award course credits without changing lesson or exam history." />
  {selected && <form ref={form} onSubmit={submit} className="grid scroll-mt-6 gap-4 rounded-xl bg-white p-5 sm:p-6">
   <h2 className="font-heading text-xl text-[#1c3873]">Review {selected.courseTitle}</h2><p className="text-sm text-slate-600">{selected.studentName} · {selected.currentCredits} current course credits{selected.currentCredits !== selected.requestedCredits ? ` (${selected.requestedCredits} when requested)` : ''}</p>
   <label className="grid gap-1.5 text-sm font-medium">Decision<select value={decision} onChange={e => setDecision(e.target.value as typeof decision)} className="h-11 rounded-md border bg-white px-3"><option value="APPROVED">Approve full course credit</option><option value="REJECTED">Reject request</option></select></label>
   {decision === 'APPROVED' && <><p className="text-sm text-slate-600">Approval satisfies prerequisites and mandatory program requirements for this course. An existing credit award is preserved.</p><label className="grid gap-1.5 text-sm font-medium">Academic semester (optional)<select value={semesterId} onChange={e => setSemesterId(e.target.value)} className="h-11 min-w-0 rounded-md border bg-white px-3"><option value="">No semester assigned</option>{semesters.data?.map(s => <option key={s.semesterId} value={s.semesterId}>{s.name}</option>)}</select></label></>}
   <label className="grid gap-1.5 text-sm font-medium">Review reason<textarea required maxLength={5000} rows={3} value={reason} onChange={e => setReason(e.target.value)} className="rounded-md border p-3" /></label>
   {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
   <div className="flex flex-wrap gap-3"><button disabled={review.isPending} className="rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{review.isPending ? 'Saving…' : decision === 'APPROVED' ? 'Approve and award credits' : 'Reject waiver'}</button><button type="button" disabled={review.isPending} onClick={() => setSelected(undefined)} className="rounded-lg border px-4 py-2.5 text-sm">Cancel review</button></div>
  </form>}
  <section className="min-w-0 rounded-xl bg-white p-5 sm:p-6"><label className="flex flex-wrap items-center gap-3 text-sm font-medium">Request status<select value={status} onChange={e => { setStatus(e.target.value); setPage(0); setSelected(undefined) }} className="h-11 rounded-md border bg-white px-3"><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select></label>
   {query.isLoading ? <p role="status" className="mt-4">Loading requests…</p> : query.isError ? <p role="alert" className="mt-4 text-red-700">Unable to load requests. <button onClick={() => query.refetch()} className="underline">Retry</button></p> : !query.data?.length ? <p className="mt-5 text-sm text-slate-600">No {status.toLowerCase()} waiver requests on this page.</p> : <WaiverHistory items={query.data} admin review={select} />}
   <div className="mt-4 flex gap-3"><button disabled={page === 0 || query.isFetching} onClick={() => setPage(page - 1)} className="rounded-lg border px-4 py-2 text-sm disabled:opacity-40">Previous</button><button disabled={(query.data?.length ?? 0) < 50 || query.isFetching} onClick={() => setPage(page + 1)} className="rounded-lg border px-4 py-2 text-sm disabled:opacity-40">Next</button></div>
  </section>
 </div>
}
