'use client'

import Link from 'next/link'
import { useState } from 'react'
import { toast } from 'sonner'
import type { AdminCourseDetail } from '@/types'
import { SemesterPicker } from './CourseAcademicFields'
import { useCourseOfferings, useCreateOffering } from '@/services/academic.service'

export function CourseOfferings({ course }: { course: AdminCourseDetail }) {
  const id = course.courseId ?? ''
  const query = useCourseOfferings(id)
  const create = useCreateOffering()
  const [semesterId, setSemesterId] = useState('')
  const [slug, setSlug] = useState('')
  const [capacity, setCapacity] = useState(0)
  const [error, setError] = useState('')

  async function submit(event: React.FormEvent) {
    event.preventDefault(); setError('')
    try {
      const offering = await create.mutateAsync({ id, data: { slug, semesterId, studyMode: 'COHORT_BASED',
        priceBdt: course.priceBdt ?? 0, isFree: course.isFree, timezone: course.timezone ?? 'Asia/Dhaka',
        capacity: capacity > 0 ? capacity : undefined, accessDurationDays: course.accessDurationDays ?? undefined } })
      setSlug(''); setSemesterId(''); toast.success('Cohort created. Assign its teachers before scheduling classes.')
      if (offering.courseId) await query.refetch()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to create cohort.') }
  }

  return <div className="grid gap-6">
    <section className="rounded-xl bg-white p-5">
      <h2 className="font-heading text-xl text-[#1c3873]">Add cohort</h2>
      <p className="mt-2 text-sm text-slate-600">Use this course curriculum for another semester or batch. Credits count once across its cohorts. Curriculum edits apply to every cohort.</p>
      <form onSubmit={submit} className="mt-5 grid gap-4 sm:grid-cols-2">
        <SemesterPicker value={semesterId} onChange={setSemesterId} required />
        <label className="grid gap-1.5 text-sm font-medium">Unique cohort slug<input required value={slug} onChange={event => setSlug(event.target.value)} placeholder="arabic-1-spring-2027-a" className="h-11 min-w-0 rounded-md border px-3" /></label>
        <label className="grid gap-1.5 text-sm font-medium">Capacity<input type="number" min={0} step={1} value={capacity} onChange={event => setCapacity(Number(event.target.value))} className="h-11 min-w-0 rounded-md border px-3" /><span className="text-xs font-normal text-slate-600">0 means no course capacity limit.</span></label>
        <p className="self-center text-sm text-slate-600">Teaching and registration dates come from the semester. Price: {(course.priceBdt ?? 0).toLocaleString()} BDT. You can edit the cohort after creation.</p>
        {error && <p role="alert" className="text-sm text-red-700 sm:col-span-2">{error}</p>}
        <button disabled={create.isPending} className="justify-self-start rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{create.isPending ? 'Creating…' : 'Create cohort'}</button>
      </form>
    </section>
    <section className="rounded-xl bg-white p-5">
      <h2 className="font-heading text-xl text-[#1c3873]">Course cohorts</h2>
      {query.isLoading ? <p role="status" className="mt-4 text-sm text-slate-600">Loading cohorts…</p> : query.isError ? <p role="alert" className="mt-4 text-sm text-red-700">Unable to load cohorts. <button type="button" onClick={() => query.refetch()} className="underline">Retry</button></p>
        : <ul className="mt-4 divide-y">{query.data?.map(item => <li key={item.courseId} className="py-3"><Link href={`/admin/courses/${item.courseId}`} className="font-medium text-[#1c3873] underline underline-offset-4">{item.slug}</Link><p className="mt-1 text-sm text-slate-600">{item.semester?.name ?? 'Semester not assigned'} · {item.status}</p></li>)}</ul>}
    </section>
  </div>
}
