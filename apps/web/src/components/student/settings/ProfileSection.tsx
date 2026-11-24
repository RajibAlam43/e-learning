'use client'

import { useTranslations } from 'next-intl'

import { useState } from 'react'
import { z } from 'zod/v4'
import Image from 'next/image'
import type { MeResponse } from '@/types'
import type { components } from '@/types/api'
import { useAuthStore } from '@/store/auth.store'
import { useProfile, useUpdateProfile } from '@/hooks/useStudentDashboard'
import { User, Mail, Phone, Check, Loader2, Globe, Clock, Briefcase, GraduationCap, Award, BookOpen, Star } from 'lucide-react'

type UpdateProfileRequest = components['schemas']['UpdateProfileRequest']

const profileSchema = z.object({
  fullName: z
    .string()
    .min(2, 'Name must be at least 2 characters')
    .max(100, 'Name must be under 100 characters')
    .regex(/^[a-zA-Z\s.'-]+$/, 'Name can only contain letters, spaces, dots, hyphens'),
  email: z.union([z.literal(''), z.string().email('Please enter a valid email address')]),
  phone: z
    .string()
    .regex(/^$|^[\d+\-() ]{7,20}$/, 'Please enter a valid phone number')
    .or(z.literal('')),
  bio: z.string().max(300, 'Bio must be under 300 characters').optional(),
  timezone: z.string().optional(),
  locale: z.string().optional(),
  avatarUrl: z.string().optional(),
  // Instructor fields
  headline: z.string().max(150, 'Headline must be under 150 characters').optional(),
  institution: z.string().max(100).optional(),
  expertiseArea: z.string().max(100).optional(),
  about: z.string().max(1000).optional(),
  credentialsText: z.string().max(300).optional(),
  yearsExperience: z.coerce.number().min(0).max(50).optional().or(z.literal('')),
  specialties: z.string().optional(), // We'll split this by comma before sending
})

type ProfileForm = z.infer<typeof profileSchema>

const emptyForm: ProfileForm = {
  fullName: '', email: '', phone: '', bio: '', timezone: '', locale: '',
  avatarUrl: '', headline: '', institution: '', expertiseArea: '', about: '',
  credentialsText: '', yearsExperience: '', specialties: '',
}

function toProfileForm(profile?: MeResponse | null): ProfileForm {
  const instructor = profile?.instructorProfile
  return {
    ...emptyForm,
    fullName: profile?.fullName ?? '',
    email: profile?.email ?? '',
    phone: profile?.phone ?? '',
    bio: profile?.bio ?? '',
    timezone: profile?.timezone ?? '',
    locale: profile?.locale ?? '',
    avatarUrl: profile?.avatarUrl ?? '',
    headline: instructor?.headline ?? '',
    institution: instructor?.institution ?? '',
    expertiseArea: instructor?.expertiseArea ?? '',
    about: instructor?.about ?? '',
    credentialsText: instructor?.credentialsText ?? '',
    yearsExperience: instructor?.yearsExperience ?? '',
    specialties: instructor?.specialties?.join(', ') ?? '',
  }
}

function InputField({ label, type = 'text', icon, value, onChange, error, placeholder }: {
  label: string
  type?: string
  icon: React.ReactNode
  value: string | number
  onChange: (v: string) => void
  error?: string
  placeholder: string
}) {
  return (
    <div className='flex flex-col gap-1'>
      <label className='text-sm font-semibold text-[#1b1c1a]'>{label}</label>
      <div className={`flex h-11 items-center gap-2 rounded-lg border bg-white px-3 py-2 transition-colors ${error ? 'border-[#fca5a5]' : 'border-[#e5e7eb] focus-within:border-[#1c3873]'}`}>
        <span className='text-[#667085]'>{icon}</span>
        <input
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className='flex-1 border-none bg-transparent text-sm text-[#5e5f5c] outline-none'
        />
      </div>
      {error && <p className='text-xs text-red-500'>{error}</p>}
    </div>
  )
}

function TextAreaField({ label, value, onChange, error, placeholder, maxLength }: {
  label: string
  value: string
  onChange: (v: string) => void
  error?: string
  placeholder: string
  maxLength: number
}) {
  return (
    <div className='flex flex-col gap-1'>
      <label className='text-sm font-semibold text-[#1b1c1a]'>{label}</label>
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        maxLength={maxLength}
        rows={3}
        className={`w-full rounded-lg border ${error ? 'border-[#fca5a5]' : 'border-[#e5e7eb] focus:border-[#1c3873]'} bg-white px-3 py-2 text-sm text-[#5e5f5c] focus:outline-none transition-colors resize-none`}
      />
      <div className='flex justify-between'>
        {error && <p className='text-xs text-red-500'>{error}</p>}
        <p className='text-xs text-[#a3a3a3] ml-auto'>{value?.length ?? 0}/{maxLength}</p>
      </div>
    </div>
  )
}

export default function ProfileSection() {
  const ui = useTranslations('privateUi')

  const storeUser = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const { data: profile, isLoading } = useProfile()
  const updateProfile = useUpdateProfile()

  const [form, setForm] = useState<ProfileForm>(emptyForm)
  const [errors, setErrors] = useState<Partial<Record<keyof ProfileForm, string>>>({})
  const [saved, setSaved] = useState(false)
  const [isDirty, setIsDirty] = useState(false)
  const [syncedProfile, setSyncedProfile] = useState<MeResponse | undefined>()

  // React's guarded state-adjustment pattern keeps untouched fields synchronized
  // without an effect/render flash. Background refetches never overwrite a draft.
  if (profile !== syncedProfile) {
    setSyncedProfile(profile)
    if (!isDirty && profile) setForm(toProfileForm(profile))
  }

  const handleChange = (field: keyof ProfileForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
    setIsDirty(true)
    setSaved(false)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const result = profileSchema.safeParse(form)
    if (!result.success) {
      const fieldErrors: Partial<Record<keyof ProfileForm, string>> = {}
      for (const issue of result.error.issues) {
        const key = issue.path[0] as keyof ProfileForm
        if (!fieldErrors[key]) fieldErrors[key] = issue.message
      }
      setErrors(fieldErrors)
      return
    }

    const payload: UpdateProfileRequest = {
      fullName: form.fullName,
      email: form.email || undefined,
      phone: form.phone || undefined,
      bio: form.bio || undefined,
      timezone: form.timezone || undefined,
      locale: form.locale || undefined,
      avatarUrl: form.avatarUrl || undefined,
    }

    if (storeUser?.roles.includes('INSTRUCTOR')) {
      if (form.headline) payload.headline = form.headline
      if (form.institution) payload.institution = form.institution
      if (form.expertiseArea) payload.expertiseArea = form.expertiseArea
      if (form.about) payload.about = form.about
      if (form.credentialsText) payload.credentialsText = form.credentialsText
      if (form.yearsExperience !== '') payload.yearsExperience = Number(form.yearsExperience)
      if (form.specialties) payload.specialties = form.specialties.split(',').map((s) => s.trim()).filter(Boolean)
    }

    updateProfile.mutate(
      payload,
      {
        onSuccess: (data) => {
          setUser({
            id: data.userId ?? storeUser?.id ?? '',
            fullName: data.fullName ?? '',
            email: data.email,
            phone: data.phone,
            roles: data.roles as ('STUDENT' | 'INSTRUCTOR' | 'ADMIN')[] ?? storeUser?.roles ?? [],
            avatar: data.avatarUrl,
          })
          setForm(toProfileForm(data))
          setSyncedProfile(data)
          setIsDirty(false)
          setSaved(true)
        },
      },
    )
  }

  if (isLoading) {
    return (
      <section className='bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden'>
        <div className='p-4 md:p-6 border-b border-gray-100'>
          <div className='flex items-center gap-3'>
            <div className='w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center'>
              <User className='w-5 h-5 text-[#3b82f6]' />
            </div>
            <div>
              <h2 className='text-base font-medium font-switzer text-[#1c3873]'>{ui('profileInformation')}</h2>
              <p className='text-sm text-[#5e5f5c]'>{ui('updateYourPersonalDetails')}</p>
            </div>
          </div>
        </div>
        <div className='p-4 md:p-6 space-y-4 animate-pulse'>
          {[1, 2, 3].map((i) => (
            <div key={i} className='h-11 bg-gray-100 rounded-lg' />
          ))}
        </div>
      </section>
    )
  }

  const isInstructor = storeUser?.roles.includes('INSTRUCTOR')

  return (
    <section className='bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden'>
      <div className='p-4 md:p-6 border-b border-gray-100'>
        <div className='flex items-center gap-3'>
          <div className='w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center'>
            <User className='w-5 h-5 text-[#3b82f6]' />
          </div>
          <div>
            <h2 className='text-base font-medium font-switzer text-[#1c3873]'>{ui('profileInformation')}</h2>
            <p className='text-sm text-[#5e5f5c]'>{ui('updateYourPersonalDetailsAndPublicProfile')}</p>
          </div>
        </div>
      </div>
      <div className='p-4 md:p-6'>
        <form onSubmit={handleSubmit} className='flex flex-col gap-6'>
          {/* Avatar */}
          <div className='flex items-center gap-4'>
            <div className='relative w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center text-xl font-medium text-[#1c3873] font-switzer shrink-0 overflow-hidden'>
              {profile?.avatarUrl || storeUser?.avatar ? (
                <Image
                  src={profile?.avatarUrl || storeUser?.avatar || ''}
                  alt={ui('profile')}
                  fill
                  sizes="64px"
                  unoptimized
                  className="object-cover"
                />
              ) : (
                (profile?.fullName || storeUser?.fullName)?.charAt(0)?.toUpperCase() ?? 'S'
              )}
            </div>
            <div>
              <p className='text-sm font-medium text-[#1c3873]'>{(profile ?? storeUser)?.fullName}</p>
              <p className='text-xs text-[#5e5f5c]'>{(profile ?? storeUser)?.roles?.[0] ?? ui('student')}</p>
            </div>
          </div>

          <div className='grid grid-cols-1 md:grid-cols-2 gap-5'>
            <InputField label={ui('fullName')} icon={<User className='w-4 h-4' />} value={form.fullName} onChange={(v) => handleChange('fullName', v)} error={errors.fullName} placeholder={ui('enterYourFullName')} />
            <InputField label={ui('email')} type='email' icon={<Mail className='w-4 h-4' />} value={form.email} onChange={(v) => handleChange('email', v)} error={errors.email} placeholder={ui('enterYourEmail')} />
            <InputField label={ui('phone')} type='tel' icon={<Phone className='w-4 h-4' />} value={form.phone} onChange={(v) => handleChange('phone', v)} error={errors.phone} placeholder={ui('enterYourPhoneNumber')} />
            <div className='grid grid-cols-2 gap-4'>
              <InputField label={ui('timezone')} icon={<Clock className='w-4 h-4' />} value={form.timezone ?? ''} onChange={(v) => handleChange('timezone', v)} error={errors.timezone} placeholder={ui('eGUtcEst')} />
              <InputField label={ui('locale')} icon={<Globe className='w-4 h-4' />} value={form.locale ?? ''} onChange={(v) => handleChange('locale', v)} error={errors.locale} placeholder={ui('eGEnBn')} />
            </div>
          </div>

          <TextAreaField label={ui('bio')} value={form.bio ?? ''} onChange={(v) => handleChange('bio', v)} error={errors.bio} placeholder={ui('tellUsAboutYourself')} maxLength={300} />

          {isInstructor && (
            <div className='pt-4 mt-2 border-t border-gray-100 flex flex-col gap-6'>
              <div>
                <h3 className='text-sm font-medium font-switzer text-[#1c3873] mb-1'>{ui('instructorProfile')}</h3>
                <p className='text-xs text-[#5e5f5c]'>{ui('thisInformationWillBeDisplayedOnYourPublicInstructorPage')}</p>
              </div>
              
              <div className='grid grid-cols-1 md:grid-cols-2 gap-5'>
                <InputField label={ui('headline')} icon={<Briefcase className='w-4 h-4' />} value={form.headline ?? ''} onChange={(v) => handleChange('headline', v)} error={errors.headline} placeholder={ui('eGSeniorArabicInstructor')} />
                <InputField label={ui('institution')} icon={<GraduationCap className='w-4 h-4' />} value={form.institution ?? ''} onChange={(v) => handleChange('institution', v)} error={errors.institution} placeholder={ui('eGAlAzharUniversity')} />
                <InputField label={ui('expertiseArea')} icon={<Star className='w-4 h-4' />} value={form.expertiseArea ?? ''} onChange={(v) => handleChange('expertiseArea', v)} error={errors.expertiseArea} placeholder={ui('eGQuranicStudiesFiqh')} />
                <InputField label={ui('yearsOfExp')} type='number' icon={<Award className='w-4 h-4' />} value={form.yearsExperience ?? ''} onChange={(v) => handleChange('yearsExperience', v)} error={errors.yearsExperience} placeholder={ui('eG10')} />
                <div className='md:col-span-2'>
                  <InputField label={ui('specialtiesCommaSeparated')} icon={<BookOpen className='w-4 h-4' />} value={form.specialties ?? ''} onChange={(v) => handleChange('specialties', v)} error={errors.specialties} placeholder={ui('eGTajweedHadithArabicGrammar')} />
                </div>
              </div>

              <TextAreaField label={ui('about')} value={form.about ?? ''} onChange={(v) => handleChange('about', v)} error={errors.about} placeholder={ui('writeADetailedAboutMeSection')} maxLength={1000} />
              <TextAreaField label={ui('credentialsCertifications')} value={form.credentialsText ?? ''} onChange={(v) => handleChange('credentialsText', v)} error={errors.credentialsText} placeholder={ui('listYourCredentials')} maxLength={300} />
            </div>
          )}

          <div className='flex items-center justify-end gap-3 pt-4 mt-2 border-t border-gray-100'>
            {saved && (
              <span className='inline-flex items-center gap-1 text-sm text-green-600 animate-in fade-in'>
                <Check className='w-4 h-4' /> {ui('saved')}</span>
            )}
            <button
              type='submit'
              disabled={updateProfile.isPending}
              className='h-10 px-5 flex items-center justify-center gap-2 rounded-md bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] text-white text-sm font-medium font-switzer shadow-[inset_0px_1px_0px_#ffffff1f,inset_0px_-1px_0px_#0000002e] disabled:opacity-50 hover:opacity-90 transition-opacity cursor-pointer'
            >
              {updateProfile.isPending && <Loader2 className='w-4 h-4 animate-spin' />}
              {ui('saveChanges')}</button>
          </div>
        </form>
      </div>
    </section>
  )
}
