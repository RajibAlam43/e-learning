import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocale } from 'next-intl'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'
export type FacultyGroup = components['schemas']['FacultyGroupResponse']
export type FacultyMember = components['schemas']['FacultyMemberResponse']
export type GroupInput = components['schemas']['SaveFacultyGroupRequest']
export type MemberInput = components['schemas']['SaveFacultyMemberRequest']
export function useFacultyAdmin() {
  return useQuery({ queryKey: ['admin', 'faculty'], queryFn: async () => (await apiClient.get<FacultyGroup[]>('/admin/faculty')).data })
}
export function useFaculty() {
  const locale = useLocale()
  return useQuery({ queryKey: ['faculty', locale], queryFn: async ({ signal }) => (await apiClient.get<FacultyGroup[]>('/public/faculty', { signal, headers: { 'Accept-Language': locale } })).data })
}
export function useFeaturedFaculty() {
  const locale = useLocale()
  return useQuery({ queryKey: ['faculty-featured', locale], queryFn: async ({ signal }) => (await apiClient.get<FacultyMember[]>('/public/faculty/featured', { signal, headers: { 'Accept-Language': locale } })).data })
}
export function useFacultyMutation() {
  const cache = useQueryClient()
  return useMutation({ mutationFn: async ({ kind, id, data }: { kind: 'groups' | 'members'; id?: string; data?: GroupInput | MemberInput }) => {
    const path = `/admin/faculty/${kind}${id ? '/' + id : ''}`
    return !data ? apiClient.delete(path) : id ? apiClient.put(path, data) : apiClient.post(path, data)
  }, onSuccess: () => { cache.invalidateQueries({ queryKey: ['admin'] }); cache.invalidateQueries({ queryKey: ['faculty'] }); cache.invalidateQueries({ queryKey: ['faculty-featured'] }); cache.invalidateQueries({ queryKey: ['instructors'] }) } })
}
export function facultyError(cause: unknown) {
  return cause && typeof cause === 'object' && 'message' in cause && typeof cause.message === 'string' ? cause.message : 'Unable to save. Please try again.'
}
