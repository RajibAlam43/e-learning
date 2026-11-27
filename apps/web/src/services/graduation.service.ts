import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'

export type AcademicProgress = components['schemas']['AcademicProgressResponse']
export type ProgramGraduation = components['schemas']['ProgramGraduationResponse']
export function useAcademicProgress(userId?: string) {
  return useQuery({ queryKey: [userId ? 'admin' : 'student', 'academic-progress', userId ?? 'me'],
    queryFn: async () => (await apiClient.get<AcademicProgress>(userId
      ? `/admin/students/${userId}/academic-progress` : '/student/academic-progress')).data })
}
export function useProgramGraduation(id: string) {
  return useQuery({ queryKey: ['student', 'graduation', id], enabled: !!id,
    queryFn: async () => (await apiClient.get<ProgramGraduation>(`/student/collections/${id}/graduation`)).data })
}
export function useCheckGraduation(id: string, userId?: string) {
  const cache = useQueryClient()
  return useMutation({ mutationFn: async () => (await apiClient.post<ProgramGraduation>(userId
    ? `/admin/students/${userId}/programs/${id}/graduation/check`
    : `/student/collections/${id}/graduation/check`)).data,
    onSuccess: async () => { await Promise.all([
      cache.invalidateQueries({ queryKey: ['student'] }),
      cache.invalidateQueries({ queryKey: ['admin', 'academic-progress', userId] }),
    ]) } })
}
