'use client'

import { useState, useEffect, useRef } from 'react'
import { useGoogleLogin } from '@/hooks/useAuth'
import { startGoogleSignIn } from '@/lib/google-signin'
import { useTranslations } from 'next-intl'
import { User, Phone, Mail, ChevronRight } from 'lucide-react'
import { useSignupContext } from './SignupContext'
import type { SignupChannel } from './SignupContext'
import { FormInput } from './FormInput'
import { StepLayout } from './StepLayout'
import { createAccountSchema, type CreateAccountInput } from '@/lib/validators'

export function CreateAccountStep() {
  const t = useTranslations('auth.signup')
  const googleMutation = useGoogleLogin()
  const [googleError, setGoogleError] = useState('')
  const [googleLoading, setGoogleLoading] = useState(false)
  const googleController = useRef<AbortController | null>(null)
  useEffect(() => () => googleController.current?.abort(), [])
  const handleGoogleLogin = async () => {
    setGoogleError('')
    setGoogleLoading(true)
    googleController.current?.abort()
    const controller = new AbortController()
    googleController.current = controller
    try { await startGoogleSignIn(credential => googleMutation.mutate({ credential }), controller.signal) }
    catch (error) { setGoogleError(error instanceof Error && error.message === 'GOOGLE_NOT_CONFIGURED' ? t('googleNotConfigured') : t('googleFailed')) }
    finally { setGoogleLoading(false) }
  }
  const { formData, updateField, goToStep } = useSignupContext()
  const channel = formData.channel
  const [errors, setErrors] = useState<Partial<Record<keyof CreateAccountInput, string>>>({})

  const validate = (): boolean => {
    const result = createAccountSchema.safeParse({
      fullname: formData.fullname,
      channel,
      email: formData.email,
      phone: formData.phone,
    })
    if (result.success) {
      setErrors({})
      return true
    }
    const fieldErrors: typeof errors = {}
    for (const issue of result.error.issues) {
      const field = issue.path[0] as keyof CreateAccountInput
      fieldErrors[field] = issue.message
    }
    setErrors(fieldErrors)
    return false
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (validate()) goToStep('password')
  }

  const setChannel = (c: SignupChannel) => {
    updateField('channel', c)
    updateField('email', '')
    updateField('phone', '')
    setErrors({})
  }

  const clearFieldError = (field: keyof CreateAccountInput) => {
    if (errors[field]) setErrors((prev) => {
      const rest = { ...prev }
      delete rest[field]
      return rest
    })
  }

  const hasErrors = Object.keys(errors).length > 0
  const allFilled = formData.fullname.trim().length > 0 &&
    (channel === 'EMAIL' ? formData.email.trim().length > 0 : formData.phone.trim().length > 0)

  return (
    <StepLayout
      title={t('title')}
      subtitle={t('subtitle')}
      onSubmit={handleSubmit}
      actions={
        <>
          <button
            type="submit"
            disabled={!allFilled || hasErrors}
            className={`flex h-[46px] w-full items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold text-white transition-all duration-200 ${
              allFilled && !hasErrors
                ? 'cursor-pointer shadow-[0px_-1px_0px_rgba(0,0,0,0.18)_inset,_0px_1px_0px_rgba(255,255,255,0.12)_inset] [background:linear-gradient(19.99deg,#4f67d8,#3a4fc2_60%,#2e3fa8)] hover:opacity-90'
                : 'cursor-not-allowed bg-[#d0d5dd]'
            }`}
          >
            {t('next')}
            <ChevronRight className="h-6 w-6" />
          </button>

          <button
            type="button"
            onClick={handleGoogleLogin}
            disabled={googleLoading || googleMutation.isPending}
            className="flex h-[46px] w-full cursor-pointer items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold text-[#1d4ed8] shadow-[0px_1px_0px_rgba(255,255,255,0.8)_inset,_0px_1px_2px_rgba(0,0,0,0.05),_0px_3px_8px_rgba(0,0,0,0.08)] [background:unset] transition-all duration-200 hover:bg-[#f9fafb]"
          >
            <svg viewBox="0 0 24 24" className="h-5 w-5">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            {googleLoading || googleMutation.isPending ? t('signingIn') : t('continueWithGoogle')}
          </button>
          {(googleError || googleMutation.isError) && <p role="alert" className="text-sm text-red-700">{googleError || t('googleFailed')}</p>}
        </>
      }
    >
      {/* Channel toggle */}
      <div className="flex w-full gap-6">
        <button
          type="button"
          onClick={() => setChannel('EMAIL')}
          className="flex flex-1 flex-col items-center gap-2"
        >
          <span className={`text-sm leading-5 ${channel === 'EMAIL' ? 'font-semibold text-[#1c3873]' : 'leading-[150%] text-[#5e5f5c]'}`}>
            {t('signupWithEmail')}
          </span>
          <div className={`h-px w-full ${channel === 'EMAIL' ? 'border-t border-[#1c3873]' : 'border-t border-[#d4d4d4]'}`} />
        </button>
        <button
          type="button"
          onClick={() => setChannel('PHONE')}
          className="flex flex-1 flex-col items-center gap-2"
        >
          <span className={`text-sm leading-5 ${channel === 'PHONE' ? 'font-semibold text-[#1c3873]' : 'leading-[150%] text-[#5e5f5c]'}`}>
            {t('signupWithPhone')}
          </span>
          <div className={`h-px w-full ${channel === 'PHONE' ? 'border-t border-[#1c3873]' : 'border-t border-[#d4d4d4]'}`} />
        </button>
      </div>

      <FormInput
        label={t('fullName')}
        name="fullname"
        placeholder={t('fullNamePlaceholder')}
        icon={<User className="h-4 w-4" />}
        value={formData.fullname}
        onChange={(e) => { updateField('fullname', e.target.value); clearFieldError('fullname') }}
        filled={formData.fullname.length > 0}
        error={errors.fullname}
      />

      {channel === 'EMAIL' ? (
        <FormInput
          label={t('email')}
          name="email"
          type="email"
          placeholder={t('emailPlaceholder')}
          icon={<Mail className="h-4 w-4" />}
          value={formData.email}
          onChange={(e) => { updateField('email', e.target.value); clearFieldError('email') }}
          error={errors.email}
        />
      ) : (
        <FormInput
          label={t('phone')}
          name="phone"
          type="tel"
          placeholder={t('phonePlaceholder')}
          icon={<Phone className="h-4 w-4" />}
          value={formData.phone}
          onChange={(e) => { updateField('phone', e.target.value); clearFieldError('phone') }}
          error={errors.phone}
        />
      )}
    </StepLayout>
  )
}
