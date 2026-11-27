'use client'
import Link from 'next/link'
import { useProgramGraduation } from '@/services/graduation.service'
import { ProgramGraduationSummary } from './ProgramGraduationSummary'

export function ProgramGraduationPanel({ id }: { id: string }) {
  const q = useProgramGraduation(id)
  return <div className="grid min-w-0 gap-4"><Link className="justify-self-start text-sm text-[#1c3873] underline underline-offset-4" href="/student/academic-record">Academic record and program progress</Link>
    {q.isLoading ? <p role="status">Loading graduation progress…</p> : q.isError || !q.data ? <p role="alert">Unable to load graduation progress. <button className="underline" onClick={() => q.refetch()}>Retry</button></p> : <ProgramGraduationSummary report={q.data} showTitle={false} />}
  </div>
}
