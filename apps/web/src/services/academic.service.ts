import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'
import type { AdminCourseDetail, RepeatCourseRequest } from '@/types'

export type Semester = components['schemas']['SemesterResponse']
export type SaveSemester = components['schemas']['SaveSemesterRequest']
export type AcademicRecord = components['schemas']['AcademicRecordResponse']

export function useSemesters() {
  return useQuery({ queryKey: ['admin', 'semesters'], queryFn: async () =>
    (await apiClient.get<Semester[]>('/admin/semesters')).data })
}

export function useSaveSemester() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: SaveSemester }) => id
      ? (await apiClient.put<Semester>(`/admin/semesters/${id}`, data)).data
      : (await apiClient.post<Semester>('/admin/semesters', data)).data,
    onSuccess: () => client.invalidateQueries({ queryKey: ['admin'] }),
  })
}

export function useCourseOfferings(id: string) {
  return useQuery({ queryKey: ['admin', 'course-offerings', id], enabled: Boolean(id),
    queryFn: async () => (await apiClient.get<AdminCourseDetail[]>(`/admin/courses/${id}/offerings`)).data })
}

export function useCreateOffering() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: RepeatCourseRequest }) =>
      (await apiClient.post<AdminCourseDetail>(`/admin/courses/${id}/offerings`, data)).data,
    onSuccess: () => client.invalidateQueries({ queryKey: ['admin'] }),
  })
}
