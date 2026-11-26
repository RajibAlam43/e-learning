import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'
import type { AdminCollectionDetail } from '@/types'
export type ProgramSnapshot = components['schemas']['ProgramSnapshot']
export type SaveCurriculum = components['schemas']['SaveProgramCurriculumRequest']
export type ProgramRegistration = components['schemas']['ProgramRegistrationResponse']
export type ProgramOffering = components['schemas']['ProgramOfferingResponse']
export type StudentCurriculum = components['schemas']['StudentProgramCurriculumResponse']
export function useSaveProgramCurriculum(id: string) {
 const cache = useQueryClient()
 return useMutation({ mutationFn: async (body: SaveCurriculum) => (await apiClient.put<AdminCollectionDetail>(`/admin/collections/${id}/curriculum`, body)).data,
 onSuccess: () => { void cache.invalidateQueries({ queryKey: ['admin'] }) } })
}
export function useStudentProgramCurriculum(id: string) {
 return useQuery({ queryKey: ['student', 'program-curriculum', id], enabled: !!id, queryFn: async () => (await apiClient.get<StudentCurriculum>(`/student/collections/${id}/curriculum`)).data })
}

export function programErrorMessage(cause: unknown, fallback: string): string {
 if (cause && typeof cause === 'object' && 'message' in cause && typeof cause.message === 'string') return cause.message
 return fallback
}

export function useRegisterProgramCourse(id: string) {
 const cache = useQueryClient()
 return useMutation({ mutationFn: async (courseId: string) => (await apiClient.post<ProgramRegistration>(`/student/collections/${id}/registrations`, { courseId })).data,
 onSuccess: () => cache.invalidateQueries({ queryKey: ['student'] }),
 onError: () => cache.invalidateQueries({ queryKey: ['student', 'program-curriculum', id] }) })
}
export function useAdminProgramRegistrations(userId: string, page = 0) {
 return useQuery({ queryKey: ['admin', 'program-registrations', userId, page], enabled: !!userId,
 queryFn: async () => (await apiClient.get<ProgramRegistration[]>(`/admin/students/${userId}/program-registrations?page=${page}`)).data })
}
export function useWithdrawProgramRegistration(userId: string) {
 const cache = useQueryClient()
 return useMutation({ mutationFn: async ({ id, reason }: { id: string; reason: string }) => (await apiClient.post<ProgramRegistration>(`/admin/students/${userId}/program-registrations/${id}/withdraw`, { reason })).data,
 onSuccess: () => cache.invalidateQueries({ queryKey: ['admin', 'program-registrations', userId] }) })
}
