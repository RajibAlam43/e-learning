import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'

type StudentDashboardResponse = components['schemas']['StudentDashboardResponse']
type StudentCourseSummaryResponse = components['schemas']['StudentCourseSummaryResponse']
type StudentLiveClassSummaryResponse = components['schemas']['StudentLiveClassSummaryResponse']
type StudentCourseHomeResponse = components['schemas']['StudentCourseHomeResponse']
type LessonContentResponse = components['schemas']['LessonContentResponse']
type MediaPlaybackResponse = components['schemas']['MediaPlaybackResponse']
type StudentOrderSummaryResponse = components['schemas']['StudentOrderSummaryResponse']
type UpdateProfileRequest = components['schemas']['UpdateProfileRequest']
type MeResponse = components['schemas']['MeResponse']
type StudentCertificateSummaryResponse = components['schemas']['StudentCertificateSummaryResponse']
type CertificateDownloadUrlResponse = components['schemas']['CertificateDownloadUrlResponse']
type StudentCollectionSummaryResponse = components['schemas']['StudentCollectionSummaryResponse']
type StudentCollectionDetailsResponse = components['schemas']['StudentCollectionDetailsResponse']
type ReceiptResponse = components['schemas']['ReceiptResponse']
type CreateCourseReviewRequest = components['schemas']['CreateCourseReviewRequest']
type UpdateCourseReviewRequest = components['schemas']['UpdateCourseReviewRequest']
type CourseReviewResponse = components['schemas']['CourseReviewResponse']
type StudentLearningStreakResponse = components['schemas']['StudentLearningStreakResponse']
type StudentCompletedLessonsResponse = components['schemas']['StudentCompletedLessonsResponse']
type PageResponseCourseAnnouncementResponse = components['schemas']['PageResponseCourseAnnouncementResponse']
type CertificateDetailResponse = components['schemas']['CertificateDetailResponse']

export const studentService = {
  getDashboard: async (): Promise<StudentDashboardResponse> => {
    const res = await apiClient.get<StudentDashboardResponse>('/student/dashboard')
    return res.data
  },

  getMyCourses: async (): Promise<StudentCourseSummaryResponse[]> => {
    const res = await apiClient.get<StudentCourseSummaryResponse[]>('/student/courses')
    return res.data
  },

  enrollInFreeCourse: async (courseId: string): Promise<void> => {
    await apiClient.post(`/student/courses/${courseId}/enroll`)
  },

  getLearningStreak: async (): Promise<StudentLearningStreakResponse> =>
    (await apiClient.get<StudentLearningStreakResponse>('/student/progress/streak')).data,

  getCompletedLessons: async (): Promise<StudentCompletedLessonsResponse> =>
    (await apiClient.get<StudentCompletedLessonsResponse>('/student/progress/completed-lessons')).data,

  getAnnouncements: async (page = 0, size = 10): Promise<PageResponseCourseAnnouncementResponse> =>
    (await apiClient.get<PageResponseCourseAnnouncementResponse>('/student/announcements', { params: { page, size } })).data,

  getLiveClasses: async (): Promise<StudentLiveClassSummaryResponse[]> => {
    const res = await apiClient.get<StudentLiveClassSummaryResponse[]>('/student/live-classes')
    return res.data
  },
  getCourseLiveClasses: async (courseId: string): Promise<StudentLiveClassSummaryResponse[]> => (await apiClient.get<StudentLiveClassSummaryResponse[]>(`/student/courses/${courseId}/live-classes`)).data,

  getCourseHome: async (courseId: string): Promise<StudentCourseHomeResponse> => {
    const res = await apiClient.get<StudentCourseHomeResponse>(`/student/courses/${courseId}`)
    return res.data
  },

  getLessonContent: async (lessonId: string): Promise<LessonContentResponse> => {
    const res = await apiClient.get<LessonContentResponse>(`/learn/lessons/${lessonId}`)
    return res.data
  },

  getCourseLessonContent: async (courseId: string, lessonId: string): Promise<LessonContentResponse> => {
    const res = await apiClient.get<LessonContentResponse>(`/learn/courses/${courseId}/lessons/${lessonId}`)
    return res.data
  },

  getLessonPlayback: async (lessonId: string): Promise<MediaPlaybackResponse> => {
    const res = await apiClient.get<MediaPlaybackResponse>(`/learn/lessons/${lessonId}/playback`)
    return res.data
  },

  getCourseLessonPlayback: async (courseId: string, lessonId: string): Promise<MediaPlaybackResponse> => {
    const res = await apiClient.get<MediaPlaybackResponse>(`/learn/courses/${courseId}/lessons/${lessonId}/playback`)
    return res.data
  },

  completeLesson: async (lessonId: string): Promise<void> => {
    await apiClient.post(`/learn/lessons/${lessonId}/complete`)
  },

  completeCourseLesson: async (courseId: string, lessonId: string): Promise<void> => {
    await apiClient.post(`/learn/courses/${courseId}/lessons/${lessonId}/complete`)
  },

  // Profile — GET /me returns MeResponse, PATCH /me/profile updates it
  getProfile: async (): Promise<MeResponse> => {
    const res = await apiClient.get<MeResponse>('/me')
    return res.data
  },

  updateProfile: async (data: UpdateProfileRequest): Promise<MeResponse> => {
    const res = await apiClient.patch<MeResponse>('/me/profile', data)
    return res.data
  },

  // Certificates
  getCertificates: async (): Promise<StudentCertificateSummaryResponse[]> => {
    const res = await apiClient.get<StudentCertificateSummaryResponse[]>('/student/certificates')
    return res.data
  },

  getCertificateDownloadUrl: async (certificateId: string): Promise<CertificateDownloadUrlResponse> => {
    const res = await apiClient.get<CertificateDownloadUrlResponse>(`/student/certificates/${certificateId}/download`)
    return res.data
  },

  getCertificate: async (certificateId: string): Promise<CertificateDetailResponse> =>
    (await apiClient.get<CertificateDetailResponse>(`/student/certificates/${certificateId}`)).data,

  getOrders: async (): Promise<StudentOrderSummaryResponse[]> => {
    const res = await apiClient.get<StudentOrderSummaryResponse[]>('/student/orders')
    return res.data
  },

  getOrderReceipt: async (orderId: string): Promise<ReceiptResponse> => {
    const res = await apiClient.get<ReceiptResponse>(`/student/orders/${orderId}/receipt`)
    return res.data
  },

  getOrderStatus: async (orderId: string) => (await apiClient.get<components['schemas']['CheckoutOrderResponse']>(`/checkout/orders/${orderId}`)).data,
  createCourseReview: async (courseId: string, data: CreateCourseReviewRequest): Promise<CourseReviewResponse> => (await apiClient.post<CourseReviewResponse>(`/student/courses/${courseId}/reviews`, data)).data,
  updateCourseReview: async (courseId: string, data: UpdateCourseReviewRequest): Promise<CourseReviewResponse> => (await apiClient.patch<CourseReviewResponse>(`/student/courses/${courseId}/reviews`, data)).data,
  deleteCourseReview: async (courseId: string): Promise<void> => { await apiClient.delete(`/student/courses/${courseId}/reviews`) },
  getCollections: async (): Promise<StudentCollectionSummaryResponse[]> => (await apiClient.get<StudentCollectionSummaryResponse[]>('/student/collections')).data,
  getCollection: async (id: string): Promise<StudentCollectionDetailsResponse> => (await apiClient.get<StudentCollectionDetailsResponse>(`/student/collections/${id}`)).data,
  issueCourseCertificate: async (courseId: string) => (await apiClient.post<components['schemas']['CertificateIssueResponse']>(`/student/courses/${courseId}/certificate/generate`)).data,
  issueCollectionCertificate: async (collectionId: string) => (await apiClient.post<components['schemas']['CertificateIssueResponse']>(`/student/collections/${collectionId}/certificate/generate`)).data,
}
