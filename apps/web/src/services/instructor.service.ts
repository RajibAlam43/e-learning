import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'

type InstructorSummary = components['schemas']['InstructorSummaryResponse']
type InstructorDetails = components['schemas']['InstructorDetailsResponse']
type InstructorDashboard = components['schemas']['InstructorDashboardResponse']
type CreateCourseAnnouncementRequest = components['schemas']['CreateCourseAnnouncementRequest']
type CourseAnnouncementResponse = components['schemas']['CourseAnnouncementResponse']

export const instructorService = {
  getPublicInstructors: async (signal?: AbortSignal, locale?: string): Promise<InstructorSummary[]> => {
    const res = await apiClient.get<InstructorSummary[]>('/public/instructors', { signal, ...(locale ? { headers: { 'Accept-Language': locale } } : {}) })
    return res.data
  },

  getInstructorBySlug: async (slug: string, signal?: AbortSignal, locale?: string): Promise<InstructorDetails> => {
    const res = await apiClient.get<InstructorDetails>(`/public/instructors/${encodeURIComponent(slug)}`, { signal, ...(locale ? { headers: { 'Accept-Language': locale } } : {}) })
    return res.data
  },

  // Authenticated instructor endpoints
  getDashboard: async (): Promise<InstructorDashboard> => {
    const res = await apiClient.get<InstructorDashboard>('/instructor/dashboard')
    return res.data
  },

  createCourseAnnouncement: async (courseId: string, data: CreateCourseAnnouncementRequest): Promise<CourseAnnouncementResponse> =>
    (await apiClient.post<CourseAnnouncementResponse>(`/instructor/courses/${courseId}/announcements`, data)).data,
}
