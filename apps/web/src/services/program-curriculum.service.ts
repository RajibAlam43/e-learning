import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'
import type { AdminCollectionDetail } from '@/types'
export type ProgramSnapshot = components['schemas']['ProgramSnapshot']
export type SaveCurriculum = components['schemas']['SaveProgramCurriculumRequest']
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
