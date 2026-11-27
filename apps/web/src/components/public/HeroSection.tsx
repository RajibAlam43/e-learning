import { StudentMilestoneBadge } from './StudentMilestone'
import Image from 'next/image'
import Link from 'next/link'
import { ChevronRight } from 'lucide-react'
import { getTranslations } from 'next-intl/server'

export async function HeroSection() {
  const t = await getTranslations('hero')

  return (
    <section className="relative -mt-[81px] h-screen w-full overflow-hidden" aria-labelledby="hero-heading">
      <Image
        src="/images/hero-bg.png"
        alt=""
        fill
        loading="eager"
        fetchPriority="high"
        sizes="100vw"
        quality={75}
        className="object-cover object-center"
        aria-hidden="true"
      />
      <div className="hero-overlay absolute inset-0" aria-hidden="true" />

      <div className="relative z-10 container mx-auto flex h-full flex-col items-center justify-center px-5 py-12">
        <div className="flex w-full flex-col items-center gap-6">
          <header className="flex flex-col items-center gap-4 self-stretch">
            <div className="relative flex flex-col items-center gap-4">
              <p className="absolute inset-x-0 -top-12 text-center font-sans text-xl font-medium leading-7 text-white max-sm:-top-10 max-sm:text-base max-sm:leading-6">
                {t('motto')}
              </p>
              <StudentMilestoneBadge />

              <h1
                id="hero-heading"
                className="max-w-[710px] self-stretch text-center font-heading text-[52px] leading-[56px] tracking-[-1px] text-white max-lg:text-[42px] max-lg:leading-[46px] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[26px] max-sm:leading-[32px]"
              >
                {t('title')}
              </h1>
            </div>

            <p className="max-w-[760px] text-center font-sans text-lg leading-[28px] text-[var(--color-neutral-200)] max-md:text-base max-md:leading-6 max-sm:text-sm max-sm:leading-5">
              {t('description')}
            </p>
          </header>

          <Link
            href="/courses"
            className="btn-blue-gradient inline-flex h-11 items-center justify-center gap-2 rounded-md px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white transition-all duration-200 hover:opacity-90 hover:scale-[1.02]"
          >
            {t('cta')}
            <ChevronRight className="h-6 w-6" />
          </Link>
        </div>
      </div>
    </section>
  )
}
