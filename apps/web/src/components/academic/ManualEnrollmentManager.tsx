'use client'
import Link from 'next/link'
import { useRef, useState } from 'react'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { useAdminCollections, useAdminCourses } from '@/hooks/useAdmin'
import { programErrorMessage } from '@/services/program-curriculum.service'
import { useManualEnrollment, useManualEnrollmentOverview } from '@/services/manual-enrollment.service'

type Mode = 'course' | 'program' | 'registration'
const input = 'w-full min-w-0 rounded-md border border-slate-300 bg-white p-3 text-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1c3873]'
const button = 'rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-[#1c3873] hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-50'
function date(value?: string) { return value ? new Date(value).toLocaleString() : 'No expiry' }

export function ManualEnrollmentManager({ userId }: { userId: string }) {
  const [page, setPage] = useState(0)
  const overview = useManualEnrollmentOverview(userId, page)
  const courses = useAdminCourses()
  const programs = useAdminCollections()
  const enroll = useManualEnrollment(userId)
  const [mode, setMode] = useState<Mode>('course')
  const [courseId, setCourseId] = useState('')
  const [programId, setProgramId] = useState('')
  const [reason, setReason] = useState('')
  const [overrideDates, setOverrideDates] = useState(false)
  const [overrideCapacity, setOverrideCapacity] = useState(false)
  const [allowUnpublished, setAllowUnpublished] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const attempt = useRef<{ body: string; requestId: string } | null>(null)
  const submitting = useRef(false)

  function changeMode(value: Mode) {
    setMode(value); setCourseId(''); setProgramId(''); setOverrideDates(false)
    setOverrideCapacity(false); setAllowUnpublished(false); setError(''); setSuccess('')
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (submitting.current) return
    submitting.current = true; setError(''); setSuccess('')
    const body = { courseId: mode !== 'program' ? courseId : undefined,
      programId: mode !== 'course' ? programId : undefined, programFunded: mode === 'registration',
      overrideDates, overrideCapacity, allowUnpublished, reason: reason.trim() }
    const serialized = JSON.stringify(body)
    if (attempt.current?.body !== serialized) attempt.current = { body: serialized, requestId: crypto.randomUUID() }
    try {
      const result = await enroll.mutateAsync({ ...body, requestId: attempt.current.requestId })
      setSuccess(result.changed
        ? `Enrollment saved. Access ends: ${date(result.accessExpiresAt)}.`
        : 'This student already has this access. The existing enrollment and expiry were preserved.')
      setPage(0); setReason(''); setCourseId(''); setProgramId(''); setOverrideDates(false)
      setOverrideCapacity(false); setAllowUnpublished(false); attempt.current = null
    } catch (cause) {
      setError(programErrorMessage(cause, 'Unable to enroll. Check the selection and try again.'))
      void overview.refetch()
    } finally { submitting.current = false }
  }

  const optionsUnavailable = mode === 'program' ? programs.isError : courses.isError
  const optionsLoading = mode === 'program' ? programs.isLoading : courses.isLoading
  return <div className="mx-auto flex max-w-5xl flex-col gap-6">
    <nav aria-label="Student administration" className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-[#1c3873]">
      <Link className="underline underline-offset-4" href="/admin/students">Back to students</Link>
      <Link className="underline underline-offset-4" href={`/admin/students/${userId}/registrations`}>Program registrations</Link>
    </nav>
    <AdminPageHeader title="Manual enrollment" description="Grant course or program access, or register a student using their program entitlement." />
    {overview.isLoading ? <p role="status">Loading student…</p> : overview.isError ? <p role="alert" className="text-red-700">Unable to load this student. <button className="underline" onClick={() => overview.refetch()}>Retry</button></p> : <>
      <div className="min-w-0"><h2 className="break-words font-heading text-xl text-[#1c3873]">{overview.data?.fullName}</h2><p className="mt-1 break-all text-sm text-slate-600">{overview.data?.email || overview.data?.phone || userId}</p></div>
      <form aria-label="Manual enrollment" onSubmit={submit} className="grid min-w-0 gap-5 rounded-xl bg-white p-5 sm:p-6">
        <label className="grid min-w-0 gap-1.5 text-sm font-medium">Enrollment type<select className={input} value={mode} onChange={e => changeMode(e.target.value as Mode)} disabled={enroll.isPending}>
          <option value="course">Course access</option><option value="program">Program access</option><option value="registration">Course through a program</option>
        </select></label>
        {mode !== 'course' && <label className="grid min-w-0 gap-1.5 text-sm font-medium">Program<select required className={input} value={programId} onChange={e => setProgramId(e.target.value)} disabled={enroll.isPending || mode === 'program' && programs.isLoading}>
          <option value="">Select a program</option>
          {mode === 'registration' ? overview.data?.programs?.filter(p => p.active).map(p => <option key={p.programId} value={p.programId}>{p.title}</option>)
            : programs.data?.map(p => <option key={p.collectionId} value={p.collectionId}>{p.title} · {p.status}</option>)}
        </select></label>}
        {mode === 'registration' && !overview.data?.programs?.some(p => p.active) && <p role="status" className="text-sm text-slate-600">This student has no active program access. Choose Program access first.</p>}
        {mode !== 'program' && <label className="grid min-w-0 gap-1.5 text-sm font-medium">Course offering<select required className={input} value={courseId} onChange={e => setCourseId(e.target.value)} disabled={enroll.isPending || courses.isLoading}>
          <option value="">Select a course offering</option>{courses.data?.map(c => <option key={c.courseId} value={c.courseId}>{c.title} · {c.slug} · {c.status}</option>)}
        </select></label>}
        <p className="max-w-prose text-sm leading-relaxed text-slate-600">{mode === 'program'
          ? 'Uses the program’s configured access period and preserves existing graduation requirements. Course seats are chosen separately. No payment or academic credits are created.'
          : mode === 'registration' ? 'Uses the student’s active program access and elective allowance. The course must be included and published. No additional payment or academic credits are created.'
          : 'Uses the course’s configured access period. Existing valid access stays unchanged. No payment or academic credits are created.'}</p>
        {mode !== 'program' && <div className="grid gap-3"><label className="flex items-start gap-3 text-sm"><input className="mt-1 size-4 shrink-0 accent-[#1c3873]" type="checkbox" checked={overrideDates} onChange={e => setOverrideDates(e.target.checked)} disabled={enroll.isPending} />Override semester and course registration dates</label>
          <label className="flex items-start gap-3 text-sm"><input className="mt-1 size-4 shrink-0 accent-[#1c3873]" type="checkbox" checked={overrideCapacity} onChange={e => setOverrideCapacity(e.target.checked)} disabled={enroll.isPending} />Override course capacity</label></div>}
        {mode !== 'registration' && <label className="flex items-start gap-3 text-sm"><input className="mt-1 size-4 shrink-0 accent-[#1c3873]" type="checkbox" checked={allowUnpublished} onChange={e => setAllowUnpublished(e.target.checked)} disabled={enroll.isPending} />Allow an unpublished {mode}</label>}
        {mode !== 'program' && <p className="max-w-prose text-sm text-slate-600">Prerequisites still apply. An exception requires an approved <Link className="text-[#1c3873] underline underline-offset-4" href="/admin/waivers">credit waiver</Link>.</p>}
        <label className="grid gap-1.5 text-sm font-medium">Enrollment reason<textarea required maxLength={5000} minLength={1} rows={3} className={input} value={reason} onChange={e => setReason(e.target.value)} disabled={enroll.isPending} /></label>
        <p className="text-sm text-slate-600">Your name, reason, time and selected overrides will be recorded.</p>
        {optionsUnavailable && <p role="alert" className="text-sm text-red-700">Unable to load enrollment options. <button type="button" className="underline" onClick={() => { void courses.refetch(); void programs.refetch() }}>Retry</button></p>}
        {error && <p role="alert" className="break-words text-sm text-red-700">{error}</p>}
        {success && <p role="status" className="break-words text-sm text-green-800">{success}</p>}
        <button disabled={enroll.isPending || optionsUnavailable || optionsLoading || !reason.trim()} className="justify-self-start rounded-lg bg-[#1c3873] px-5 py-3 text-sm font-semibold text-white hover:bg-[#152b59] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-50">{enroll.isPending ? 'Saving…' : 'Save enrollment'}</button>
      </form>
      <section aria-label="Manual enrollment history" className="min-w-0 rounded-xl bg-white p-5 sm:p-6"><h2 className="font-heading text-xl text-[#1c3873]">Enrollment history</h2>
        {!overview.data?.history?.length ? <p className="mt-4 text-sm text-slate-600">No manual enrollment actions have been recorded.</p> : <ul className="mt-3 divide-y divide-slate-200">{overview.data.history.map(a => <li key={a.id} className="grid gap-2 py-5 text-sm">
          <h3 className="break-words font-semibold">{a.courseTitle || a.programTitle}</h3>
          {a.programFunded && <p className="break-words text-slate-600">Through {a.programTitle}</p>}
          <p className="text-slate-600">{a.changed ? 'Access granted' : 'Existing access preserved'} · {date(a.createdAt)} · {a.actorName}</p>
          <p className="text-slate-600">Access ends: {date(a.accessExpiresAt)}</p>
          {(a.overrideDates || a.overrideCapacity || a.allowUnpublished) && <p className="text-slate-700">Overrides: {[a.overrideDates && 'registration dates', a.overrideCapacity && 'capacity', a.allowUnpublished && 'unpublished access'].filter(Boolean).join(', ')}</p>}
          <p className="max-w-prose whitespace-pre-wrap break-words text-slate-700">{a.reason}</p>
        </li>)}</ul>}
        <div className="mt-4 flex flex-wrap gap-3"><button className={button} disabled={page === 0 || overview.isFetching} onClick={() => setPage(page - 1)}>Previous</button><button className={button} disabled={(overview.data?.history?.length ?? 0) < 50 || overview.isFetching} onClick={() => setPage(page + 1)}>Next</button></div>
      </section>
    </>}
  </div>
}
