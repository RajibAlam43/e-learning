import Image from 'next/image'
import { getTranslations } from 'next-intl/server'

const QUOTE_BG_IMAGE = '/images/Quote.png'

export async function QuoteSection() {
  const t = await getTranslations('quote')
  const text = t('text')
  const source = t('source')

  return (
    <section className="content-auto-section relative h-[400px] w-full overflow-hidden max-md:h-auto max-md:min-h-[300px] max-sm:min-h-[250px]">
      <Image
        src={QUOTE_BG_IMAGE}
        alt=""
        aria-hidden="true"
        fill
        className="object-cover"
      />

      <div className="relative z-10 flex h-full flex-col items-center justify-center px-5 py-20 text-center max-md:py-12 max-sm:py-8">
        <div className="flex w-full max-w-[972px] flex-col items-center gap-8">
          <p className="whitespace-pre-line font-heading text-[52px] font-medium leading-[56px] tracking-[-1px] text-white max-md:text-[42px] max-md:leading-[45px] max-sm:text-[31px] max-sm:leading-[34px]">
            {text}
          </p>
          <span className="font-sans text-lg leading-7 text-neutral-200">
            {source}
          </span>
        </div>
      </div>
    </section>
  )
}
