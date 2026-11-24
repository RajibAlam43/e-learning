'use client'

import { Suspense, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import { OTPInput } from '@/components/auth/OTPInput'
import { PasswordInput } from '@/components/auth/PasswordInput'
import { StepLayout } from '@/components/auth/StepLayout'
import { useResetPassword } from '@/hooks/useAuth'
import { ROUTES } from '@/constants/routes'
import { AuthShell } from '@/components/auth/AuthShell'

function ResetPasswordForm() {
  const t = useTranslations('auth.resetPassword')
  const searchParams = useSearchParams()
  const rawChannel = searchParams.get('channel')
  const channel = rawChannel === 'PHONE' ? 'PHONE' : 'EMAIL'
  const identifier = searchParams.get('identifier') ?? ''

  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const resetMutation = useResetPassword()

  const passwordsMatch = newPassword.length > 0 && newPassword === confirmPassword
  const allValid = otp.length === 6 && newPassword.length >= 8 && passwordsMatch

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!allValid) return
    resetMutation.mutate({ channel, identifier, code: otp, newPassword })
  }

  if (!identifier) {
    return (
      <AuthShell showFooter={false}>
        <div className="flex w-full flex-col items-center gap-4 py-8">
          <p className="text-sm text-[#71717a]">{t('invalidLink')}</p>
          <Link href={ROUTES.forgotPassword} className="text-sm font-semibold text-[#4f67d8] hover:underline">{t('requestReset')}</Link>
        </div>
      </AuthShell>
    )
  }

  return (
    <AuthShell showFooter={false}>
      <StepLayout
        title={t('title')}
        subtitle={t('subtitle')}
        onSubmit={handleSubmit}
        actions={
          <>
            <button
              type="submit"
              disabled={!allValid || resetMutation.isPending}
              className={`flex h-[46px] w-full items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold text-white transition-all duration-200 ${
                allValid && !resetMutation.isPending
                  ? 'cursor-pointer shadow-[0px_-1px_0px_rgba(0,0,0,0.18)_inset,_0px_1px_0px_rgba(255,255,255,0.12)_inset] [background:linear-gradient(19.99deg,#4f67d8,#3a4fc2_60%,#2e3fa8)] hover:opacity-90'
                  : 'cursor-not-allowed bg-[#d0d5dd]'
              }`}
            >
              {resetMutation.isPending ? t('resetting') : t('submit')}
            </button>

            {resetMutation.isError && (
              <div className="rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-600">
                {resetMutation.error?.message || t('error')}
              </div>
            )}

            <Link href={ROUTES.login} className="self-center text-sm text-[#71717a] hover:text-[#4f67d8]">{t('backToLogin')}</Link>
          </>
        }
      >
        <OTPInput value={otp} onChange={setOtp} />
        <PasswordInput label={t('newPassword')} name="newPassword" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
        <PasswordInput label={t('confirmPassword')} name="confirmPassword" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} success={passwordsMatch} />
      </StepLayout>
    </AuthShell>
  )
}

export default function ResetPasswordClient() {
  return (
    <Suspense fallback={
      <div className="flex w-full items-center justify-center overflow-auto bg-[#e8ebf1] p-4 md:p-5 max-lg:min-h-screen">
        <div className="flex w-full max-w-[562px] items-center justify-center rounded-3xl bg-white px-16 py-10">
          <div className="flex flex-col items-center gap-6">
            <div className="h-14 w-14 animate-pulse rounded-xl bg-[#f3f4f6]" />
            <div className="h-6 w-48 animate-pulse rounded bg-[#f3f4f6]" />
          </div>
        </div>
      </div>
    }>
      <ResetPasswordForm />
    </Suspense>
  )
}
