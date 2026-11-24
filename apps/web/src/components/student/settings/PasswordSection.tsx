'use client'

import { useTranslations } from 'next-intl'

import { useState } from 'react'
import { z } from 'zod/v4'
import { useAuthStore } from '@/store/auth.store'
import { useForgotPassword } from '@/hooks/useAuth'
import { PasswordInput } from '@/components/auth/PasswordInput'
import { Mail, Loader2, KeyRound, Shield, Check } from 'lucide-react'

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Must contain an uppercase letter')
      .regex(/[a-z]/, 'Must contain a lowercase letter')
      .regex(/[0-9]/, 'Must contain a number')
      .regex(/[^A-Za-z0-9]/, 'Must contain a special character'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type PasswordForm = z.infer<typeof passwordSchema>

function Requirement({ met, text }: { met: boolean; text: string }) {
  return (
    <div className={`flex items-center gap-1.5 text-xs transition-colors ${met ? 'text-green-600' : 'text-[#a3a3a3]'}`}>
      <div className={`w-3.5 h-3.5 rounded-full flex items-center justify-center ${met ? 'bg-green-100' : 'bg-gray-100'}`}>
        {met && <Check className='w-2.5 h-2.5' />}
      </div>
      {text}
    </div>
  )
}

export default function PasswordSection() {
  const ui = useTranslations('privateUi')

  const forgotPassword = useForgotPassword()
  const [form, setForm] = useState<PasswordForm>({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [errors, setErrors] = useState<Partial<Record<keyof PasswordForm, string>>>({})
  const [sent, setSent] = useState(false)

  const handleChange = (field: keyof PasswordForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  // No dedicated change-password endpoint — uses forgot-password flow
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const result = passwordSchema.safeParse(form)
    if (!result.success) {
      const fieldErrors: Partial<Record<keyof PasswordForm, string>> = {}
      for (const issue of result.error.issues) {
        const key = issue.path[0] as keyof PasswordForm
        if (!fieldErrors[key]) fieldErrors[key] = issue.message
      }
      setErrors(fieldErrors)
      return
    }

    const email = useAuthStore.getState().user?.email
    if (!email) return

    forgotPassword.mutate(
      { channel: 'EMAIL', identifier: email },
      { onSuccess: () => setSent(true) },
    )
  }

  return (
    <section className='bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden'>
      <div className='p-4 md:p-6 border-b border-gray-100'>
        <div className='flex items-center gap-3'>
          <div className='w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center'>
            <Shield className='w-5 h-5 text-[#3b82f6]' />
          </div>
          <div>
            <h2 className='text-base font-medium font-switzer text-[#1c3873]'>{ui('changePassword')}</h2>
            <p className='text-sm text-[#5e5f5c]'>{ui('updateYourAccountPassword')}</p>
          </div>
        </div>
      </div>
      <div className='p-4 md:p-6'>
        {sent ? (
          <div className='flex flex-col items-center gap-3 py-6 text-center'>
            <div className='w-12 h-12 rounded-full bg-green-50 flex items-center justify-center'>
              <Mail className='w-6 h-6 text-green-600' />
            </div>
            <p className='text-sm text-[#5e5f5c]'>{ui('passwordResetLinkSentToYourEmail')}</p>
            <button
              type='button'
              onClick={() => {
                setSent(false)
                setForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
              }}
              className='text-sm text-[#3b82f6] font-medium hover:underline'
            >
              {ui('sendAgain')}</button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className='flex flex-col gap-5'>
            <PasswordInput
              label={ui('currentPassword')}
              name='currentPassword'
              value={form.currentPassword}
              onChange={(e) => handleChange('currentPassword', e.target.value)}
              error={errors.currentPassword}
            />
            <PasswordInput
              label={ui('newPassword')}
              name='newPassword'
              value={form.newPassword}
              onChange={(e) => handleChange('newPassword', e.target.value)}
              error={errors.newPassword}
            />
            <PasswordInput
              label={ui('confirmNewPassword')}
              name='confirmPassword'
              value={form.confirmPassword}
              onChange={(e) => handleChange('confirmPassword', e.target.value)}
              error={errors.confirmPassword}
            />

            <div className='flex flex-col gap-1'>
              <p className='text-xs font-medium text-[#5e5f5c]'>{ui('passwordMustContain')}</p>
              <Requirement met={form.newPassword.length >= 8} text='At least 8 characters' />
              <Requirement met={/[A-Z]/.test(form.newPassword)} text='One uppercase letter' />
              <Requirement met={/[a-z]/.test(form.newPassword)} text='One lowercase letter' />
              <Requirement met={/[0-9]/.test(form.newPassword)} text='One number' />
              <Requirement met={/[^A-Za-z0-9]/.test(form.newPassword)} text='One special character' />
            </div>

            <div className='flex justify-end pt-2'>
              <button
                type='submit'
                disabled={forgotPassword.isPending}
                className='h-10 px-5 flex items-center justify-center gap-2 rounded-md bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] text-white text-sm font-medium font-switzer shadow-[inset_0px_1px_0px_#ffffff1f,inset_0px_-1px_0px_#0000002e] disabled:opacity-50 hover:opacity-90 transition-opacity cursor-pointer'
              >
                {forgotPassword.isPending ? <Loader2 className='w-4 h-4 animate-spin' /> : <KeyRound className='w-4 h-4' />}
                {ui('updatePassword')}</button>
            </div>
          </form>
        )}
      </div>
    </section>
  )
}
