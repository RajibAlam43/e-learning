'use client'

import { useState } from 'react'
import Link from 'next/link'
import { Mail, Phone, CheckCircle } from 'lucide-react'
import { FormInput } from '@/components/auth/FormInput'
import { StepLayout } from '@/components/auth/StepLayout'
import { useForgotPassword } from '@/hooks/useAuth'
import { ROUTES } from '@/constants/routes'
import { AuthShell } from '@/components/auth/AuthShell'
import { useTranslations } from 'next-intl'

type Channel = 'EMAIL' | 'PHONE'

export default function ForgotPasswordClient() {
  const t = useTranslations('auth.forgotPassword')
  const [channel, setChannel] = useState<Channel>('EMAIL')
  const [identifier, setIdentifier] = useState('')
  const [sent, setSent] = useState(false)

  const forgotMutation = useForgotPassword()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!identifier.trim()) return
    forgotMutation.mutate(
      { channel, identifier: identifier.trim() },
      { onSuccess: () => setSent(true) }
    )
  }

  if (sent) {
    return (
      <AuthShell showFooter={false}>
        <div className="flex w-full flex-col items-center gap-6">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[#dcfce7]">
            <CheckCircle className="h-8 w-8 text-[#22c55e]" />
          </div>
          <div className="flex flex-col items-center gap-1 text-center">
            <h3 className="m-0 font-heading text-2xl font-semibold text-[#1c3873]">
              {t('success')}
            </h3>
            <p className="text-sm text-[#71717a]">
              {t('checkMessage', { channel: channel === 'EMAIL' ? t('email') : t('phone') })}
            </p>
          </div>
          <Link
            href={{
              pathname: ROUTES.resetPassword,
              query: { channel, identifier },
            }}
            className="flex h-[46px] w-full items-center justify-center rounded-md bg-[#4f67d8] px-5 py-2.5 text-base font-semibold text-white hover:opacity-90"
          >
            {t('enterResetCode')}
          </Link>
          <Link
            href={ROUTES.login}
            className="text-sm text-[#71717a] hover:text-[#4f67d8]"
          >
            {t('backToLogin')}
          </Link>
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
              disabled={!identifier.trim() || forgotMutation.isPending}
              className={`flex h-[46px] w-full items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold text-white transition-all duration-200 ${
                identifier.trim()
                  ? 'cursor-pointer shadow-[0px_-1px_0px_rgba(0,0,0,0.18)_inset,_0px_1px_0px_rgba(255,255,255,0.12)_inset] [background:linear-gradient(19.99deg,#4f67d8,#3a4fc2_60%,#2e3fa8)] hover:opacity-90'
                  : 'cursor-not-allowed bg-[#d0d5dd]'
              }`}
            >
              {forgotMutation.isPending ? t('sending') : t('submit')}
            </button>

            {forgotMutation.isError && (
              <div className="rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-600">
                {t('error')}
              </div>
            )}

            <Link
              href={ROUTES.login}
              className="self-center text-sm text-[#71717a] hover:text-[#4f67d8]"
            >
              {t('backToLogin')}
            </Link>
          </>
        }
      >
        <div className="flex w-full gap-2">
          <button
            type="button"
            onClick={() => { setChannel('EMAIL'); setIdentifier('') }}
            className={`flex-1 rounded-md border px-4 py-2.5 text-sm font-semibold transition-all ${
              channel === 'EMAIL'
                ? 'border-[#4f67d8] bg-[#f4f9ff] text-[#4f67d8]'
                : 'border-[#e5e7eb] bg-white text-[#71717a] hover:bg-[#f9fafb]'
            }`}
          >
            <div className="flex items-center justify-center gap-2">
              <Mail className="h-4 w-4" />
              {t('email')}
            </div>
          </button>
          <button
            type="button"
            onClick={() => { setChannel('PHONE'); setIdentifier('') }}
            className={`flex-1 rounded-md border px-4 py-2.5 text-sm font-semibold transition-all ${
              channel === 'PHONE'
                ? 'border-[#4f67d8] bg-[#f4f9ff] text-[#4f67d8]'
                : 'border-[#e5e7eb] bg-white text-[#71717a] hover:bg-[#f9fafb]'
            }`}
          >
            <div className="flex items-center justify-center gap-2">
              <Phone className="h-4 w-4" />
              {t('phone')}
            </div>
          </button>
        </div>

        {channel === 'EMAIL' ? (
          <FormInput
            label={t('emailAddress')}
            name="email"
            type="email"
            placeholder="your@email.com"
            icon={<Mail className="h-4 w-4" />}
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            filled={identifier.length > 0}
          />
        ) : (
          <FormInput
            label={t('phoneNumber')}
            name="phone"
            type="tel"
            placeholder="+880 XXX XXX XXXX"
            icon={<Phone className="h-4 w-4" />}
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            filled={identifier.length > 0}
          />
        )}
      </StepLayout>
    </AuthShell>
  )
}
