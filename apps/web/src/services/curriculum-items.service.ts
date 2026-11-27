import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { AdminLiveClassItemResponse, AdminQuizDetail, CreateLiveClassItemRequest, UpdateQuizRequest } from '@/types'

export function curriculumError(cause: unknown): string {
  return cause && typeof cause === 'object' && 'message' in cause && typeof cause.message === 'string'
    ? cause.message : 'The request could not be completed. Try again.'
}
export function useLiveClassItem(courseId: string, itemId: string, enabled: boolean) {
  return useQuery({ queryKey: ['admin', 'live-class-item', courseId, itemId], enabled,
    queryFn: async () => (await apiClient.get<AdminLiveClassItemResponse>(`/admin/courses/${courseId}/live-classes/${itemId}`)).data })
}
export function useLiveClassItemMutation(courseId: string, itemId: string) {
  const cache = useQueryClient()
  return useMutation({ mutationFn: async (data?: CreateLiveClassItemRequest) => data
    ? apiClient.put(`/admin/courses/${courseId}/live-classes/${itemId}`, data)
    : apiClient.delete(`/admin/courses/${courseId}/live-classes/${itemId}`),
    onSuccess: () => cache.invalidateQueries({ queryKey: ['admin'] }) })
}
export function useAdminQuizQuestions(id: string, enabled: boolean) {
  return useQuery({ queryKey: ['admin', 'quiz-definition', id], enabled,
    queryFn: async () => (await apiClient.get<AdminQuizDetail>(`/admin/quizzes/${id}`)).data })
}
export function useSaveQuizDefinition(id: string) {
  const cache = useQueryClient()
  return useMutation({ mutationFn: (data: UpdateQuizRequest) => apiClient.patch(`/admin/quizzes/${id}`, data),
    onSuccess: () => cache.invalidateQueries({ queryKey: ['admin'] }) })
}
