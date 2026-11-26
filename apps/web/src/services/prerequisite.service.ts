import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'
export type Waiver = components['schemas']['WaiverResponse']
export type CourseOption = components['schemas']['CourseOption']
export type Prerequisites = components['schemas']['PrerequisiteResponse']
export function usePrerequisites(courseId: string) {
 return useQuery({ queryKey: ['admin', 'prerequisites', courseId], enabled: !!courseId, queryFn: async () => (await apiClient.get<Prerequisites>(`/admin/courses/${courseId}/prerequisites`)).data })
}
export function useSavePrerequisites(courseId: string) {
 const cache = useQueryClient()
 return useMutation({ mutationFn: async (courseTemplateIds: string[]) => (await apiClient.put<Prerequisites>(`/admin/courses/${courseId}/prerequisites`, { courseTemplateIds })).data,
 onSuccess: () => cache.invalidateQueries({ queryKey: ['admin', 'prerequisites', courseId] }) })
}
export function useWaiverCourses() {
 return useQuery({ queryKey: ['student', 'waiver-courses'], queryFn: async () => (await apiClient.get<CourseOption[]>('/student/waivers/courses')).data })
}
export function useWaivers(admin: boolean, status = 'PENDING', page = 0) {
 return useQuery({ queryKey: [admin ? 'admin' : 'student', 'waivers', status, page], queryFn: async () => (await apiClient.get<Waiver[]>(admin ? `/admin/waivers?status=${status}&page=${page}` : `/student/waivers?page=${page}`)).data })
}
export function useCreateWaiver() {
 const cache = useQueryClient()
 return useMutation({ mutationFn: async ({ courseTemplateId, reason, file }: { courseTemplateId: string; reason: string; file?: File }) => {
  let evidenceObjectKey: string | undefined
  if (file) {
   const upload = (await apiClient.post<components['schemas']['Upload']>('/student/waivers/evidence-upload', { fileName: file.name, contentType: file.type, sizeBytes: file.size })).data
   if (!upload.uploadUrl || !upload.objectKey) throw new Error('Unable to prepare the supporting file. Try again.')
   const result = await fetch(upload.uploadUrl, { method: 'PUT', credentials: 'omit', headers: { 'Content-Type': file.type }, body: file })
   if (!result.ok) throw new Error('Supporting file could not be uploaded. Try again.')
   evidenceObjectKey = upload.objectKey
  }
  return (await apiClient.post<Waiver>('/student/waivers', { courseTemplateId, reason, evidenceObjectKey })).data
 }, onSuccess: () => cache.invalidateQueries({ queryKey: ['student'] }) })
}
export function useReviewWaiver() {
 const cache = useQueryClient()
 return useMutation({ mutationFn: async ({ id, ...data }: { id: string; decision: 'APPROVED' | 'REJECTED'; reason: string; expectedCredits: number; semesterId?: string }) => (await apiClient.post<Waiver>(`/admin/waivers/${id}/review`, data)).data,
 onSuccess: () => cache.invalidateQueries({ queryKey: ['admin'] }) })
}
export async function downloadWaiverEvidence(id: string, admin: boolean) {
 const result = (await apiClient.get<components['schemas']['PresignedDownload']>(`/${admin ? 'admin' : 'student'}/waivers/${id}/evidence`)).data
 if (!result.downloadUrl) throw new Error('Unable to download the supporting file.')
 window.location.assign(result.downloadUrl)
}
