'use client'

import { useState, useEffect, useMemo } from 'react'
import { useUpdateInstructor, useAdminInstructor } from '@/hooks/useAdmin'
import { toast } from 'sonner'
import { Loader2 } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'
import type { AdminInstructorSummary } from '@/types'

interface EditTeacherDialogProps {
  teacher: AdminInstructorSummary | null
  onClose: () => void
}

interface FormState {
  fullName: string
  displayName: string
  email: string
  phone: string
  headline: string
  headlineEn: string
  institution: string
  institutionEn: string
  expertiseArea: string
  expertiseAreaEn: string
  about: string
  aboutEn: string
  photoUrl: string
  isPublic: boolean
  specialties: string
  specialtiesEn: string
  yearsExperience: string
  credentialsText: string
  credentialsTextEn: string
}

/** Map API detail to the form state shape. */
function detailToForm(d: {
  fullName?: string
  displayName?: string
  email?: string
  phone?: string
  headline?: string
  headlineEn?: string
  institution?: string
  institutionEn?: string
  expertiseArea?: string
  expertiseAreaEn?: string
  about?: string
  aboutEn?: string
  photoUrl?: string
  isPublic?: boolean
  specialties?: string[]
  specialtiesEn?: string[]
  yearsExperience?: number
  credentialsText?: string
  credentialsTextEn?: string
}): FormState {
  return {
    fullName: d.fullName ?? '',
    displayName: d.displayName ?? '',
    email: d.email ?? '',
    phone: d.phone ?? '',
    headline: d.headline ?? '',
    headlineEn: d.headlineEn ?? '',
    institution: d.institution ?? '',
    institutionEn: d.institutionEn ?? '',
    expertiseArea: d.expertiseArea ?? '',
    expertiseAreaEn: d.expertiseAreaEn ?? '',
    about: d.about ?? '',
    aboutEn: d.aboutEn ?? '',
    photoUrl: d.photoUrl ?? '',
    isPublic: d.isPublic ?? true,
    specialties: Array.isArray(d.specialties) ? d.specialties.join(', ') : '',
    specialtiesEn: Array.isArray(d.specialtiesEn) ? d.specialtiesEn.join(', ') : '',
    yearsExperience: d.yearsExperience != null ? String(d.yearsExperience) : '',
    credentialsText: d.credentialsText ?? '',
    credentialsTextEn: d.credentialsTextEn ?? '',
  }
}

export function EditTeacherDialog({ teacher, onClose }: EditTeacherDialogProps) {
  const updateInstructor = useUpdateInstructor()
  // Fetch full detail — the summary only has 8 fields, but the form has 13.
  const { data: detail, isLoading: detailLoading } = useAdminInstructor(
    teacher?.userId ?? null,
  )

  const [form, setForm] = useState<FormState>(() => detailToForm({}))
  const [loadedFor, setLoadedFor] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState('')

  // When the detail arrives (or teacher changes), populate the form once.
  useEffect(() => {
    // Hydrate the form from the full instructor response.
    if (!teacher?.userId) return
    if (loadedFor === teacher.userId && form) return
    if (!detail) return
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setForm(detailToForm(detail))
    setLoadedFor(teacher.userId)
  }, [teacher?.userId, detail, loadedFor, form])

  const initialForm = useMemo(
    () => (detail ? detailToForm(detail) : null),
    [detail],
  )

  const isDirty = useMemo(() => {
    if (!initialForm) return false
    return (Object.keys(form) as Array<keyof FormState>).some(
      (k) => form[k] !== initialForm[k],
    )
  }, [form, initialForm])

  const isSaving = updateInstructor.isPending

  function handleOpenChange(next: boolean) {
    if (next) return
    if (isSaving) return
    if (isDirty) {
      const ok = window.confirm('You have unsaved changes. Discard them?')
      if (!ok) return
    }
    onClose()
    setLoadedFor(null)
  }

  function handleChange(key: keyof FormState, value: string | boolean) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitError('')
    if (!teacher?.userId || !form.fullName.trim() || !form.displayName.trim()) {
      const message = 'Full name and display name are required'
      setSubmitError(message)
      toast.error(message)
      return
    }

    try {
      await updateInstructor.mutateAsync({
        id: teacher.userId,
        data: {
          fullName: form.fullName.trim(),
          displayName: form.displayName.trim(),
          email: form.email.trim() || undefined,
          phone: form.phone.trim() || undefined,
          headline: form.headline.trim() || undefined,
          headlineEn: form.headlineEn.trim() || undefined,
          institution: form.institution.trim() || undefined,
          institutionEn: form.institutionEn.trim() || undefined,
          expertiseArea: form.expertiseArea.trim() || undefined,
          expertiseAreaEn: form.expertiseAreaEn.trim() || undefined,
          about: form.about.trim() || undefined,
          aboutEn: form.aboutEn.trim() || undefined,
          photoUrl: form.photoUrl.trim() || undefined,
          isPublic: form.isPublic,
          credentialsText: form.credentialsText.trim() || undefined,
          credentialsTextEn: form.credentialsTextEn.trim() || undefined,
          specialties: form.specialties.trim()
            ? form.specialties.split(',').map((s) => s.trim())
            : undefined,
          specialtiesEn: form.specialtiesEn.trim()
            ? form.specialtiesEn.split(',').map((s) => s.trim()).filter(Boolean)
            : undefined,
          yearsExperience: form.yearsExperience
            ? Number(form.yearsExperience)
            : undefined,
        },
      })
      toast.success('Teacher updated successfully')
      onClose()
      setLoadedFor(null)
    } catch (err) {
      const e = err as { message?: string; status?: number }
      const message = e?.message || 'Failed to update teacher'
      setSubmitError(message)
      toast.error(message)
      if (e?.status === 422) {
        toast.warning('Some fields need attention. Check the form and try again.')
      }
    }
  }

  return (
    <Dialog open={!!teacher} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Edit Teacher</DialogTitle>
        </DialogHeader>

        {detailLoading ? (
          <div className='flex items-center justify-center py-12' aria-busy='true'>
            <Loader2 className='h-6 w-6 text-[#3b82f6] animate-spin' aria-hidden='true' />
            <span className='ml-2 text-sm text-[#5e5f5c]'>Loading teacher data…</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {submitError && <div role='alert' className='rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800'>{submitError}</div>}
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-full-name">Full Name *</Label>
                <Input id="edit-full-name" value={form.fullName} onChange={(e) => handleChange('fullName', e.target.value)} placeholder="Full name" />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-display-name">Display Name *</Label>
                <Input id="edit-display-name" value={form.displayName} onChange={(e) => handleChange('displayName', e.target.value)} placeholder="Display name" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-email">Email</Label>
                <Input id="edit-email" type="email" value={form.email} onChange={(e) => handleChange('email', e.target.value)} placeholder="email@example.com" />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-phone">Phone</Label>
                <Input id="edit-phone" value={form.phone} onChange={(e) => handleChange('phone', e.target.value)} placeholder="Phone number" />
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-headline">Headline (Bangla)</Label>
              <Input id="edit-headline" value={form.headline} onChange={(e) => handleChange('headline', e.target.value)} placeholder="e.g. Senior Mathematics Instructor" />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-headline-en">Headline (English)</Label>
              <Input id="edit-headline-en" value={form.headlineEn} onChange={(e) => handleChange('headlineEn', e.target.value)} />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-institution">Institution (Bangla)</Label>
                <Input id="edit-institution" value={form.institution} onChange={(e) => handleChange('institution', e.target.value)} placeholder="Institution" />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-expertise">Expertise Area (Bangla)</Label>
                <Input id="edit-expertise" value={form.expertiseArea} onChange={(e) => handleChange('expertiseArea', e.target.value)} placeholder="e.g. Physics" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5"><Label htmlFor="edit-institution-en">Institution (English)</Label><Input id="edit-institution-en" value={form.institutionEn} onChange={(e) => handleChange('institutionEn', e.target.value)} /></div>
              <div className="flex flex-col gap-1.5"><Label htmlFor="edit-expertise-en">Expertise Area (English)</Label><Input id="edit-expertise-en" value={form.expertiseAreaEn} onChange={(e) => handleChange('expertiseAreaEn', e.target.value)} /></div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-about">About (Bangla)</Label>
              <Textarea id="edit-about" value={form.about} onChange={(e) => handleChange('about', e.target.value)} placeholder="Short bio" rows={3} />
            </div>
            <div className="flex flex-col gap-1.5"><Label htmlFor="edit-about-en">About (English)</Label><Textarea id="edit-about-en" value={form.aboutEn} onChange={(e) => handleChange('aboutEn', e.target.value)} rows={3} /></div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-photo">Photo URL</Label>
              <Input id="edit-photo" value={form.photoUrl} onChange={(e) => handleChange('photoUrl', e.target.value)} placeholder="https://..." />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="edit-specialties">Specialties (Bangla, comma-separated)</Label>
              <Input id="edit-specialties" value={form.specialties} onChange={(e) => handleChange('specialties', e.target.value)} placeholder="Algebra, Calculus, Geometry" />
            </div>
            <div className="flex flex-col gap-1.5"><Label htmlFor="edit-specialties-en">Specialties (English, comma-separated)</Label><Input id="edit-specialties-en" value={form.specialtiesEn} onChange={(e) => handleChange('specialtiesEn', e.target.value)} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-years">Years of Experience</Label>
                <Input id="edit-years" type="number" min={0} value={form.yearsExperience} onChange={(e) => handleChange('yearsExperience', e.target.value)} placeholder="0" />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="edit-creds">Credentials (Bangla)</Label>
                <Input id="edit-creds" value={form.credentialsText} onChange={(e) => handleChange('credentialsText', e.target.value)} placeholder="e.g. PhD, M.Sc." />
              </div>
            </div>
            <div className="flex flex-col gap-1.5"><Label htmlFor="edit-creds-en">Credentials (English)</Label><Input id="edit-creds-en" value={form.credentialsTextEn} onChange={(e) => handleChange('credentialsTextEn', e.target.value)} /></div>
            <label className="flex items-center gap-2 text-sm text-[#5e5f5c]">
              <input type="checkbox" checked={form.isPublic} onChange={(e) => handleChange('isPublic', e.target.checked)} className="h-4 w-4 rounded border-[#e5e5e5] accent-[#1c3873]" />
              Publicly visible
            </label>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)} disabled={isSaving}>
                Cancel
              </Button>
              <Button type="submit" disabled={isSaving || !isDirty} className="btn-blue-gradient text-white">
                {isSaving ? 'Saving…' : 'Save Changes'}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}
