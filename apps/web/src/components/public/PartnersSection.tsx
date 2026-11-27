'use client'
import Image from 'next/image'
import { useTranslations } from 'next-intl'
import { useHomepageSettings } from '@/hooks/useHomepageSettings'
export function PartnersSection() {
  const t = useTranslations('partners')
  const { settings, isLoading, isError } = useHomepageSettings()
  if (isLoading || isError || !settings.partners.length) return null
  return <section className="content-auto-section w-full bg-white py-12 max-md:py-8"><div className="container mx-auto px-5">
    <h2 className="text-center font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[var(--color-text-headline-primary)] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[24px] max-sm:leading-[29px]">{t('title')}</h2>
    <div className="mt-5 grid grid-cols-2 items-center justify-items-center gap-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {settings.partners.map((partner, index) => {
        const logo = <Image unoptimized src={partner.logoUrl} alt={partner.name} width={149} height={48} className="h-12 w-[149px] max-w-full object-contain" />
        return <div key={`${partner.name}-${index}`} className="flex h-16 w-full items-center justify-center py-2">{partner.websiteUrl ? <a href={partner.websiteUrl} target="_blank" rel="noopener noreferrer" className="rounded focus-visible:outline-2 focus-visible:outline-offset-4">{logo}</a> : logo}</div>
      })}
    </div>
  </div></section>
}
