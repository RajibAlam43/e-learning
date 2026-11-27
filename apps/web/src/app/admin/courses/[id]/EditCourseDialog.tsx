'use client'

import { useState, useEffect, useMemo } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { useAdminCategories, useUpdateCourse } from '@/hooks/useAdmin'
import { EditCourseFields } from '@/components/admin/course/EditCourseFields'
import type { AdminCourseDetail } from '@/types'
import { getYouTubeVideoId } from '@/lib/video-embed'
import { dateTimeInputToIso } from '@/lib/date-time'
import { courseEditValues } from '@/lib/course-edit-values'

interface EditCourseDialogProps {
  course: AdminCourseDetail
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function EditCourseDialog({
  course,
  open,
  onOpenChange,
}: EditCourseDialogProps) {
  const updateCourse = useUpdateCourse()
  const categories = useAdminCategories()

  const initialForm = useMemo(() => courseEditValues(course), [course])

  const [form, setForm] = useState(initialForm)
  const [submitError, setSubmitError] = useState('')

  // Dirty detection — compare current form to initial values.
  const isDirty = useMemo(() => {
    return (Object.keys(form) as Array<keyof typeof form>).some(
      (k) => Array.isArray(form[k]) && Array.isArray(initialForm[k])
        ? JSON.stringify(form[k]) !== JSON.stringify(initialForm[k])
        : form[k] !== initialForm[k]
    )
  }, [form, initialForm])

  const isSaving = updateCourse.isPending

  // Block close during save; warn on close with unsaved changes.
  function handleOpenChange(next: boolean) {
    if (next) return // opening — never blocked
    if (isSaving) return // never close while saving
    if (isDirty) {
      const ok = window.confirm('You have unsaved changes. Discard them?')
      if (!ok) return
    }
    onOpenChange(false)
  }

  // When the course data arrives (or changes), reset the form to match.
  // useState(initialForm) only takes the first value; this keeps form synced.
  useEffect(() => {
    // Reset the controlled editor when a different course is opened.
    if (open && course) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setForm(courseEditValues(course))
    }
  }, [open, course])

  function handleChange(key: string, value: string | number | boolean | null | undefined) {
    if (value === null) return
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!course.courseId) return
    if (form.videoUrl && !getYouTubeVideoId(form.videoUrl)) {
      const message = 'Enter a valid YouTube URL or 11-character video ID.'
      setSubmitError(message)
      toast.error(message)
      return
    }

    const splitLines = (text: string) =>
      text
        .split('\n')
        .map((l) => l.trim())
        .filter(Boolean)

    try {
      setSubmitError('')
      await updateCourse.mutateAsync({
        id: course.courseId,
        data: {
          title: form.title || undefined,
          titleEn: form.titleEn || undefined,
          slug: form.slug || undefined,
          // thumbnailObjectKey comes from the R2 upload flow (ThumbnailUploader).
          thumbnailObjectKey: form.thumbnailObjectKey || undefined,
          video: form.videoUrl
            ? { provider: 'YOUTUBE', sourceId: getYouTubeVideoId(form.videoUrl) ?? form.videoUrl.trim() }
            : undefined,
          categoryIds: form.categoryIds.length > 0 ? form.categoryIds : undefined,
          shortDescription: form.shortDescription || undefined,
          shortDescriptionEn: form.shortDescriptionEn || undefined,
          description: form.description || undefined,
          descriptionEn: form.descriptionEn || undefined,
          priceBdt: form.priceBdt,
          isFree: form.isFree,
          level: form.level || undefined,
          language: form.language || undefined,
          studyMode: form.studyMode || undefined,
          credits: form.credits,
          semesterId: form.semesterId || undefined,
          estimatedDurationMinutes: form.estimatedDurationMinutes || undefined,
          timezone: form.timezone || undefined,
          enrollmentStartsAt: dateTimeInputToIso(form.enrollmentStartsAt, form.timezone, course.enrollmentStartsAt),
          enrollmentEndsAt: dateTimeInputToIso(form.enrollmentEndsAt, form.timezone, course.enrollmentEndsAt),
          startsAt: dateTimeInputToIso(form.startsAt, form.timezone, course.startsAt),
          endsAt: dateTimeInputToIso(form.endsAt, form.timezone, course.endsAt),
          capacity: form.capacity || null,
          accessDurationDays: form.accessDurationDays || null,
          courseOutcomes: splitLines(form.courseOutcomes),
          courseOutcomesEn: splitLines(form.courseOutcomesEn),
          requirements: splitLines(form.requirements),
          requirementsEn: splitLines(form.requirementsEn),
          highlights: splitLines(form.highlights),
          highlightsEn: splitLines(form.highlightsEn),
          targetAudience: form.targetAudience || undefined,
          targetAudienceEn: form.targetAudienceEn || undefined,
          prerequisites: form.prerequisites || undefined,
          prerequisitesEn: form.prerequisitesEn || undefined,
        },
      })
      toast.success('Course updated successfully')
      onOpenChange(false)
    } catch (cause) {
      const message = cause && typeof cause === 'object' && 'message' in cause && typeof cause.message === 'string'
        ? cause.message
        : 'Failed to update course'
      setSubmitError(message)
      toast.error(message)
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Edit Course</DialogTitle>
        </DialogHeader>
        {submitError && <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{submitError}</div>}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <EditCourseFields form={form} setForm={setForm} categories={categories} handleChange={handleChange} />

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={isSaving}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isSaving || !isDirty}
              className="btn-blue-gradient text-white"
            >
              {updateCourse.isPending ? 'Saving...' : 'Save Changes'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
