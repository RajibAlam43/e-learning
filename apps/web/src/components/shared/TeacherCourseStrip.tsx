'use client'

import { useTranslations } from 'next-intl'

import Image from 'next/image'
import React from 'react'
import type { StudentCourseSummary } from '@/types'
import { Video } from 'lucide-react'

interface TeacherCourseStripProps {
  course: StudentCourseSummary
}

const TeacherCourseStrip = ({ course }: TeacherCourseStripProps) => {
  const ui = useTranslations('privateUi')

  return (
    <div className='flex flex-row justify-between px-3 xl:px-6 py-4 bg-white rounded-xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] items-center'>
      <div className='w-3/5 flex flex-row items-center gap-3'>
        <Image src='/images/play-btn.png' alt={ui('play')} width={40} height={40} className='rounded-lg' />
        <div className='flex flex-col gap-1'>
          <h3 className='text-sm font-medium font-switzer leading-4 text-gray-500 truncate'>
            {course.courseName}
          </h3>
          <p className='text-gray-400 text-xs leading-4 font-switzer font-normal truncate'>
            {course.instructorName}
          </p>
        </div>
      </div>

      {/* Action Button */}
      <div className='w-2/5 flex flex-col xl:flex-row justify-end items-center gap-y-1 xl:gap-y-0 gap-4 xl:gap-8'>
        <button className="h-9 px-5 py-2.5 bg-linear-290 from-indigo-500 via-indigo-700 via 60% to-blue-800 rounded-md shadow-[0px_4px_12px_0px_rgba(63,79,194,0.38)] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.14)] shadow-[inset_0px_1px_0px_0px_rgba(255,255,255,0.12)] shadow-[inset_0px_-1px_0px_0px_rgba(0,0,0,0.18)] inline-flex justify-center items-center gap-2 text-white text-sm font-medium font-switzer leading-5 cursor-pointer transition-opacity hover:opacity-90">
          <Video className="w-4 h-4" />
          {ui('joinClass')}</button>
      </div>
    </div>
  )
}

export default TeacherCourseStrip
