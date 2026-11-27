'use client'
import { useState } from 'react'
import { X } from 'lucide-react'
import { toast } from 'sonner'
import { QuizBuilder } from '@/app/admin/courses/[id]/QuizBuilder'
import { curriculumError, useAdminQuizQuestions, useSaveQuizDefinition } from '@/services/curriculum-items.service'
import type { AdminQuizDetail, CreateQuizRequest, AdminCourseSection } from '@/types'

export function QuizQuestionsButton({ quizId, section }: { quizId: string; section: AdminCourseSection }) {
  const [open, setOpen] = useState(false)
  const q = useAdminQuizQuestions(quizId, open)
  return <><button type="button" onClick={() => setOpen(true)} className="rounded-md px-2 py-1 text-xs font-medium text-slate-700 hover:bg-slate-100">Questions</button>
    {open && <div role="dialog" aria-modal="true" aria-labelledby="edit-quiz-questions-title" className="fixed inset-0 z-50 overflow-y-auto bg-black/45 p-4"><div className="mx-auto my-8 w-full max-w-4xl rounded-2xl bg-white p-5 sm:p-6"><div className="mb-4 flex items-center justify-between gap-3"><h2 id="edit-quiz-questions-title" className="font-heading text-xl text-[#1c3873]">Edit quiz questions</h2><button type="button" aria-label="Close" onClick={() => setOpen(false)} className="rounded-lg p-2 hover:bg-slate-100"><X className="h-5 w-5" /></button></div>
      <p className="mb-4 max-w-prose text-sm text-slate-600">Questions and choices can be replaced before students attempt this quiz. Recorded attempts are protected; use Edit for settings only after attempts exist.</p>
      {q.isLoading ? <p role="status">Loading quiz…</p> : q.isError || !q.data ? <p role="alert">Unable to load quiz questions. <button className="underline" onClick={() => q.refetch()}>Retry</button></p> : <QuizDefinitionForm key={quizId} quiz={q.data} section={section} close={() => setOpen(false)} />}
    </div></div>}
  </>
}

function QuizDefinitionForm({ quiz, section, close }: { quiz: AdminQuizDetail; section: AdminCourseSection; close: () => void }) {
  const save = useSaveQuizDefinition(quiz.quizId ?? '')
  const [error, setError] = useState('')
  const [form, setForm] = useState<CreateQuizRequest>({ title: quiz.title ?? '', titleEn: quiz.titleEn ?? '', position: quiz.position ?? 1, passingScorePct: quiz.passingScorePct ?? 70, maxAttempts: quiz.maxAttempts ?? 1, timeLimitSec: quiz.timeLimitSec,
    questions: (quiz.questions ?? []).map((q, index) => ({ position: index + 1, questionText: q.questionText ?? '', questionTextEn: q.questionTextEn ?? '', questionType: q.questionType ?? 'MCQ', points: q.points ?? 1, explanationText: q.explanationText ?? '', explanationTextEn: q.explanationTextEn ?? '', choices: (q.choices ?? []).map(c => ({ choiceText: c.choiceText ?? '', choiceTextEn: c.choiceTextEn ?? '', isCorrect: c.isCorrect ?? false })) })) })
  const available = !section.items?.some(i => i.itemId !== quiz.quizId && i.position === form.position)
  async function submit(event: React.FormEvent) {
    event.preventDefault(); setError('')
    if (!form.title.trim() || !form.questions.length || form.questions.some(q => !q.questionText.trim() || q.points < 1 || q.choices.length < 2 || q.choices.some(c => !c.choiceText.trim()) || q.choices.filter(c => c.isCorrect).length !== 1)) {
      setError('Complete each question, at least two choices, and exactly one correct answer.'); return
    }
    try { await save.mutateAsync({ ...form, sectionId: quiz.sectionId }); toast.success('Quiz questions updated'); close() }
    catch (cause) { setError(curriculumError(cause)) }
  }
  return <>{error && <p role="alert" className="mb-4 text-sm text-red-700">{error}</p>}<QuizBuilder value={form} onChange={setForm} saving={save.isPending} onSubmit={submit} positionAvailable={available} submitLabel="Save quiz questions" /></>
}
