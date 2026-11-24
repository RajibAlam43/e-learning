'use client'

import { useTranslations } from 'next-intl'
import Image from 'next/image'

interface CourseOutcomesProps {
  outcomes?: string[]
}

const ICONS = ['/images/book.svg', '/images/speak.svg', '/images/checkmark.svg']

export function CourseOutcomes({ outcomes }: CourseOutcomesProps) {
  const t = useTranslations('courses.detail')
  const safeOutcomes = outcomes ?? []
  if (safeOutcomes.length === 0) return null

  return (
    <section
      aria-labelledby="course-outcomes-heading"
      className="w-full"
    >
      <div className="container mx-auto flex flex-col items-start gap-6 px-5 py-20 max-[450px]:py-12">
        <h2
          id="course-outcomes-heading"
          className="mt-[-1px] text-[36px] font-normal leading-[44px] tracking-[0px] text-[#1c3873] font-heading whitespace-nowrap max-[450px]:text-[28px] max-[450px]:leading-[34px]"
        >
          {t('outcomesTitle.title')}
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {safeOutcomes.map((outcome, i) => (
            <article
              key={outcome}
              className="group flex w-full flex-col items-center md:items-start gap-12 rounded-xl bg-white p-8 shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] max-[450px]:gap-8 max-[450px]:p-5 transition-all duration-300 hover:-translate-y-1 hover:shadow-lg opacity-0 animate-fade-in-up"
              style={{ animationDelay: `${i * 100}ms` }}
            >
              {/* Icon */}
              <div className="flex h-10 w-10 items-center justify-center  rounded-lg bg-[#8c7355] transition-transform duration-300 group-hover:scale-110">
                <Image
                  src={ICONS[i % ICONS.length]!}
                  alt=""
                  aria-hidden="true"
                  width={16}
                  height={16}
                  className="h-4 w-4"
                />
              </div>

              {/* Text */}
              <div className="flex w-full flex-col items-center md:items-start gap-3">
                <h3 className="mt-[-1px] text-xl font-normal text-center md:text-left leading-5 tracking-[0px] text-[#1b1c1a] leading-[28px] font-heading max-[450px]:text-lg">
                  {outcome}
                </h3>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
