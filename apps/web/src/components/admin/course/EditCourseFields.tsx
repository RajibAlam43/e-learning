'use client'

import type { Dispatch, SetStateAction } from 'react'
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
import type { courseEditValues } from '@/lib/course-edit-values'
import { CourseAcademicFields } from './CourseAcademicFields'
import { TIMEZONES } from '@/lib/timezones'

type Props = {
  form: ReturnType<typeof courseEditValues>
  setForm: Dispatch<SetStateAction<ReturnType<typeof courseEditValues>>>
  categories: ReturnType<typeof useAdminCategories>
  handleChange: (key: string, value: string | number | boolean | null | undefined) => void
}

export function EditCourseFields({ form, setForm, categories, handleChange }: Props) {
  return <>
          <CourseAcademicFields credits={form.credits} semesterId={form.semesterId}
            onCreditsChange={value => handleChange('credits', value)}
            onSemesterChange={value => handleChange('semesterId', value)} />
          <div className="grid grid-cols-2 gap-3">
            <FormField label="Title (English) *">
              <Input
                value={form.titleEn}
                onChange={(e) => handleChange('titleEn', e.target.value)}
                placeholder="Course title"
              />
            </FormField>
            <FormField label="Slug">
              <Input
                value={form.slug}
                onChange={(e) => handleChange('slug', e.target.value)}
              />
            </FormField>
          </div>

          <FormField label="Title (Bangla) *">
            <Input
              value={form.title}
              onChange={(e) => handleChange('title', e.target.value)}
              placeholder="কোর্সের শিরোনাম"
            />
          </FormField>

          <ThumbnailUploader
            ownerType="COURSE"
            value={form.thumbnailObjectKey}
            onChange={(v) => handleChange('thumbnailObjectKey', v ?? '')}
            previewUrl={form.thumbnailUrl || undefined}
          />

          <FormField label="Course intro video (YouTube URL)">
            <Input
              type="url"
              value={form.videoUrl}
              onChange={(e) => handleChange('videoUrl', e.target.value)}
              placeholder="https://www.youtube.com/watch?v=..."
            />
          </FormField>

          <FormField label="Categories">
            <div className="grid gap-2 rounded-lg border p-3 sm:grid-cols-2">
              {(categories.data ?? []).filter((category) => category.id).map((category) => {
                const id = category.id as string
                const checked = form.categoryIds.includes(id)
                return (
                  <label key={id} className="flex items-center gap-2 text-sm text-slate-700">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={(event) => setForm((current) => ({
                        ...current,
                        categoryIds: event.target.checked
                          ? [...current.categoryIds, id]
                          : current.categoryIds.filter((value) => value !== id),
                      }))}
                      className="h-4 w-4 accent-[#1c3873]"
                    />
                    {category.nameEn || category.name || category.slug}
                  </label>
                )
              })}
              {!categories.isLoading && (categories.data ?? []).length === 0 && <span className="text-sm text-slate-500">No categories available.</span>}
            </div>
          </FormField>

          <FormField label="Short Description (Bangla)">
            <Input
              value={form.shortDescription}
              onChange={(e) => handleChange('shortDescription', e.target.value)}
            />
          </FormField>

          <FormField label="Short Description (English)">
            <Input
              value={form.shortDescriptionEn}
              onChange={(e) => handleChange('shortDescriptionEn', e.target.value)}
              placeholder="Optional English summary"
            />
          </FormField>

          <FormField label="Description (Bangla)">
            <Textarea
              value={form.description}
              onChange={(e) => handleChange('description', e.target.value)}
              rows={3}
            />
          </FormField>

          <FormField label="Description (English)">
            <Textarea
              value={form.descriptionEn}
              onChange={(e) => handleChange('descriptionEn', e.target.value)}
              rows={3}
              placeholder="Optional English description"
            />
          </FormField>

          <div className="grid grid-cols-2 gap-3">
            <FormField label="Price (BDT)">
              <Input
                type="number"
                min={0}
                disabled={form.isFree}
                value={form.priceBdt}
                onChange={(e) => handleChange('priceBdt', Number(e.target.value))}
              />
            </FormField>
            <div className="flex items-end pb-2">
              <label className="flex items-center gap-2 text-sm text-[#5e5f5c]">
                <input
                  type="checkbox"
                  checked={form.isFree}
                  onChange={(e) => {
                    handleChange('isFree', e.target.checked)
                    if (e.target.checked) handleChange('priceBdt', 0)
                  }}
                  className="h-4 w-4 rounded accent-[#1c3873]"
                />
                Free course
              </label>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <FormField label="Level">
              <Select
                value={form.level}
                onValueChange={(v) => handleChange('level', v)}
              >
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="BEGINNER">Beginner</SelectItem>
                  <SelectItem value="INTERMEDIATE">Intermediate</SelectItem>
                  <SelectItem value="ADVANCED">Advanced</SelectItem>
                </SelectContent>
              </Select>
            </FormField>
            <FormField label="Language">
              <Select
                value={form.language}
                onValueChange={(v) => handleChange('language', v)}
              >
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="BN">Bangla</SelectItem>
                  <SelectItem value="EN">English</SelectItem>
                </SelectContent>
              </Select>
            </FormField>
            <FormField label="Study Mode">
              <Select
                value={form.studyMode}
                onValueChange={(v) => handleChange('studyMode', v)}
              >
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="COHORT_BASED">Cohort based</SelectItem>
                  <SelectItem value="SELF_PACED">Self Paced</SelectItem>
                </SelectContent>
              </Select>
            </FormField>
          </div>

          <FormField label="Duration (minutes)">
            <Input
              type="number"
              min={0}
              value={form.estimatedDurationMinutes || ''}
              onChange={(e) =>
                handleChange('estimatedDurationMinutes', Number(e.target.value))
              }
            />
          </FormField>

          <div className="grid grid-cols-2 gap-3">
            <FormField label="Timezone" hint="All dates below use this timezone."><select value={form.timezone || 'Asia/Dhaka'} onChange={(e) => handleChange('timezone', e.target.value)} className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus:border-[#1c3873] focus:ring-2 focus:ring-[#1c3873]/10">{TIMEZONES.map((timezone) => <option key={timezone} value={timezone}>{timezone}</option>)}</select></FormField>
            <FormField label="Capacity"><Input type="number" min={0} value={form.capacity ?? 0} onChange={(e) => handleChange('capacity', e.target.value === '' ? 0 : Number(e.target.value))} /></FormField>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <FormField label="Enrollment starts"><Input type="datetime-local" value={form.enrollmentStartsAt} onChange={(e) => handleChange('enrollmentStartsAt', e.target.value)} /></FormField>
            <FormField label="Enrollment ends"><Input type="datetime-local" value={form.enrollmentEndsAt} onChange={(e) => handleChange('enrollmentEndsAt', e.target.value)} /></FormField>
            <FormField label="Course starts"><Input type="datetime-local" value={form.startsAt} onChange={(e) => handleChange('startsAt', e.target.value)} /></FormField>
            <FormField label="Course ends"><Input type="datetime-local" value={form.endsAt} onChange={(e) => handleChange('endsAt', e.target.value)} /></FormField>
          </div>
          <FormField label="Access duration (days)"><Input type="number" min={0} value={form.accessDurationDays ?? 0} onChange={(e) => handleChange('accessDurationDays', e.target.value === '' ? 0 : Number(e.target.value))} /></FormField>

          <FormField label="Outcomes (one per line)">
            <Textarea
              value={form.courseOutcomes}
              onChange={(e) => handleChange('courseOutcomes', e.target.value)}
              rows={3}
            />
          </FormField>
          <FormField label="Outcomes — English (one per line)"><Textarea value={form.courseOutcomesEn} onChange={(e) => handleChange('courseOutcomesEn', e.target.value)} rows={3} /></FormField>

          <FormField label="Requirements (one per line)">
            <Textarea
              value={form.requirements}
              onChange={(e) => handleChange('requirements', e.target.value)}
              rows={3}
            />
          </FormField>
          <FormField label="Requirements — English (one per line)"><Textarea value={form.requirementsEn} onChange={(e) => handleChange('requirementsEn', e.target.value)} rows={3} /></FormField>

          <FormField label="Highlights (one per line)">
            <Textarea
              value={form.highlights}
              onChange={(e) => handleChange('highlights', e.target.value)}
              rows={3}
            />
          </FormField>
          <FormField label="Highlights — English (one per line)"><Textarea value={form.highlightsEn} onChange={(e) => handleChange('highlightsEn', e.target.value)} rows={3} /></FormField>

          <div className="grid grid-cols-2 gap-3">
            <FormField label="Target Audience">
              <Input
                value={form.targetAudience}
                onChange={(e) => handleChange('targetAudience', e.target.value)}
              />
            </FormField>
            <FormField label="Prerequisites">
              <Input
                value={form.prerequisites}
                onChange={(e) => handleChange('prerequisites', e.target.value)}
              />
            </FormField>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <FormField label="Target Audience — English"><Input value={form.targetAudienceEn} onChange={(e) => handleChange('targetAudienceEn', e.target.value)} /></FormField>
            <FormField label="Prerequisites — English"><Input value={form.prerequisitesEn} onChange={(e) => handleChange('prerequisitesEn', e.target.value)} /></FormField>
          </div>


  </>
}

function FormField({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label className="text-sm text-[#5e5f5c]">{label}</Label>
      {hint && <span className="text-xs text-gray-400 -mt-1">{hint}</span>}
      {children}
    </div>
  )
}
