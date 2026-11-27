'use client'
import Link from 'next/link'
import { useAcademicProgress } from '@/services/graduation.service'
import { ProgramGraduationSummary } from './ProgramGraduationSummary'

export function AcademicProgressView({ userId }: { userId?: string }) {
  const q = useAcademicProgress(userId)
  const record = q.data?.academicRecord
  const semesters = new Map(q.data?.semesters?.map(s => [s.id, `${s.code} · ${s.name}`]))
  return <div className="mx-auto grid w-full min-w-0 max-w-5xl gap-6">
    <nav aria-label="Academic navigation" className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-[#1c3873]">{userId ? <><Link className="underline underline-offset-4" href="/admin/students">Students</Link><Link className="underline underline-offset-4" href={`/admin/students/${userId}/enrollments`}>Manual enrollment</Link><Link className="underline underline-offset-4" href={`/admin/students/${userId}/registrations`}>Program registrations</Link></> : <><Link className="underline underline-offset-4" href="/student/collections">My Programs</Link><Link className="underline underline-offset-4" href="/student/waivers">Credit waivers</Link></>}</nav>
    <header><h1 className="font-heading text-3xl text-[#1c3873]">Academic record</h1><p className="mt-2 max-w-prose text-sm text-slate-600">Awarded course credits and progress toward each program’s original graduation requirements.</p></header>
    {q.isLoading ? <p role="status">Loading academic record…</p> : q.isError || !q.data ? <p role="alert" className="text-red-700">Unable to load the academic record. <button className="underline" onClick={() => q.refetch()}>Retry</button></p> : <>
      <div className="min-w-0"><h2 className="break-words font-heading text-xl text-[#1c3873]">{q.data.fullName}</h2><p className="mt-1 break-all text-sm text-slate-600">{q.data.email || q.data.phone}</p></div>
      <section aria-label="Awarded academic credits" className="min-w-0 rounded-xl bg-white p-5 sm:p-6"><h2 className="font-heading text-xl text-[#1c3873]">Awarded credits</h2><p className="mt-3 text-sm text-slate-700">{record?.totalCredits ?? 0} academic credits earned. Each course curriculum is counted once on this record.</p>
        {!record?.achievements?.length ? <p className="mt-4 text-sm text-slate-600">No academic credits have been awarded yet. Credits appear after course completion or an approved credit waiver.</p> : <ul className="mt-3 divide-y divide-slate-200">{record.achievements.map(a => <li key={a.achievementId} className="grid gap-1.5 py-4 text-sm"><h3 className="break-words font-semibold">{a.courseTitle}</h3><p className="text-slate-700">{a.credits} credits · {a.source === 'EXEMPTION' ? 'Approved waiver' : 'Course completion'}</p><p className="break-words text-slate-600">{a.semesterId ? semesters.get(a.semesterId) ?? 'Semester recorded' : 'No semester assigned'}{a.achievedAt ? ` · ${new Date(a.achievedAt).toLocaleDateString()}` : ''}</p></li>)}</ul>}
      </section>
      <h2 className="font-heading text-2xl text-[#1c3873]">Program progress</h2>
      {!q.data.programs?.length ? <p className="text-sm text-slate-600">No program enrollments have been recorded.</p> : q.data.programs.map(p => <ProgramGraduationSummary key={p.programId} report={p} userId={userId} />)}
    </>}
  </div>
}
