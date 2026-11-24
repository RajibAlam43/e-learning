import { apiClient } from '@/lib/api-client'
import { collectPages } from '@/lib/pagination'
import type {
  AdminCourseSummary,
  AdminCourseDetail,
  AdminInstructorSummary,
  AdminInstructorDetail,
  AdminOrderSummary,
  AdminOrderDetail,
  PageAdminLiveClassSummary,
  AdminLiveClassDetail,
  AdminLiveClassStart,
  AdminCourseSection,
  AdminLessonDetail,
  AdminQuizDetail,
  AdminMediaAsset,
  CreateCourseRequest,
  UpdateCourseRequest,
  CreateSectionRequest,
  UpdateSectionRequest,
  CreateLessonRequest,
  UpdateLessonRequest,
  CreateQuizRequest,
  UpdateQuizRequest,
  CreateInstructorRequest,
  UpdateInstructorRequest,
  UpdateOrderRequest,
  CreateLiveClassRequest,
  UpdateLiveClassRequest,
  CreateMediaAssetRequest,
  UpdateMediaAssetRequest,
  ThumbnailUploadRequest,
  ThumbnailUploadResponse,
  AssignInstructorRequest,
  ReorderCourseStructureRequest,
  AdminCollectionSummary,
  AdminCollectionDetail,
  CreateCollectionRequest,
  UpdateCollectionRequest,
  SetCollectionCoursesRequest,
  AdminCategory, CreateCategoryRequest, FeatureCourseRequest, AdminCourseReview,
  AdminSupportTicket, UpdateSupportTicketRequest, AdminAppSetting, UpsertAppSettingRequest,
  AdminLessonResource, CreateLessonResourceRequest, CreateLessonResourceUploadRequest, LessonResourceUploadResponse, UpdateLessonResourceRequest,
  CreateLessonVideoUploadRequest, LessonVideoUploadResponse, RepeatCourseRequest,
  CreateLiveClassItemRequest, AdminLiveClassItemResponse, ScheduleLiveClassRequest,
  ScheduleExternalLiveClassRequest, AdminLiveClassDetailResponse,
} from '@/types'

export const adminService = {
  getCategories: async () => (await apiClient.get<AdminCategory[]>('/admin/categories')).data,
  createCategory: async (data: CreateCategoryRequest) => (await apiClient.post<AdminCategory>('/admin/categories', data)).data,
  updateCategory: async (id: string, data: Partial<CreateCategoryRequest>) => (await apiClient.patch<AdminCategory>(`/admin/categories/${id}`, data)).data,
  featureCourse: async (id: string, data: FeatureCourseRequest) => (await apiClient.post(`/admin/courses/${id}/feature`, data)).data,
  unfeatureCourse: async (id: string) => { await apiClient.post(`/admin/courses/${id}/unfeature`) },
  deleteQuiz: async (id: string) => { await apiClient.delete(`/admin/quizzes/${id}`) },
  deleteInstructor: async (id: string) => { await apiClient.delete(`/admin/instructors/${id}`) },
  listReviews: async (pending?: boolean) => (await apiClient.get<AdminCourseReview[]>('/admin/reviews', { params: pending === undefined ? undefined : { pending } })).data,
  publishReview: async (id: string) => { await apiClient.post(`/admin/reviews/${id}/publish`) },
  unpublishReview: async (id: string) => { await apiClient.post(`/admin/reviews/${id}/unpublish`) },
  deleteReview: async (id: string) => { await apiClient.delete(`/admin/reviews/${id}`) },
  listSupportTickets: async () => (await apiClient.get<AdminSupportTicket[]>('/admin/support/tickets')).data,
  getSupportTicket: async (id: string) => (await apiClient.get<AdminSupportTicket>(`/admin/support/tickets/${id}`)).data,
  updateSupportTicket: async (id: string, data: UpdateSupportTicketRequest) => (await apiClient.patch<AdminSupportTicket>(`/admin/support/tickets/${id}`, data)).data,
  listSettings: async () => (await apiClient.get<AdminAppSetting[]>('/admin/settings')).data,
  getSetting: async (key: string) => (await apiClient.get<AdminAppSetting>(`/admin/settings/${encodeURIComponent(key)}`)).data,
  upsertSetting: async (key: string, data: UpsertAppSettingRequest) => (await apiClient.put<AdminAppSetting>(`/admin/settings/${encodeURIComponent(key)}`, data)).data,
  deleteSetting: async (key: string) => { await apiClient.delete(`/admin/settings/${encodeURIComponent(key)}`) },
  getLesson: async (id: string) => (await apiClient.get<AdminLessonDetail>(`/admin/lessons/${id}`)).data,
  createLessonResource: async (lessonId: string, data: CreateLessonResourceRequest) => (await apiClient.post<AdminLessonResource>(`/admin/lessons/${lessonId}/resources`, data)).data,
  requestLessonResourceUpload: async (lessonId: string, data: CreateLessonResourceUploadRequest) => (await apiClient.post<LessonResourceUploadResponse>(`/admin/lessons/${lessonId}/resources/upload-url`, data)).data,
  updateLessonResource: async (id: string, data: UpdateLessonResourceRequest) => (await apiClient.patch<AdminLessonResource>(`/admin/lesson-resources/${id}`, data)).data,
  deleteLessonResource: async (id: string) => { await apiClient.delete(`/admin/lesson-resources/${id}`) },
  getLessonResourceDownloadUrl: async (id: string) => (await apiClient.get<{ downloadUrl?: string }>(`/admin/lesson-resources/${id}/download-url`)).data,
  requestLessonVideoUpload: async (lessonId: string, data: CreateLessonVideoUploadRequest) =>
    (await apiClient.post<LessonVideoUploadResponse>(`/admin/lessons/${lessonId}/video/upload-url`, data)).data,
  // ─── Courses ───
  getCourses: async () => {
    const res = await apiClient.get<AdminCourseSummary[]>('/admin/courses')
    return res.data
  },

  getCourse: async (id: string) => {
    const res = await apiClient.get<AdminCourseDetail>(`/admin/courses/${id}`)
    return res.data
  },

  createCourse: async (data: CreateCourseRequest) => {
    const res = await apiClient.post<AdminCourseDetail>('/admin/courses', data)
    return res.data
  },

  updateCourse: async (id: string, data: UpdateCourseRequest) => {
    const res = await apiClient.patch<AdminCourseDetail>(`/admin/courses/${id}`, data)
    return res.data
  },

  publishCourse: async (id: string) => {
    await apiClient.post(`/admin/courses/${id}/publish`)
  },

  unpublishCourse: async (id: string) => {
    await apiClient.post(`/admin/courses/${id}/unpublish`)
  },

  repeatCourse: async (courseId: string, data: RepeatCourseRequest) =>
    (await apiClient.post<AdminCourseDetail>(`/admin/courses/${courseId}/repeat`, data)).data,

  // ─── Sections ───
  createSection: async (courseId: string, data: CreateSectionRequest) => {
    const res = await apiClient.post<AdminCourseSection>(`/admin/courses/${courseId}/sections`, data)
    return res.data
  },

  updateSection: async (id: string, data: UpdateSectionRequest) => {
    const res = await apiClient.patch<AdminCourseSection>(`/admin/sections/${id}`, data)
    return res.data
  },

  deleteSection: async (id: string) => {
    await apiClient.delete(`/admin/sections/${id}`)
  },

  publishSection: async (id: string) => {
    await apiClient.post(`/admin/sections/${id}/publish`)
  },

  unpublishSection: async (id: string) => {
    await apiClient.post(`/admin/sections/${id}/unpublish`)
  },

  // ─── Lessons ───
  createLesson: async (sectionId: string, data: CreateLessonRequest) => {
    const res = await apiClient.post<AdminLessonDetail>(`/admin/sections/${sectionId}/lessons`, data)
    return res.data
  },

  updateLesson: async (id: string, data: UpdateLessonRequest) => {
    const res = await apiClient.patch<AdminLessonDetail>(`/admin/lessons/${id}`, data)
    return res.data
  },

  deleteLesson: async (id: string) => {
    await apiClient.delete(`/admin/lessons/${id}`)
  },

  publishLesson: async (id: string) => {
    await apiClient.post(`/admin/lessons/${id}/publish`)
  },

  unpublishLesson: async (id: string) => {
    await apiClient.post(`/admin/lessons/${id}/unpublish`)
  },

  // ─── Quizzes ───
  createQuiz: async (sectionId: string, data: CreateQuizRequest) => {
    const res = await apiClient.post<AdminQuizDetail>(`/admin/sections/${sectionId}/quizzes`, data)
    return res.data
  },

  updateQuiz: async (id: string, data: UpdateQuizRequest) => {
    const res = await apiClient.patch<AdminQuizDetail>(`/admin/quizzes/${id}`, data)
    return res.data
  },

  publishQuiz: async (id: string) => {
    await apiClient.post(`/admin/quizzes/${id}/publish`)
  },

  unpublishQuiz: async (id: string) => {
    await apiClient.post(`/admin/quizzes/${id}/unpublish`)
  },

  createLiveClassItem: async (courseId: string, data: CreateLiveClassItemRequest) =>
    (await apiClient.post<AdminLiveClassItemResponse>(`/admin/courses/${courseId}/live-classes`, data)).data,

  scheduleLiveClass: async (courseId: string, liveClassId: string, data: ScheduleLiveClassRequest) =>
    (await apiClient.post<AdminLiveClassDetailResponse>(`/admin/courses/${courseId}/live-classes/${liveClassId}/schedule`, data)).data,

  scheduleExternalLiveClass: async (courseId: string, liveClassId: string, data: ScheduleExternalLiveClassRequest) =>
    (await apiClient.post<AdminLiveClassDetailResponse>(`/admin/courses/${courseId}/live-classes/${liveClassId}/schedule-with-url`, data)).data,

  // ─── Course Structure ───
  reorderCourseStructure: async (courseId: string, data: ReorderCourseStructureRequest) => {
    await apiClient.post(`/admin/courses/${courseId}/structure/reorder`, data)
  },

  assignInstructor: async (courseId: string, data: AssignInstructorRequest) => {
    await apiClient.post(`/admin/courses/${courseId}/instructors`, data)
  },

  // ─── Instructors ───
  getInstructors: async () => {
    const res = await apiClient.get<AdminInstructorSummary[]>('/admin/instructors')
    return res.data
  },

  createInstructor: async (data: CreateInstructorRequest) => {
    const res = await apiClient.post<AdminInstructorDetail>('/admin/instructors', data)
    return res.data
  },

  updateInstructor: async (id: string, data: UpdateInstructorRequest) => {
    const res = await apiClient.patch<AdminInstructorDetail>(`/admin/instructors/${id}`, data)
    return res.data
  },

  // ─── Orders ───
  getOrders: async () => {
    const res = await apiClient.get<AdminOrderSummary[]>('/admin/orders')
    return res.data
  },

  getOrder: async (id: string) => {
    const res = await apiClient.get<AdminOrderDetail>(`/admin/orders/${id}`)
    return res.data
  },

  updateOrder: async (id: string, data: UpdateOrderRequest) => {
    const res = await apiClient.patch<AdminOrderDetail>(`/admin/orders/${id}`, data)
    return res.data
  },

  // ─── Live Classes ───
  // Collect every page so client-side filters and dashboard totals remain complete.
  getLiveClasses: async () => {
    const requestPage = async (page: number) => (await apiClient.get<PageAdminLiveClassSummary>('/admin/live-classes', { params: { page, size: 50 } })).data
    return collectPages(await requestPage(0), requestPage)
  },

  updateLiveClass: async (id: string, data: UpdateLiveClassRequest) => {
    const res = await apiClient.patch<AdminLiveClassDetail>(`/live-classes/${id}`, data)
    return res.data
  },

  startLiveClass: async (id: string) => {
    const res = await apiClient.post<AdminLiveClassStart>(`/live-classes/${id}/start`)
    return res.data
  },

  deleteLiveClass: async (id: string) => { await apiClient.delete(`/live-classes/${id}`) },

  createLiveClass: async (courseId: string, data: CreateLiveClassRequest) => {
    const res = await apiClient.post<AdminLiveClassDetail>(`/live-classes/courses/${courseId}`, data)
    return res.data
  },

  // ─── Media Assets ───
  createMediaAsset: async (data: CreateMediaAssetRequest) => {
    const res = await apiClient.post<AdminMediaAsset>('/admin/media-assets', data)
    return res.data
  },

  updateMediaAsset: async (id: string, data: UpdateMediaAssetRequest) => {
    const res = await apiClient.patch<AdminMediaAsset>(`/admin/media-assets/${id}`, data)
    return res.data
  },

  // ─── Thumbnail Upload (Cloudflare R2 presign) ───
  // Step 1 of 2: ask backend for a presigned upload URL. Step 2 (the actual
  // PUT/POST to R2) happens in the ThumbnailUploader component — the browser
  // uploads the file bytes directly to R2, then we attach the returned objectKey
  // to the entity (course / collection / media asset).
  requestThumbnailUpload: async (data: ThumbnailUploadRequest) => {
    const res = await apiClient.post<ThumbnailUploadResponse>('/admin/thumbnails/upload-url', data)
    return res.data
  },

  getCollections: async () => (await apiClient.get<AdminCollectionSummary[]>('/admin/collections')).data,
  createCollection: async (data: CreateCollectionRequest) => (await apiClient.post<AdminCollectionDetail>('/admin/collections', data)).data,
  getCollection: async (id: string) => (await apiClient.get<AdminCollectionDetail>(`/admin/collections/${id}`)).data,
  updateCollection: async (id: string, data: UpdateCollectionRequest) => (await apiClient.patch<AdminCollectionDetail>(`/admin/collections/${id}`, data)).data,
  setCollectionCourses: async (id: string, data: SetCollectionCoursesRequest) => (await apiClient.post<AdminCollectionDetail>(`/admin/collections/${id}/courses`, data)).data,
  publishCollection: async (id: string) => { await apiClient.post(`/admin/collections/${id}/publish`) },
  unpublishCollection: async (id: string) => { await apiClient.post(`/admin/collections/${id}/unpublish`) },

  revokeCertificate: async (certificateId: string, reason?: string) => {
    await apiClient.post(`/admin/certificates/${certificateId}/revoke`, reason ? { reason } : undefined)
  },

  reinstateCertificate: async (certificateId: string) => {
    await apiClient.post(`/admin/certificates/${certificateId}/reinstate`)
  },
}
