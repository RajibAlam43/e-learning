'use client'

import { useTranslations } from 'next-intl'

import ModuleAccordion from './ModuleAccordion'
import type { components } from '@/types/api'

type StudentCourseHome = components['schemas']['StudentCourseHomeResponse']
type StudentSectionHome = components['schemas']['StudentSectionHomeResponse']

interface CourseContentSidebarProps {
  course: StudentCourseHome
  activeLessonId: string | null
  isLoading?: boolean
}

const CourseContentSidebar = ({ course, activeLessonId, isLoading }: CourseContentSidebarProps) => {
  const ui = useTranslations('privateUi')

  if (isLoading) {
    return (
      <div className='flex h-[70dvh] min-h-0 w-full flex-col rounded-2xl border border-blue-100 bg-white lg:h-full lg:max-h-none lg:rounded-none lg:border-y-0 lg:border-r-0'>
        <div className='p-6 border-b border-blue-100 flex flex-col gap-4'>
          <div className='h-5 w-36 bg-gray-100 rounded animate-pulse' />
          <div className='flex justify-between'>
            <div className='h-4 w-20 bg-gray-100 rounded animate-pulse' />
            <div className='h-4 w-20 bg-gray-100 rounded animate-pulse' />
          </div>
          <div className='h-2 bg-gray-100 rounded-full animate-pulse' />
        </div>
        <div className='p-4 flex flex-col gap-4'>
          {[1, 2, 3].map((i) => (
            <div key={i} className='h-14 bg-gray-100 rounded-xl animate-pulse' />
          ))}
        </div>
      </div>
    )
  }

  const sections = course.sections ?? []
  const totalLessons = course.totalItems ?? course.totalLessons ?? 0
  const completedLessons = course.completedItems ?? course.completedLessons ?? 0
  const percentage = course.completionPercentage ?? 0

  return (
    <div className='flex h-[70dvh] min-h-0 w-full flex-col overflow-hidden rounded-2xl border border-blue-100 bg-white lg:h-full lg:max-h-none lg:rounded-none lg:border-y-0 lg:border-r-0'>
      {/* Header + Progress */}
      <div className='p-6 border-t border-b border-blue-100 flex flex-col gap-4'>
        <h2 className='text-lg font-semibold font-heading text-[#1c3873]'>
          {ui('courseContent')}</h2>
        <div className='flex flex-col gap-2'>
          <div className='flex justify-between items-center'>
            <span className='text-sm text-[#71717a]'>{percentage}{ui('complete')}</span>
            <span className='text-sm font-medium text-[#1c3873]'>{completedLessons}/{totalLessons} {ui('items')}</span>
          </div>
          <div className='w-full h-2 bg-blue-100 rounded-full overflow-hidden'>
            <div
              className='h-full bg-[#3b82f6] rounded-full transition-all duration-500'
              style={{ width: `${percentage}%` }}
            />
          </div>
        </div>
      </div>

      {/* Module list */}
      <nav className='flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto overscroll-contain touch-pan-y p-4 [scrollbar-gutter:stable]'>
        {sections.map((section: StudentSectionHome) => (
          <ModuleAccordion
            key={section.sectionId}
            section={section}
            activeLessonId={activeLessonId}
            courseId={course.courseId ?? ''}
          />
        ))}
      </nav>
    </div>
  )
}

export default CourseContentSidebar
