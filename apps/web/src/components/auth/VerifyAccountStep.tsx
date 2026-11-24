'use client'

import { useTranslations } from 'next-intl'
import { useSignupContext } from './SignupContext'
import { OTPInput } from './OTPInput'
import { StepLayout } from './StepLayout'
import { useSendVerification, useVerifyCode } from '@/hooks/useAuth'

export function VerifyAccountStep() {
  const t = useTranslations('auth.verify')
  const { formData, updateField } = useSignupContext()
  const verifyMutation = useVerifyCode()
  const sendVerification = useSendVerification()

  const channel = formData.channel
  const identifier = channel === 'EMAIL' ? formData.email : formData.phone
  const purpose = channel === 'EMAIL' ? 'EMAIL_VERIFICATION' : 'PHONE_VERIFICATION'

  const handleVerify = (e: React.FormEvent) => {
    e.preventDefault()
    if (formData.otp.length !== 6) return
    verifyMutation.mutate({ channel, identifier, code: formData.otp, purpose })
  }

  const handleResend = () => {
    sendVerification.mutate({ channel, identifier, purpose })
  }

  return (
    <StepLayout
      title={t('title')}
      subtitle={t('subtitle')}
      subtitleLineTwo={t('subtitleLineTwo')}
      subtitleColor="text-[#5e5f5c]"
      actionsGap="gap-5"
      onSubmit={handleVerify}
      actions={
        <>
          <button
            type="submit"
            disabled={formData.otp.length !== 6 || verifyMutation.isPending}
            className="flex h-[46px] w-full items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold text-white shadow-[0px_-1px_0px_rgba(0,0,0,0.18)_inset,_0px_1px_0px_rgba(255,255,255,0.12)_inset] [background:linear-gradient(19.99deg,#4f67d8,#3a4fc2_60%,#2e3fa8)] transition-all duration-200 hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {verifyMutation.isPending ? t('verifying') : t('submit')}
          </button>

          {verifyMutation.isError && (
            <div role="alert" className="text-sm text-red-700">
              {(verifyMutation.error as { message?: string })?.message || t('verificationFailed')}
            </div>
          )}

          <div className="w-full text-center text-sm text-[#5e5f5c]">
            <span className="leading-[150%]">{t('didntGetCode')} </span>
            <button
              type="button"
              onClick={handleResend}
              disabled={sendVerification.isPending}
              className="font-semibold text-[#b91c1c] hover:underline disabled:opacity-50"
            >
              {sendVerification.isPending ? t('sending') : t('resend')}
            </button>
          </div>
        </>
      }
    >
      <OTPInput
        value={formData.otp}
        onChange={(val) => updateField('otp', val)}
      />
    </StepLayout>
  )
}
