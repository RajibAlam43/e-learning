import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { studentService } from '@/services/student.service'
import { authService } from '@/services/auth.service'
import { QUERY_KEYS } from '@/constants/queryKeys'
import { useAuthStore } from '@/store/auth.store'

export function useStudentDashboard() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  return useQuery({
    queryKey: QUERY_KEYS.studentDashboard(),
    queryFn: () => studentService.getDashboard(),
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useMyCourses() {
  // Also called from the public course-detail page (to compute "already
  // enrolled"). Without this gate every anonymous visitor fires an
  // authenticated /student/courses request that can only ever 401.
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isSessionReady = useAuthStore((s) => s.isSessionReady)

  return useQuery({
    queryKey: QUERY_KEYS.myCourses(),
    queryFn: () => studentService.getMyCourses(),
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useLiveClasses() {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.liveClasses(),
    queryFn: () => studentService.getLiveClasses(),
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useStudentAnnouncements(page = 0, size = 10) {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.studentAnnouncements(page, size),
    queryFn: () => studentService.getAnnouncements(page, size),
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useLearningStreak() {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.learningStreak(),
    queryFn: studentService.getLearningStreak,
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useCompletedLessons() {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.completedLessons(),
    queryFn: studentService.getCompletedLessons,
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useEnrollInFreeCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (courseId: string) => studentService.enrollInFreeCourse(courseId),
    onSuccess: (_, courseId) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.myCourses() })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseHome(courseId) })
    },
  })
}
export function useCourseLiveClasses(courseId: string | null) { const isSessionReady = useAuthStore((s) => s.isSessionReady); const isAuthenticated = useAuthStore((s) => s.isAuthenticated); return useQuery({ queryKey: ['student','courses',courseId,'live-classes'], queryFn:()=>studentService.getCourseLiveClasses(courseId!), enabled:!!courseId && isSessionReady && isAuthenticated }) }

export function useCourseHome(courseId: string) {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.courseHome(courseId),
    queryFn: () => studentService.getCourseHome(courseId),
    enabled: !!courseId && isSessionReady && isAuthenticated,
  })
}

export function useLessonContent(lessonId: string | null, courseId?: string | null) {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: courseId ? QUERY_KEYS.courseLessonContent(courseId, lessonId ?? '') : QUERY_KEYS.lessonContent(lessonId ?? ''),
    queryFn: () => courseId ? studentService.getCourseLessonContent(courseId, lessonId!) : studentService.getLessonContent(lessonId!),
    enabled: !!lessonId && isSessionReady && isAuthenticated,
  })
}

export function useLessonPlayback(lessonId: string | null, courseId?: string | null) {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: courseId ? QUERY_KEYS.courseLessonPlayback(courseId, lessonId ?? '') : QUERY_KEYS.lessonPlayback(lessonId ?? ''),
    queryFn: () => courseId ? studentService.getCourseLessonPlayback(courseId, lessonId!) : studentService.getLessonPlayback(lessonId!),
    enabled: !!lessonId && isSessionReady && isAuthenticated,
  })
}

export function useCompleteLesson(courseId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (lessonId: string) => studentService.completeCourseLesson(courseId, lessonId),
    onSuccess: (_, lessonId) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseLessonContent(courseId, lessonId) })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonContent(lessonId) })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseProgress(courseId) })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.completedLessons() })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.studentDashboard() })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseHome(courseId) })
    },
  })
}

// Profile — shares query key with useMe to avoid duplicate /user/me calls
export function useProfile() {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.user(),
    queryFn: () => authService.getMe(),
    staleTime: 0,
    refetchOnMount: 'always',
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useUpdateProfile() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: Parameters<typeof studentService.updateProfile>[0]) =>
      studentService.updateProfile(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.user() })
    },
  })
}

// Certificates
export function useCertificates() {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.certificates(),
    queryFn: () => studentService.getCertificates(),
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useCertificateDownload(certificateId: string | null) {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.certificateDownload(certificateId ?? ''),
    queryFn: () => studentService.getCertificateDownloadUrl(certificateId!),
    enabled: !!certificateId && isSessionReady && isAuthenticated,
  })
}

export function useCertificate(certificateId: string | null) {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.certificate(certificateId ?? ''),
    queryFn: () => studentService.getCertificate(certificateId!),
    enabled: !!certificateId && isSessionReady && isAuthenticated,
  })
}

export function useStudentOrders() {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.studentOrders(),
    queryFn: () => studentService.getOrders(),
    enabled: isSessionReady && isAuthenticated,
  })
}

export function useOrderReceipt(orderId: string | null) {
  const isSessionReady = useAuthStore((s) => s.isSessionReady)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.studentOrder(orderId ?? ''),
    queryFn: () => studentService.getOrderReceipt(orderId!),
    enabled: !!orderId && isSessionReady && isAuthenticated,
    retry: 1,
  })
}

export function useStudentCollections() { const isSessionReady = useAuthStore((s) => s.isSessionReady); const isAuthenticated = useAuthStore((s) => s.isAuthenticated); return useQuery({ queryKey: QUERY_KEYS.studentCollections(), queryFn: studentService.getCollections, enabled: isSessionReady && isAuthenticated }) }
export function useStudentCollection(id: string | null) { const isSessionReady = useAuthStore((s) => s.isSessionReady); const isAuthenticated = useAuthStore((s) => s.isAuthenticated); return useQuery({ queryKey: QUERY_KEYS.studentCollection(id ?? ''), queryFn: () => studentService.getCollection(id!), enabled: !!id && isSessionReady && isAuthenticated }) }
export function useIssueCourseCertificate() { const qc=useQueryClient(); return useMutation({ mutationFn: (courseId:string)=>studentService.issueCourseCertificate(courseId), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.certificates()}) }) }
export function useIssueCollectionCertificate() { const qc=useQueryClient(); return useMutation({ mutationFn: (collectionId:string)=>studentService.issueCollectionCertificate(collectionId), onSuccess:()=>qc.invalidateQueries({queryKey:QUERY_KEYS.certificates()}) }) }
export function useCreateCourseReview() { const qc=useQueryClient(); return useMutation({ mutationFn: ({courseId,data}:{courseId:string;data:Parameters<typeof studentService.createCourseReview>[1]})=>studentService.createCourseReview(courseId,data), onSuccess:(_,v)=>qc.invalidateQueries({queryKey:QUERY_KEYS.publicCourseReviews(v.courseId)}) }) }
export function useUpdateCourseReview() { const qc=useQueryClient(); return useMutation({ mutationFn: ({courseId,data}:{courseId:string;data:Parameters<typeof studentService.updateCourseReview>[1]})=>studentService.updateCourseReview(courseId,data), onSuccess:(_,v)=>qc.invalidateQueries({queryKey:QUERY_KEYS.publicCourseReviews(v.courseId)}) }) }
export function useDeleteCourseReview() { const qc=useQueryClient(); return useMutation({ mutationFn: (courseId:string)=>studentService.deleteCourseReview(courseId), onSuccess:(_,courseId)=>qc.invalidateQueries({queryKey:QUERY_KEYS.publicCourseReviews(courseId)}) }) }
