import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { learningService } from '@/services/learning.service'
import { QUERY_KEYS } from '@/constants/queryKeys'
import { useAuthStore } from '@/store/auth.store'
import type { components } from '@/types/api'
import type { SubmitQuizAttemptRequest } from '@/types'

// ─── Quiz taking ───

export function useQuiz(quizId: string | null | undefined, courseId?: string | null) {
  const ready = useAuthStore(s => s.isSessionReady && s.isAuthenticated)
  return useQuery({
    queryKey: courseId ? QUERY_KEYS.courseQuiz(courseId, quizId ?? '') : QUERY_KEYS.quiz(quizId ?? ''),
    queryFn: () => courseId ? learningService.getCourseQuiz(courseId, quizId as string) : learningService.getQuiz(quizId as string),
    enabled: !!quizId && ready,
    staleTime: 5 * 60 * 1000, // quiz content rarely changes mid-attempt
  })
}

export function useQuizAttempts(courseId: string | null | undefined, quizId: string | null | undefined) {
  const ready = useAuthStore(s => s.isSessionReady && s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.quizAttempts(courseId ?? '', quizId ?? ''),
    queryFn: () => learningService.getCourseQuizAttempts(courseId as string, quizId as string),
    enabled: !!courseId && !!quizId && ready,
    staleTime: 0,
    refetchOnMount: 'always',
  })
}

export function useQuizAttemptResult(attemptId: string | null | undefined) {
  const ready = useAuthStore(s => s.isSessionReady && s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.quizAttempt(attemptId ?? ''),
    queryFn: () => learningService.getAttempt(attemptId as string),
    enabled: !!attemptId && ready,
    staleTime: 5 * 60 * 1000,
  })
}

export function useStartQuizAttempt(courseId?: string | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (quizId: string) => courseId ? learningService.startCourseAttempt(courseId, quizId) : learningService.startAttempt(quizId),
    onSuccess: (_, quizId) => {
      qc.invalidateQueries({
        queryKey: courseId ? QUERY_KEYS.courseQuiz(courseId, quizId) : QUERY_KEYS.quiz(quizId),
      })
      if (courseId) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.quizAttempts(courseId, quizId) })
      }
    },
  })
}

export function useSubmitQuizAttempt(courseId?: string | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ attemptId, data }: { attemptId: string; data: SubmitQuizAttemptRequest; quizId?: string | null }) =>
      learningService.submitAttempt(attemptId, data),
    onSuccess: (result, variables) => {
      qc.setQueryData(QUERY_KEYS.quizAttempt(variables.attemptId), result)
      if (courseId) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.courseHome(courseId) })
        qc.invalidateQueries({ queryKey: QUERY_KEYS.courseProgress(courseId) })
      }
      const quizId = variables.quizId ?? result.quizId
      if (courseId && quizId) {
        qc.setQueryData<components['schemas']['QuizAttemptSummaryResponse'][]>(
          QUERY_KEYS.quizAttempts(courseId, quizId),
          old => old?.map(attempt => attempt.attemptId === variables.attemptId
            ? { ...attempt, status: 'SUBMITTED', submittedAt: result.submittedAt }
            : attempt),
        )
        qc.invalidateQueries({ queryKey: QUERY_KEYS.courseQuiz(courseId, quizId) })
        qc.invalidateQueries({ queryKey: QUERY_KEYS.quizAttempts(courseId, quizId) })
      }
    },
  })
}

// ─── Lesson progress ───

export function useSaveLessonProgress(courseId?: string | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ lessonId, lastPositionSec }: { lessonId: string; lastPositionSec: number }) =>
      courseId
        ? learningService.saveCourseLessonProgress(courseId, lessonId, lastPositionSec).then(() => undefined)
        : learningService.saveLessonProgress(lessonId, lastPositionSec),
    onSuccess: (_, { lessonId, lastPositionSec }) => {
      const key = courseId ? QUERY_KEYS.courseLessonContent(courseId, lessonId) : QUERY_KEYS.lessonContent(lessonId)
      qc.setQueryData<components['schemas']['LessonContentResponse']>(key, old => old ? {
        ...old, userProgress: { ...old.userProgress, lastPositionSec },
      } : old)
    },
  })
}

// ─── Course progress ───

export function useCourseProgress(courseId: string | null | undefined) {
  return useQuery({
    queryKey: QUERY_KEYS.courseProgress(courseId ?? ''),
    queryFn: () => learningService.getCourseProgress(courseId as string),
    enabled: !!courseId,
  })
}

// ─── Lesson resources ───

export function useLessonResources(lessonId: string | null | undefined) {
  return useQuery({
    queryKey: QUERY_KEYS.lessonResources(lessonId ?? ''),
    queryFn: () => learningService.listLessonResources(lessonId as string),
    enabled: !!lessonId,
  })
}

export function useDownloadLessonResource() {
  return useMutation({
    mutationFn: (resourceId: string) => learningService.getResourceDownloadUrl(resourceId),
    onSuccess: (data) => {
      // Open the presigned URL in a new tab to trigger download
      if (data.downloadUrl && typeof window !== 'undefined') {
        window.open(data.downloadUrl, '_blank', 'noopener,noreferrer')
      }
    },
  })
}

// ─── Live class join ───

export function useJoinLiveClass() {
  return useMutation({
    mutationFn: (liveClassId: string) => learningService.joinLiveClass(liveClassId),
  })
}
