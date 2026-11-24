'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { CurriculumSection } from '@/components/public/CurriculumSection'
import { LessonPreviewDialog } from '@/components/public/LessonPreviewDialog'
import type { components } from '@/types/api'

type Section = components['schemas']['CourseSectionResponse']
type Lesson = components['schemas']['LessonSummaryResponse']

interface CurriculumStructureProps {
  sections?: Section[]
}

export function CurriculumStructure({ sections }: CurriculumStructureProps) {
  const t = useTranslations('courses.detail')
  const [previewLesson, setPreviewLesson] = useState<Lesson | null>(null)

  const safeSections = sections ?? []
  if (safeSections.length === 0) return null

  return (
    <section
      aria-labelledby="curriculum-structure-heading"
      className="w-full bg-[#f5f5f5] py-20 max-[450px]:py-12"
    >
      <div className="container mx-auto flex flex-col items-start px-5">
        <div className="flex w-full flex-col items-center justify-center gap-6">
          <header className="flex w-full flex-col items-start">
            <h2
              id="curriculum-structure-heading"
              className="mt-[-1px] font-heading text-[36px] font-normal leading-[44px] tracking-[0px] text-[#1c3873] max-[450px]:text-[28px] max-[450px]:leading-[34px]"
            >
              {t('curriculumTitle.title')}
            </h2>
          </header>

          <ol className="m-0 flex w-full list-none flex-col items-start gap-2 p-0">
            {safeSections.map((section, idx) => (
              <CurriculumSection
                key={section.id ?? `${section.title}-${idx}`}
                section={section}
                index={idx}
                // Open the first section so the lesson list is discoverable
                // without requiring a click.
                defaultExpanded={idx === 0}
                onPreview={setPreviewLesson}
              />
            ))}
          </ol>
        </div>
      </div>

      <LessonPreviewDialog
        lesson={previewLesson}
        onClose={() => setPreviewLesson(null)}
      />
    </section>
  )
}
