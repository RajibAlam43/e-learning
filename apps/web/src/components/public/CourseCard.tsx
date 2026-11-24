'use client'

import Image from 'next/image'
import Link from 'next/link'
import { Star, Clock, MonitorPlay, Users, ChevronRight } from 'lucide-react'
import { useTranslations } from 'next-intl'
import type { CourseSummary } from '@/types'

interface CourseCardProps {
  course: CourseSummary
}

export function CourseCard({ course }: CourseCardProps) {
  const t = useTranslations('courses')

  const isFree = (course.priceBdt ?? -1) === 0
  const badge = isFree ? 'popular' : 'shortCourse'

  const priceParts = course.priceBdt === 0
    ? { amount: 'Free', unit: '' }
    : course.priceBdt != null
      ? { amount: course.priceBdt.toLocaleString(), unit: ' BDT' }
      : { amount: 'TBA', unit: '' }

  return (
    <article className="group flex flex-1 flex-col items-start relative bg-white overflow-hidden rounded-lg transition-all duration-300 hover:shadow-[0px_8px_24px_rgba(16,24,40,0.12)] hover:-translate-y-1">
      {/* Thumbnail */}
      <div className="relative h-[200px] w-full shrink-0 overflow-hidden sm:h-[220px] md:h-[274px]">
        {course.thumbnailUrl ? (
          <Image
            src={course.thumbnailUrl}
            alt={course.title ?? 'Course'}
            width={400}
            height={274}
            className="object-cover w-full h-full transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-[#e5e5e5]">
            <MonitorPlay className="h-12 w-12 text-[#a3a3a3]" />
          </div>
        )}

        {/* Single badge */}
        <span
          className={`absolute top-4 left-4 z-10 inline-flex items-center justify-center rounded-[4px] px-2 py-1 font-sans text-xs font-medium leading-5 transition-transform duration-300 group-hover:scale-105 ${
            badge === 'popular'
              ? 'bg-[#b45309] text-[#fffbeb]'
              : 'bg-[#f4f9ff] text-[#3b82f6]'
          }`}
        >
          {t(`badge.${badge}`)}
        </span>
      </div>

      {/* Card body */}
      <div className="flex w-full flex-col items-start gap-3 p-4 sm:p-5 md:gap-4 md:p-6 relative z-0">
        {/* Rating + Title */}
        <header className="flex flex-col items-start gap-1 w-full">
          <div className="inline-flex items-center gap-1" aria-label="Course rating 4.9 out of 5 from 52 reviews">
            <Star className="h-4 w-4 fill-amber-500 text-amber-500 transition-transform duration-300 group-hover:rotate-12" />
            <span className="font-sans text-sm leading-[150%] text-[#1c3873]">
              4.9 (52)
            </span>
          </div>
          <h2 className="self-stretch font-heading text-lg font-medium leading-7 text-[#1c3873] sm:text-xl sm:leading-8 md:text-2xl md:leading-8">
            <Link
              href={`/courses/${course.slug}`}
              className="no-underline transition-colors duration-300 hover:text-[#3b82f6] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#3b82f6]"
            >
              {course.title}
            </Link>
          </h2>
        </header>

        {/* Metadata */}
        <dl className="flex flex-wrap items-start content-start gap-x-[12px] gap-y-[8px] self-stretch">
          {course.level && (
            <div className="inline-flex items-center gap-1">
              <dt className="sr-only">{t(`filters.${course.level.toLowerCase()}`)}</dt>
              <dd className="inline-flex items-center gap-1 m-0">
                <Clock className="h-4 w-4 text-[#6b6b68]" />
                <span className="font-sans text-sm leading-[150%] text-[#6b6b68]">
                  {t(`filters.${course.level.toLowerCase()}`)}
                </span>
              </dd>
            </div>
          )}
          <div className="inline-flex items-center gap-1">
            <dt className="sr-only">videos</dt>
            <dd className="inline-flex items-center gap-1 m-0">
              <MonitorPlay className="h-4 w-4 text-[#6b6b68]" />
              <span className="font-sans text-sm leading-[150%] text-[#6b6b68]">
                {t('card.videos', { count: 10 })}
              </span>
            </dd>
          </div>
          <div className="inline-flex items-center gap-1">
            <dt className="sr-only">live class</dt>
            <dd className="inline-flex items-center gap-1 m-0">
              <Users className="h-4 w-4 text-[#6b6b68]" />
              <span className="font-sans text-sm leading-[150%] text-[#6b6b68]">
                {t('card.liveClass', { count: 1 })}
              </span>
            </dd>
          </div>
        </dl>

        {/* Price + CTA */}
        <div className="flex w-full items-center justify-between gap-3 self-stretch sm:gap-5">
          <p className="font-sans text-base font-semibold leading-6 text-[#806600] whitespace-nowrap" aria-label={`Price ${priceParts.amount}${priceParts.unit}`}>
            {priceParts.amount}{priceParts.unit && <span className="font-normal">{priceParts.unit}</span>}
          </p>
          <Link
            href={`/courses/${course.slug}`}
            className="inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-[6px] px-4 py-2.5 font-sans text-sm font-semibold leading-6 text-white no-underline cursor-pointer transition-all duration-300 hover:shadow-[0px_4px_12px_rgba(79,103,216,0.4)] hover:brightness-110 bg-[linear-gradient(19.99deg,#4f67d8,#3a4fc2_60%,#2e3fa8)] shadow-[inset_0px_-1px_0px_rgba(0,0,0,0.18),inset_0px_1px_0px_rgba(255,255,255,0.12)] sm:h-[46px] sm:text-base sm:px-5"
          >
            {t('card.seeDetails')}
            <ChevronRight className="h-5 w-5 transition-transform duration-300 group-hover:translate-x-0.5 sm:h-6 sm:w-6" />
          </Link>
        </div>
      </div>
    </article>
  )
}
