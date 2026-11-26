'use client'
import Link from 'next/link'
import { useState } from 'react'
import { toast } from 'sonner'
import { programErrorMessage, useRegisterProgramCourse, type ProgramOffering } from '@/services/program-curriculum.service'
function date(value?: string) { return value ? new Date(value).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : '' }
export function ProgramOfferingChoices({ programId, title, mandatory, offerings, enrolled = false }: { programId: string; title: string; mandatory: boolean; offerings: ProgramOffering[]; enrolled?: boolean }) {
 const registration = useRegisterProgramCourse(programId)
 const [error, setError] = useState('')
 async function register(courseId: string) {
  setError('')
  try { await registration.mutateAsync(courseId); toast.success('Course registration confirmed. You can now continue to your course.') }
  catch (cause) { setError(programErrorMessage(cause, 'Unable to register. Refresh the program and try again.')) }
 }
 return <details className="mt-4 w-full rounded-lg border border-slate-200 p-4">
  <summary className="cursor-pointer py-1 text-sm font-semibold text-[#1c3873]">{enrolled ? 'Use program access for' : 'Choose an offering for'} {title}</summary>
  <p className="mt-3 max-w-prose text-sm text-slate-600">{mandatory ? 'This mandatory course does not use your elective allowance.' : 'Registering reserves the course credits from your elective allowance.'} Ask an administrator if you need to change an unfinished selection.</p>
  {enrolled && <p className="mt-2 text-sm text-slate-600">Your existing course access will be preserved.</p>}
  <ul className="mt-3 divide-y divide-slate-200">{offerings.map(o => <li key={o.courseId} className="py-4">
   <h4 className="font-medium text-slate-900">{o.name}</h4>
   <p className="mt-1 text-sm text-slate-600">{o.semester?.name ?? (o.studyMode === 'SELF_PACED' ? 'Self-paced' : 'Semester not assigned')} · {o.credits} credits{o.seatsAvailable != null ? ` · ${o.seatsAvailable} seats available` : ''}</p>
   {o.startsAt && <p className="mt-1 text-sm text-slate-600">{date(o.startsAt)}{o.endsAt ? ` – ${date(o.endsAt)}` : ''}</p>}
   {o.accessExpiresAt && <p className="mt-1 text-sm text-slate-600">Study access through {date(o.accessExpiresAt)}.</p>}
   {o.blockedReason && <p className="mt-2 text-sm text-slate-700">{o.blockedReason}</p>}
   {o.blockedReason && !!o.missingPrerequisites?.length && <ul className="mt-2 space-y-2 text-sm">{o.missingPrerequisites.map(r => <li key={r.courseTemplateId}>{r.title} <Link className="ml-2 text-[#1c3873] underline underline-offset-4" href={`/student/waivers?courseTemplateId=${r.courseTemplateId}`}>Request a waiver</Link></li>)}</ul>}
   {!enrolled && o.slug && o.blockedReason === 'Insufficient elective credit allowance' && <Link href={`/courses/${o.slug}/checkout`} className="mt-2 block w-fit text-sm text-[#1c3873] underline underline-offset-4">Buy this course separately</Link>}
   <button type="button" disabled={!o.canRegister || registration.isPending} onClick={() => register(o.courseId!)} aria-label={`Register for ${o.name}`} className="mt-3 rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50">{registration.isPending ? 'Registering…' : 'Register for this offering'}</button>
  </li>)}</ul>
  {error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}
 </details>
}
