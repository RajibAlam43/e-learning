'use client'
import Link from 'next/link'
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useIssueCollectionCertificate } from '@/hooks/useStudentDashboard'
import { programErrorMessage } from '@/services/program-curriculum.service'
import { useCheckGraduation, type ProgramGraduation } from '@/services/graduation.service'

const button = 'rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-[#1c3873] hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-50'
const labels = { COMPLETED: 'Program completed', ELIGIBLE: 'Graduation requirements met', IN_PROGRESS: 'Requirements remaining', ACCESS_INACTIVE: 'Program access inactive' }

export function ProgramGraduationSummary({ report, userId, showTitle = true }: { report: ProgramGraduation; userId?: string; showTitle?: boolean }) {
  const check = useCheckGraduation(report.programId ?? '', userId)
  const issue = useIssueCollectionCertificate()
  const cache = useQueryClient()
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const p = report.creditProgress
  async function checkCompletion() {
    setError(''); setMessage('')
    try { const result = await check.mutateAsync(); setMessage(result.completedAt ? 'Program completion is recorded.' : 'Requirements are still remaining. Review the course breakdown below.') }
    catch (cause) { setError(programErrorMessage(cause, 'Unable to check graduation. Try again.')) }
  }
  async function certificate() {
    if (!report.programId) return
    setError(''); setMessage('')
    try {
      const result = await issue.mutateAsync(report.programId)
      await cache.invalidateQueries({ queryKey: ['student'] })
      setMessage('Your certificate is ready in My Certificates.')
      if (result.downloadUrl || result.pdfUrl) window.open(result.downloadUrl || result.pdfUrl, '_blank', 'noopener,noreferrer')
    } catch (cause) { setError(programErrorMessage(cause, 'Unable to prepare the certificate. Try again.')) }
  }
  return <section aria-label={`Graduation progress for ${report.title}`} className="min-w-0 rounded-xl bg-white p-5 sm:p-6">
    <h2 className="break-words font-heading text-xl text-[#1c3873]">{showTitle ? report.title : 'Graduation progress'}</h2>
    <p className="mt-3 font-semibold text-[#1c3873]">{report.status ? labels[report.status] : 'Graduation progress'}</p>
    {report.completedAt && <p className="mt-1 text-sm text-slate-600">Completion recorded {new Date(report.completedAt).toLocaleDateString()}. Earned credits and recorded completion are retained when study access ends.</p>}
    {!report.accessActive && <p className="mt-2 max-w-prose text-sm text-slate-600">Study access is inactive or expired. {report.certificateId && !report.certificateRevoked ? 'Your issued certificate remains available.' : 'Contact an administrator to restore access before a new completion check or certificate request.'}</p>}
    {p ? <><p className="mt-3 text-sm text-slate-700">{p.earnedCredits} / {p.requiredCredits} counted credits · {p.mandatoryCoursesCompleted} / {p.mandatoryCoursesRequired} mandatory courses satisfied</p>
      <p className="mt-2 text-sm text-slate-600">{!report.completedAt && `${report.creditsRemaining} more credits needed. `}Elective credits counted: {p.electiveCreditsCounted} / {p.electiveCreditLimit}.</p>
      <p className="mt-2 max-w-prose text-sm text-slate-600">Your original mandatory requirements and credit target apply. Approved waivers award academic credit; they do not mark lessons or exams completed. Eligible credits can count toward more than one program.</p>
      <details className="mt-5"><summary className="cursor-pointer font-medium text-[#1c3873] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2">Counted credits and remaining courses</summary>
        <ul className="mt-3 divide-y divide-slate-200">{report.courses?.map(c => <li key={c.courseTemplateId} className="grid gap-1.5 py-4 text-sm"><h3 className="break-words font-semibold">{c.title}</h3>
          <p className="text-slate-600">{c.mandatory ? 'Mandatory' : 'Elective'} · {c.expectedCredits} course credits · {c.awardedCredits} awarded · {c.countedCredits} counted</p>
          <p className="text-slate-600">{c.source === 'EXEMPTION' ? 'Credit awarded by approved waiver' : c.source === 'COMPLETION' ? 'Credit awarded for course completion' : c.mandatory ? 'Mandatory course credit still required' : 'No academic credit awarded yet'}{c.achievedAt ? ` · ${new Date(c.achievedAt).toLocaleDateString()}` : ''}</p>
          {!c.mandatory && (c.awardedCredits ?? 0) > (c.countedCredits ?? 0) && <p className="text-slate-700">{(c.awardedCredits ?? 0) - (c.countedCredits ?? 0)} awarded credits exceed this program’s elective allowance. They remain on the academic record.</p>}
        </li>)}</ul>
      </details>
    </> : <p className="mt-3 max-w-prose text-sm text-slate-600">This program uses its original mandatory-course completion requirements. Academic credit totals do not replace those legacy requirements.</p>}
    {report.certificateId && <p className="mt-4 break-words text-sm text-slate-700">Certificate: {report.certificateRevoked ? 'Revoked. Contact the institute.' : report.certificateCode}</p>}
    <div className="mt-5 flex flex-wrap items-center gap-3">
      {report.canCheckCompletion && <button className={button} disabled={check.isPending || issue.isPending} onClick={checkCompletion}>{check.isPending ? 'Checking…' : 'Check graduation'}</button>}
      {!userId && <><button className={button} disabled={!report.canRequestCertificate || issue.isPending || check.isPending} onClick={certificate}>{issue.isPending ? 'Preparing…' : report.certificateId ? 'Get certificate' : 'Get program certificate'}</button><Link className="text-sm text-[#1c3873] underline underline-offset-4" href="/student/certificates">My Certificates</Link></>}
      {userId && report.certificateCode && !report.certificateRevoked && <Link className="text-sm text-[#1c3873] underline underline-offset-4" href={`/verify-certificate/${encodeURIComponent(report.certificateCode)}`}>Verify certificate</Link>}
    </div>
    {error && <p role="alert" className="mt-3 break-words text-sm text-red-700">{error}</p>}{message && <p role="status" className="mt-3 text-sm text-slate-700">{message}</p>}
  </section>
}
