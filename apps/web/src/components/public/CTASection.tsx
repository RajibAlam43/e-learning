'use client'

import Link from 'next/link'
import Image from 'next/image'
import { ChevronRight } from 'lucide-react'
import { useTranslations } from 'next-intl'

const CTA_IMAGE = '/images/quran.avif'
const CTA_BG_IMAGE = '/images/CTA-bg.webp'
const CTA_LINK = '/courses'

export function CTASection() {
  const t = useTranslations('cta')
  const title = t('title')
  const description = t('description')
  const ctaText = t('cta')

  return (
    <section
      className="content-auto-section relative flex w-full h-[409px] max-h-[409px] items-center overflow-hidden bg-[#1a2744] bg-cover bg-center bg-no-repeat"
      style={{ backgroundImage: `url(${CTA_BG_IMAGE})` }}
    >
      {/* Left — content inside container */}
      <div className="relative z-10 mx-auto flex h-full max-h-[409px] w-full max-w-[1184px] items-center px-5 overflow-hidden">
        <div className="max-w-[442px]">
          <h2 className="m-0 font-heading text-[28px] font-semibold leading-[34px] tracking-[-1px] text-white sm:text-[36px] sm:leading-[40px] md:text-[42px] md:leading-[45px] lg:text-[52px] lg:leading-[56px]">
            {title}
          </h2>
          <p className="mt-4 max-w-[354px] font-sans text-sm leading-5 text-white/80 sm:text-base sm:leading-6">
            {description}
          </p>
          <Link
            href={CTA_LINK}
            className="btn-gold-gradient mt-6 inline-flex h-11 cursor-pointer items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white transition-all duration-200 hover:opacity-90 hover:scale-[1.02]"
          >
            {ctaText}
            <ChevronRight className="h-6 w-6" />
          </Link>
        </div>
      </div>

      {/* Right — image at far edge, contained in section */}
      <div className="absolute right-0 top-0 hidden h-[409px] w-[35%] max-w-[500px] overflow-hidden sm:block md:w-[40%] lg:max-w-[746px]">
        <Image
          src={CTA_IMAGE}
          alt=""
          width={746}
          height={409}
          className="h-full w-full object-cover"
          loading="lazy"
        />
      </div>
    </section>
  )
}
