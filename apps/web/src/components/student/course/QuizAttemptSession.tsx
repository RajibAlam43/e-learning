'use client'

import { useTranslations } from 'next-intl'

import { useEffect, useRef, useState } from 'react'
import QuizSection, { type QuizQuestion } from './QuizSection'
import { readQuizDraft, type QuizAnswers } from '@/lib/quiz-session'

interface Props {
  storageKey: string
  deadline: number | null
  questions: QuizQuestion[]
  quizTitle?: string
  courseName: string
  moduleName: string
  isSubmitting: boolean
  onComplete: (answers: QuizAnswers) => Promise<void>
}

export default function QuizAttemptSession(props: Props) {
  const ui = useTranslations('privateUi')

  const [draft] = useState(() => {
    try { return readQuizDraft(sessionStorage.getItem(props.storageKey)) }
    catch { return readQuizDraft(null) }
  })
  const [answers, setAnswers] = useState<QuizAnswers>(() => Object.fromEntries(
    props.questions.flatMap(q => q.options.some(o => o.id === draft.answers[q.id]) ? [[q.id, draft.answers[q.id]!]] : []),
  ))
  // A saved server deadline survives reloads; it must never be extended by a fresh render.
  const deadline = draft.deadline !== null && props.deadline !== null
    ? Math.min(draft.deadline, props.deadline) : draft.deadline ?? props.deadline
  const [now, setNow] = useState(() => Date.now())
  const [storageFailed, setStorageFailed] = useState(false)
  const autoSubmitted = useRef(false)
  const remaining = deadline === null ? null : Math.max(0, Math.ceil((deadline - now) / 1000))
  const expired = remaining === 0

  useEffect(() => {
    if (deadline === null) return
    const tick = () => setNow(Date.now())
    const timer = setInterval(tick, 500)
    window.addEventListener('focus', tick)
    return () => { clearInterval(timer); window.removeEventListener('focus', tick) }
  }, [deadline])

  useEffect(() => {
    try { sessionStorage.setItem(props.storageKey, JSON.stringify({ answers, deadline })) }
    catch { /* Selection changes surface storage failures without interrupting the quiz. */ }
  }, [props.storageKey, answers, deadline])

  // Persist on each selection, synchronously, so an immediate reload keeps the answer.
  function updateAnswers(next: QuizAnswers) {
    setAnswers(next)
    try { sessionStorage.setItem(props.storageKey, JSON.stringify({ answers: next, deadline })) }
    catch { setStorageFailed(true) }
  }

  useEffect(() => {
    if (!expired || autoSubmitted.current || props.isSubmitting) return
    autoSubmitted.current = true
    if (Object.keys(answers).length) void props.onComplete(answers)
  }, [expired, answers, props])

  return <div className='flex flex-col gap-4'>
    {remaining !== null && <p role='timer' className='font-medium text-[#1c3873]'>
      {expired ? ui('timeIsUpAnswersAreLocked') : ui('timeRemaining', { time: `${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')}` })}
    </p>}
    {storageFailed && <p role='alert' className='text-sm text-amber-800'>{ui('thisBrowserCouldNotSaveYourDraftKeepThisPageOpenUntilYouSubmit')}</p>}
    {expired && !Object.keys(answers).length && <p role='status'>{ui('noAnswersWereSelectedBeforeTheDeadlineRefreshTheQuizToCheckYourR')}</p>}
    <QuizSection questions={props.questions} quizTitle={props.quizTitle}
      courseName={props.courseName} moduleName={props.moduleName} categoryName={props.courseName}
      answers={answers} onAnswersChange={updateAnswers} locked={expired}
      onComplete={props.onComplete} isSubmitting={props.isSubmitting} />
    {expired && Object.keys(answers).length > 0 && <button type='button' disabled={props.isSubmitting}
      className='self-start text-sm text-blue-700 underline' onClick={() => void props.onComplete(answers)}>
      {ui('retrySubmittingSavedAnswers')}</button>}
  </div>
}
