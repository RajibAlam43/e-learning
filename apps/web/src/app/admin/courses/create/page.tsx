'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { useCreateCourse, useAdminCategories } from '@/hooks/useAdmin'
import { useCourse } from '@/hooks/useCourses'
import { ROUTES } from '@/constants/routes'
import { getYouTubeVideoId } from '@/lib/video-embed'

import { courseSchema, type CourseFormValues } from '@/lib/course-create-schema'
import { dateTimeInputToIso } from '@/lib/date-time'
import { CreateCourseFields } from '@/components/admin/course/CreateCourseFields'

function generateSlug(title: string): string {
  return title
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
}

export default function CourseCreatePage() {
  const router = useRouter()
  const createCourse = useCreateCourse()
  const categories = useAdminCategories()
  const [submitError, setSubmitError] = useState('')

  const {
    register,
    handleSubmit,
    control,
    setValue,
    setError,
    clearErrors,
    formState: { errors, isDirty },
  } = useForm<CourseFormValues>({
    resolver: zodResolver(courseSchema),
    defaultValues: {
      credits: 3,
      semesterId: '',
      title: '',
      slug: '',
      priceBdt: 0,
      isFree: false,
      level: 'BEGINNER',
      language: 'BN',
      studyMode: 'COHORT_BASED',
      timezone: 'Asia/Dhaka',
      capacity: 0,
      accessDurationDays: 0,
      categoryIds: [],
    },
  })

  const titleEn = useWatch({ control, name: 'titleEn' })
  const titleBn = useWatch({ control, name: 'title' })
  const isFree = useWatch({ control, name: 'isFree' })

  // Auto-generate slug from whichever title field is filled.
  // English is primary now, so prefer it; fall back to Bangla.
  useEffect(() => {
    const source = titleEn || titleBn
    if (source) {
      setValue('slug', generateSlug(source))
    }
  }, [titleEn, titleBn, setValue])

  // ─── Slug uniqueness check ───
  // Debounce the slug, then query the public endpoint to see if a course
  // with that slug already exists. If it does, set an inline form error.
  const slugValue = useWatch({ control, name: 'slug' })
  const [debouncedSlug, setDebouncedSlug] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSlug(slugValue?.trim() ?? ''), 400)
    return () => clearTimeout(t)
  }, [slugValue])

  // useCourse hits GET /public/courses/{slug} — returns data if slug exists.
  // Only check when slug is non-empty and at least 3 chars (avoid noise).
  const slugCheck = useCourse(debouncedSlug.length >= 3 ? debouncedSlug : '__skip__')

  useEffect(() => {
    if (!debouncedSlug) return
    if (slugCheck.isLoading) return
    if (slugCheck.isError) {
      // 404 = slug is available — clear any previous error
      clearErrors('slug')
      return
    }
    if (slugCheck.data) {
      // A course with this slug already exists
      setError('slug', {
        type: 'manual',
        message: 'This slug is already in use. Try a different one.',
      })
    }
  }, [debouncedSlug, slugCheck.data, slugCheck.isLoading, slugCheck.isError, setError, clearErrors])

  useEffect(() => {
    if (isFree) {
      setValue('priceBdt', 0)
    }
  }, [isFree, setValue])

  // Unsaved changes warning — prompts user before navigating away or closing
  // the tab while the form has unsaved (dirty) data.
  useEffect(() => {
    if (!isDirty) return
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [isDirty])

  async function onSubmit(values: CourseFormValues) {
    try {
      setSubmitError('')
      const splitLines = (text?: string) =>
        text
          ? text
              .split('\n')
              .map((l) => l.trim())
              .filter(Boolean)
          : undefined

      const result = await createCourse.mutateAsync({
        // API requires `title` — if Bangla is empty, send English so the field
        // is never blank. The backend stores both separately.
        title: values.title,
        titleEn: values.titleEn,
        slug: values.slug,
        // thumbnailObjectKey comes from the R2 upload flow (ThumbnailUploader).
        thumbnailObjectKey: values.thumbnailObjectKey || undefined,
        video: values.videoUrl
          ? { provider: 'YOUTUBE', sourceId: getYouTubeVideoId(values.videoUrl)! }
          : undefined,
        shortDescription: values.shortDescription || undefined,
        shortDescriptionEn: values.shortDescriptionEn || undefined,
        description: values.description || undefined,
        descriptionEn: values.descriptionEn || undefined,
        priceBdt: values.priceBdt,
        isFree: values.isFree,
        level: values.level,
        language: values.language,
        studyMode: values.studyMode,
        credits: values.credits ?? 3,
        semesterId: values.semesterId || undefined,
        estimatedDurationMinutes: values.estimatedDurationMinutes || undefined,
        timezone: values.timezone || undefined,
        enrollmentStartsAt: dateTimeInputToIso(values.enrollmentStartsAt ?? '', values.timezone || 'Asia/Dhaka'),
        enrollmentEndsAt: dateTimeInputToIso(values.enrollmentEndsAt ?? '', values.timezone || 'Asia/Dhaka'),
        startsAt: dateTimeInputToIso(values.startsAt ?? '', values.timezone || 'Asia/Dhaka'),
        endsAt: dateTimeInputToIso(values.endsAt ?? '', values.timezone || 'Asia/Dhaka'),
        capacity: values.capacity || undefined,
        accessDurationDays: values.accessDurationDays || undefined,
        courseOutcomes: splitLines(values.courseOutcomes),
        courseOutcomesEn: splitLines(values.courseOutcomesEn),
        requirements: splitLines(values.requirements),
        requirementsEn: splitLines(values.requirementsEn),
        highlights: splitLines(values.highlights),
        highlightsEn: splitLines(values.highlightsEn),
        targetAudience: values.targetAudience || undefined,
        targetAudienceEn: values.targetAudienceEn || undefined,
        prerequisites: values.prerequisites || undefined,
        prerequisitesEn: values.prerequisitesEn || undefined,
        categoryIds: values.categoryIds,
      })

      toast.success('Course created successfully')
      const id = (result as { courseId?: string })?.courseId
      if (id) {
        router.push(ROUTES.admin.courseDetail(id))
      } else {
        router.push(ROUTES.admin.courses)
      }
    } catch (err) {
      // Try to extract a backend field-level error (422 validation) and surface
      // a specific message; fall back to generic.
      const e = err as { message?: string; status?: number; code?: string }
      const msg = e?.message || 'Failed to create course'
      setSubmitError(msg)
      toast.error(msg)
      if (e?.status === 422) {
        // Backend rejected input — mark the form as touched so inline errors show.
        // (Future: parse field-level errors from response and setError() each field.)
        toast.warning('Some fields need attention. Check the form and try again.')
      }
    }
  }

  return (
    <div className="mx-auto flex w-[900px] max-w-full flex-col gap-6 pb-10">
      <div className="flex items-center gap-3">
        <Link href={ROUTES.admin.courses}>
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-5 w-5" />
          </Button>
        </Link>
        <h1 className="font-heading text-2xl font-semibold text-[#1c3873]">
          Create Course
        </h1>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="flex flex-col gap-6 rounded-2xl bg-white p-6 shadow-[0px_2px_6px_rgba(16,24,40,0.06)]"
      >
        {submitError && <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{submitError}</div>}
        <CreateCourseFields register={register} control={control} errors={errors}
          categories={categories} isFree={isFree}
          slugHint={slugCheck.isLoading && debouncedSlug ? 'Checking availability…'
            : !errors.slug && debouncedSlug && !slugCheck.data && !slugCheck.isLoading
              ? '✓ Slug is available' : undefined} />

        <div className="flex justify-end gap-3 border-t border-[#f0f0f0] pt-4">
          <Link href={ROUTES.admin.courses}>
            <Button variant="outline" type="button">
              Cancel
            </Button>
          </Link>
          <Button
            type="submit"
            disabled={createCourse.isPending}
            className="btn-blue-gradient text-white"
          >
            {createCourse.isPending ? 'Creating...' : 'Create Course'}
          </Button>
        </div>
      </form>
    </div>
  )
}
