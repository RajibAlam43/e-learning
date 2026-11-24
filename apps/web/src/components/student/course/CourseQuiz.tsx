'use client'

import { useTranslations } from 'next-intl'

import { useRef, useState } from 'react'
import { useQuiz, useQuizAttemptResult, useQuizAttempts, useStartQuizAttempt, useSubmitQuizAttempt } from '@/hooks/useLearning'
import { useAuthStore } from '@/store/auth.store'
import { latestAttempts, quizDeadline, quizDraftKey, type QuizAnswers } from '@/lib/quiz-session'
import type { QuizAttemptResult } from '@/types'
import type { components } from '@/types/api'
import QuizAttemptSession from './QuizAttemptSession'
import QuizResultSection from './QuizResultSection'

type Timing = components['schemas']['QuizAttemptStartResponse']
interface Props { courseId: string; quizId: string; courseName: string; moduleName: string }

function message(error: unknown, fallback: string) {
  return error && typeof error === 'object' && 'message' in error && typeof error.message === 'string'
    ? error.message : fallback
}

export default function CourseQuiz({ courseId, quizId, courseName, moduleName }: Props) {
  const ui = useTranslations('privateUi')

  const userId = useAuthStore(s => s.user?.id)
  const quiz = useQuiz(quizId, courseId)
  const history = useQuizAttempts(courseId, quizId)
  const { active, submitted } = latestAttempts(history.data ?? [])
  const previous = useQuizAttemptResult(submitted?.attemptId)
  const start = useStartQuizAttempt(courseId)
  const submit = useSubmitQuizAttempt(courseId)
  const [started, setStarted] = useState<Timing | null>(null)
  const [result, setResult] = useState<QuizAttemptResult | null>(null)
  const [retaking, setRetaking] = useState(false)
  const [error, setError] = useState('')
  const requestInFlight = useRef(false)
  const timing = started ?? active
  const attemptId = timing?.attemptId
  const deadline = timing ? quizDeadline(timing, quiz.data?.timeLimitSec) : null
  const storageKey = userId && attemptId ? quizDraftKey(userId, courseId, quizId, attemptId) : null
  const displayedResult = result ?? (!attemptId && !retaking ? previous.data : null)
  const exhausted = quiz.data?.canRetry === false || quiz.data?.remainingAttempts === 0

  async function startAttempt() {
    if (requestInFlight.current || attemptId || !userId || exhausted) return
    requestInFlight.current = true
    setError('')
    try {
      // Confirm history again before consuming an attempt, including after a lost response.
      const fresh = await history.refetch()
      if (fresh.error) throw fresh.error
      if (latestAttempts(fresh.data ?? []).active) return
      const response = await start.mutateAsync(quizId)
      if (!response.attemptId) throw new Error(ui('attemptIdMissing'))
      setStarted(response)
      setResult(null)
      setRetaking(false)
      try {
        sessionStorage.setItem(quizDraftKey(userId, courseId, quizId, response.attemptId), JSON.stringify({
          answers: {}, deadline: quizDeadline(response, quiz.data?.timeLimitSec),
        }))
      } catch { /* The session component reports draft storage failures. */ }
    } catch (cause) {
      setError(message(cause, ui('requestFailed')))
      void history.refetch()
    } finally { requestInFlight.current = false }
  }

  async function submitAnswers(answers: QuizAnswers) {
    if (!attemptId || requestInFlight.current || !Object.keys(answers).length) return
    requestInFlight.current = true
    setError('')
    try {
      const response = await submit.mutateAsync({ attemptId, quizId,
        data: { answers: Object.entries(answers).map(([questionId, choiceId]) => ({ questionId, choiceId })) },
      })
      setResult(response)
      setStarted(null)
      if (storageKey) { try { sessionStorage.removeItem(storageKey) } catch { /* Optional browser storage. */ } }
    } catch (cause) {
      const status = cause && typeof cause === 'object' && 'status' in cause ? cause.status : undefined
      setError(status === 408 ? ui('theServerReportsThatThisAttemptHasExpiredRefreshToCheckYourResul') : message(cause, ui('requestFailed')))
    } finally { requestInFlight.current = false }
  }

  function refresh() {
    setStarted(null)
    void quiz.refetch()
    void history.refetch()
    if (submitted?.attemptId) void previous.refetch()
  }

  const loading = quiz.isLoading || history.isLoading || !history.isFetchedAfterMount || (!attemptId && previous.isLoading)
  const loadError = quiz.error || history.error || (!attemptId && previous.error)
  const questions = (quiz.data?.questions ?? []).filter(q => q.questionId).map(q => ({
    id: q.questionId!, question: q.questionText ?? '', correctOptionId: '',
    options: (q.choices ?? []).filter(c => c.choiceId).map(c => ({ id: c.choiceId!, text: c.choiceText ?? '' })),
  }))

  if (loading) return <p role='status' className='p-6'>{ui('loadingQuizAndPreviousAttempts')}</p>
  if (loadError) return <div role='alert' className='p-6 text-red-700'>{message(loadError, ui('requestFailed'))}
    <button type='button' onClick={refresh} className='ml-3 underline'>{ui('tryAgain')}</button>
  </div>
  if (displayedResult) return <QuizResultSection serverResult={displayedResult}
    courseName={courseName} moduleName={moduleName} canRetry={!exhausted && displayedResult.canRetry !== false}
    onRetake={() => { setResult(null); setStarted(null); setRetaking(true); setError('') }} />

  return <div className='flex flex-col gap-4'>
    {error && <div role='alert' className='rounded-xl bg-red-50 p-4 text-sm text-red-700'>{error}
      <button type='button' onClick={refresh} disabled={start.isPending || submit.isPending} className='ml-3 underline'>{ui('refreshQuiz')}</button>
    </div>}
    {attemptId && storageKey ? (
      quiz.data?.timeLimitSec && deadline === null ? <p role='alert'>{ui('theAttemptTimingIsUnavailableRefreshTheQuizBeforeContinuing')}</p> :
      <QuizAttemptSession key={storageKey} storageKey={storageKey} deadline={deadline}
        questions={questions} quizTitle={quiz.data?.quizTitle} courseName={courseName}
        moduleName={moduleName} onComplete={submitAnswers} isSubmitting={submit.isPending} />
    ) : <section className='rounded-2xl border border-blue-100 bg-white p-6'>
      <h1 className='text-xl font-medium text-[#1c3873]'>{quiz.data?.quizTitle ?? ui('quiz')}</h1>
      {quiz.data?.instructions && <p className='mt-3 whitespace-pre-wrap'>{quiz.data.instructions}</p>}
      <p className='mt-3'>{ui('questionCount', { count: questions.length })}{quiz.data?.timeLimitSec ? ` · ${ui('minuteCount', { count: Math.ceil(quiz.data.timeLimitSec / 60) })}` : ''}</p>
      <p className='mt-2 text-sm text-slate-600'>{exhausted ? ui('youHaveNoAttemptsRemaining') : ui('theAttemptBeginsWhenYouPressStartQuiz')}</p>
      <button type='button' onClick={() => void startAttempt()}
        disabled={exhausted || !userId || !questions.length || start.isPending || history.isFetching}
        className='mt-4 rounded-lg bg-[#1c3873] px-5 py-2 text-white disabled:opacity-50'>
        {start.isPending ? ui('starting') : ui('startQuiz')}
      </button>
    </section>}
  </div>
}
