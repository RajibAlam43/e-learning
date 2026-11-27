import Image from 'next/image'
import { getTranslations } from 'next-intl/server'

const POST_IMAGES = ['/images/alumni.png', '/images/balance-life.png', '/images/a-joourney.png'] as const

function SideCard({
  image,
  heading,
  supportingText,
}: {
  image: string
  heading: string
  supportingText: string
}) {
  return (
    <div className="flex items-start gap-4 rounded-lg p-2 transition-all duration-300 hover:scale-[1.02] hover:shadow-lg sm:gap-6">
      <Image
        src={image}
        alt=""
        width={224}
        height={160}
        className="h-auto w-32 shrink-0 rounded-lg object-cover sm:w-40 md:w-48 lg:w-56"
      />
      <div className="flex min-w-0 flex-1 flex-col items-start gap-4 sm:gap-6">
        <div className="flex flex-col items-start gap-0 md:gap-2 self-stretch">
          <div className="self-stretch font-sans text-base font-semibold leading-6 text-[#1c3873] sm:text-lg">
            {heading}
          </div>
        </div>
        <div className="self-stretch font-sans text-sm leading-[150%] text-[#5e5f5c]">
          {supportingText}
        </div>
      </div>
    </div>
  )
}

export async function BlogSection() {
  const t = await getTranslations('learningJourney')

  const posts = [
    { image: POST_IMAGES[0], title: t('items.0.title'), excerpt: t('items.0.description') },
    { image: POST_IMAGES[1], title: t('items.1.title'), excerpt: t('items.1.description') },
    { image: POST_IMAGES[2], title: t('items.2.title'), excerpt: t('items.2.description') },
  ]

  const title = t('title')
  const featured = posts[0]!
  const sideCards = posts.slice(1)

  return (
    <section aria-labelledby="learning-journey-heading" className="content-auto-section w-full bg-white py-12 px-5 xl:px-32 max-md:py-8">
      <div className="container mx-auto">
        <h2 id="learning-journey-heading" className="m-0 scroll-mt-24 self-stretch font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[#3b82f6] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[24px] max-sm:leading-[29px]">
          {title}
        </h2>

        <div className="mt-8 flex flex-col gap-8 lg:flex-row lg:gap-6">
          {/* Featured learning topic - left column */}
          <section className="flex w-full flex-1 flex-col items-start gap-4 rounded-lg p-2 transition-all duration-300 hover:scale-[1.02] hover:shadow-lg lg:min-w-0">
            <Image
              src={featured.image}
              alt=""
              width={600}
              height={400}
              className="w-full rounded-lg object-cover"
            />
            <div className="flex flex-col items-start gap-2 self-stretch">
              <div className="self-stretch font-sans text-lg font-semibold leading-6 text-[#1c3873]">
                {featured.title}
              </div>
              <div className="self-stretch font-sans text-sm leading-[150%] text-[#5e5f5c]">
                {featured.excerpt}
              </div>
            </div>
          </section>

          {/* Supporting learning topics - right column */}
          <section className="flex w-full flex-1 flex-col items-start gap-6 lg:min-w-0 sm:gap-8">
            {sideCards.map((card) => (
              <SideCard
                key={card.title}
                image={card.image}
                heading={card.title}
                supportingText={card.excerpt}
              />
            ))}
          </section>
        </div>
      </div>
    </section>
  )
}
