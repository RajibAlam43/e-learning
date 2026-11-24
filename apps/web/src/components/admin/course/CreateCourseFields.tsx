'use client'

import { Controller, type UseFormReturn } from 'react-hook-form'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectTrigger,
  SelectContent,
  SelectItem,
  SelectValue,
} from '@/components/ui/select'
import { ThumbnailUploader } from '@/components/admin/shared/ThumbnailUploader'
import type { useAdminCategories } from '@/hooks/useAdmin'
import type { CourseFormValues } from '@/lib/course-create-schema'
import { TIMEZONES } from '@/lib/timezones'

type Props = Pick<UseFormReturn<CourseFormValues>, 'register' | 'control'> & {
  errors: UseFormReturn<CourseFormValues>['formState']['errors']
  categories: ReturnType<typeof useAdminCategories>
  isFree?: boolean
  slugHint?: string
}

export function CreateCourseFields({ register, control, errors, categories, isFree, slugHint }: Props) {
  return <>
        <FormSection title="Basic Info">
          <FieldRow>
            <Field label="Title (English)" error={errors.titleEn?.message}>
              <Input {...register('titleEn')} placeholder="Course title" />
            </Field>
            <Field
              label="Slug *"
              error={errors.slug?.message}
              hint={slugHint}
            >
              <Input {...register('slug')} placeholder="course-slug" />
            </Field>
          </FieldRow>
          <Field label="Title (Bangla) *" error={errors.title?.message}>
            <Input {...register('title')} placeholder="কোর্সের শিরোনাম" />
          </Field>
          <Controller
            control={control}
            name="thumbnailObjectKey"
            render={({ field }) => (
              <ThumbnailUploader
                ownerType="COURSE"
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
          <Field label="Course intro video (YouTube URL)" error={errors.videoUrl?.message}>
            <Input
              {...register('videoUrl')}
              type="url"
              placeholder="https://www.youtube.com/watch?v=..."
              aria-invalid={Boolean(errors.videoUrl)}
              className={errors.videoUrl ? 'border-red-500 focus-visible:ring-red-500' : undefined}
            />
          </Field>
          <Field label="Short Description (Bangla)">
            <Input
              {...register('shortDescription')}
              placeholder="সংক্ষিপ্ত বিবরণ"
            />
          </Field>
          <Field label="Short Description (English)">
            <Input
              {...register('shortDescriptionEn')}
              placeholder="Brief summary"
            />
          </Field>
          <Field label="Description (Bangla)">
            <Textarea
              {...register('description')}
              placeholder="কোর্সের বিস্তারিত বিবরণ"
              rows={4}
            />
          </Field>
          <Field label="Description (English)">
            <Textarea
              {...register('descriptionEn')}
              placeholder="Full course description"
              rows={4}
            />
          </Field>
        </FormSection>

        <FormSection title="Pricing">
          <FieldRow>
            <Field label="Price (BDT) *" error={errors.priceBdt?.message}>
              <Input
                type="number"
                min={0}
                readOnly={!!isFree}
                aria-readonly={!!isFree}
                className={isFree ? 'bg-slate-50 text-slate-500' : undefined}
                {...register('priceBdt', {
                  setValueAs: (value) => value === '' ? 0 : Number(value),
                })}
              />
            </Field>
            <div className="flex items-end pb-2">
              <label className="flex items-center gap-2 text-sm text-[#5e5f5c]">
                <input
                  type="checkbox"
                  {...register('isFree')}
                  className="h-4 w-4 rounded accent-[#1c3873]"
                />
                Free course
              </label>
            </div>
          </FieldRow>
        </FormSection>

        <FormSection title="Details">
          <Field label="Categories *" error={errors.categoryIds?.message}>
            <div className="grid gap-2 rounded-lg border p-3 sm:grid-cols-2">
              {categories.isLoading && <span className="text-sm text-gray-500">Loading categories...</span>}
              {(categories.data ?? []).filter((category) => category.id).map((category) => (
                <label key={category.id} className="flex items-center gap-2 text-sm">
                  <input type="checkbox" value={category.id as string} {...register('categoryIds')} className="h-4 w-4 accent-[#1c3873]" />
                  {category.nameEn || category.name || category.slug}
                </label>
              ))}
              {!categories.isLoading && (categories.data ?? []).length === 0 && <span className="text-sm text-gray-500">Create a category before creating a course.</span>}
            </div>
          </Field>
          <FieldRow>
            <Field label="Level *" error={errors.level?.message}>
              <Controller
                name="level"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select level" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="BEGINNER">Beginner</SelectItem>
                      <SelectItem value="INTERMEDIATE">Intermediate</SelectItem>
                      <SelectItem value="ADVANCED">Advanced</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
            <Field label="Language *" error={errors.language?.message}>
              <Controller
                name="language"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select language" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="BN">Bangla</SelectItem>
                      <SelectItem value="EN">English</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
          </FieldRow>
          <FieldRow>
            <Field label="Study Mode *" error={errors.studyMode?.message}>
              <Controller
                name="studyMode"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select mode" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="COHORT_BASED">Cohort based</SelectItem>
                      <SelectItem value="SELF_PACED">Self Paced</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
            <Field label="Duration (minutes)">
              <Input
                type="number"
                min={0}
                {...register('estimatedDurationMinutes', {
                  setValueAs: (value) => value === '' ? undefined : Number(value),
                })}
                placeholder="e.g. 120"
              />
            </Field>
          </FieldRow>
          <FieldRow>
            <Field label="Timezone" hint="All dates below use this timezone.">
              <select
                {...register('timezone')}
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus:border-[#1c3873] focus:ring-2 focus:ring-[#1c3873]/10"
              >
                {TIMEZONES.map((timezone) => (
                  <option key={timezone} value={timezone}>{timezone}</option>
                ))}
              </select>
            </Field>
            <Field label="Capacity" error={errors.capacity?.message}>
              <Input
                type="number"
                min={0}
                placeholder="Unlimited"
                aria-invalid={Boolean(errors.capacity)}
                className={errors.capacity ? 'border-red-500 focus-visible:ring-red-500' : undefined}
                {...register('capacity', {
                  setValueAs: (value) => value === '' ? 0 : Number(value),
                })}
              />
            </Field>
          </FieldRow>
          <FieldRow>
            <Field label="Enrollment starts">
              <Input type="datetime-local" {...register('enrollmentStartsAt')} />
            </Field>
            <Field label="Enrollment ends">
              <Input type="datetime-local" {...register('enrollmentEndsAt')} />
            </Field>
          </FieldRow>
          <FieldRow>
            <Field label="Course starts">
              <Input type="datetime-local" {...register('startsAt')} />
            </Field>
            <Field label="Course ends">
              <Input type="datetime-local" {...register('endsAt')} />
            </Field>
          </FieldRow>
          <Field label="Access duration (days)" error={errors.accessDurationDays?.message}>
            <Input
              type="number"
              min={0}
              placeholder="Unlimited"
              aria-invalid={Boolean(errors.accessDurationDays)}
              className={errors.accessDurationDays ? 'border-red-500 focus-visible:ring-red-500' : undefined}
              {...register('accessDurationDays', {
                setValueAs: (value) => value === '' ? 0 : Number(value),
              })}
            />
          </Field>
        </FormSection>

        <FormSection title="Content">
          <Field label="Course Outcomes (one per line)">
            <Textarea
              {...register('courseOutcomes')}
              placeholder={"Learn React fundamentals\nBuild real projects"}
              rows={3}
            />
          </Field>
          <Field label="Course Outcomes — English (one per line)">
            <Textarea {...register('courseOutcomesEn')} placeholder={"Understand the fundamentals\nApply concepts in projects"} rows={3} />
          </Field>
          <Field label="Requirements (one per line)">
            <Textarea
              {...register('requirements')}
              placeholder={"Basic HTML knowledge\nA computer with internet"}
              rows={3}
            />
          </Field>
          <Field label="Requirements — English (one per line)">
            <Textarea {...register('requirementsEn')} placeholder={"Basic prior knowledge\nReliable internet connection"} rows={3} />
          </Field>
          <Field label="Highlights (one per line)">
            <Textarea
              {...register('highlights')}
              placeholder={"10+ projects\nCertificate included"}
              rows={3}
            />
          </Field>
          <Field label="Highlights — English (one per line)">
            <Textarea {...register('highlightsEn')} placeholder={"Practical projects\nCertificate included"} rows={3} />
          </Field>
          <FieldRow>
            <Field label="Target Audience">
              <Input
                {...register('targetAudience')}
                placeholder="e.g. Students & professionals"
              />
            </Field>
            <Field label="Prerequisites">
              <Input
                {...register('prerequisites')}
                placeholder="e.g. Basic programming"
              />
            </Field>
          </FieldRow>
          <FieldRow>
            <Field label="Target Audience — English">
              <Input {...register('targetAudienceEn')} placeholder="e.g. Students and professionals" />
            </Field>
            <Field label="Prerequisites — English">
              <Input {...register('prerequisitesEn')} placeholder="e.g. Basic programming knowledge" />
            </Field>
          </FieldRow>
        </FormSection>
  </>
}

function FormSection({
  title,
  children,
}: {
  title: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-4">
      <h2 className="font-heading text-base font-semibold text-[#1c3873]">
        {title}
      </h2>
      {children}
    </div>
  )
}

function FieldRow({ children }: { children: React.ReactNode }) {
  return <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">{children}</div>
}

function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string
  hint?: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label className="text-sm text-[#5e5f5c]">{label}</Label>
      {children}
      {hint && <span className="text-xs text-gray-400">{hint}</span>}
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  )
}
