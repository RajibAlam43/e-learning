'use client'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import { usePrerequisites, useSavePrerequisites } from '@/services/prerequisite.service'
import type { AdminCourseSummary } from '@/types'
import { toast } from 'sonner'
export function CoursePrerequisiteEditor({ courseId, templateId }: { courseId: string; templateId: string }) {
 const query = usePrerequisites(courseId)
 const options = useQuery({ queryKey: ['admin', 'prerequisite-options'], queryFn: async () => (await apiClient.get<AdminCourseSummary[]>('/admin/courses')).data })
 const save = useSavePrerequisites(courseId)
 const [selection, setSelection] = useState<string[]>()
 const [error, setError] = useState('')
 const selected = selection ?? query.data?.requiredCourses?.flatMap(c => c.courseTemplateId ? [c.courseTemplateId] : []) ?? []
 const courses = [...new Map(options.data?.filter(c => c.courseTemplateId && c.courseTemplateId !== templateId).map(c => [c.courseTemplateId, c])).values()]
 async function submit(event: React.FormEvent) {
  event.preventDefault(); setError('')
  try { await save.mutateAsync(selected); setSelection(undefined); toast.success('Prerequisites saved.') }
  catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to save prerequisites.') }
 }
 return <form onSubmit={submit} className="rounded-xl bg-white p-6">
  <h2 className="font-heading text-xl text-[#1c3873]">Required courses</h2>
  <p className="mt-2 max-w-prose text-sm text-slate-600">Students must complete these courses or receive a credit waiver before enrolling. These rules apply to every cohort sharing this curriculum.</p>
  {query.isLoading || options.isLoading ? <p role="status" className="mt-4">Loading prerequisites…</p> : query.isError || options.isError ? <p role="alert" className="mt-4 text-red-700">Unable to load courses. <button type="button" onClick={() => { void query.refetch(); void options.refetch() }} className="underline">Retry</button></p> : <>
   <fieldset className="mt-4 max-h-64 overflow-y-auto"><legend className="sr-only">Prerequisite courses</legend>
    {courses.length === 0 && <p className="text-sm text-slate-600">No other courses are available.</p>}
    {courses.map(c => <label key={c.courseTemplateId} className="flex min-h-11 items-center gap-3 py-2 text-sm"><input type="checkbox" checked={selected.includes(c.courseTemplateId!)} onChange={e => setSelection(e.target.checked ? [...selected, c.courseTemplateId!] : selected.filter(id => id !== c.courseTemplateId))} className="h-4 w-4 accent-[#1c3873]" /><span>{c.title} · {c.credits} credits</span></label>)}
    {query.data?.requiredCourses?.filter(c => !courses.some(o => o.courseTemplateId === c.courseTemplateId)).map(c => <label key={c.courseTemplateId} className="flex min-h-11 items-center gap-3 py-2 text-sm"><input type="checkbox" checked={selected.includes(c.courseTemplateId!)} onChange={e => setSelection(e.target.checked ? [...selected, c.courseTemplateId!] : selected.filter(id => id !== c.courseTemplateId))} /><span>{c.title} · {c.credits} credits</span></label>)}
   </fieldset>
   {error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}
   <button disabled={save.isPending} className="mt-4 rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{save.isPending ? 'Saving…' : 'Save prerequisites'}</button>
  </>}
 </form>
}
