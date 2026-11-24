'use client'

import { AdminRecentCourses } from './AdminRecentCourses'
import { AdminActivity } from './AdminActivity'
import { AdminCourseCompletion } from './AdminCourseCompletion'
import { AdminTopInstructors } from './AdminTopInstructors'
import type { AdminCourseSummary, AdminInstructorSummary } from '@/types'
import type { DashboardActivity } from '@/hooks/useAdminDashboard'

interface AdminDashboardGridProps {
  recentCourses: AdminCourseSummary[]
  topInstructors: AdminInstructorSummary[]
  recentActivity: DashboardActivity[]
  liveClassCount: number
  isLoading: boolean
}

export function AdminDashboardGrid({
  recentCourses,
  topInstructors,
  recentActivity,
  liveClassCount,
  isLoading,
}: AdminDashboardGridProps) {
  return (
    <div className='flex flex-col gap-4 w-full'>
      <div className='flex flex-col lg:flex-row items-start gap-4 w-full'>
        <AdminRecentCourses courses={recentCourses} isLoading={isLoading} />
        <AdminActivity items={recentActivity} isLoading={isLoading} />
      </div>

      <div className='grid grid-cols-1 lg:grid-cols-2 gap-4 w-full'>
        <AdminCourseCompletion liveClassCount={liveClassCount} courseCount={recentCourses.length} />
        <AdminTopInstructors instructors={topInstructors} isLoading={isLoading} />
      </div>
    </div>
  )
}
