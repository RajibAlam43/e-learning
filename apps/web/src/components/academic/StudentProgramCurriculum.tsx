'use client'
import Link from 'next/link'
import { useStudentProgramCurriculum } from '@/services/program-curriculum.service'
import { ProgramOfferingChoices } from './ProgramOfferingChoices'
export function StudentProgramCurriculum({ id }: { id: string }) {
 const query = useStudentProgramCurriculum(id)
 if (query.isLoading) return <p role="status">Loading program requirements…</p>
 if (query.isError || !query.data) return <p role="alert">Unable to load program requirements. <button onClick={() => query.refetch()} className="underline">Retry</button></p>
 const item = query.data, rules = item.graduationRequirements, progress = item.creditProgress, allowance = item.electiveAllowance
 return <section className="rounded-xl bg-white p-5"><h2 className="font-heading text-xl text-[#1c3873]">Program courses and requirements</h2>
  {progress && <p className="mt-3 text-sm text-slate-700">{progress.earnedCredits} / {progress.requiredCredits} credits · {progress.mandatoryCoursesCompleted} / {progress.mandatoryCoursesRequired} mandatory courses · {progress.electiveCreditsCounted} / {progress.electiveCreditLimit} elective credits</p>}
  {rules?.requiredCredits && <p className="mt-2 max-w-prose text-sm text-slate-600">These graduation requirements were fixed when you purchased the program. Later course additions do not increase your required credits.</p>}
  {allowance?.limit != null && <p className="mt-2 text-sm text-slate-700">Elective allowance: {allowance.used} / {allowance.limit} credits reserved or credited · {allowance.remaining} remaining.</p>}
  <p className="mt-2 text-sm text-slate-600">{item.expiresAt ? `Program access ends ${new Date(item.expiresAt).toLocaleDateString()}.` : 'No program access end date.'}{!item.accessActive && ' Program access is inactive; new registrations are unavailable.'}</p>
  <ul className="mt-4 divide-y divide-slate-200">{item.availableCourses?.map(c => <li key={c.courseTemplateId} className="py-4">
   <div className="flex flex-wrap items-center justify-between gap-3"><div className="min-w-0"><h3 className="break-words font-medium">{c.title}</h3><p className="mt-1 text-sm text-slate-600">{c.credits} credits · {c.mandatoryForGraduation ? 'Mandatory for your graduation' : 'Optional'}{!c.offerings?.length && !c.enrolled ? ' · Planned' : ''}</p></div>
    {c.enrolled && <Link href={`/student/courses/${c.courseId}`} className="text-sm text-[#1c3873] underline underline-offset-4">Continue course</Link>}
   </div>
   {c.earnedCredits != null && <p className="mt-2 text-sm text-slate-700">{c.earnedCredits} academic credits already awarded. No additional registration is required.</p>}
   {c.registration && <p className="mt-2 text-sm text-slate-600">Selected: {c.registration.cohortName}.{!c.registration.mandatory ? ` ${c.registration.credits} elective credits reserved.` : ''}{!c.enrolled ? ' Contact an administrator to change this selection.' : ''}</p>}
   {!c.registration && c.earnedCredits == null && !!c.offerings?.length && item.accessActive && <ProgramOfferingChoices programId={id} title={c.title ?? 'Course'} mandatory={!!c.mandatoryForGraduation} enrolled={c.enrolled} offerings={c.offerings} />}
   {!c.enrolled && !c.registration && !c.offerings?.length && c.earnedCredits == null && <p className="mt-2 text-sm text-slate-600">No published offerings yet. Check back for an upcoming semester.</p>}
  </li>)}</ul>
 </section>
}
