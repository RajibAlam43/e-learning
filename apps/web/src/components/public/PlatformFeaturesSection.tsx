'use client'

import { useState } from 'react'
import Image from 'next/image'
import { ChevronRight } from 'lucide-react'
import { useTranslations } from 'next-intl'

const FEATURES_KEYS = ['dashboard', 'progress', 'recorded'] as const
const FEATURES_IMAGES = ['/images/Card.png', '/images/balance-life.png', '/images/a-joourney.png'] as const

export function PlatformFeaturesSection() {
  const t = useTranslations('platformFeatures')
  const [active, setActive] = useState(0)
  const [animKey, setAnimKey] = useState(0)

  const goTo = (idx: number) => {
    setActive(idx)
    setAnimKey((k) => k + 1)
  }

  const title = t('title')
  const subtitle = t('subtitle')

  return (
    <section className="content-auto-section flex w-full flex-col items-center bg-white py-12 gap-20 max-md:gap-10 max-sm:gap-5 max-lg:px-5 max-md:py-8">
      <div className="flex w-[492px] max-w-full flex-col items-center gap-4 text-center">
        <h2 className="self-stretch font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[var(--color-text-headline-secondary)] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[24px] max-sm:leading-[29px]">
          {title}
        </h2>
        <p className="self-stretch font-sans text-lg leading-7 text-[var(--color-text-body-secondary)]">
          {subtitle}
        </p>
      </div>

      <div className="container mx-auto px-5 overflow-hidden">
        {FEATURES_KEYS.map((key, idx) => (
          <div
            key={`${key}-${animKey}`}
            className={`relative flex w-full items-center gap-16 max-md:gap-8 max-sm:gap-6 max-lg:flex-col max-lg:px-5 ${
              idx !== active ? 'hidden' : 'animate-slide-in'
            }`}
          >
            <div className="flex w-[473px] shrink-0 flex-col items-start gap-6 text-left max-lg:w-full max-lg:items-center max-lg:text-center">
              <span className="inline-flex items-center justify-center rounded-full bg-[var(--color-secondary-blue-50)] px-4 py-2 font-sans text-sm font-medium leading-5 text-[var(--color-secondary-blue-600)]">
                {t(`features.${key}.badge`)}
              </span>
              <h3 className="self-stretch font-heading text-[40px] font-bold leading-[48px] tracking-[-1px] text-[var(--color-secondary-blue-500)] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[24px] max-sm:leading-[29px]">
                {t(`features.${key}.title`)}
              </h3>
              <p className="self-stretch font-sans text-lg leading-7 text-[var(--color-text-body-secondary)]">
                {t(`features.${key}.description`)}
              </p>
              {key === 'progress' && (
                <button className="btn-blue-gradient inline-flex h-[46px] cursor-pointer items-center justify-center gap-2 rounded-md px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white transition-all duration-200 hover:opacity-90 hover:scale-[1.02]">
                  {t('startCourse')}
                  <ChevronRight className="h-5 w-5" />
                </button>
              )}
            </div>

            <div className="relative flex h-[620px] w-[775px] items-start justify-end rounded-3xl bg-[var(--color-secondary-blue-50)] py-[69px] max-sm:py-[45px] max-lg:h-auto max-lg:w-full max-lg:py-8">
              <Image
                src={FEATURES_IMAGES[idx]!}
                alt={t(`features.${key}.badge`)}
                width={685}
                height={482}
                className="h-[482px] w-[685px] rounded-tl-[20px] rounded-bl-[20px] object-cover shadow-[0px_20px_40px_-8px_rgba(16,24,40,0.1)] max-lg:h-auto max-lg:w-full max-lg:rounded-bl-none max-lg:rounded-br-[20px]"
              />

              <button
                type="button"
                onClick={() => goTo((active + 1) % FEATURES_KEYS.length)}
                className="absolute right-6 top-1/2 -translate-y-1/2 flex items-center rounded-xl bg-white p-6 shadow-md transition-all duration-200 hover:scale-105 hover:shadow-lg cursor-pointer"
                aria-label="Next slide"
              >
                <ChevronRight className="h-9 w-9" />
              </button>
            </div>

            {idx === active && (
              <div className="absolute bottom-0 left-32 flex items-center gap-2.5 max-md:left-16 max-sm:left-0 max-sm:right-0 max-sm:justify-center max-lg:relative max-lg:bottom-auto max-lg:left-auto max-lg:mt-8">
                {FEATURES_KEYS.map((_, dotIdx) => (
                  <button
                    key={dotIdx}
                    type="button"
                    onClick={() => goTo(dotIdx)}
                    aria-label={`Go to slide ${dotIdx + 1}`}
                    className={`h-4 w-4 cursor-pointer rounded-full transition-all duration-200 ${
                      dotIdx === active
                        ? 'bg-[var(--color-secondary-blue-500)]'
                        : 'bg-[var(--color-secondary-blue-50)]'
                    }`}
                  />
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  )
}
