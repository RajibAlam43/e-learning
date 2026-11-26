'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminService } from '@/services/admin.service'
import { QUERY_KEYS } from '@/constants/queryKeys'
import type {
  AdminUsersParams,
  CreateCourseRequest,
  UpdateCourseRequest,
  CreateInstructorRequest,
  UpdateInstructorRequest,
  UpdateOrderRequest,
  UpdateLiveClassRequest,
  CreateLiveClassRequest,
  CreateSectionRequest,
  UpdateSectionRequest,
  CreateLessonRequest,
  UpdateLessonRequest,
  CreateQuizRequest,
  UpdateQuizRequest,
  AssignInstructorRequest,
  ThumbnailUploadRequest,
  CreateCollectionRequest,
  UpdateCollectionRequest,
  SetCollectionCoursesRequest,
  CreateMediaAssetRequest,
  UpdateMediaAssetRequest,
  CreateCategoryRequest, FeatureCourseRequest, UpdateSupportTicketRequest,
  UpsertAppSettingRequest, CreateLessonResourceRequest, CreateLessonResourceUploadRequest, UpdateLessonResourceRequest,
  CreateLessonVideoUploadRequest, RepeatCourseRequest, CreateLiveClassItemRequest, ScheduleLiveClassRequest,
  ScheduleExternalLiveClassRequest, AdminLessonDetail,
} from '@/types'

// ─── Users ───

export function useAdminUsers(params: AdminUsersParams) {
  return useQuery({
    queryKey: QUERY_KEYS.adminUsers(params),
    queryFn: ({ signal }) => adminService.getUsers(params, signal),
    staleTime: 0,
  })
}

// ─── Courses ───

export function useAdminCourses() {
  return useQuery({
    queryKey: QUERY_KEYS.adminCourses(),
    queryFn: adminService.getCourses,
  })
}

export function useAdminCourse(id: string) {
  return useQuery({
    queryKey: QUERY_KEYS.adminCourse(id),
    queryFn: () => adminService.getCourse(id),
    enabled: !!id,
  })
}

export function useCreateCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCourseRequest) => adminService.createCourse(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

export function useUpdateCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCourseRequest }) =>
      adminService.updateCourse(id, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourse(id) })
    },
  })
}

export function usePublishCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminService.publishCourse(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

export function useUnpublishCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminService.unpublishCourse(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

// ─── Instructors ───

export function useAdminInstructors() {
  return useQuery({
    queryKey: QUERY_KEYS.adminInstructors(),
    queryFn: adminService.getInstructors,
  })
}

export function useAdminInstructor(id: string | null) {
  return useQuery({
    queryKey: QUERY_KEYS.adminInstructor(id ?? ''),
    queryFn: async () => (await adminService.getInstructors()).find((item) => item.userId === id) ?? null,
    enabled: !!id,
  })
}

export function useAdminCollections() { return useQuery({ queryKey: QUERY_KEYS.adminCollections(), queryFn: adminService.getCollections }) }
export function useAdminCollection(id: string | null) { return useQuery({ queryKey: QUERY_KEYS.adminCollection(id ?? ''), queryFn: () => adminService.getCollection(id!), enabled: !!id }) }
export function useCreateCollection() { const qc=useQueryClient(); return useMutation({ mutationFn: (data: CreateCollectionRequest) => adminService.createCollection(data), onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCollections() }) }) }
export function useUpdateCollection() { const qc=useQueryClient(); return useMutation({ mutationFn: ({id,data}:{id:string;data:UpdateCollectionRequest}) => adminService.updateCollection(id,data), onSuccess: (_,v) => { qc.invalidateQueries({queryKey:QUERY_KEYS.adminCollections()}); qc.invalidateQueries({queryKey:QUERY_KEYS.adminCollection(v.id)}) } }) }
export function useSetCollectionCourses() { const qc=useQueryClient(); return useMutation({ mutationFn:({id,data}:{id:string;data:SetCollectionCoursesRequest})=>adminService.setCollectionCourses(id,data), onSuccess:(_,v)=>{qc.invalidateQueries({queryKey:QUERY_KEYS.adminCollections()});qc.invalidateQueries({queryKey:QUERY_KEYS.adminCollection(v.id)})} }) }
export function usePublishCollection() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.publishCollection(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCollections()}) }) }
export function useUnpublishCollection() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.unpublishCollection(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCollections()}) }) }

export function useCreateInstructor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateInstructorRequest) => adminService.createInstructor(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminInstructors() }) },
  })
}

export function useUpdateInstructor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateInstructorRequest }) =>
      adminService.updateInstructor(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminInstructors() }) },
  })
}

// ─── Orders ───

export function useAdminOrders() {
  return useQuery({
    queryKey: QUERY_KEYS.adminOrders(),
    queryFn: adminService.getOrders,
  })
}

export function useAdminOrder(id: string | null) {
  return useQuery({
    queryKey: QUERY_KEYS.adminOrder(id ?? ''),
    queryFn: () => adminService.getOrder(id!),
    enabled: !!id,
  })
}

export function useUpdateOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateOrderRequest }) =>
      adminService.updateOrder(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminOrders() })
    },
  })
}

// ─── Live Classes ───

export function useAdminLiveClasses() {
  return useQuery({
    queryKey: QUERY_KEYS.adminLiveClasses(),
    queryFn: adminService.getLiveClasses,
  })
}

export function useUpdateLiveClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLiveClassRequest }) =>
      adminService.updateLiveClass(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminLiveClasses() }) },
  })
}

export function useStartLiveClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminService.startLiveClass(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminLiveClasses() }) },
  })
}

export function useCreateLiveClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, data }: { courseId: string; data: CreateLiveClassRequest }) =>
      adminService.createLiveClass(courseId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminLiveClasses() }) },
  })
}

export function useRequestLessonVideoUpload() {
  return useMutation({
    mutationFn: ({ lessonId, data }: { lessonId: string; data: CreateLessonVideoUploadRequest }) =>
      adminService.requestLessonVideoUpload(lessonId, data),
  })
}

export function useRepeatCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, data }: { courseId: string; data: RepeatCourseRequest }) => adminService.repeatCourse(courseId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }),
  })
}

export function useCreateLiveClassItem() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, data }: { courseId: string; data: CreateLiveClassItemRequest }) => adminService.createLiveClassItem(courseId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }),
  })
}

export function useScheduleLiveClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, liveClassId, data }: { courseId: string; liveClassId: string; data: ScheduleLiveClassRequest }) => adminService.scheduleLiveClass(courseId, liveClassId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }),
  })
}

export function useScheduleExternalLiveClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, liveClassId, data }: { courseId: string; liveClassId: string; data: ScheduleExternalLiveClassRequest }) => adminService.scheduleExternalLiveClass(courseId, liveClassId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }),
  })
}

export function useDeleteLiveClass() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminService.deleteLiveClass(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }),
  })
}

export function useRevokeCertificate() {
  return useMutation({ mutationFn: ({ certificateId, reason }: { certificateId: string; reason?: string }) => adminService.revokeCertificate(certificateId, reason) })
}

export function useReinstateCertificate() {
  return useMutation({ mutationFn: (certificateId: string) => adminService.reinstateCertificate(certificateId) })
}

export function useReorderCourseStructure() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, data }: { courseId: string; data: import('@/types').ReorderCourseStructureRequest }) => adminService.reorderCourseStructure(courseId, data),
    onSuccess: (_, variables) => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourse(variables.courseId) }),
  })
}

// ─── Curriculum ───

export function useCreateSection() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, data }: { courseId: string; data: CreateSectionRequest }) =>
      adminService.createSection(courseId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

export function useUpdateSection() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateSectionRequest }) =>
      adminService.updateSection(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

export function useDeleteSection() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminService.deleteSection(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}
export function usePublishSection() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.publishSection(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }
export function useUnpublishSection() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.unpublishSection(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }

export function useCreateLesson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ sectionId, data }: { sectionId: string; data: CreateLessonRequest }) =>
      adminService.createLesson(sectionId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

export function useUpdateLesson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLessonRequest }) =>
      adminService.updateLesson(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

export function useDeleteLesson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminService.deleteLesson(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}
export function usePublishLesson() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.publishLesson(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }
export function useUnpublishLesson() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.unpublishLesson(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }

export function useCreateQuiz() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ sectionId, data }: { sectionId: string; data: CreateQuizRequest }) =>
      adminService.createQuiz(sectionId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

export function useUpdateQuiz() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateQuizRequest }) =>
      adminService.updateQuiz(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}
export function usePublishQuiz() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.publishQuiz(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }
export function useUnpublishQuiz() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.unpublishQuiz(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }

export function useAssignInstructor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, data }: { courseId: string; data: AssignInstructorRequest }) =>
      adminService.assignInstructor(courseId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }) },
  })
}

// ─── Thumbnail Upload (Cloudflare R2 presign) ───
// Returns the presign info; the actual file upload to R2 happens in the
// ThumbnailUploader component (browser → R2 directly, bypassing the backend).
export function useRequestThumbnailUpload() {
  return useMutation({
    mutationFn: (data: ThumbnailUploadRequest) => adminService.requestThumbnailUpload(data),
  })
}

export function useCreateMediaAsset() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateMediaAssetRequest) => adminService.createMediaAsset(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }),
  })
}

export function useAdminCategories() { return useQuery({ queryKey: QUERY_KEYS.adminCategories(), queryFn: adminService.getCategories }) }
export function useCreateCategory() { const qc=useQueryClient(); return useMutation({ mutationFn:(data:CreateCategoryRequest)=>adminService.createCategory(data), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCategories()}) }) }
export function useUpdateCategory() { const qc=useQueryClient(); return useMutation({ mutationFn:({id,data}:{id:string;data:Partial<CreateCategoryRequest>})=>adminService.updateCategory(id,data), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCategories()}) }) }
export function useFeatureCourse() { const qc=useQueryClient(); return useMutation({ mutationFn:({id,data}:{id:string;data:FeatureCourseRequest})=>adminService.featureCourse(id,data), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }
export function useUnfeatureCourse() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.unfeatureCourse(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }
export function useDeleteQuiz() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.deleteQuiz(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()}) }) }
export function useDeleteInstructor() { const qc=useQueryClient(); return useMutation({ mutationFn:(id:string)=>adminService.deleteInstructor(id), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminInstructors()}) }) }
export function useAdminReviews(pending?:boolean) { return useQuery({ queryKey:QUERY_KEYS.adminReviews(pending), queryFn:()=>adminService.listReviews(pending) }) }
export function useReviewAction() { const qc=useQueryClient(); return { publish:useMutation({mutationFn:adminService.publishReview,onSuccess:()=>qc.invalidateQueries({queryKey:['admin','reviews']})}), unpublish:useMutation({mutationFn:adminService.unpublishReview,onSuccess:()=>qc.invalidateQueries({queryKey:['admin','reviews']})}), remove:useMutation({mutationFn:adminService.deleteReview,onSuccess:()=>qc.invalidateQueries({queryKey:['admin','reviews']})}) } }
export function useAdminSupportTickets() { return useQuery({queryKey:QUERY_KEYS.adminSupportTickets(),queryFn:adminService.listSupportTickets}) }
export function useUpdateSupportTicket() { const qc=useQueryClient(); return useMutation({mutationFn:({id,data}:{id:string;data:UpdateSupportTicketRequest})=>adminService.updateSupportTicket(id,data),onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminSupportTickets()})}) }
export function useAdminSettings() { return useQuery({queryKey:QUERY_KEYS.adminSettings(),queryFn:adminService.listSettings}) }
export function useUpsertSetting() { const qc=useQueryClient(); return useMutation({mutationFn:({key,data}:{key:string;data:UpsertAppSettingRequest})=>adminService.upsertSetting(key,data),onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminSettings()})}) }
export function useDeleteSetting() { const qc=useQueryClient(); return useMutation({mutationFn:adminService.deleteSetting,onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminSettings()})}) }
export function useCreateLessonResource() { const qc=useQueryClient(); return useMutation({mutationFn:({lessonId,data}:{lessonId:string;data:CreateLessonResourceRequest})=>adminService.createLessonResource(lessonId,data),onSuccess:(_,v)=>{qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()});qc.invalidateQueries({queryKey:['admin','lessons',v.lessonId]})}}) }
export function useLessonResourceUpload() { return useMutation({mutationFn:({lessonId,data}:{lessonId:string;data:CreateLessonResourceUploadRequest})=>adminService.requestLessonResourceUpload(lessonId,data)}) }
export function useUpdateLessonResource() { const qc=useQueryClient(); return useMutation({mutationFn:({id,data}:{id:string;data:UpdateLessonResourceRequest})=>adminService.updateLessonResource(id,data),onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()})}) }
export function useDeleteLessonResource() { const qc=useQueryClient(); return useMutation({mutationFn:adminService.deleteLessonResource,onSuccess:()=>{qc.invalidateQueries({queryKey:QUERY_KEYS.adminCourses()});qc.invalidateQueries({queryKey:['admin','lessons']})}}) }
export function useLessonResourceDownload() { return useMutation({ mutationFn: adminService.getLessonResourceDownloadUrl }) }
export function useAdminLesson(id: string | null, pollMedia = false) { return useQuery({ queryKey: ['admin','lessons',id], queryFn: () => adminService.getLesson(id!), enabled: !!id, refetchOnMount: 'always', refetchInterval: pollMedia ? query => { const status = (query.state.data as AdminLessonDetail | undefined)?.mediaAsset?.status?.toUpperCase(); return status === 'UPLOADING' || status === 'PROCESSING' ? 3000 : false } : false }) }

export function useUpdateMediaAsset() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateMediaAssetRequest }) => adminService.updateMediaAsset(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.adminCourses() }),
  })
}
