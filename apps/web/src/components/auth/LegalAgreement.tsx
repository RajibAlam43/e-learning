'use client'

import Link from 'next/link'
import { useId } from 'react'
import { useTranslations } from 'next-intl'
import { usePublicSettings } from '@/hooks/useSettings'
import { getPolicyLinks } from '@/lib/policy-links'

export function LegalAgreement({ checked, onChange, disabled, error }: {
  checked: boolean
  onChange: (checked: boolean) => void
  disabled?: boolean
  error?: string
}) {
  const id = useId()
  const t = useTranslations('auth.signup')
  const settings = usePublicSettings()
  const links = getPolicyLinks(settings.data)
  return (
    <div className="flex flex-col gap-2 text-sm text-[#5e5f5c]">
      <div className="flex items-start gap-2">
        <input id={id} type="checkbox" checked={checked} onChange={event => onChange(event.target.checked)} disabled={disabled} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} className="m-0 mt-1 h-[22px] w-[22px] shrink-0 cursor-pointer disabled:cursor-not-allowed" />
        <label htmlFor={id} className="font-sans leading-[150%]">
          {t.rich('termsAgree', {
            terms: chunks => links.terms ? <a href={links.terms} target="_blank" rel="noopener noreferrer" className="text-[#1d4ed8] underline">{chunks}</a> : <span>{chunks}</span>,
            privacy: chunks => links.privacy ? <a href={links.privacy} target="_blank" rel="noopener noreferrer" className="text-[#1d4ed8] underline">{chunks}</a> : <span>{chunks}</span>,
          })}
        </label>
      </div>
      {(!links.terms || !links.privacy) && <Link href="/contact" className="self-start text-xs text-[#1d4ed8] underline">{t('policyRequest')}</Link>}
      {error && <p id={`${id}-error`} className="text-sm text-red-700">{error}</p>}
    </div>
  )
}
