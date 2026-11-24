'use client'

import { useState, useMemo } from 'react'
import { toast } from 'sonner'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'
import { Plus } from 'lucide-react'
import { useCreateLiveClass, useAdminCourses, useAdminCourse } from '@/hooks/useAdmin'
import { Select, SelectTrigger, SelectContent, SelectItem } from '@/components/ui/select'

type Provider = 'ZOOM' | 'GOOGLE_MEET' | 'TEAMS' | 'OTHER'

const PROVIDER_OPTIONS: { value: Provider; label: string }[] = [
  { value: 'ZOOM', label: 'Zoom' },
  { value: 'GOOGLE_MEET', label: 'Google Meet' },
  { value: 'TEAMS', label: 'Microsoft Teams' },
  { value: 'OTHER', label: 'Other' },
]

const initialForm = {
  courseId: '',
  sectionId: '',
  title: '',
  description: '',
  startsAt: '',
  endsAt: '',
  provider: 'OTHER' as Provider,
  maxCapacity: 100,
}

export function CreateLiveClassDialog() {
  const [open, setOpen] = useState(false)
  const createMutation = useCreateLiveClass()
  const { data: courses } = useAdminCourses()

  const [form, setForm] = useState(initialForm)
  const [submitError, setSubmitError] = useState('')

  // Load sections for the selected course (sections are embedded in AdminCourseDetail).
  const { data: courseDetail } = useAdminCourse(form.courseId)
  const sections = courseDetail?.sections ?? []

  function resetForm() {
    setForm(initialForm)
    setSubmitError('')
  }

  function handleChange(key: keyof typeof form, value: string | number) {
    setSubmitError('')
    setForm((prev) => {
      // Reset section when course changes so we never hold a stale sectionId.
      if (key === 'courseId') return { ...prev, courseId: String(value), sectionId: '' }
      return { ...prev, [key]: value }
    })
  }

  // Dirty detection — has the user entered any data?
  const isDirty = useMemo(() => {
    return (Object.keys(form) as Array<keyof typeof form>).some((k) => {
      if (k === 'maxCapacity') return form[k] !== initialForm[k]
      if (k === 'provider') return form[k] !== initialForm[k]
      return form[k] !== ''
    })
  }, [form])

  const isSaving = createMutation.isPending

  function handleOpenChange(next: boolean) {
    if (next) {
      setOpen(true)
      return
    }
    if (isSaving) return // never close while saving
    if (isDirty) {
      const ok = window.confirm('You have unsaved changes. Discard them?')
      if (!ok) return
    }
    resetForm()
    setOpen(false)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.courseId || !form.sectionId || !form.title.trim() || !form.startsAt || !form.endsAt) {
      setSubmitError('Course, section, title, start time, and end time are required.')
      toast.error('Course, section, title, start time, and end time are required')
      return
    }
    if (new Date(form.endsAt) <= new Date(form.startsAt)) {
      setSubmitError('End time must be later than start time.')
      toast.error('End time must be later than start time')
      return
    }

    try {
      setSubmitError('')
      await createMutation.mutateAsync({
        courseId: form.courseId,
        data: {
          sectionId: form.sectionId,
          title: form.title.trim(),
          description: form.description.trim() || (undefined as unknown as string),
          startsAt: new Date(form.startsAt).toISOString(),
          endsAt: new Date(form.endsAt).toISOString(),
          provider: form.provider,
          maxCapacity: form.maxCapacity,
        },
      })
      toast.success('Live class created successfully')
      resetForm()
      setOpen(false)
    } catch (err) {
      const e = err as { message?: string; status?: number }
      setSubmitError(e?.message || 'Failed to create live class')
      toast.error(e?.message || 'Failed to create live class')
      if (e?.status === 422) {
        toast.warning('Some fields need attention. Check the form and try again.')
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger
        render={
          <Button className="btn-blue-gradient text-white gap-1.5">
            <Plus className="h-4 w-4" />
            Create Class
          </Button>
        }
      />
      <DialogContent className="sm:max-w-lg max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Live Class</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {submitError && <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{submitError}</div>}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Course *</Label>
              <Select value={form.courseId} onValueChange={(v) => handleChange('courseId', v ?? '')}>
                <SelectTrigger className="h-10">
                  {form.courseId ? (
                    <span>{courses?.find((c) => c.courseId === form.courseId)?.title ?? 'Unknown course'}</span>
                  ) : (
                    <span className="text-gray-400">Select a course</span>
                  )}
                </SelectTrigger>
                <SelectContent>
                  {courses?.map((c) => (
                    <SelectItem key={c.courseId} value={c.courseId ?? ''}>
                      {c.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Section *</Label>
              <Select
                value={form.sectionId}
                onValueChange={(v) => handleChange('sectionId', v ?? '')}
                disabled={!form.courseId}
              >
                <SelectTrigger className="h-10">
                  {form.sectionId ? (
                    <span>{sections.find((s) => s.sectionId === form.sectionId)?.title ?? 'Unknown section'}</span>
                  ) : (
                    <span className="text-gray-400">{form.courseId ? 'Select a section' : 'Select a course first'}</span>
                  )}
                </SelectTrigger>
                <SelectContent>
                  {sections.map((s) => (
                    <SelectItem key={s.sectionId} value={s.sectionId ?? ''}>
                      {s.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>Title *</Label>
            <Input value={form.title} onChange={(e) => handleChange('title', e.target.value)} placeholder="Live class title" />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>Description</Label>
            <Textarea value={form.description} onChange={(e) => handleChange('description', e.target.value)} placeholder="Optional description" rows={3} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Start Time *</Label>
              <Input type="datetime-local" value={form.startsAt} onChange={(e) => handleChange('startsAt', e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>End Time *</Label>
              <Input type="datetime-local" value={form.endsAt} onChange={(e) => handleChange('endsAt', e.target.value)} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Provider *</Label>
              <Select
                value={form.provider}
                onValueChange={(v) => handleChange('provider', (v ?? 'OTHER') as Provider)}
              >
                <SelectTrigger className="h-10">
                  {form.provider ? (
                    <span>{PROVIDER_OPTIONS.find((o) => o.value === form.provider)?.label ?? form.provider}</span>
                  ) : (
                    <span className="text-gray-400">Select provider</span>
                  )}
                </SelectTrigger>
                <SelectContent>
                  {PROVIDER_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Max Capacity *</Label>
              <Input
                type="number"
                min={0}
                value={form.maxCapacity}
                onChange={(e) => handleChange('maxCapacity', Number(e.target.value) || 0)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)} disabled={isSaving}>Cancel</Button>
            <Button type="submit" disabled={isSaving} className="btn-blue-gradient text-white">
              {isSaving ? 'Creating…' : 'Create Class'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
