'use client'

import { useTranslations } from 'next-intl'

import { FileText, Image as ImageIcon, Download } from 'lucide-react'
import VideoPlayer from './VideoPlayer'
import { Breadcrumb } from '@/components/shared/Breadcrumb'
import LessonInfoCard from './LessonInfoCard'
import type { components } from '@/types/api'

type StudentCourseHome = components['schemas']['StudentCourseHomeResponse']
type LessonContent = components['schemas']['LessonContentResponse']
type MediaPlayback = components['schemas']['MediaPlaybackResponse']

interface LessonOverviewSectionProps {
  course: StudentCourseHome
  lesson: LessonContent | undefined
  playback: MediaPlayback | undefined
  isLoading: boolean
  isMarkingComplete: boolean
  onMarkComplete: () => void
  isExpanded?: boolean
  onToggleExpand?: () => void
  instructorName?: string
  instructorBio?: string
  level?: string
}

const LessonOverviewSection = ({
  course,
  lesson,
  playback,
  isLoading,
  isMarkingComplete,
  onMarkComplete,
  isExpanded,
  onToggleExpand,
  instructorName,
  instructorBio,
  level,
}: LessonOverviewSectionProps) => {
  const ui = useTranslations('privateUi')

  const isCompleted = lesson?.userProgress?.completed ?? false

  const section = lesson?.sectionId
    ? course.sections?.find(item => item.sectionId === lesson.sectionId)
    : undefined
  const moduleName = section ? `Module ${section.position}` : undefined

  return (
    <div className='flex flex-col gap-6'>
      {/* Breadcrumb */}
      <Breadcrumb items={[
        { label: ui('myCourses'), href: '/student/courses' },
        { label: course.courseName ?? '' },
        ...(lesson?.sectionId ? [{ label: ui('currentLesson') }] : []),
      ]} />

      {/* Lesson title */}
      {isLoading ? (
        <div className='h-9 w-64 md:w-80 bg-gray-100 rounded animate-pulse' />
      ) : (
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>
          {lesson?.title ?? ui('loadingLesson')}
        </h1>
      )}

      {/* Video Player */}
      <VideoPlayer
        key={lesson?.lessonId}
        courseId={course.courseId}
        lessonId={lesson?.lessonId}
        initialPosition={lesson?.userProgress?.lastPositionSec}
        playback={playback}
        courseLabel={moduleName ? `${course.courseName} : ${moduleName}` : course.courseName}
        isAccessible={lesson?.isAccessible}
        isLoading={isLoading}
        isExpanded={isExpanded}
        onToggleExpand={onToggleExpand}
      />

      {/* Lesson Info Card */}
      <LessonInfoCard
        lesson={lesson}
        isLoading={isLoading}
        isMarkingComplete={isMarkingComplete}
        isCompleted={isCompleted}
        onMarkComplete={onMarkComplete}
        moduleName={moduleName}
        level={level}
        instructorName={instructorName}
        instructorBio={instructorBio}
      />

      {/* Lesson Resources — only render when the lesson has any */}
      {lesson?.resources && lesson.resources.length > 0 && (
        <section className='flex flex-col gap-3'>
          <h2 className='text-lg font-heading font-medium text-[#1c3873]'>{ui('lessonResources')}</h2>
          <ul className='flex flex-col gap-2'>
            {lesson.resources
              .slice()
              .sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
              .map((r) => {
                const Icon = r.resourceType === 'IMAGE' ? ImageIcon : FileText
                return (
                  <li key={r.resourceId}>
                    <a
                      href={r.downloadUrl || '#'}
                      target='_blank'
                      rel='noopener noreferrer'
                      className='flex items-center gap-3 p-3 bg-white rounded-xl border border-blue-100 hover:border-blue-200 hover:bg-blue-50/50 transition-colors'
                    >
                      <span className='w-9 h-9 rounded-lg bg-blue-50 flex items-center justify-center shrink-0'>
                        <Icon className='w-4 h-4 text-[#3b82f6]' />
                      </span>
                      <span className='flex-1 min-w-0'>
                        <span className='block text-sm font-medium text-[#1b1c1a] truncate'>
                          {r.title || ui('untitledResource')}
                        </span>
                        <span className='block text-xs text-[#5e5f5c]'>
                          {r.mimeType || r.resourceType || ui('file')}
                        </span>
                      </span>
                      <Download className='w-4 h-4 text-[#5e5f5c] shrink-0' />
                    </a>
                  </li>
                )
              })}
          </ul>
        </section>
      )}
    </div>
  )
}

export default LessonOverviewSection
