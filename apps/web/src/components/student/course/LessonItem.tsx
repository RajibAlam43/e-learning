'use client'

import { useTranslations } from 'next-intl'

import Link from 'next/link'
import { CalendarDays, CheckCircle2, FileQuestion, Lock, Play, Video } from 'lucide-react'
import type { StudentCourseItem } from '@/lib/student-course-items'

interface LessonItemProps {
  item: StudentCourseItem
  isActive: boolean
  courseId: string
}

const LessonItem = ({ item, isActive, courseId }: LessonItemProps) => {
  const ui = useTranslations('privateUi')

  const title = item.itemType === 'QUIZ'
    ? item.quiz.quizTitle
    : item.itemType === 'LIVE_CLASS'
      ? item.liveClass.title
      : item.lesson.lessonTitle
  const isLocked = item.itemType === 'QUIZ' ? item.quiz.isAccessible === false : false
  const isCompleted = item.itemType === 'QUIZ'
    ? item.quiz.completed
    : item.itemType === 'LIVE_CLASS'
      ? item.liveClass.completed || item.liveClass.status === 'COMPLETED'
      : item.lesson.completed
  const subtitle = item.itemType === 'QUIZ'
    ? item.quiz.timeLimitSec
      ? `${Math.ceil(item.quiz.timeLimitSec / 60)} min quiz`
      : 'Knowledge check'
    : item.itemType === 'LIVE_CLASS'
      ? item.liveClass.status === 'LIVE'
        ? 'Live now'
        : item.liveClass.status === 'COMPLETED'
          ? 'Recording available in Live Classes'
          : item.liveClass.startsAt
            ? `Starts ${formatDateTime(item.liveClass.startsAt)}`
            : 'Live class'
      : item.lesson.durationLabel
  const Icon = item.itemType === 'QUIZ'
    ? FileQuestion
    : item.itemType === 'LIVE_CLASS'
      ? item.liveClass.status === 'LIVE' ? Video : CalendarDays
      : Play
  const href = `/student/courses/${courseId}?lessonId=${item.itemId}`

  // Locked item
  if (isLocked) {
    return (
      <div className='flex items-center gap-4 px-6 py-4 opacity-50'>
        <div className='flex items-center justify-center w-5 h-5'>
          <Lock className='w-4 h-4 text-[#a3a3a3]' />
        </div>
        <div className='flex flex-col gap-1 flex-1'>
          <span className='text-sm text-[#a3a3a3]'>{title ?? ui('untitledItem')}</span>
          <span className='text-xs text-[#a3a3a3]'>{subtitle ?? ui('locked')}</span>
        </div>
      </div>
    )
  }

  // Active item
  if (isActive) {
    return (
      <Link
        href={href}
        className='flex items-center gap-4 px-6 py-4 bg-[#1d4ed8] transition-colors'
      >
        <div className='flex items-center justify-center w-5 h-5'>
          <Icon className={`w-4 h-4 text-white ${item.itemType === 'LESSON' ? 'fill-white' : ''}`} />
        </div>
        <div className='flex flex-col gap-1 flex-1'>
          <span className='text-sm font-medium text-white'>{title ?? ui('untitledItem')}</span>
          <span className='text-xs text-white/80'>{subtitle ?? ui('nowPlaying')}</span>
        </div>
        <div className='w-1.5 h-1.5 rounded-full bg-blue-700' />
      </Link>
    )
  }

  // Completed item
  if (isCompleted) {
    return (
      <Link
        href={href}
        className='flex items-center gap-4 px-6 py-4 hover:bg-gray-50 transition-colors'
      >
        <div className='flex items-center justify-center w-5 h-5'>
          <CheckCircle2 className='w-5 h-5 text-green-500' />
        </div>
        <div className='flex flex-col gap-1 flex-1'>
          <span className='text-sm text-[#1c3873]'>{title ?? ui('untitledItem')}</span>
          <span className='text-xs text-[#5e5f5c]'>{subtitle ?? ui('completed2')}</span>
        </div>
      </Link>
    )
  }

  // Normal accessible item
  return (
    <Link
      href={href}
      className='flex items-center gap-4 px-6 py-4 hover:bg-gray-50 transition-colors'
    >
      <div className='flex items-center justify-center w-5 h-5'>
        <Icon className={`w-4 h-4 text-[#1c3873] ${item.itemType === 'LESSON' ? 'fill-[#1c3873]' : ''}`} />
      </div>
      <div className='flex flex-col gap-1 flex-1'>
        <span className='text-sm text-[#1c3873]'>{title ?? ui('untitledItem')}</span>
        <span className='text-xs text-[#5e5f5c]'>{subtitle ?? ''}</span>
      </div>
    </Link>
  )
}

function formatDateTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'soon'
  return date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export default LessonItem
