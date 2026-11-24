'use client'

import { useTranslations } from 'next-intl'

import type { components } from '@/types/api'

type LessonContent = components['schemas']['LessonContentResponse']

interface LessonInfoCardProps {
  lesson: LessonContent | undefined
  isLoading: boolean
  isMarkingComplete: boolean
  isCompleted: boolean
  onMarkComplete: () => void
  moduleName?: string
  level?: string
  instructorName?: string
  instructorBio?: string
}

export default function LessonInfoCard({
  lesson,
  isLoading,
  isMarkingComplete,
  isCompleted,
  onMarkComplete,
  moduleName,
  level,
  instructorName,
  instructorBio,
}: LessonInfoCardProps) {
  const ui = useTranslations('privateUi')

  if (isLoading) {
    return (
      <article className="flex flex-col items-start gap-6 md:gap-8 p-4 md:p-8 bg-white rounded-2xl border border-solid border-blue-100 animate-pulse">
        <header className="flex flex-col md:flex-row items-start md:items-center gap-4 md:gap-8 self-stretch w-full">
          <div className="flex items-center gap-4 flex-1 flex-wrap">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-3 w-20 bg-gray-100 rounded" />
            ))}
          </div>
          <div className="h-11 w-40 bg-gray-100 rounded-md" />
        </header>
        <div className="self-stretch space-y-2">
          <div className="h-4 w-full bg-gray-100 rounded" />
          <div className="h-4 w-3/4 bg-gray-100 rounded" />
        </div>
        <div className="self-stretch h-px bg-gray-100" />
        <div className="space-y-2">
          <div className="h-3 w-24 bg-gray-100 rounded" />
          <div className="h-4 w-32 bg-gray-100 rounded" />
        </div>
      </article>
    )
  }

  if (!lesson) return null

  const durationMin = lesson.durationSeconds ? Math.round(lesson.durationSeconds / 60) : null

  const metaItems = [
    ...(durationMin ? [{ id: 'duration', icon: <ClockIcon />, label: `${durationMin} min` }] : []),
    ...(level ? [{ id: 'level', icon: <LevelIcon />, label: level }] : []),
    ...(moduleName ? [{ id: 'module', icon: <BookIcon />, label: moduleName }] : []),
  ]

  return (
    <article className="flex flex-col items-start gap-6 md:gap-8 p-4 md:p-8 bg-white rounded-2xl border border-solid border-blue-100">
      {/* Header: meta items + mark complete button */}
      <header className="flex flex-col md:flex-row items-start md:items-center gap-4 md:gap-8 self-stretch w-full">
        <ul className="flex items-center gap-4 flex-1 list-none m-0 p-0" aria-label={ui('lessonDetails')}>
          {metaItems.map((item) => (
            <li key={item.id} className="inline-flex items-center gap-1">
              <div className="inline-flex flex-col items-start" aria-hidden="true">
                {item.icon}
              </div>
              <span className="flex items-center w-fit -mt-px text-xs text-[#3b82f6] tracking-normal leading-4 whitespace-nowrap">
                {item.label}
              </span>
            </li>
          ))}
        </ul>

        <button
          type="button"
          aria-pressed={isCompleted}
          aria-label={isCompleted ? ui('lessonMarkedAsComplete') : ui('markLessonAsComplete')}
          onClick={onMarkComplete}
          disabled={isMarkingComplete}
          className="inline-flex w-full md:w-auto h-11 items-center justify-center gap-2 px-3 md:px-5 py-2.5 rounded-md shadow-[inset_0px_1px_0px_#ffffff1f,inset_0px_-1px_0px_#0000002e] bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3b82f6] disabled:opacity-50 cursor-pointer"
        >
          <DoneRoundedIcon />
          <span className="flex items-center w-fit text-sm font-medium text-white tracking-normal leading-5 whitespace-nowrap">
            {isCompleted ? ui('completed2') : isMarkingComplete ? ui('completing') : ui('markAsComplete')}
          </span>
        </button>
      </header>

      {/* Description */}
      {lesson.description && (
        <section className="flex flex-col items-start self-stretch w-full">
          <p className="flex items-center self-stretch -mt-px text-sm text-[#5e5f5c] leading-[150%]">
            {lesson.description}
          </p>
        </section>
      )}

      {/* Divider */}
      <div className="self-stretch w-full h-px bg-blue-100" aria-hidden="true" />

      {/* Instructor section */}
      {(instructorName || lesson.courseName) && (
        <section className="flex flex-col items-start gap-3">
          <div className="flex items-center w-fit -mt-px text-sm text-[#49608f] whitespace-nowrap leading-4">
            {ui('instructor')}</div>
          <div className="flex flex-col items-start gap-2 self-stretch w-full">
            <h2 className="flex items-center w-fit -mt-px text-base text-[#1b1c1a] leading-[18px] whitespace-nowrap">
              {instructorName ?? lesson.courseName}
            </h2>
            {instructorBio && (
              <div className="inline-flex flex-col items-start">
                <p className="w-fit -mt-px text-sm text-[#a3a3a3] leading-4">
                  {instructorBio}
                </p>
              </div>
            )}
          </div>
        </section>
      )}
    </article>
  )
}

/* ── Figma-matching icons ── */

function ClockIcon() {
  return (
    <svg className="w-[14.25px] h-[14.25px]" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <polyline points="12,6 12,12 16,14" />
    </svg>
  )
}

function BookIcon() {
  return (
    <svg className="w-[13.99px] h-[11.13px]" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
    </svg>
  )
}

function LevelIcon() {
  return (
    <svg className="w-[14.86px] h-[15.32px]" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 20h.01" />
      <path d="M7 20v-4" />
      <path d="M12 20v-8" />
      <path d="M17 20V8" />
      <path d="M22 20V4" />
    </svg>
  )
}

function DoneRoundedIcon() {
  return (
    <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
      <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z" />
    </svg>
  )
}
