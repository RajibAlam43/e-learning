'use client'

import { useTranslations } from 'next-intl'

import Link from 'next/link'
import Image from 'next/image'
import { useInstructorDashboard } from '@/hooks/useInstructorDashboard'
import { Skeleton } from '@/components/ui/skeleton'
import { ErrorState } from '@/components/shared/ErrorState'
import { EmptyState } from '@/components/shared/EmptyState'
import { BookOpen } from 'lucide-react'

export default function InstructorCoursesPage() {
  const ui = useTranslations('privateUi')

  const { data: dashboard, isLoading, isError, refetch } = useInstructorDashboard()
  const courses = dashboard?.assignedCourses ?? []

  return (
    <div className='flex flex-col gap-6'>
      <div className='flex flex-col gap-1'>
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('myCourses')}</h1>
        <p className='text-sm text-[#5e5f5c]'>
          {!isLoading && !isError ? `${courses.length} courses assigned` : ''}
        </p>
      </div>

      {isLoading && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className='h-48 rounded-2xl' />
          ))}
        </div>
      )}

      {isError && !isLoading && (
        <ErrorState message={ui('failedToLoadYourCoursesPleaseTryAgain')} onRetry={() => refetch()} />
      )}

      {!isLoading && !isError && courses.length === 0 && (
        <EmptyState
          icon={<BookOpen className='h-10 w-10' />}
          title={ui('noCoursesAssigned')}
          description={ui('coursesAssignedToYouWillAppearHere')}
        />
      )}

      {!isLoading && !isError && courses.length > 0 && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {courses.map((c) => (
            <Link
              key={c.courseId}
              href={c.courseSlug ? `/courses/${encodeURIComponent(c.courseSlug)}` : '/courses'}
              className='bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] overflow-hidden hover:shadow-lg transition-shadow'
            >
              <div className='relative h-32 bg-gray-100'>
                <Image
                  src='/images/course-quran.jpg'
                  alt={c.courseName ?? ui('course')}
                  fill
                  className='object-cover'
                  sizes='(max-width: 768px) 100vw, 33vw'
                />
              </div>
              <div className='p-4 flex flex-col gap-2'>
                <h3 className='text-base font-medium text-[#1c3873] truncate'>
                  {c.courseName ?? ui('untitledCourse')}
                </h3>
                <div className='flex items-center gap-3 text-xs text-[#71717a]'>
                  <span>{c.totalEnrolledStudents ?? 0} {ui('students')}</span>
                  <span>{c.totalLessons ?? 0} {ui('lessons')}</span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
