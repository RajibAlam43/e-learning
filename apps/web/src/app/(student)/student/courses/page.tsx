'use client'

import { useTranslations } from 'next-intl'

import CourseCard from '@/components/shared/CourseCard'
import EmptyCoursesCard from '@/components/shared/EmptyCoursesCard'
import { ErrorState } from '@/components/shared/ErrorState'
import { Skeleton } from '@/components/ui/skeleton'
import { useMyCourses } from '@/hooks/useStudentDashboard'

export default function MyCoursesPage() {
  const ui = useTranslations('privateUi')

  const { data: courses, isLoading, isError, refetch } = useMyCourses()

  const activeCourses = (courses ?? [])
    .filter((c) => c.enrollmentStatus === 'ACTIVE')
    .map((c) => ({
      id: c.courseId ?? '',
      title: c.courseName ?? '',
      thumbnail: c.courseThumbnailUrl ?? '/images/course-quran.jpg',
      href: `/student/courses/${c.courseId}`,
      progress: Math.round(c.completionPercentage ?? 0),
      lessonsLeft: (c.totalLessons ?? 0) - (c.completedLessons ?? 0),
    }))

  return (
    <div className='flex flex-col gap-6'>
      <div className='flex flex-col gap-1'>
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('myCourses')}</h1>
        <p className='text-sm text-[#5e5f5c]'>
          {!isLoading && activeCourses.length > 0 ? `${activeCourses.length} courses enrolled` : ''}
        </p>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className='h-48 rounded-2xl' />
          ))}
        </div>
      )}

      {/* Error */}
      {isError && !isLoading && (
        <ErrorState message={ui('failedToLoadYourCoursesPleaseTryAgain')} onRetry={() => refetch()} />
      )}

      {/* Empty */}
      {!isLoading && !isError && activeCourses.length === 0 && <EmptyCoursesCard />}

      {/* Course grid */}
      {!isLoading && !isError && activeCourses.length > 0 && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {activeCourses.map((course) => (
            <CourseCard
              key={course.id}
              title={course.title}
              thumbnail={course.thumbnail}
              href={course.href}
              progress={course.progress}
              lessonsLeft={course.lessonsLeft}
            />
          ))}
        </div>
      )}
    </div>
  )
}
