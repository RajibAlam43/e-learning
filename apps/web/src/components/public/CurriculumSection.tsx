'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import {
  ChevronDown,
  FileText,
  Lock,
  PlayCircle,
  Radio,
  ClipboardList,
  HelpCircle,
} from 'lucide-react'
import { formatLessonDuration } from '@/lib/video-embed'
import type { components } from '@/types/api'

type Section = components['schemas']['CourseSectionResponse']
type Lesson = components['schemas']['LessonSummaryResponse']
type LessonType = NonNullable<Lesson['lessonType']>
type SectionItem = components['schemas']['CourseSectionItemResponse']
type DisplayItem = Pick<SectionItem, 'itemId' | 'position'> & {
  lesson?: Lesson
  liveClass?: NonNullable<SectionItem['liveClass']>
}

const LESSON_TYPE_ICONS: Record<LessonType, typeof PlayCircle> = {
  VIDEO: PlayCircle,
  QUIZ: HelpCircle,
  LIVE: Radio,
  ASSIGNMENT: ClipboardList,
  PDF: FileText,
}

interface CurriculumSectionProps {
  section: Section
  /** Zero-based index, used for the display number and the stagger delay. */
  index: number
  defaultExpanded?: boolean
  onPreview: (lesson: Lesson) => void
}

export function CurriculumSection({
  section,
  index,
  defaultExpanded = false,
  onPreview,
}: CurriculumSectionProps) {
  const t = useTranslations('courses.detail.curriculumTitle')
  const [expanded, setExpanded] = useState(defaultExpanded)

  const items: DisplayItem[] = section.items?.length
        ? section.items
        .slice()
        .sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
        // Quizzes are available to enrolled students from the learning area,
        // but are intentionally not disclosed on public course pages.
        .filter((item): item is SectionItem & DisplayItem => Boolean(item.lesson || item.liveClass) && item.itemType !== 'QUIZ')
    : (section.lessons ?? [])
        .filter((lesson) => lesson.lessonType !== 'QUIZ')
        .map((lesson) => ({ itemType: 'LESSON' as const, position: lesson.position, lesson }))
  const itemCount = items.length
  const hasItems = itemCount > 0
  const num = String(section.position ?? index + 1).padStart(2, '0')
  const panelId = `curriculum-panel-${section.id ?? index}`

  return (
    <li
      className="w-full overflow-hidden rounded-xl bg-white opacity-0 animate-fade-in-up transition-shadow duration-300 hover:shadow-md"
      style={{ animationDelay: `${index * 80}ms` }}
    >
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        aria-expanded={expanded}
        aria-controls={hasItems ? panelId : undefined}
        disabled={!hasItems}
        className="flex w-full items-center gap-6 p-6 text-left max-[450px]:gap-4 max-[450px]:p-4 enabled:cursor-pointer disabled:cursor-default focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-[#3b82f6]"
      >
        <span
          aria-hidden="true"
          className="mt-[-1px] whitespace-nowrap font-heading text-[32px] font-medium leading-8 tracking-[-1px] text-[#d4d4d4] max-[450px]:text-[24px] max-[450px]:leading-6"
        >
          {num}
        </span>

        <span className="flex min-w-0 flex-1 flex-col gap-1">
          <span className="font-sans text-lg font-normal leading-6 text-[#1c3873] max-[450px]:text-base max-[450px]:leading-5">
            {section.title}
          </span>
          {section.description && (
            <span className="font-sans text-sm font-normal leading-[150%] text-[#5e5f5c] max-[450px]:text-xs">
              {section.description}
            </span>
          )}
          {hasItems && (
            <span className="font-sans text-xs font-normal leading-4 text-[#999]">
              {itemCount} {t('lessons')}
            </span>
          )}
        </span>

        {hasItems && (
          <ChevronDown
            aria-hidden="true"
            className={`h-5 w-5 shrink-0 text-[#5e5f5c] transition-transform duration-200 ${
              expanded ? 'rotate-180' : ''
            }`}
          />
        )}
      </button>

      {hasItems && expanded && (
        <ol id={panelId} className="m-0 list-none border-t border-[#ededed] p-0">
          {items.map((item, lessonIdx) => {
            const lesson = item.lesson
            const liveClass = item.liveClass
            if (!lesson) {
              const title = liveClass?.title ?? 'Live class'
              const details = liveClass?.startsAt ? formatDateTime(liveClass.startsAt) : 'Live class'
              return (
                <li key={item.itemId ?? `${panelId}-${lessonIdx}`} className="border-b border-[#f5f5f5] last:border-b-0">
                  <div className="flex w-full items-center gap-3 px-6 py-3 max-[450px]:px-4">
                    <Radio aria-hidden="true" className="h-5 w-5 shrink-0 text-[#a3a3a3]" />
                    <span className="min-w-0 flex-1 truncate font-sans text-sm text-[#1c3873]">{title}</span>
                    <Lock aria-hidden="true" className="h-4 w-4 shrink-0 text-[#a3a3a3]" />
                    <span className="shrink-0 font-sans text-xs text-[#999]">{details}</span>
                  </div>
                </li>
              )
            }
            const isFree = lesson.isPreviewFree === true
            const type = lesson.lessonType
            const Icon = isFree ? PlayCircle : type ? LESSON_TYPE_ICONS[type] : PlayCircle
            const duration = formatLessonDuration(lesson.durationSeconds)

            const rowInner = (
              <>
                <Icon
                  aria-hidden="true"
                  className={`h-5 w-5 shrink-0 ${isFree ? 'text-[#3b82f6]' : 'text-[#a3a3a3]'}`}
                />
                <span className="min-w-0 flex-1 truncate font-sans text-sm text-[#1c3873]">
                  {lesson.title}
                </span>
                {isFree ? (
                  <span className="shrink-0 rounded bg-[#f4f9ff] px-2 py-0.5 font-sans text-xs font-medium text-[#3b82f6]">
                    {t('freePreview')}
                  </span>
                ) : (
                  <Lock aria-hidden="true" className="h-4 w-4 shrink-0 text-[#a3a3a3]" />
                )}
                {duration && (
                  <span className="shrink-0 font-sans text-xs tabular-nums text-[#999]">
                    {duration}
                  </span>
                )}
              </>
            )

            return (
              <li
                key={item.itemId ?? lesson.id ?? `${panelId}-${lessonIdx}`}
                className="border-b border-[#f5f5f5] last:border-b-0"
              >
                {isFree ? (
                  <button
                    type="button"
                    onClick={() => onPreview(lesson)}
                    className="flex w-full cursor-pointer items-center gap-3 px-6 py-3 text-left transition-colors duration-200 hover:bg-[#f9fbff] focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-[#3b82f6] max-[450px]:px-4"
                  >
                    {rowInner}
                  </button>
                ) : (
                  <div
                    className="flex w-full items-center gap-3 px-6 py-3 max-[450px]:px-4"
                    title={t('lockedHint')}
                  >
                    {rowInner}
                    <span className="sr-only">{t('locked')}</span>
                  </div>
                )}
              </li>
            )
          })}
        </ol>
      )}
    </li>
  )
}

function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Scheduled' : date.toISOString().slice(0, 16).replace('T', ' ')
}
