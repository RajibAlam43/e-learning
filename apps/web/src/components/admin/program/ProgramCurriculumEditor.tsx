'use client'
import { useState } from 'react'
import { ArrowUp, ArrowDown } from 'lucide-react'
import { toast } from 'sonner'
import { programErrorMessage, useSaveProgramCurriculum } from '@/services/program-curriculum.service'
import type { AdminCollectionDetail, AdminCourseSummary } from '@/types'
type Item = { courseId: string; mandatory: boolean }
export function ProgramCurriculumEditor({ program, courses }: { program: AdminCollectionDetail; courses: AdminCourseSummary[] }) {
 const snapshot = program.programRequirements
 const initial = program.courses?.flatMap(c => c.courseId ? [{ courseId: c.courseId, mandatory: c.isMandatory !== false }] : []) ?? []
 const [items, setItems] = useState<Item[]>(initial)
 const [required, setRequired] = useState(snapshot?.requiredCredits ?? initial.reduce((sum, item) => sum + (item.mandatory ? courses.find(c => c.courseId === item.courseId)?.credits ?? 3 : 0), 0))
 const [allowance, setAllowance] = useState(snapshot?.electiveCreditLimit ?? 0)
 const [duration, setDuration] = useState(snapshot?.expectedDurationMonths ?? 12)
 const [access, setAccess] = useState(program.accessDurationMonths == null ? '' : String(program.accessDurationMonths))
 const [chosen, setChosen] = useState('')
 const [error, setError] = useState('')
 const save = useSaveProgramCurriculum(program.collectionId!)
 const courseById = new Map(courses.map(c => [c.courseId, c]))
 // Include already-attached archived courses even if the admin listing changes.
 for (const c of program.courses ?? []) if (!courseById.has(c.courseId)) courseById.set(c.courseId, { courseId: c.courseId, title: c.courseTitle, credits: c.credits, courseTemplateId: c.courseTemplateId, status: c.status })
 const templates = new Set(items.map(i => courseById.get(i.courseId)?.courseTemplateId))
 const available = [...new Map(courses.filter(c => c.courseTemplateId && !templates.has(c.courseTemplateId)).map(c => [c.courseTemplateId, c])).values()]
 const mandatoryCredits = items.reduce((sum, i) => sum + (i.mandatory ? courseById.get(i.courseId)?.credits ?? 0 : 0), 0)
 const electiveCredits = items.reduce((sum, i) => sum + (!i.mandatory ? courseById.get(i.courseId)?.credits ?? 0 : 0), 0)
 function move(index: number, delta: number) { const next = [...items]; const current = next[index], target = next[index + delta]; if (!current || !target) return; next[index] = target; next[index + delta] = current; setItems(next) }
 async function submit(event: React.FormEvent) {
  event.preventDefault(); setError('')
  try { await save.mutateAsync({ requiredCredits: required, electiveCreditLimit: allowance, expectedDurationMonths: duration, accessDurationMonths: access ? Number(access) : undefined, items: items.map((i, index) => ({ courseId: i.courseId, position: index + 1, isMandatory: i.mandatory })) }); toast.success('Program curriculum saved. Existing purchasers keep their original graduation requirements.') }
  catch (cause) { setError(programErrorMessage(cause, 'Unable to save curriculum.')) }
 }
 return <form onSubmit={submit} className="grid gap-6 rounded-xl bg-white p-5 sm:p-6">
  <div><h2 className="font-heading text-xl text-[#1c3873]">Curriculum and graduation requirements</h2><p className="mt-2 max-w-prose text-sm text-slate-600">Add published or planned courses, then set mandatory courses and the elective credit allowance. Each curriculum counts once across its cohorts.</p>{!snapshot?.requiredCredits && <p className="mt-2 max-w-prose text-sm text-slate-600">Saving credit requirements enables program access for new purchases. Individual course registration is separate. Existing purchasers retain their original requirements.</p>}</div>
  <div className="grid gap-4 sm:grid-cols-2">
   <label className="grid gap-1.5 text-sm font-medium">Credits required to graduate<input required type="number" min={1} max={100000} value={required} onChange={e => setRequired(Number(e.target.value))} className="h-11 min-w-0 rounded-md border px-3" /></label>
   <label className="grid gap-1.5 text-sm font-medium">Elective credit allowance<input required type="number" min={0} max={100000} value={allowance} onChange={e => setAllowance(Number(e.target.value))} className="h-11 min-w-0 rounded-md border px-3" /><span className="text-sm font-normal text-slate-600">Maximum elective credits that count toward this program.</span></label>
   <label className="grid gap-1.5 text-sm font-medium">Expected duration (months)<input required type="number" min={1} max={1200} value={duration} onChange={e => setDuration(Number(e.target.value))} className="h-11 min-w-0 rounded-md border px-3" /></label>
   <label className="grid gap-1.5 text-sm font-medium">Access duration (months, optional)<input type="number" min={1} max={2400} value={access} onChange={e => setAccess(e.target.value)} placeholder={String(duration + 24)} className="h-11 min-w-0 rounded-md border px-3" /><span className="text-sm font-normal text-slate-600">Default: {duration + 24} months from payment confirmation, including 2 extra years.</span></label>
  </div>
  <section aria-label="Program courses"><h3 className="font-semibold text-slate-800">Courses</h3><p className="mt-1 text-sm text-slate-600">{mandatoryCredits} mandatory credits · {electiveCredits} elective credits offered · up to {Math.min(electiveCredits, allowance)} elective credits count</p>
   {!items.length && <p className="mt-4 text-sm text-slate-600">Add the first course below. Planned courses remain unavailable for individual study until published.</p>}
   <ol className="mt-3 divide-y divide-slate-200">{items.map((item, index) => { const course = courseById.get(item.courseId); return <li key={item.courseId} className="flex flex-wrap items-center justify-between gap-3 py-4">
    <div className="min-w-0"><p className="break-words font-medium">{index + 1}. {course?.title ?? program.courses?.find(c => c.courseId === item.courseId)?.courseTitle}</p><p className="mt-1 text-sm text-slate-600">{course?.credits ?? 3} credits · {course?.status === 'PUBLISHED' ? 'Published' : 'Planned / unpublished'}</p></div>
    <div className="flex flex-wrap items-center gap-2"><label className="sr-only" htmlFor={`course-rule-${item.courseId}`}>Requirement for {course?.title}</label><select id={`course-rule-${item.courseId}`} value={item.mandatory ? 'mandatory' : 'elective'} onChange={e => setItems(items.map((i, at) => at === index ? { ...i, mandatory: e.target.value === 'mandatory' } : i))} className="h-11 rounded-md border bg-white px-2 text-sm"><option value="mandatory">Mandatory</option><option value="elective">Elective</option></select><button type="button" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`Move ${course?.title} up`} className="rounded-md border p-2.5 disabled:opacity-40"><ArrowUp className="h-4 w-4" /></button><button type="button" disabled={index === items.length - 1} onClick={() => move(index, 1)} aria-label={`Move ${course?.title} down`} className="rounded-md border p-2.5 disabled:opacity-40"><ArrowDown className="h-4 w-4" /></button><button type="button" onClick={() => setItems(items.filter((_, at) => at !== index))} className="px-2 py-2 text-sm text-red-700">Remove<span className="sr-only"> {course?.title}</span></button></div>
   </li> })}</ol>
   <div className="mt-4 flex flex-wrap items-end gap-3"><label className="grid min-w-0 flex-1 gap-1.5 text-sm font-medium">Add a course<select value={chosen} onChange={e => setChosen(e.target.value)} className="h-11 w-full min-w-0 rounded-md border bg-white px-3"><option value="">Select a course curriculum</option>{available.map(c => <option key={c.courseId} value={c.courseId}>{c.title} · {c.credits} credits{c.status !== 'PUBLISHED' ? ' · Planned' : ''}</option>)}</select></label><button type="button" disabled={!chosen} onClick={() => { setItems([...items, { courseId: chosen, mandatory: true }]); setChosen('') }} className="h-11 rounded-lg border px-4 text-sm text-[#1c3873] disabled:opacity-40">Add course</button></div>
  </section>
  {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
  <div><p className="mb-3 max-w-prose text-sm text-slate-600">Later course additions become available to existing purchasers. Their original mandatory courses, credit requirement and elective allowance stay fixed.</p><button disabled={save.isPending || !items.length} className="rounded-lg bg-[#1c3873] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{save.isPending ? 'Saving…' : 'Save curriculum'}</button></div>
 </form>
}
