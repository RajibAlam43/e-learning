import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'

type QuizQuestionsResponse = components['schemas']['QuizQuestionsResponse']
type QuizAttemptStartResponse = components['schemas']['QuizAttemptStartResponse']
type QuizAttemptResultResponse = components['schemas']['QuizAttemptResultResponse']
type QuizAttemptSummaryResponse = components['schemas']['QuizAttemptSummaryResponse']
type SubmitQuizAttemptRequest = components['schemas']['SubmitQuizAttemptRequest']
type LessonProgressResponse = components['schemas']['LessonProgressResponse']
type CourseProgressResponse = components['schemas']['CourseProgressResponse']
type ResourceDownloadUrlResponse = components['schemas']['ResourceDownloadUrlResponse']
type LessonResourceResponse = components['schemas']['LessonResourceResponse']
type CourseLessonResources = components['schemas']['LessonResourceResponse'][]
type StudentLiveClassJoinResponse = components['schemas']['StudentLiveClassJoinResponse']

/**
 * Learning service — handles quiz taking, lesson progress, and resources.
 * These endpoints are all under /learn/* and /live-classes/* (student-auth).
 */
export const learningService = {
  // ─── Quiz taking flow ───
  // quizId comes from the quiz entry in the course section curriculum.
  getQuiz: async (quizId: string): Promise<QuizQuestionsResponse> => {
    const res = await apiClient.get<QuizQuestionsResponse>(`/learn/quizzes/${quizId}`)
    return res.data
  },

  getCourseQuiz: async (courseId: string, quizId: string): Promise<QuizQuestionsResponse> => {
    const res = await apiClient.get<QuizQuestionsResponse>(`/learn/courses/${courseId}/quizzes/${quizId}`)
    return res.data
  },

  startAttempt: async (quizId: string): Promise<QuizAttemptStartResponse> => {
    const res = await apiClient.post<QuizAttemptStartResponse>(`/learn/quizzes/${quizId}/attempts`)
    return res.data
  },

  startCourseAttempt: async (courseId: string, quizId: string): Promise<QuizAttemptStartResponse> => {
    const res = await apiClient.post<QuizAttemptStartResponse>(`/learn/courses/${courseId}/quizzes/${quizId}/attempts`)
    return res.data
  },

  getCourseQuizAttempts: async (courseId: string, quizId: string): Promise<QuizAttemptSummaryResponse[]> =>
    (await apiClient.get<QuizAttemptSummaryResponse[]>(`/learn/courses/${courseId}/quizzes/${quizId}/attempts`)).data,

  getAttempt: async (attemptId: string): Promise<QuizAttemptResultResponse> => {
    const res = await apiClient.get<QuizAttemptResultResponse>(`/learn/quiz-attempts/${attemptId}`)
    return res.data
  },

  submitAttempt: async (
    attemptId: string,
    data: SubmitQuizAttemptRequest,
  ): Promise<QuizAttemptResultResponse> => {
    const res = await apiClient.post<QuizAttemptResultResponse>(
      `/learn/quiz-attempts/${attemptId}/submit`,
      data,
    )
    return res.data
  },

  // ─── Lesson progress ───
  saveLessonProgress: async (
    lessonId: string,
    lastPositionSec: number,
  ): Promise<LessonProgressResponse> => {
    const res = await apiClient.post<LessonProgressResponse>(
      `/learn/lessons/${lessonId}/progress`,
      { lastPositionSec },
    )
    return res.data
  },

  saveCourseLessonProgress: async (courseId: string, lessonId: string, lastPositionSec: number): Promise<void> => {
    await apiClient.post(`/learn/courses/${courseId}/lessons/${lessonId}/progress`, { lastPositionSec })
  },

  // ─── Course progress ───
  getCourseProgress: async (courseId: string): Promise<CourseProgressResponse> => {
    const res = await apiClient.get<CourseProgressResponse>(`/learn/courses/${courseId}/progress`)
    return res.data
  },

  // ─── Lesson resources ───
  // NOTE: resources[] is already embedded on LessonContentResponse; this endpoint
  // is only for fetching resources independently of lesson content.
  listLessonResources: async (lessonId: string): Promise<LessonResourceResponse[]> => {
    const res = await apiClient.get<LessonResourceResponse[]>(`/learn/lessons/${lessonId}/resources`)
    return res.data
  },

  listCourseLessonResources: async (courseId: string, lessonId: string): Promise<CourseLessonResources> =>
    (await apiClient.get<CourseLessonResources>(`/learn/courses/${courseId}/lessons/${lessonId}/resources`)).data,

  getResourceDownloadUrl: async (resourceId: string): Promise<ResourceDownloadUrlResponse> => {
    const res = await apiClient.get<ResourceDownloadUrlResponse>(
      `/learn/resources/${resourceId}/download-url`,
    )
    return res.data
  },

  getCourseResourceDownloadUrl: async (courseId: string, resourceId: string): Promise<ResourceDownloadUrlResponse> =>
    (await apiClient.get<ResourceDownloadUrlResponse>(`/learn/courses/${courseId}/resources/${resourceId}/download-url`)).data,

  // ─── Live class join (student) ───
  joinLiveClass: async (liveClassId: string): Promise<StudentLiveClassJoinResponse> => {
    const res = await apiClient.post<StudentLiveClassJoinResponse>(`/live-classes/${liveClassId}/join`)
    return res.data
  },
}
