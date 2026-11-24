'use client'

import { useState, useMemo } from 'react'
import { useAdminInstructors, useCreateInstructor } from '@/hooks/useAdmin'
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
import type { ApiError } from '@/types'

export function CreateTeacherDialog() {
  const [open, setOpen] = useState(false)
  const createInstructor = useCreateInstructor()
  const instructors = useAdminInstructors()
  const [submitError, setSubmitError] = useState('')
  const [createdTeacher, setCreatedTeacher] = useState<{ name: string; email?: string; temporaryPassword?: string } | null>(null)

  const [form, setForm] = useState({
    fullName: '',
    displayName: '',
    email: '',
    phone: '',
    headline: '',
    headlineEn: '',
    institution: '',
    institutionEn: '',
    expertiseArea: '',
    expertiseAreaEn: '',
    about: '',
    aboutEn: '',
    photoUrl: '',
    isPublic: true,
    specialties: '',
    specialtiesEn: '',
    yearsExperience: '',
    credentialsText: '',
    credentialsTextEn: '',
  })

  function resetForm() {
    setSubmitError('')
    setForm({
      fullName: '',
      displayName: '',
      email: '',
      phone: '',
      headline: '',
      headlineEn: '',
      institution: '',
      institutionEn: '',
      expertiseArea: '',
      expertiseAreaEn: '',
      about: '',
      aboutEn: '',
      photoUrl: '',
      isPublic: true,
      specialties: '',
      specialtiesEn: '',
      yearsExperience: '',
      credentialsText: '',
      credentialsTextEn: '',
    })
  }

  function handleChange(key: string, value: string | boolean) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitError('')
    if (!form.fullName.trim() || !form.displayName.trim()) {
      toast.error('Full name and display name are required')
      return
    }
    if (!form.email.trim() && !form.phone.trim()) {
      const message = 'Enter an email address or phone number.'
      setSubmitError(message)
      toast.error(message)
      return
    }
    const email = form.email.trim().toLowerCase()
    const duplicate = (instructors.data ?? []).find((item) =>
      email && item.email?.trim().toLowerCase() === email,
    )
    if (duplicate) {
      const message = `${duplicate.fullName ?? 'This user'} is already an instructor with this email address. Open the existing instructor record to edit it.`
      setSubmitError(message)
      toast.error(message)
      return
    }

    try {
      const created = await createInstructor.mutateAsync({
        fullName: form.fullName.trim(),
        displayName: form.displayName.trim(),
        email: email || undefined,
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
      })
      toast.success('Teacher created successfully')
      setCreatedTeacher({ name: created.displayName ?? created.fullName ?? form.displayName, email: created.email, temporaryPassword: created.temporaryPassword })
    } catch (error) {
      const apiError = error as ApiError
      const message = apiError.status === 409
        ? apiError.message && !/^(request failed|conflict)/i.test(apiError.message)
          ? apiError.message
          : 'An account with this email or phone is already registered as an instructor. Edit the existing instructor instead.'
        : apiError.message || 'Failed to create teacher'
      setSubmitError(message)
      toast.error(message)
    }
  }

  // Track whether the user has entered any data — used to warn before closing.
  const isDirty = useMemo(() => {
    return Object.values(form).some((v) => {
      if (typeof v === 'boolean') return false // isPublic default is intentional
      return v !== ''
    })
  }, [form])

  const isSaving = createInstructor.isPending

  function handleOpenChange(next: boolean) {
    if (next) {
      setCreatedTeacher(null)
      setOpen(true)
      return
    }
    if (isSaving) return // never close during save
    if (isDirty) {
      const ok = window.confirm('You have unsaved changes. Discard them?')
      if (!ok) return
    }
    setOpen(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger
        render={
          <Button className="btn-blue-gradient text-white gap-1.5">
            <Plus className="h-4 w-4" />
            Add Teacher
          </Button>
        }
      />
      <DialogContent className="sm:max-w-lg max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add New Teacher</DialogTitle>
        </DialogHeader>
        {createdTeacher ? (
          <div className="grid gap-5">
            <div><p className="font-medium text-[#1c3873]">{createdTeacher.name} was created successfully.</p><p className="mt-1 text-sm text-slate-500">Provide these sign-in details to the teacher through a secure channel.</p></div>
            <div className="grid gap-3 rounded-lg border bg-slate-50 p-4">
              {createdTeacher.email && <div><Label>Email</Label><div className="mt-1 flex gap-2"><Input readOnly value={createdTeacher.email} /><Button type="button" variant="outline" onClick={() => navigator.clipboard.writeText(createdTeacher.email!)}>Copy</Button></div></div>}
              <div><Label>Temporary password</Label>{createdTeacher.temporaryPassword ? <div className="mt-1 flex gap-2"><Input readOnly value={createdTeacher.temporaryPassword} /><Button type="button" variant="outline" onClick={() => navigator.clipboard.writeText(createdTeacher.temporaryPassword!)}>Copy</Button></div> : <p className="mt-1 text-sm text-amber-700">The backend did not return a temporary password. Use the forgot-password flow for this account.</p>}</div>
            </div>
            <Button type="button" onClick={() => { resetForm(); setCreatedTeacher(null); setOpen(false) }}>Done</Button>
          </div>
        ) : <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {submitError && (
            <div role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
              {submitError}
            </div>
          )}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Full Name *</Label>
              <Input
                value={form.fullName}
                onChange={(e) => handleChange('fullName', e.target.value)}
                placeholder="Full name"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Display Name *</Label>
              <Input
                value={form.displayName}
                onChange={(e) => handleChange('displayName', e.target.value)}
                placeholder="Display name"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Email</Label>
              <Input
                type="email"
                value={form.email}
                onChange={(e) => { setSubmitError(''); handleChange('email', e.target.value) }}
                placeholder="email@example.com"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Phone</Label>
              <Input
                value={form.phone}
                onChange={(e) => { setSubmitError(''); handleChange('phone', e.target.value) }}
                placeholder="Phone number"
              />
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>Headline (Bangla)</Label>
            <Input
              value={form.headline}
              onChange={(e) => handleChange('headline', e.target.value)}
              placeholder="e.g. Senior Mathematics Instructor"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>Headline (English)</Label>
            <Input value={form.headlineEn} onChange={(e) => handleChange('headlineEn', e.target.value)} placeholder="e.g. Senior Mathematics Instructor" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Institution (Bangla)</Label>
              <Input
                value={form.institution}
                onChange={(e) => handleChange('institution', e.target.value)}
                placeholder="Institution"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Expertise Area (Bangla)</Label>
              <Input
                value={form.expertiseArea}
                onChange={(e) => handleChange('expertiseArea', e.target.value)}
                placeholder="e.g. Physics"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5"><Label>Institution (English)</Label><Input value={form.institutionEn} onChange={(e) => handleChange('institutionEn', e.target.value)} /></div>
            <div className="flex flex-col gap-1.5"><Label>Expertise (English)</Label><Input value={form.expertiseAreaEn} onChange={(e) => handleChange('expertiseAreaEn', e.target.value)} /></div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>About (Bangla)</Label>
            <Textarea
              value={form.about}
              onChange={(e) => handleChange('about', e.target.value)}
              placeholder="Short bio"
              rows={3}
            />
          </div>
          <div className="flex flex-col gap-1.5"><Label>About (English)</Label><Textarea value={form.aboutEn} onChange={(e) => handleChange('aboutEn', e.target.value)} rows={3} /></div>
          <div className="flex flex-col gap-1.5">
            <Label>Photo URL</Label>
            <Input
              value={form.photoUrl}
              onChange={(e) => handleChange('photoUrl', e.target.value)}
              placeholder="https://..."
            />
          </div>
          <div className="flex flex-col gap-1.5"><Label>Specialties — English (comma-separated)</Label><Input value={form.specialtiesEn} onChange={(e) => handleChange('specialtiesEn', e.target.value)} /></div>
          <div className="flex flex-col gap-1.5">
            <Label>Specialties (Bangla, comma-separated)</Label>
            <Input
              value={form.specialties}
              onChange={(e) => handleChange('specialties', e.target.value)}
              placeholder="Algebra, Calculus, Geometry"
            />
          </div>
          <div className="flex flex-col gap-1.5"><Label>Credentials (English)</Label><Input value={form.credentialsTextEn} onChange={(e) => handleChange('credentialsTextEn', e.target.value)} /></div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Years of Experience</Label>
              <Input
                type="number"
                min={0}
                value={form.yearsExperience}
                onChange={(e) => handleChange('yearsExperience', e.target.value)}
                placeholder="0"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Credentials (Bangla)</Label>
              <Input
                value={form.credentialsText}
                onChange={(e) => handleChange('credentialsText', e.target.value)}
                placeholder="e.g. PhD, M.Sc."
              />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-[#5e5f5c]">
            <input
              type="checkbox"
              checked={form.isPublic}
              onChange={(e) => handleChange('isPublic', e.target.checked)}
              className="h-4 w-4 rounded border-[#e5e5e5] accent-[#1c3873]"
            />
            Publicly visible
          </label>
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
              disabled={isSaving || !form.fullName.trim() || !form.displayName.trim()}
              className="btn-blue-gradient text-white"
            >
              {isSaving ? 'Creating…' : 'Create Teacher'}
            </Button>
          </DialogFooter>
        </form>}
      </DialogContent>
    </Dialog>
  )
}
