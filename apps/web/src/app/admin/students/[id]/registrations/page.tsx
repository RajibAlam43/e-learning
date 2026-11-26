'use client'
import Link from 'next/link'
import { useParams } from 'next/navigation'
import { useState, useRef } from 'react'
import { toast } from 'sonner'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { useAdminProgramRegistrations, useWithdrawProgramRegistration, programErrorMessage, type ProgramRegistration } from '@/services/program-curriculum.service'
export default function StudentProgramRegistrationsPage() {
 const { id } = useParams<{ id: string }>()
 const [page, setPage] = useState(0)
 const query = useAdminProgramRegistrations(id, page)
 const withdraw = useWithdrawProgramRegistration(id)
 const [selected, setSelected] = useState<ProgramRegistration>()
 const [reason, setReason] = useState('')
 const [error, setError] = useState('')
 const form = useRef<HTMLFormElement>(null)
 function select(item: ProgramRegistration) { setSelected(item); setReason(''); setError(''); requestAnimationFrame(() => { form.current?.scrollIntoView({ block: 'start' }); form.current?.querySelector('textarea')?.focus() }) }
 async function submit(event: React.FormEvent) {
  event.preventDefault(); if (!selected?.id) return; setError('')
  try { await withdraw.mutateAsync({ id: selected.id, reason: reason.trim() }); setSelected(undefined); toast.success('Registration withdrawn.') }
  catch (cause) { setError(programErrorMessage(cause, 'Unable to withdraw registration.')); void query.refetch() }
 }
 return <div className="mx-auto flex max-w-5xl flex-col gap-6"><Link className="text-sm text-[#1c3873] underline underline-offset-4" href="/admin/students">Back to students</Link><AdminPageHeader title="Program registrations" description="Withdraw unfinished course selections to let a student choose another offering. Completed courses and academic credit stay intact." />
  {selected && <form ref={form} onSubmit={submit} className="grid scroll-mt-6 gap-4 rounded-xl bg-white p-5 sm:p-6"><h2 className="font-heading text-xl text-[#1c3873]">Withdraw {selected.courseTitle}</h2><p className="text-sm text-slate-600">{selected.programTitle} · {selected.cohortName} · {selected.credits} credits. Separate valid course access is preserved.</p><label className="grid gap-1.5 text-sm font-medium">Withdrawal reason<textarea required maxLength={5000} rows={3} value={reason} onChange={e => setReason(e.target.value)} className="rounded-md border p-3" /></label>{error && <p role="alert" className="text-sm text-red-700">{error}</p>}<div className="flex flex-wrap gap-3"><button disabled={withdraw.isPending} className="rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{withdraw.isPending ? 'Withdrawing…' : 'Withdraw registration'}</button><button type="button" disabled={withdraw.isPending} onClick={() => setSelected(undefined)} className="rounded-lg border px-4 py-2.5 text-sm">Cancel</button></div></form>}
  <section aria-label="Program registration history" className="min-w-0 rounded-xl bg-white p-5 sm:p-6"><h2 className="font-heading text-xl text-[#1c3873]">Latest registrations</h2>
   {query.isLoading ? <p role="status" className="mt-4">Loading registrations…</p> : query.isError ? <p role="alert" className="mt-4 text-red-700">Unable to load registrations. <button onClick={() => query.refetch()} className="underline">Retry</button></p> : !query.data?.length ? <p className="mt-4 text-sm text-slate-600">No program course selections have been recorded for this student.</p> : <ul className="mt-3 divide-y divide-slate-200">{query.data.map(item => <li key={item.id} className="py-5"><div className="flex flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h3 className="break-words font-semibold">{item.courseTitle}</h3><p className="mt-1 break-words text-sm text-slate-600">{item.programTitle} · {item.cohortName}</p><p className="mt-1 text-sm text-slate-600">{item.status} · {item.credits} credits · {item.mandatory ? 'Mandatory' : 'Elective'}</p></div>{item.canWithdraw && <button onClick={() => select(item)} className="rounded-lg border px-4 py-2 text-sm text-[#1c3873]">Withdraw<span className="sr-only"> {item.courseTitle}</span></button>}</div>{item.withdrawalReason && <p className="mt-3 max-w-prose whitespace-pre-wrap break-words text-sm text-slate-700">{item.withdrawalReason}{item.withdrawnByName ? ` · ${item.withdrawnByName}` : ''}</p>}</li>)}</ul>}
   <div className="mt-4 flex gap-3"><button disabled={page === 0 || query.isFetching} onClick={() => setPage(page - 1)} className="rounded-lg border px-4 py-2 text-sm disabled:opacity-40">Previous</button><button disabled={(query.data?.length ?? 0) < 50 || query.isFetching} onClick={() => setPage(page + 1)} className="rounded-lg border px-4 py-2 text-sm disabled:opacity-40">Next</button></div>
  </section>
 </div>
}
