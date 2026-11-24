import Image from 'next/image'
import { getTranslations } from 'next-intl/server'

const PARTNER_LOGOS = Array.from({ length: 10 }, (_, i) => `/images/logo${i + 1}.png`)

const logoSizes = [
  { h: 'h-[35px]', w: 'w-[149px]' },
  { h: 'h-[35px]', w: 'w-[163px]' },
  { h: 'h-[35px]', w: 'w-[149px]' },
  { h: 'h-10', w: 'w-[104px]' },
  { h: 'h-[28px]', w: 'w-[149px]' },
  { h: 'h-[31px]', w: 'w-[134px]' },
  { h: 'h-10', w: 'w-[124px]' },
  { h: 'h-[29px]', w: 'w-[162px]' },
  { h: 'h-8', w: 'w-[154px]' },
  { h: 'h-12', w: 'w-[76px]' },
]

export async function PartnersSection() {
  const t = await getTranslations('partners')
  const title = t('title')

  return (
    <section className="content-auto-section w-full bg-white py-12 max-md:py-8">
      <div className="container mx-auto px-5">
        <h2 className="text-center font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[var(--color-text-headline-primary)] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[24px] max-sm:leading-[29px]">
          {title}
        </h2>

        <div className="mt-5 grid grid-cols-2 items-center justify-items-center gap-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {PARTNER_LOGOS.map((logo, idx) => (
            <div
              key={idx}
              className="flex h-16 w-full cursor-pointer items-center justify-center py-3 opacity-70 transition-all duration-300 hover:scale-110 hover:opacity-100"
            >
              <Image
                src={logo}
                alt={`Partner ${idx + 1}`}
                width={149}
                height={35}
                className={`${logoSizes[idx]?.h ?? 'h-10'} ${logoSizes[idx]?.w ?? 'w-24'} max-w-full object-contain`}
              />
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
