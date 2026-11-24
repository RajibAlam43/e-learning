import Image from 'next/image'
import Link from 'next/link'
import { getTranslations } from 'next-intl/server'
import { ChevronRight } from 'lucide-react'

export async function AboutSection() {
  const t = await getTranslations('about')

  const eyebrow = t('eyebrow')
  const title = t('title')
  const description = t('description')
  const ctaText = t('cta')
  const ctaLink = '/about'
  const logoImage = '/images/gii-logo.png'
  const videoThumbnail = '/images/about-video-placeholder.png'

  return (
    <section className="content-auto-section bg-[var(--color-neutral-100)] py-20 max-md:py-12 max-sm:py-8">
      <div className="container mx-auto flex px-5 gap-20 max-lg:gap-10 max-md:gap-5 max-md:flex-wrap">
        {/* Left column - logo + video */}
        <div className="flex flex-1 flex-col items-center justify-center gap-4 min-w-0 max-md:min-w-full">
          <div className="flex h-[174px] w-full items-center justify-center rounded-md bg-[var(--color-brand-primary-800)]">
            <Image
              src={logoImage}
              alt="Global Islamic Institute"
              width={193}
              height={117}
              className="h-[117px] w-[193px] object-contain"
            />
          </div>
          <div className="relative h-[329px] w-full overflow-hidden rounded-md shadow-[0px_25px_60px_-15px_rgba(16,24,40,0.2)]">
            <Image
              src={videoThumbnail}
              alt="Watch our video"
              fill
              className="object-cover"
              sizes="(max-width: 768px) 100vw, 50vw"
            />
            <button
              type="button"
              aria-label="Play video"
              className="absolute left-1/2 top-1/2 flex h-16 w-16 -translate-x-1/2 -translate-y-1/2 items-center justify-center cursor-pointer transition-all duration-200 hover:scale-110"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width={64} height={64} viewBox="0 0 24 24">
                <g fill="none" stroke="#fff" strokeWidth="1">
                  <path fill="#fff" strokeLinecap="round" strokeLinejoin="round" d="m14 12l-3.5 2v-4z" />
                  <path d="M2 12.708v-1.416c0-2.895 0-4.343.905-5.274c.906-.932 2.332-.972 5.183-1.053C9.438 4.927 10.818 4.9 12 4.9s2.561.027 3.912.065c2.851.081 4.277.121 5.182 1.053S22 8.398 22 11.292v1.415c0 2.896 0 4.343-.905 5.275c-.906.931-2.331.972-5.183 1.052c-1.35.039-2.73.066-3.912.066s-2.561-.027-3.912-.066c-2.851-.08-4.277-.12-5.183-1.052S2 15.602 2 12.708Z" />
                </g>
              </svg>
            </button>
          </div>
        </div>

        {/* Right column - text + CTA */}
        <div className="flex flex-1 flex-col items-start gap-12 max-md:min-w-full max-md:gap-6">
          <div className="flex flex-col items-start gap-6">
            <p className="font-sans text-lg font-medium leading-[28px] text-[var(--color-text-headline-secondary)]">
              {eyebrow}
            </p>
            <h2 className="font-heading text-[36px] font-semibold leading-[44px] text-[var(--color-text-headline-primary)] max-md:text-[29px] max-md:leading-[35px]">
              {title}
            </h2>
          </div>

          <p className="whitespace-pre-line font-sans text-base leading-6 text-[var(--color-text-body-secondary)]">
            {description}
          </p>

          <Link
            href={ctaLink}
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-solid border-[rgba(37,99,235,0.3)] bg-white px-5 py-2.5 font-sans text-base font-semibold leading-6 text-[var(--color-secondary-blue-600)] transition-all duration-200 hover:bg-[var(--color-secondary-blue-50)] hover:shadow-sm"
          >
            {ctaText}
            <ChevronRight className="h-5 w-5" />
          </Link>
        </div>
      </div>
    </section>
  )
}
