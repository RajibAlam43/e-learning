'use client'

import { Suspense, useState, useEffect, useRef } from 'react'
import { useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import { Mail, Phone } from 'lucide-react'
import { AuthShell } from '@/components/auth/AuthShell'
import { OTPInput } from '@/components/auth/OTPInput'
import { useVerifyCode, useSendVerification } from '@/hooks/useAuth'
import { ROUTES } from '@/constants/routes'

function VerifyEmailForm() {
  const t = useTranslations('auth.verify')
  const searchParams = useSearchParams()

  const rawChannel = searchParams.get('channel')
  const channel = rawChannel === 'PHONE' ? 'PHONE' as const : 'EMAIL' as const
  const identifier = searchParams.get('identifier') || ''
  const redirect = searchParams.get('redirect') || undefined

  const [otp, setOtp] = useState('')
  const [countdown, setCountdown] = useState(0)
  const verifyMutation = useVerifyCode(redirect)
  const sendVerification = useSendVerification()
  const hasAutoSent = useRef(false)

  const purpose = channel === 'EMAIL' ? 'EMAIL_VERIFICATION' : 'PHONE_VERIFICATION'

  useEffect(() => {
    if (!identifier || hasAutoSent.current) return
    hasAutoSent.current = true
    sendVerification.mutate({ channel, identifier, purpose })
    setCountdown(30)
  }, [identifier, channel, purpose, sendVerification])

  useEffect(() => {
    if (countdown <= 0) return
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000)
    return () => clearTimeout(timer)
  }, [countdown])

  const handleVerify = (e: React.FormEvent) => {
    e.preventDefault()
    if (otp.length !== 6 || !identifier) return
    verifyMutation.mutate({ channel, identifier, code: otp, purpose })
  }

  const handleResend = () => {
    if (!identifier || sendVerification.isPending || countdown > 0) return
    sendVerification.mutate({ channel, identifier, purpose })
    setCountdown(30)
  }

  if (!identifier) {
    return (
      <AuthShell showFooter={false}>
        <div className="flex w-full flex-col items-center gap-4 py-8">
          <p className="text-sm text-[#71717a]">{t('invalidLink')}</p>
          <Link href={ROUTES.login} className="text-sm font-semibold text-[#4f67d8] hover:underline">{t('backToLogin')}</Link>
        </div>
      </AuthShell>
    )
  }

  return (
    <AuthShell showFooter={false}>
      <form onSubmit={handleVerify} className="flex w-full flex-col items-start gap-6">
        <div className="flex w-full flex-col items-start gap-1">
          <h3 className="m-0 w-full font-heading text-2xl font-semibold leading-8 text-[#1c3873] max-md:text-xl max-md:leading-7">{t('title')}</h3>
          <p className="w-full text-sm leading-[150%] text-[#5e5f5c]">{t('subtitle')}</p>
          <div className="flex items-center gap-2 text-sm text-[#71717a]">
            {channel === 'EMAIL' ? <Mail className="h-4 w-4" /> : <Phone className="h-4 w-4" />}
            <span className="font-medium text-[#1c3873]">{identifier}</span>
          </div>
        </div>

        <OTPInput value={otp} onChange={setOtp} />

        <div className="flex w-full flex-col items-start gap-3">
          <button
            type="submit"
            disabled={otp.length !== 6 || verifyMutation.isPending}
            className="flex h-11 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold leading-6 text-white transition-all duration-200 btn-blue-gradient hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {verifyMutation.isPending ? t('verifying') : t('submit')}
          </button>

          {verifyMutation.isError && (
            <div className="w-full rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-600">
              {(verifyMutation.error as { message?: string })?.message || t('verificationFailed')}
            </div>
          )}

          <div className="w-full text-center text-sm text-[#5e5f5c]">
            <span className="leading-[150%]">{t('didntGetCode')} </span>
            <button
              type="button"
              onClick={handleResend}
              disabled={sendVerification.isPending || countdown > 0}
              className="font-semibold text-[#ef4444] hover:underline disabled:opacity-50"
            >
              {sendVerification.isPending
                ? t('sending')
                : countdown > 0
                  ? t('resendIn', { seconds: countdown })
                  : t('resend')}
            </button>
          </div>

          <Link href={ROUTES.login} className="self-center text-sm text-[#71717a] hover:text-[#4f67d8]">{t('backToLogin')}</Link>
        </div>
      </form>
    </AuthShell>
  )
}

export default function VerifyAccountClient() {
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
      <VerifyEmailForm />
    </Suspense>
  )
}
