import Image from 'next/image'
import { getTranslations } from 'next-intl/server'
import {
  Route,
  CalendarCheck,
  Clock,
  Award,
  GraduationCap,
  MessageSquare,
  BookOpen,
  Mic,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

interface FeatureCardProps {
  title: string
  description: string
  Icon: LucideIcon
}

function FeatureCard({ title, description, Icon }: FeatureCardProps) {
  return (
    <article className="flex h-[200px] flex-col items-start justify-between rounded-2xl bg-white p-2 md:p-6 transition-all duration-300 hover:shadow-md hover:-translate-y-1 max-sm:h-auto max-sm:gap-3">
      <div className="flex h-12 w-12 items-center justify-center rounded bg-[#0f1f3f]">
        <Icon className="h-7 w-7 text-white" />
      </div>
      <div className="flex flex-col gap-2 self-stretch">
        <h3 className="font-heading text-xl font-medium leading-5 text-[var(--color-text-headline-secondary)]">
          {title}
        </h3>
        <p className="self-stretch font-sans text-sm leading-[150%] text-[var(--color-text-body-secondary)]">
          {description}
        </p>
      </div>
    </article>
  )
}

function FeatureRow({ cards }: { cards: FeatureCardProps[] }) {
  return (
    <>
      {[0, 1].map((row) => (
        <div key={row} className="grid grid-cols-2 gap-4 self-stretch max-sm:grid-cols-2">
          {cards.slice(row * 2, row * 2 + 2).map((card) => (
            <FeatureCard key={card.title} {...card} />
          ))}
        </div>
      ))}
    </>
  )
}

export async function LearningSystemSection() {
  const t = await getTranslations('learningSystem')

  const learningSystemCards: FeatureCardProps[] = [
    {
      title: t('features.structuredPath.title'),
      description: t('features.structuredPath.description'),
      Icon: Route,
    },
    {
      title: t('features.weeklySystem.title'),
      description: t('features.weeklySystem.description'),
      Icon: CalendarCheck,
    },
    {
      title: t('features.flexibleSchedule.title'),
      description: t('features.flexibleSchedule.description'),
      Icon: Clock,
    },
    {
      title: t('features.certification.title'),
      description: t('features.certification.description'),
      Icon: Award,
    },
  ]

  const learningExperienceCards: FeatureCardProps[] = [
    {
      title: t('features.expertGuidance.title'),
      description: t('features.expertGuidance.description'),
      Icon: GraduationCap,
    },
    {
      title: t('features.personalFeedback.title'),
      description: t('features.personalFeedback.description'),
      Icon: MessageSquare,
    },
    {
      title: t('features.understanding.title'),
      description: t('features.understanding.description'),
      Icon: BookOpen,
    },
    {
      title: t('features.tajweedTraining.title'),
      description: t('features.tajweedTraining.description'),
      Icon: Mic,
    },
  ]

  return (
    <section className="content-auto-section flex w-full flex-col items-center bg-white pt-20 pb-20 max-sm:pt-12 max-sm:pb-12">
      <div className="container mx-auto px-5">
        <header className="mb-12 flex flex-col gap-4 text-left max-sm:mb-8 2xl:items-center 2xl:text-center">
          <h2 className="font-heading text-[52px] font-semibold leading-[56px] tracking-[-1px] text-[#3b82f6] max-md:text-[40px] max-md:leading-[48px] max-sm:text-[28px] max-sm:leading-[34px]">
            {t('title')}
          </h2>
          <p className="font-sans text-base leading-6 text-[var(--color-text-body-secondary)]">
            {t('subtitle')}
          </p>
        </header>

        <div className="flex flex-col gap-16 max-md:gap-10 max-sm:gap-8">
          {/* Row 1: Learning System — image left, cards right */}
          <div className="flex flex-col items-center gap-8 lg:flex-row lg:items-center lg:justify-center">
            <Image
              src="/images/Left Image Container Inner.png"
              alt=""
              width={568}
              height={577}
              className="h-auto w-full rounded-xl object-cover transition-all duration-300 hover:shadow-lg lg:h-[577px] lg:w-[568px] lg:shrink-0"
            />
            <div className="flex w-full flex-col items-start gap-4 lg:w-[568px]">
              <div className="flex items-center justify-between self-stretch">
                <span className="font-sans text-lg font-medium leading-7 text-[var(--color-text-body-secondary)]">
                  {t('system')}
                </span>
                <div className="h-px flex-1 bg-neutral-200" />
              </div>
              <div className="flex flex-col gap-4 self-stretch">
                <FeatureRow cards={learningSystemCards} />
              </div>
            </div>
          </div>

          {/* Row 2: Learning Experience — cards left, image right */}
          <div className="flex flex-col-reverse items-center gap-8 lg:flex-row lg:items-center lg:justify-center">
            <div className="flex w-full flex-col items-start justify-center gap-4 lg:w-[568px]">
              <div className="flex items-center justify-between self-stretch">
                <span className="font-sans text-lg font-medium leading-7 text-[var(--color-text-body-secondary)]">
                  {t('experience')}
                </span>
                <div className="h-px flex-1 bg-neutral-200" />
              </div>
              <div className="flex flex-col gap-4 self-stretch">
                <FeatureRow cards={learningExperienceCards} />
              </div>
            </div>
            <Image
              src="/images/Right Image Container.png"
              alt=""
              width={568}
              height={577}
              className="h-auto w-full rounded-xl object-cover transition-all duration-300 hover:shadow-lg lg:h-[577px] lg:w-[568px] lg:shrink-0"
            />
          </div>
        </div>
      </div>
    </section>
  )
}
