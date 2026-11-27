'use client'

import { useRef, useState } from 'react'
import { toast } from 'sonner'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { useSemesters, useSaveSemester, type Semester } from '@/services/academic.service'
import { dateTimeInputToIso, toDateTimeInput } from '@/lib/date-time'

const zone = 'Asia/Dhaka'
const empty = { code: '', name: '', startsAt: '', endsAt: '', registrationStartsAt: '', registrationEndsAt: '' }
const dateFields = [ ['startsAt', 'Teaching starts'], ['endsAt', 'Teaching ends'], ['registrationStartsAt', 'Registration opens'], ['registrationEndsAt', 'Registration closes'] ] as const

export default function SemestersPage() {
  const query = useSemesters()
  const save = useSaveSemester()
  const formRef = useRef<HTMLFormElement>(null)
  const [editing, setEditing] = useState<Semester>()
  const id = editing?.semesterId
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')

  function edit(semester: Semester) {
    setEditing(semester)
    setForm({ code: semester.code ?? '', name: semester.name ?? '',
      startsAt: toDateTimeInput(semester.startsAt, zone), endsAt: toDateTimeInput(semester.endsAt, zone),
      registrationStartsAt: toDateTimeInput(semester.registrationStartsAt, zone),
      registrationEndsAt: toDateTimeInput(semester.registrationEndsAt, zone) })
    setError('')
    formRef.current?.scrollIntoView({ block: 'start' })
    formRef.current?.querySelector('input')?.focus()
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    try {
      const dates = Object.fromEntries(dateFields.map(([key]) => [key, dateTimeInputToIso(form[key], zone, editing?.[key])]))
      await save.mutateAsync({ id, data: { ...form, ...dates } })
      toast.success(id ? 'Semester updated.' : 'Semester created.')
      setEditing(undefined); setForm(empty)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to save semester.') }
  }

  return <div className="mx-auto flex w-full max-w-5xl flex-col gap-6">
    <AdminPageHeader title="Semesters" description="Set institution-wide teaching and registration dates for course cohorts." />
    <form ref={formRef} onSubmit={submit} className="scroll-mt-6 rounded-xl bg-white p-5 sm:p-6">
      <h2 className="font-heading text-xl text-[#1c3873]">{id ? 'Edit semester' : 'Create semester'}</h2>
      <p className="mt-2 text-sm text-slate-600">All times are in Asia/Dhaka. Cohorts inherit these dates unless you set a narrower course window.</p>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <label className="grid gap-1.5 text-sm font-medium">Name<input required maxLength={150} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} placeholder="Spring 2027" className="h-11 min-w-0 rounded-md border px-3" /></label>
        <label className="grid gap-1.5 text-sm font-medium">Code<input required maxLength={40} pattern="[A-Za-z0-9_-]+" value={form.code} onChange={event => setForm({ ...form, code: event.target.value })} placeholder="SPRING-2027" className="h-11 min-w-0 rounded-md border px-3" /></label>
        {dateFields.map(([key, label]) => <label key={key} className="grid min-w-0 gap-1.5 text-sm font-medium">{label}<input required type="datetime-local" value={form[key]} onChange={event => setForm({ ...form, [key]: event.target.value })} className="h-11 w-full min-w-0 rounded-md border px-3" /></label>)}
      </div>
      {error && <p role="alert" className="mt-4 text-sm text-red-700">{error}</p>}
      <div className="mt-5 flex flex-wrap gap-3">
        <button disabled={save.isPending} className="rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{save.isPending ? 'Saving…' : id ? 'Save changes' : 'Create semester'}</button>
        {id && <button type="button" onClick={() => { setEditing(undefined); setForm(empty); setError('') }} className="rounded-lg border px-4 py-2.5 text-sm">Cancel edit</button>}
      </div>
    </form>
    <section aria-label="Institution semesters" className="min-w-0 rounded-xl bg-white p-5 sm:p-6">
      <h2 className="font-heading text-xl text-[#1c3873]">Institution semesters</h2>
      {query.isLoading ? <p role="status" className="mt-4 text-sm text-slate-600">Loading semesters…</p>
        : query.isError ? <p role="alert" className="mt-4 text-sm text-red-700">Unable to load semesters. <button onClick={() => query.refetch()} className="underline">Retry</button></p>
        : query.data?.length === 0 ? <p className="mt-4 text-sm text-slate-600">Create your first semester above, then select it when adding a cohort.</p>
        : <ul className="mt-4 divide-y divide-slate-200">{query.data?.map(item => <li key={item.semesterId} className="flex flex-wrap items-center justify-between gap-3 py-4">
          <div><h3 className="font-semibold text-slate-800">{item.name} <span className="font-normal text-slate-600">({item.code})</span></h3><p className="mt-1 text-sm text-slate-600">{toDateTimeInput(item.startsAt, zone).replace('T', ' ')} – {toDateTimeInput(item.endsAt, zone).replace('T', ' ')} · {item.status?.replaceAll('_', ' ')}</p></div>
          <button type="button" onClick={() => edit(item)} aria-label={`Edit ${item.name}`} className="rounded-lg border px-4 py-2 text-sm text-[#1c3873] hover:bg-slate-50">Edit</button>
        </li>)}</ul>}
    </section>
  </div>
}
