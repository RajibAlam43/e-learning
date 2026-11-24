'use client'

import { useTranslations } from 'next-intl'

import React, { use, useMemo, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { useCourseHome, useCourseLiveClasses, useLessonContent, useLessonPlayback, useCompleteLesson } from '@/hooks/useStudentDashboard'
import LessonOverviewSection from '@/components/student/course/LessonOverviewSection'
import CourseContentSidebar from '@/components/student/course/CourseContentSidebar'
import CourseQuiz from '@/components/student/course/CourseQuiz'
import LiveClassOverviewSection from '@/components/student/course/LiveClassOverviewSection'
import type { components } from '@/types/api'
import type { StudentLiveClassSummary } from '@/types'
import { findStudentCourseItem, getStudentSectionItems } from '@/lib/student-course-items'

type StudentSectionHome = components['schemas']['StudentSectionHomeResponse']

function findFirstAccessibleItem(sections: StudentSectionHome[]): string | null {
  for (const section of sections) {
    for (const item of getStudentSectionItems(section)) {
      const isAccessible = item.itemType === 'QUIZ'
        ? item.quiz.isAccessible !== false
        : item.itemType === 'LIVE_CLASS'
          ? item.liveClass.status !== 'CANCELLED' && item.liveClass.status !== 'FAILED'
          : item.lesson.isAccessible !== false
      const isCompleted = item.itemType === 'QUIZ'
        ? item.quiz.completed
        : item.itemType === 'LIVE_CLASS'
          ? item.liveClass.completed || item.liveClass.status === 'COMPLETED'
          : item.lesson.completed

      if (!isCompleted && isAccessible) {
        return item.itemId
      }
    }
  }

  for (const section of sections) {
    for (const item of getStudentSectionItems(section)) {
      const isAccessible = item.itemType === 'QUIZ'
        ? item.quiz.isAccessible !== false
        : item.itemType === 'LIVE_CLASS'
          ? item.liveClass.status !== 'CANCELLED' && item.liveClass.status !== 'FAILED'
          : item.lesson.isAccessible !== false

      if (isAccessible) {
        return item.itemId
      }
    }
  }
  return null
}

export default function CourseLearnPage({ params }: { params: Promise<{ courseId: string }> }) {
  const ui = useTranslations('privateUi')

  const { courseId } = use(params)
  const searchParams = useSearchParams()
  const queryLessonId = searchParams.get('lessonId')

  const { data: course, isLoading: courseLoading } = useCourseHome(courseId)
  const { data: courseLiveClasses } = useCourseLiveClasses(courseId)

  const sections = course?.sections ?? []
  const requestedCourseItem = findStudentCourseItem(sections as StudentSectionHome[], queryLessonId)
  const activeLessonId = requestedCourseItem
    ? queryLessonId
    : findFirstAccessibleItem(sections as StudentSectionHome[])
  const activeCourseItem = requestedCourseItem ?? findStudentCourseItem(sections as StudentSectionHome[], activeLessonId)
  const isQuiz = activeCourseItem?.item.itemType === 'QUIZ'
    || (activeCourseItem?.item.itemType === 'LESSON' && activeCourseItem.item.lesson.lessonType === 'QUIZ')
  const isLiveClass = activeCourseItem?.item.itemType === 'LIVE_CLASS'
  const activeContentLessonId = isQuiz || isLiveClass ? null : activeLessonId

  const activeLiveClass = useMemo<(StudentLiveClassSummary & { provider?: string }) | undefined>(() => {
    if (!isLiveClass || !activeCourseItem || activeCourseItem.item.itemType !== 'LIVE_CLASS') return undefined
    const liveClassId = activeCourseItem.item.liveClass.liveClassId ?? activeCourseItem.item.itemId
    const listedLiveClass = courseLiveClasses?.find((liveClass) => liveClass.liveClassId === liveClassId)
    if (listedLiveClass) return listedLiveClass

    const liveClass = activeCourseItem.item.liveClass as typeof activeCourseItem.item.liveClass & {
      expectedDurationMinutes?: number
      durationMinutes?: number
    }
    return {
      liveClassId,
      title: liveClass.title,
      description: liveClass.description,
      startsAt: liveClass.startsAt,
      endsAt: liveClass.endsAt,
      durationMinutes: liveClass.durationMinutes ?? liveClass.expectedDurationMinutes,
      provider: liveClass.provider,
      courseId,
      status: liveClass.status,
      hasRecording: false,
      canJoin: liveClass.status === 'LIVE',
    }
  }, [activeCourseItem, courseId, courseLiveClasses, isLiveClass])

  const { data: lesson, isLoading: lessonLoading } = useLessonContent(activeContentLessonId, courseId)
  const { data: playback, isLoading: playbackLoading } = useLessonPlayback(activeContentLessonId, courseId)
  const lessonIsLoading = lessonLoading || playbackLoading

  const { mutate: completeLesson, isPending: isMarkingComplete } = useCompleteLesson(courseId)
  const [expanded, setExpanded] = useState(false)

  const quizId = isQuiz && activeCourseItem
    ? activeCourseItem.item.itemType === 'QUIZ'
      ? activeCourseItem.item.quiz.quizId ?? activeCourseItem.item.itemId
      : activeCourseItem.item.itemType === 'LESSON'
        ? activeCourseItem.item.lesson.lessonId ?? activeCourseItem.item.itemId
        : null
    : null
  const moduleName = activeCourseItem?.section.position
    ? `Module ${activeCourseItem.section.position}`
    : ''

  if (!course && courseLoading) {
    return (
      <div className='flex flex-col lg:flex-row gap-6'>
        <div className='flex-1 min-w-0 flex flex-col gap-6'>
          <div className='h-5 w-64 bg-gray-100 rounded animate-pulse' />
          <div className='h-9 w-80 bg-gray-100 rounded animate-pulse' />
          <div className='w-full aspect-video bg-gray-100 rounded-2xl animate-pulse' />
          <div className='h-48 bg-gray-100 rounded-2xl animate-pulse' />
        </div>
        <div className='w-full lg:w-96 shrink-0'>
          <div className='bg-white border border-blue-100 rounded-2xl p-6 flex flex-col gap-4'>
            <div className='h-5 w-36 bg-gray-100 rounded animate-pulse' />
            <div className='h-2 bg-gray-100 rounded-full animate-pulse' />
            {[1, 2, 3].map((i) => (
              <div key={i} className='h-14 bg-gray-100 rounded-xl animate-pulse' />
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (!course) {
    return (
      <div className='flex items-center justify-center py-20'>
        <p className='text-[#5e5f5c]'>{ui('courseNotFound')}</p>
      </div>
    )
  }

  return (
    <div className='flex min-h-0 flex-col items-start lg:flex-row gap-6 -m-5 lg:-m-0'>
      {/* Left — Lesson, Quiz, or Results */}
      <div className={`flex-1 min-w-0 transition-all duration-300 ${expanded ? 'w-full' : ''}`}>
        {isLiveClass && activeLiveClass ? (
          <LiveClassOverviewSection
            liveClass={activeLiveClass}
            courseName={course.courseName ?? ''}
            moduleName={moduleName}
          />
        ) : isQuiz && quizId ? (
          <CourseQuiz key={quizId} courseId={courseId} quizId={quizId}
            courseName={course.courseName ?? ''} moduleName={moduleName} />
        ) : (
          <LessonOverviewSection
            course={course}
            lesson={lesson}
            playback={playback}
            isLoading={lessonIsLoading}
            isMarkingComplete={isMarkingComplete}
            onMarkComplete={() => completeLesson(activeLessonId ?? '')}
            isExpanded={expanded}
            onToggleExpand={() => setExpanded((e) => !e)}
            instructorName={undefined}
            instructorBio={undefined}
            level={undefined}
          />
        )}
      </div>

      {/* Right — Course content sidebar */}
      {(!expanded || isQuiz) && (
        <div className='h-[70dvh] min-h-0 w-full shrink-0 lg:sticky lg:top-5 lg:h-[calc(100dvh-2.5rem)] lg:w-96 lg:self-start'>
          <CourseContentSidebar
            course={course}
            activeLessonId={activeLessonId}
          />
        </div>
      )}
    </div>
  )
}
