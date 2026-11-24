import type { components } from '@/types/api'

type Attempt = components['schemas']['QuizAttemptSummaryResponse']
type Timing = components['schemas']['QuizAttemptStartResponse']
export type QuizAnswers = Record<string, string>

const TERMINAL = new Set(['SUBMITTED', 'COMPLETED', 'PASSED', 'FAILED', 'EXPIRED', 'TIMED_OUT', 'CANCELLED', 'ABANDONED'])

export function latestAttempts(attempts: Attempt[]) {
  const sorted = [...attempts].sort((a, b) =>
    (b.attemptNumber ?? 0) - (a.attemptNumber ?? 0) ||
    Date.parse(b.startedAt ?? '1970-01-01') - Date.parse(a.startedAt ?? '1970-01-01'))
  return {
    active: sorted.find(a => a.attemptId && a.startedAt && !a.submittedAt && !TERMINAL.has((a.status ?? '').toUpperCase())),
    submitted: sorted.find(a => a.attemptId && (a.submittedAt || ['SUBMITTED', 'COMPLETED', 'PASSED', 'FAILED'].includes((a.status ?? '').toUpperCase()))),
  }
}

export function quizDeadline(timing: Timing, fallbackLimit?: number, now = Date.now()): number | null {
  const deadline = Date.parse(timing.deadline ?? '')
  if (Number.isFinite(deadline)) return deadline
  const limit = timing.timeLimitSec ?? fallbackLimit
  const started = Date.parse(timing.startedAt ?? '')
  if (limit && limit > 0 && Number.isFinite(started)) return started + limit * 1000
  if (typeof timing.timeRemainingSeconds === 'number' && timing.timeRemainingSeconds >= 0) {
    return now + timing.timeRemainingSeconds * 1000
  }
  return null
}

export function quizDraftKey(userId: string, courseId: string, quizId: string, attemptId: string): string {
  return `gii-quiz:${JSON.stringify([userId, courseId, quizId, attemptId])}`
}

export function readQuizDraft(raw: string | null): { answers: QuizAnswers; deadline: number | null } {
  try {
    const value: unknown = JSON.parse(raw ?? 'null')
    if (!value || typeof value !== 'object' || !('answers' in value)) throw new Error()
    const answers = value.answers
    if (!answers || typeof answers !== 'object' || Array.isArray(answers)) throw new Error()
    return {
      answers: Object.fromEntries(Object.entries(answers).filter(([, choice]) => typeof choice === 'string')),
      deadline: 'deadline' in value && typeof value.deadline === 'number' && Number.isFinite(value.deadline) ? value.deadline : null,
    }
  } catch {
    return { answers: {}, deadline: null }
  }
}
