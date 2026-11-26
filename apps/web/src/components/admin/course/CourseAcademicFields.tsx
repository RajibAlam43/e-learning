'use client'

import Link from 'next/link'
import { useController, useWatch, type Control } from 'react-hook-form'
import { useSemesters } from '@/services/academic.service'
import type { CourseFormValues } from '@/lib/course-create-schema'

export function SemesterPicker({ value, onChange, required = false }: {
  value: string; onChange: (value: string) => void; required?: boolean
}) {
  const semesters = useSemesters()
  return <label className="grid gap-1.5 text-sm font-medium text-[#1c3873]">Semester{required ? ' *' : ''}
    <select value={value} onChange={event => onChange(event.target.value)} required={required}
      disabled={semesters.isLoading || semesters.isError}
      className="h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm text-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-60">
      <option value="">{semesters.isLoading ? 'Loading semesters…' : 'Select semester'}</option>
      {(semesters.data ?? []).map(item => <option key={item.semesterId} value={item.semesterId}>{item.name} ({item.code})</option>)}
    </select>
    {semesters.isError && <span role="alert" className="text-sm text-red-700">Unable to load semesters. <button type="button" onClick={() => semesters.refetch()} className="underline">Retry</button></span>}
    {!semesters.isLoading && !semesters.isError && semesters.data?.length === 0 && <span className="font-normal text-slate-600">Create a semester before adding a cohort. <Link href="/admin/semesters" className="text-blue-700 underline">Manage semesters</Link></span>}
  </label>
}

export function CourseAcademicFields({ credits, semesterId, onCreditsChange, onSemesterChange, requiredSemester = false }: {
  credits: number; semesterId: string; onCreditsChange: (value: number) => void
  onSemesterChange: (value: string) => void; requiredSemester?: boolean
}) {
  return <div className="grid gap-4 sm:grid-cols-2">
    <label className="grid gap-1.5 text-sm font-medium text-[#1c3873]">Course credits
      <input type="number" min={1} step={1} required value={Number.isNaN(credits) ? '' : credits}
        onChange={event => onCreditsChange(event.target.valueAsNumber)}
        className="h-11 min-w-0 rounded-md border border-slate-300 bg-white px-3 text-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2" />
      <span className="text-xs font-normal text-slate-600">Default: 3. Previously awarded credits remain unchanged.</span>
    </label>
    <SemesterPicker value={semesterId} onChange={onSemesterChange} required={requiredSemester} />
  </div>
}

export function CreateCourseAcademicFields({ control }: { control: Control<CourseFormValues> }) {
  const credits = useController({ control, name: 'credits' })
  const semester = useController({ control, name: 'semesterId' })
  const mode = useWatch({ control, name: 'studyMode' })
  return <CourseAcademicFields credits={credits.field.value ?? 3} semesterId={semester.field.value ?? ''}
    onCreditsChange={credits.field.onChange} onSemesterChange={semester.field.onChange}
    requiredSemester={mode === 'COHORT_BASED'} />
}
