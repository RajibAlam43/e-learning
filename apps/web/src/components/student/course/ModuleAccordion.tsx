'use client'

import { useTranslations } from 'next-intl'

import { ChevronDown } from 'lucide-react'
import { useState } from 'react'
import LessonItem from './LessonItem'
import type { components } from '@/types/api'
import { getStudentSectionItems } from '@/lib/student-course-items'

type StudentSectionHome = components['schemas']['StudentSectionHomeResponse']

interface ModuleAccordionProps {
  section: StudentSectionHome
  activeLessonId: string | null
  courseId: string
  defaultExpanded?: boolean
}

const ModuleAccordion = ({ section, activeLessonId, courseId, defaultExpanded = false }: ModuleAccordionProps) => {
  const ui = useTranslations('privateUi')

  const [expanded, setExpanded] = useState(defaultExpanded)

  const items = getStudentSectionItems(section)
  const hasActiveItem = items.some((item) => item.itemId === activeLessonId)
  const isExpanded = expanded || hasActiveItem

  const completedCount = items.filter((item) => {
    if (item.itemType === 'LESSON') return item.lesson.completed
    if (item.itemType === 'QUIZ') return item.quiz.completed
    return item.liveClass.completed || item.liveClass.status === 'COMPLETED'
  }).length

  return (
    <div className='shrink-0 rounded-xl border border-blue-100 overflow-hidden bg-white'>
      {/* Header */}
      <button
        onClick={() => setExpanded((prev) => !prev)}
        aria-expanded={isExpanded}
        className={`flex w-full items-center justify-between gap-4 p-4 text-left transition-colors ${
          hasActiveItem ? 'bg-blue-50' : 'hover:bg-gray-50'
        }`}
      >
        <div className='min-w-0 flex-1 flex flex-col gap-1'>
          <span className='text-sm font-medium text-[#1c3873]'>
            {section.sectionTitle}
          </span>
          <span className='text-xs text-[#5e5f5c]'>
            {completedCount}/{items.length} {ui('completed3')}</span>
        </div>
        <span className='grid h-6 w-6 shrink-0 place-items-center' aria-hidden='true'>
          <ChevronDown
            className={`h-4 w-4 text-[#5e5f5c] transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}
          />
        </span>
      </button>

      {/* Lessons */}
      {isExpanded && (
        <div className='border-t border-blue-100'>
          {items.map((item) => (
            <LessonItem
              key={`${item.itemType}-${item.itemId}`}
              item={item}
              isActive={item.itemId === activeLessonId}
              courseId={courseId}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export default ModuleAccordion
