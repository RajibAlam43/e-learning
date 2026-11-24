'use client'

import { useTranslations } from 'next-intl'

import CourseStrip from './CourseStrip'
import type { StudentCourseSummary } from '@/types'

interface ContinuedCoursesProps {
  courses: StudentCourseSummary[]
  userName?: string
  isLoading?: boolean
}

const MAX_VISIBLE = 3

const stripStyles = [
  { zIndex: 'z-10', opacity: 'opacity-100', maxWidth: '', delay: '' },
  { zIndex: 'z-[9]', opacity: 'opacity-50', maxWidth: 'max-w-[calc(100%-10px)]', delay: '' },
  { zIndex: 'z-[8]', opacity: 'opacity-30', maxWidth: 'max-w-[calc(100%-20px)]', delay: 'delay-150' },
]

const ContinuedCourses = ({ courses, userName = 'Student', isLoading }: ContinuedCoursesProps) => {
  const ui = useTranslations('privateUi')

  const visibleCourses = courses.slice(0, MAX_VISIBLE)

  if (isLoading) {
    return (
      <div className='w-full rounded-2xl p-8 bg-gradient-to-br from-indigo-800/20 to-blue-500/20 animate-pulse'>
        <div className='h-8 w-52 bg-white/20 rounded' />
        <div className='h-5 w-48 bg-white/15 rounded mt-2' />
        <div className='h-20 w-full bg-white/10 rounded-xl mt-6' />
      </div>
    )
  }

  return (
    <div className="group w-full rounded-2xl p-8 shadow-[0px_4px_8px_0px_rgba(20,26,184,0.10),0px_15px_15px_0px_rgba(20,26,184,0.09),0px_34px_20px_0px_rgba(20,26,184,0.05),0px_60px_24px_0px_rgba(20,26,184,0.01)] bg-gradient-to-br from-indigo-800 to-blue-500">
      {/* Header */}
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-medium leading-8 text-white">
          {ui('assalamuAlaikum')}{' '}{userName}
        </h1>
        <p className="text-sm font-normal leading-5 text-white/80 font-switzer">
          {ui('startFromWhereYouLeftOff')}</p>
      </div>

      {/* Stacked strips */}
      {visibleCourses.length > 0 && (
        <div className="relative mt-4 flex flex-col">
          {visibleCourses.map((course, i) => {
            const s = stripStyles[Math.min(i, MAX_VISIBLE - 1)]!
            const isTop = i === 0

            return (
              <div
                key={course.courseId ?? i}
                className={`relative w-full mx-auto transition-[margin,opacity,max-width] duration-700 ease-[cubic-bezier(0.25,0.1,0.25,1)] ${s.zIndex} ${s.opacity} ${s.delay} ${
                  isTop
                    ? ''
                    : `-mt-[95px] xl:-mt-[64px] ${s.maxWidth} group-hover:-mt-0 group-hover:mt-3 group-hover:max-w-none group-hover:opacity-100`
                }`}
              >
                <CourseStrip course={course} />
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default ContinuedCourses
