'use client'

import { useState } from 'react'
import { useTranslations } from 'next-intl'
import { Check, Loader2 } from 'lucide-react'
import { useSignupContext } from './SignupContext'
import { PasswordInput } from './PasswordInput'
import { StepLayout } from './StepLayout'
import { LegalAgreement } from './LegalAgreement'
import { useRegister } from '@/hooks/useAuth'
import { passwordSchema, type PasswordInput as PasswordInputType } from '@/lib/validators'

export function SetupPasswordStep() {
  const t = useTranslations('auth.signup')
  const { formData, updateField, goToStep } = useSignupContext()
  const { password, confirmPassword, agreedToTerms } = formData

  const [registerError, setRegisterError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof PasswordInputType, string>>>({})

  const registerMutation = useRegister()

  const checks = {
    length: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    number: /[0-9]/.test(password),
  }
  const strength = Object.values(checks).filter(Boolean).length
  const passwordsMatch = password.length > 0 && password === confirmPassword

  const validate = (): boolean => {
    const result = passwordSchema.safeParse({
      password,
      confirmPassword,
      agreedToTerms,
    })
    if (result.success) {
      setFieldErrors({})
      return true
    }
    const errs: typeof fieldErrors = {}
    for (const issue of result.error.issues) {
      const field = issue.path[0] as keyof PasswordInputType
      errs[field] = issue.message
    }
    setFieldErrors(errs)
    return false
  }

  const isBusy = registerMutation.isPending

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setRegisterError(null)

    const payload = {
      fullName: formData.fullname,
      email: formData.channel === 'EMAIL' ? formData.email : undefined,
      phoneNumber: formData.channel === 'PHONE' ? formData.phone : undefined,
      password: formData.password,
    }

    registerMutation.mutate(payload, {
      onSuccess: () => goToStep('verify'),
      onError: (err) => {
        setRegisterError((err as { message?: string })?.message || t('signupFailed'))
      },
    })
  }

  const clearFieldError = (field: keyof PasswordInputType) => {
    if (fieldErrors[field]) setFieldErrors((prev) => {
      const rest = { ...prev }
      delete rest[field]
      return rest
    })
  }

  const hasFieldErrors = Object.keys(fieldErrors).length > 0

  return (
    <StepLayout
      title={t('password')}
      subtitle={t('subtitle')}
      onSubmit={handleSubmit}
      actions={
        <>
          <button
            type="submit"
            disabled={isBusy || hasFieldErrors}
            className={`flex h-[46px] w-full items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold text-white transition-all duration-200 ${
              !isBusy && !hasFieldErrors
                ? 'cursor-pointer shadow-[0px_-1px_0px_rgba(0,0,0,0.18)_inset,_0px_1px_0px_rgba(255,255,255,0.12)_inset] [background:linear-gradient(19.99deg,#4f67d8,#3a4fc2_60%,#2e3fa8)] hover:opacity-90'
                : 'cursor-not-allowed bg-[#d0d5dd]'
            }`}
          >
            {isBusy ? (
              <>
                <Loader2 className="h-5 w-5 animate-spin" />
                {t('creating')}
              </>
            ) : (
              t('submit')
            )}
          </button>

          {!isBusy && (
            <button
              type="button"
              onClick={() => goToStep('create')}
              className="cursor-pointer text-sm text-[#71717a] hover:text-[#4f67d8]"
            >
              {t('back')}
            </button>
          )}
        </>
      }
    >
      {/* Error message */}
      {registerError && (
        <div className="w-full rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-600">
          {registerError}
        </div>
      )}

      <div className={isBusy ? 'pointer-events-none opacity-60' : ''}>
        <PasswordInput
          label={t('enterPassword')}
          name="password"
          value={password}
          onChange={(e) => { updateField('password', e.target.value); clearFieldError('password') }}
          error={fieldErrors.password}
          disabled={isBusy}
        />

        {password.length > 0 && (
          <div className="flex flex-col items-start gap-3 text-xs">
            <div className="flex w-full items-center gap-1">
              {[0, 1, 2, 3].map((i) => (
                <div
                  key={i}
                  className={`h-1 flex-1 rounded-sm transition-colors duration-300 ${
                    i < strength ? 'bg-[#f59e0b]' : 'bg-[#e5e7eb]'
                  }`}
                />
              ))}
            </div>
            <div className="self-stretch font-semibold leading-5">{t('passwordStrength')}</div>
          </div>
        )}

        <div className="flex self-stretch flex-col gap-[5px] rounded bg-[#f3f4f6] p-3">
          {[
            { label: t('ruleLength'), met: checks.length },
            { label: t('ruleUppercase'), met: checks.uppercase },
            { label: t('ruleLowercase'), met: checks.lowercase },
            { label: t('ruleNumber'), met: checks.number },
          ].map((rule) => (
            <div key={rule.label} className="flex self-stretch items-center gap-2">
              <Check
                className={`h-4 w-4 shrink-0 ${rule.met ? 'text-[#22c55e]' : 'text-[#d0d5dd]'}`}
                strokeWidth={rule.met ? 2.5 : 2}
              />
              <div className="text-sm leading-[150%] text-[#71717a]">{rule.label}</div>
            </div>
          ))}
        </div>

        <PasswordInput
          label={t('confirmPassword')}
          name="confirmPassword"
          value={confirmPassword}
          onChange={(e) => { updateField('confirmPassword', e.target.value); clearFieldError('confirmPassword') }}
          success={passwordsMatch && !fieldErrors.confirmPassword}
          error={fieldErrors.confirmPassword}
          disabled={isBusy}
        />

        {passwordsMatch && !fieldErrors.confirmPassword && (
          <div className="flex items-center gap-2.5 rounded-md bg-[#dcfce7] px-4 py-2.5">
            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-[#22c55e]">
              <Check className="h-3 w-3 text-white" strokeWidth={3} />
            </div>
            <span className="text-sm font-semibold text-[#15803d]">{t('passwordsMatched')}</span>
          </div>
        )}

        <LegalAgreement checked={agreedToTerms} onChange={checked => { updateField('agreedToTerms', checked); clearFieldError('agreedToTerms') }} disabled={isBusy} error={fieldErrors.agreedToTerms} />
      </div>
    </StepLayout>
  )
}
