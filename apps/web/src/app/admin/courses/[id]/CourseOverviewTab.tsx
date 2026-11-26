'use client'

import { useState } from 'react'
import { CoursePrerequisiteEditor } from '@/components/admin/course/CoursePrerequisiteEditor'
import dynamic from 'next/dynamic'
import { Pencil } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { AdminCourseDetail } from '@/types'

// Lazy-load the edit dialog — it's heavy (large form with bilingual fields +
// ThumbnailUploader) and only needed when the user clicks "Edit Course".
const EditCourseDialog = dynamic(
  () => import('./EditCourseDialog').then((m) => m.EditCourseDialog),
  { ssr: false },
)

interface CourseOverviewTabProps {
  course: AdminCourseDetail
}

export function CourseOverviewTab({ course }: CourseOverviewTabProps) {
  const [editOpen, setEditOpen] = useState(false)

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-end">
        <Button
          variant="outline"
          className="gap-1.5"
          onClick={() => setEditOpen(true)}
        >
          <Pencil className="h-4 w-4" />
          Edit Course
        </Button>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-[0px_2px_6px_rgba(16,24,40,0.06)]">
        <div className="grid grid-cols-1 gap-x-10 gap-y-5 sm:grid-cols-2">
          <InfoField label="Title" value={course.title} />
          <InfoField label="Title (English)" value={course.titleEn} />
          <InfoField label="Slug" value={course.slug} />
          <InfoField label="Level" value={course.level} />
          <InfoField
            label="Language"
            value={course.language === 'BN' ? 'Bangla' : 'English'}
          />
          <InfoField
            label="Study Mode"
            value={course.studyMode?.replace('_', ' ')}
          />
          <InfoField
            label="Price"
            value={course.isFree ? 'Free' : `${course.priceBdt ?? 0} BDT`}
          />
          <InfoField
            label="Duration"
            value={
              course.estimatedDurationMinutes
                ? `${course.estimatedDurationMinutes} minutes`
                : '-'
            }
          />
          <InfoField label="Credits" value={String(course.credits ?? 3)} />
          <InfoField label="Semester" value={course.semester?.name ?? 'Not assigned'} />
          <InfoField label="Target Audience" value={course.targetAudience} />
          <InfoField label="Target Audience (English)" value={course.targetAudienceEn} />
          <InfoField label="Prerequisites" value={course.prerequisites} />
          <InfoField label="Prerequisites (English)" value={course.prerequisitesEn} />
          <InfoField label="Thumbnail" value={course.thumbnailUrl} />
        </div>
      </div>

      {course.shortDescription && (
        <DetailBlock title="Short Description" text={course.shortDescription} />
      )}
      {course.description && (
        <DetailBlock title="Description" text={course.description} />
      )}
      {course.descriptionEn && <DetailBlock title="Description (English)" text={course.descriptionEn} />}

      <ListBlock title="Outcomes" items={course.courseOutcomes} />
      <ListBlock title="Outcomes (English)" items={course.courseOutcomesEn} />
      <ListBlock title="Requirements" items={course.requirements} />
      <ListBlock title="Requirements (English)" items={course.requirementsEn} />
      <ListBlock title="Highlights" items={course.highlights} />
      <ListBlock title="Highlights (English)" items={course.highlightsEn} />

      {course.courseId && course.courseTemplateId && <CoursePrerequisiteEditor courseId={course.courseId} templateId={course.courseTemplateId} />}

      {course.courseId && (
        <EditCourseDialog
          course={course}
          open={editOpen}
          onOpenChange={setEditOpen}
        />
      )}
    </div>
  )
}

function InfoField({
  label,
  value,
}: {
  label: string
  value?: string | null
}) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs font-medium uppercase tracking-wider text-[#71717a]">
        {label}
      </span>
      <span className="text-sm text-[#1b1c1a]">{value || '-'}</span>
    </div>
  )
}

function DetailBlock({ title, text }: { title: string; text: string }) {
  return (
    <div className="rounded-2xl bg-white p-6 shadow-[0px_2px_6px_rgba(16,24,40,0.06)]">
      <h3 className="mb-2 font-heading text-sm font-semibold text-[#1c3873]">
        {title}
      </h3>
      <p className="whitespace-pre-wrap text-sm text-[#5e5f5c]">{text}</p>
    </div>
  )
}

function ListBlock({
  title,
  items,
}: {
  title: string
  items?: string[]
}) {
  if (!items || items.length === 0) return null
  return (
    <div className="rounded-2xl bg-white p-6 shadow-[0px_2px_6px_rgba(16,24,40,0.06)]">
      <h3 className="mb-3 font-heading text-sm font-semibold text-[#1c3873]">
        {title}
      </h3>
      <ul className="flex flex-col gap-1.5">
        {items.map((item, i) => (
          <li
            key={i}
            className="flex items-start gap-2 text-sm text-[#5e5f5c]"
          >
            <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-[#1c3873]" />
            {item}
          </li>
        ))}
      </ul>
    </div>
  )
}
