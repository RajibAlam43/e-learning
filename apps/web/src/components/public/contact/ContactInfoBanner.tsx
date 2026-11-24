'use client'

import { useTranslations } from 'next-intl'

const INFO_CARDS = [
  { key: 'email', labelKey: 'emailLabel', valueKey: 'emailAddress' },
  { key: 'phone', labelKey: 'phoneLabel', valueKey: 'phoneNumber' },
] as const

export function ContactInfoBanner() {
  const t = useTranslations('contact')

  return (
    <section className="relative w-full overflow-hidden">
      {/* Gradient background */}
      <div
        className="absolute inset-0"
        style={{ background: 'linear-gradient(79.48deg, #574500, #bd9600)' }}
      />

      {/* Content */}
      <div className="relative z-10 py-20 max-md:py-[52px]">
        <div className="container mx-auto flex flex-col items-start gap-[69px] px-5 lg:flex-row lg:items-start max-md:gap-[17px] md:gap-[34px]">
          {/* Left side */}
          <div className="flex w-full flex-col gap-6 lg:w-[392px] lg:min-w-0 lg:flex-1">
            <span className="font-sans text-lg font-semibold leading-6 text-white">
              {t('contactInfoLabel')}
            </span>
            <h2 className="text-[24px] font-semibold leading-7 tracking-[-1px] text-white font-heading max-md:text-[24px] max-md:leading-[29px] md:text-[32px] md:leading-[38px] lg:text-[40px] lg:leading-[48px]">
              {t('contactInfoHeading')}
            </h2>
          </div>

          {/* Right side — info cards */}
          <div className="flex w-full flex-col gap-[25px] min-w-0 md:flex-row md:gap-[51px] lg:w-auto lg:min-w-[470px] lg:flex-1 lg:gap-[102px]">
            {INFO_CARDS.map(({ key, labelKey, valueKey }) => (
              <div
                key={key}
                className="flex flex-1 flex-col gap-[27px] rounded-lg p-8 backdrop-blur-sm transition-all duration-300 hover:bg-white/10"
              >
                {/* Label + divider */}
                <div className="flex flex-col gap-[25px]">
                  <span className="font-sans text-lg font-semibold leading-6 text-white">
                    {t(labelKey)}
                  </span>
                  <div className="h-[3px] w-[27px] rounded-full bg-white transition-all duration-300 hover:w-[40px]" />
                </div>
                {/* Value + hours */}
                <div className="flex flex-col gap-[25px]">
                  <h3 className="m-0 text-xl font-semibold leading-5 text-white font-heading transition-colors duration-200 max-md:text-base max-md:leading-4">
                    {t(valueKey)}
                  </h3>
                  <p className="m-0 font-sans text-base leading-6 text-white/70">
                    {t('assistanceHoursLine1')} <br />
                    {t('assistanceHoursLine2')}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
