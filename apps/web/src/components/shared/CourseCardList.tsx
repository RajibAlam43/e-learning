import CourseCard from './CourseCard'
import Link from 'next/link'
import React from 'react'

interface CourseCardListProps {
  title: string
  viewAllHref?: string
  courses: {
    title: string
    thumbnail: string
    href: string
    nextModule?: string
    progress?: number
    lessonsLeft?: number
    badge?: {
      text: string
      variant: 'popular' | 'short'
    }
  }[]
  isLoading?: boolean
}

const CourseCardList = ({ title, viewAllHref, courses, isLoading }: CourseCardListProps) => {
  if (isLoading) {
    return (
      <div className='w-full flex flex-col gap-4'>
        <div className='flex justify-between items-center h-6'>
          <div className='h-6 w-32 bg-gray-100 rounded animate-pulse' />
          <div className='h-4 w-16 bg-gray-100 rounded animate-pulse' />
        </div>
        <div className='flex gap-3'>
          {[1, 2, 3].map((i) => (
            <div key={i} className='w-56 shrink-0 h-56 rounded-2xl bg-gray-100 animate-pulse' />
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className='w-full flex flex-col gap-4'>
      {/* Header */}
      <div className='flex justify-between items-center h-6'>
        <h2 className='text-lg font-semibold font-switzer leading-6 text-[#1c3873]'>
          {title}
        </h2>
        {viewAllHref && (
          <Link
            href={viewAllHref}
            className='text-sm font-medium font-switzer leading-4 text-[#1c3873] transition-opacity duration-200 hover:opacity-70'
          >
            View all
          </Link>
        )}
      </div>

      {/* Cards */}
      <div className='flex gap-3 overflow-x-auto pb-2 scrollbar-thin'>
        {courses.map((course) => (
          <CourseCard key={course.title} {...course} />
        ))}
      </div>
    </div>
  )
}

export default CourseCardList
