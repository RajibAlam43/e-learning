import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { instructorService } from '@/services/instructor.service'
import { QUERY_KEYS } from '@/constants/queryKeys'
import type { CreateCourseAnnouncementRequest } from '@/types'

export function useInstructorDashboard() {
  return useQuery({
    queryKey: QUERY_KEYS.instructorDashboard(),
    queryFn: () => instructorService.getDashboard(),
    staleTime: 60 * 1000, // 1 minute — dashboard data is fairly dynamic
  })
}

export function useCreateInstructorAnnouncement() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ courseId, data }: { courseId: string; data: CreateCourseAnnouncementRequest }) => instructorService.createCourseAnnouncement(courseId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.instructorDashboard() }),
  })
}
