/**
 * Type aliases derived from the generated OpenAPI types.
 * Use these aliases throughout the app instead of deep path references.
 */
import type { components, operations } from '@/types/api'

// ─── Auth Types ───
export type LoginRequest = components['schemas']['LoginRequest']
export type RegisterRequest = components['schemas']['RegisterRequest']
export type RegisterResponse = components['schemas']['RegisterResponse']
export type AuthResponse = components['schemas']['AuthResponse']
export type GoogleAuthRequest = components['schemas']['GoogleAuthRequest']
export type ForgotPasswordRequest = components['schemas']['ForgotPasswordRequest']
export type ResetPasswordRequest = components['schemas']['ResetPasswordRequest']
export type SendVerificationRequest = components['schemas']['SendVerificationRequest']
export type MeResponse = components['schemas']['MeResponse']

// ─── Auth Response Types ───
export type LoginResponse = operations['login']['responses']['200']['content']['*/*']
export type RefreshResponse = operations['refresh']['responses']['200']['content']['*/*']
export type UserResponse = operations['getMe']['responses']['200']['content']['*/*']

// ─── Course Types ───
export type CourseSummary = components['schemas']['CourseSummaryResponse']
export type CourseDetails = components['schemas']['CourseDetailsResponse']
export type CourseSection = components['schemas']['CourseSectionResponse']
export type CourseProgress = components['schemas']['CourseProgressResponse']
export type PageResponseCourse = components['schemas']['PageResponseCourseSummaryResponse']

// ─── Student Course Types ───
export type StudentCourseSummary = components['schemas']['StudentCourseSummaryResponse']
export type StudentCourseHome = components['schemas']['StudentCourseHomeResponse']
export type StudentDashboard = components['schemas']['StudentDashboardResponse']
export type StudentOngoingCourse = components['schemas']['StudentOngoingCourseSnapshot']
export type StudentLiveClassSummary = components['schemas']['StudentLiveClassSummaryResponse']
export type StudentLiveClassHome = components['schemas']['StudentLiveClassHomeResponse']
export type StudentLiveClassJoin = components['schemas']['StudentLiveClassJoinResponse']
export type StudentLearningStreak = components['schemas']['StudentLearningStreakResponse']
export type StudentCompletedLessons = components['schemas']['StudentCompletedLessonsResponse']
export type PageResponseCourseAnnouncement = components['schemas']['PageResponseCourseAnnouncementResponse']
export type CourseAnnouncement = components['schemas']['CourseAnnouncementResponse']

// ─── Learning Types (quizzes, progress, resources) ───
export type QuizContent = components['schemas']['QuizQuestionsResponse']
export type QuizAttemptStart = components['schemas']['QuizAttemptStartResponse']
export type QuizAttemptResult = components['schemas']['QuizAttemptResultResponse']
export type SubmitQuizAttemptRequest = components['schemas']['SubmitQuizAttemptRequest']
export type QuizQuestionResponseItem = components['schemas']['QuizQuestionResponse']
export type QuizChoiceResponseItem = components['schemas']['QuizChoiceResponse']
export type LessonProgress = components['schemas']['LessonProgressResponse']
export type LessonResource = components['schemas']['LessonResourceResponse']
export type ResourceDownloadUrl = components['schemas']['ResourceDownloadUrlResponse']
export type CertificateDetail = components['schemas']['CertificateDetailResponse']

// ─── Admin Course Types ───
export type AdminCourseSummary = components['schemas']['AdminCourseSummaryResponse']
export type AdminCourseDetail = components['schemas']['AdminCourseDetailResponse']
export type AdminCourseSection = components['schemas']['AdminCourseSectionResponse']
export type AdminSectionItem = components['schemas']['AdminSectionItemResponse']
export type AdminLessonDetail = components['schemas']['AdminLessonDetailResponse']
export type AdminQuizDetail = components['schemas']['AdminQuizDetailResponse']
export type AdminMediaAsset = components['schemas']['AdminMediaAssetResponse']

// ─── Admin Instructor Types ───
export type AdminInstructorSummary = components['schemas']['AdminInstructorSummaryResponse']
export type AdminInstructorDetail = components['schemas']['AdminInstructorDetailResponse']

// ─── Admin Order Types ───
export type AdminOrderSummary = components['schemas']['AdminOrderSummaryResponse']
export type AdminOrderDetail = components['schemas']['AdminOrderDetailResponse']
export type AdminOrderItem = components['schemas']['AdminOrderItemResponse']

// ─── Checkout & Payment Types ───
export type CreateCheckoutOrderRequest = components['schemas']['CreateCheckoutOrderRequest']
export type CheckoutOrderResponse = components['schemas']['CheckoutOrderResponse']
export type InitiatePaymentRequest = components['schemas']['InitiatePaymentRequest']
export type PaymentInitiationResponse = components['schemas']['PaymentInitiationResponse']
export type PaymentStatusResponse = components['schemas']['PaymentStatusResponse']
export type PaymentProvider = components['schemas']['InitiatePaymentRequest']['provider']
export type StudentOrderSummary = components['schemas']['StudentOrderSummaryResponse']
export type ReceiptResponse = components['schemas']['ReceiptResponse']
export type StudentCertificateSummary = components['schemas']['StudentCertificateSummaryResponse']
export type CertificateIssueResponse = components['schemas']['CertificateIssueResponse']
export type CertificateVerification = components['schemas']['PublicCertificateVerificationResponse']
export type StudentCollectionSummary = components['schemas']['StudentCollectionSummaryResponse']
export type StudentCollectionDetails = components['schemas']['StudentCollectionDetailsResponse']
export type CollectionSummary = components['schemas']['CollectionSummaryResponse']
export type CollectionDetails = components['schemas']['CollectionDetailsResponse']
export type AdminCollectionSummary = components['schemas']['AdminCollectionSummaryResponse']
export type AdminCollectionDetail = components['schemas']['AdminCollectionDetailResponse']
export type CreateCollectionRequest = components['schemas']['CreateCollectionRequest']
export type UpdateCollectionRequest = components['schemas']['UpdateCollectionRequest']
export type SetCollectionCoursesRequest = components['schemas']['SetCollectionCoursesRequest']
export type CreateSupportTicketRequest = components['schemas']['CreateSupportTicketRequest']

// ─── Admin Live Class Types ───
// Aug 2026 spec restored GET /admin/live-classes with a dedicated summary type
// (includes instructorName + approvedRegistrants natively — no more hacky alias).
export type AdminLiveClassSummary = components['schemas']['AdminLiveClassSummaryResponse']
export type PageAdminLiveClassSummary = components['schemas']['PageAdminLiveClassSummaryResponse']
export type AdminLiveClassDetail = components['schemas']['LiveClassUpsertResponse']
export type AdminLiveClassStart = components['schemas']['LiveClassUpsertResponse']

// ─── Thumbnail Upload (Cloudflare R2 presign) ───
export type ThumbnailUploadRequest = components['schemas']['CreateThumbnailUploadRequest']
export type ThumbnailUploadResponse = components['schemas']['ThumbnailUploadResponse']

// ─── Admin Request Types ───
export type CreateCourseRequest = components['schemas']['CreateCourseRequest']
export type AdminCategory = components['schemas']['AdminCategoryResponse']
export type CreateCategoryRequest = components['schemas']['CreateCategoryRequest']
export type FeatureCourseRequest = components['schemas']['FeatureCourseRequest']
export type CourseReview = components['schemas']['CourseReviewResponse']
export type AdminCourseReview = components['schemas']['AdminCourseReviewResponse']
export type CreateCourseReviewRequest = components['schemas']['CreateCourseReviewRequest']
export type UpdateCourseReviewRequest = components['schemas']['UpdateCourseReviewRequest']
export type AdminSupportTicket = components['schemas']['AdminSupportTicketResponse']
export type UpdateSupportTicketRequest = components['schemas']['UpdateSupportTicketRequest']
export type AdminAppSetting = components['schemas']['AdminAppSettingResponse']
export type UpsertAppSettingRequest = components['schemas']['UpsertAppSettingRequest']
export type AdminLessonResource = components['schemas']['AdminLessonResourceResponse']
export type CreateLessonResourceRequest = components['schemas']['CreateLessonResourceRequest']
export type CreateLessonResourceUploadRequest = components['schemas']['CreateLessonResourceUploadRequest']
export type LessonResourceUploadResponse = components['schemas']['LessonResourceUploadResponse']
export type UpdateLessonResourceRequest = components['schemas']['UpdateLessonResourceRequest']
export type UpdateCourseRequest = components['schemas']['UpdateCourseRequest']
export type CreateSectionRequest = components['schemas']['CreateSectionRequest']
export type UpdateSectionRequest = components['schemas']['UpdateSectionRequest']
export type CreateLessonRequest = components['schemas']['CreateLessonRequest']
export type UpdateLessonRequest = components['schemas']['UpdateLessonRequest']
export type CreateQuizRequest = components['schemas']['CreateQuizRequest']
export type UpdateQuizRequest = components['schemas']['UpdateQuizRequest']
export type CreateInstructorRequest = components['schemas']['CreateInstructorRequest']
export type UpdateInstructorRequest = components['schemas']['UpdateInstructorRequest']
export type UpdateOrderRequest = components['schemas']['UpdateOrderRequest']
export type CreateLiveClassRequest = components['schemas']['CreateLiveClassRequest']
export type UpdateLiveClassRequest = components['schemas']['UpdateLiveClassRequest']
export type CreateMediaAssetRequest = components['schemas']['CreateMediaAssetRequest']
export type UpdateMediaAssetRequest = components['schemas']['UpdateMediaAssetRequest']
export type AssignInstructorRequest = components['schemas']['AssignInstructorToCourseRequest']
export type ReorderCourseStructureRequest = components['schemas']['ReorderCourseStructureRequest']
export type CreateLessonVideoUploadRequest = components['schemas']['CreateLessonVideoUploadRequest']
export type LessonVideoUploadResponse = components['schemas']['LessonVideoUploadResponse']
export type RepeatCourseRequest = components['schemas']['RepeatCourseRequest']
export type CreateLiveClassItemRequest = components['schemas']['CreateLiveClassItemRequest']
export type AdminLiveClassItemResponse = components['schemas']['AdminLiveClassItemResponse']
export type ScheduleLiveClassRequest = components['schemas']['ScheduleLiveClassRequest']
export type ScheduleExternalLiveClassRequest = components['schemas']['ScheduleExternalLiveClassRequest']
export type AdminLiveClassDetailResponse = components['schemas']['AdminLiveClassDetailResponse']
export type CreateCourseAnnouncementRequest = components['schemas']['CreateCourseAnnouncementRequest']
export type PublicPlatformStats = components['schemas']['PublicPlatformStatsResponse']

// ─── Category Types ───
export type Category = components['schemas']['CategoryResponse']

// ─── API Error Type ───
export type ApiError = {
  message: string
  code: string
  status: number
}

// ─── Generic Types ───
export type UserRole = 'STUDENT' | 'INSTRUCTOR' | 'TEACHER' | 'ADMIN' | 'SUPER_ADMIN'
