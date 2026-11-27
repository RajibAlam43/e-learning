import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'

export type ManualEnrollmentRequest = components['schemas']['ManualEnrollmentRequest']
export type ManualEnrollment = components['schemas']['ManualEnrollmentResponse']
export type ManualEnrollmentOverview = components['schemas']['ManualEnrollmentOverviewResponse']

export function useManualEnrollmentOverview(userId: string, page: number) {
  return useQuery({ queryKey: ['admin', 'manual-enrollments', userId, page], enabled: !!userId,
    queryFn: async () => (await apiClient.get<ManualEnrollmentOverview>(`/admin/students/${userId}/manual-enrollments?page=${page}`)).data })
}

export function useManualEnrollment(userId: string) {
  const cache = useQueryClient()
  return useMutation({ mutationFn: async (body: ManualEnrollmentRequest) =>
    (await apiClient.post<ManualEnrollment>(`/admin/students/${userId}/manual-enrollments`, body)).data,
    onSuccess: () => cache.invalidateQueries({ queryKey: ['admin'] }) })
}
