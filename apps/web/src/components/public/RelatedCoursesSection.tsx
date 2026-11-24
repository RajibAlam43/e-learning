'use client'

import { useRef, useEffect, useCallback } from 'react'
import Link from 'next/link'
import Image from 'next/image'
import { useTranslations } from 'next-intl'
import { Clock, MonitorPlay, Users, ChevronRight, ChevronLeft } from 'lucide-react'

interface RelatedCoursesSectionProps {
  courses?: {
    id?: string
    title?: string
    slug?: string
    thumbnailUrl?: string
    shortDescription?: string
    priceBdt?: number
    level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  }[]
}

const CARD_WIDTH = 384
const CARD_GAP = 16
const AUTOPLAY_INTERVAL = 4000

const BADGE_MAP: Record<string, string> = {
  BEGINNER: 'BEGINNER',
  INTERMEDIATE: 'INTERMEDIATE',
  ADVANCED: 'ADVANCED',
}

export function RelatedCoursesSection({ courses }: RelatedCoursesSectionProps) {
  const t = useTranslations('courses.detail')
  const safeCourses = courses ?? []
  const scrollRef = useRef<HTMLDivElement>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const scrollAmount = CARD_WIDTH + CARD_GAP

  const stopAutoplay = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const scrollToCard = useCallback((idx: number) => {
    const el = scrollRef.current
    if (!el) return
    const cards = el.children as HTMLCollectionOf<HTMLElement>
    if (!cards[idx]) return
    const cardLeft = cards[idx].offsetLeft
    const cardWidth = cards[idx].offsetWidth
    const containerWidth = el.clientWidth
    el.scrollTo({ left: cardLeft - (containerWidth / 2) + (cardWidth / 2), behavior: 'smooth' })
  }, [])

  const startAutoplay = useCallback(() => {
    stopAutoplay()
    timerRef.current = setInterval(() => {
      if (!scrollRef.current) return
      const el = scrollRef.current
      const maxScroll = el.scrollWidth - el.clientWidth

      if (Math.ceil(el.scrollLeft) >= maxScroll) {
        el.scrollTo({ left: 0 })
        setTimeout(() => scrollToCard(0), 50)
      } else {
        el.scrollBy({ left: scrollAmount, behavior: 'smooth' })
      }
    }, AUTOPLAY_INTERVAL)
  }, [scrollAmount, stopAutoplay, scrollToCard])

  useEffect(() => {
    startAutoplay()
    return stopAutoplay
  }, [startAutoplay, stopAutoplay])

  if (safeCourses.length === 0) return null

  const handleNav = (dir: 'left' | 'right') => {
    if (!scrollRef.current) return
    const el = scrollRef.current
    const maxScroll = el.scrollWidth - el.clientWidth

    if (dir === 'left' && el.scrollLeft <= 1) {
      el.scrollTo({ left: maxScroll, behavior: 'smooth' })
    } else if (dir === 'right' && Math.ceil(el.scrollLeft) >= maxScroll) {
      el.scrollTo({ left: 0, behavior: 'smooth' })
    } else {
      el.scrollBy({
        left: dir === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth',
      })
    }
    startAutoplay()
  }

  return (
    <section
      className="flex w-full flex-col items-center gap-8 bg-white px-5 py-20 max-[450px]:py-12"
      aria-label="Featured courses"
    >
      <div className="container gap-y-8">

      
      {/* Header */}
      <div className="flex w-full max-w-[1440px] items-center justify-between gap-4 mb-4">
        <h2 className="mt-[-1px] text-[20px] md:text-[40px] font-normal leading-[48px] tracking-[-1px] text-[#1c3873] font-heading whitespace-nowrap leading-[34px]">
          {t('relatedTitle.title')}
        </h2>
        <Link
          href="/courses"
          className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-[#3b82f64c] bg-white px-5 py-2.5 text-[14px] md:text-base font-semibold leading-6 text-[#3b82f6] no-underline transition-colors hover:bg-blue-50"
        >
          {t('relatedTitle.exploreAll')}
          <ChevronRight className="h-6 w-6 text-[#3b82f6]" />
        </Link>
      </div>

      {/* Carousel */}
      <div className="relative w-full max-w-[1440px]">
        <div
          ref={scrollRef}
          className="flex items-center gap-4 overflow-x-auto"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
          onMouseEnter={stopAutoplay}
          onMouseLeave={startAutoplay}
        >
          {safeCourses.map((course) => {
            const isFree = (course.priceBdt ?? -1) === 0
            const priceDisplay = isFree
              ? t('relatedTitle.free')
              : course.priceBdt != null
                ? `${course.priceBdt.toLocaleString()} BDT`
                : t('relatedTitle.tba')

            return (
              <article
                key={course.id}
                className="flex w-full md:w-96 shrink-0 flex-col items-start bg-white transition-all duration-300 hover:scale-[1.02] hover:shadow-lg"
              >
                <div className="relative h-[274px] w-full shrink-0 overflow-hidden rounded-md group">
                  {course.thumbnailUrl ? (
                    <Image
                      src={course.thumbnailUrl}
                      alt={course.title ?? 'Course'}
                      fill
                      className="object-cover transition-transform duration-500 group-hover:scale-105"
                      sizes="384px"
                    />
                  ) : (
                    <div className="h-full w-full bg-[#e8ebf1]" />
                  )}
                  {course.level && (
                    <span className="absolute left-4 top-4 rounded bg-[#b45309] px-2 py-1 font-sans text-xs font-medium leading-5 text-[#fffbeb]">
                      {BADGE_MAP[course.level] ?? course.level}
                    </span>
                  )}
                </div>

                <div className="flex w-full flex-col gap-4 p-6">
                  <h3 className="font-heading text-2xl font-medium leading-8 text-[#3b82f6]">
                    <Link href={`/courses/${course.slug}`} className="hover:underline no-underline text-inherit">
                      {course.title}
                    </Link>
                  </h3>

                  <div className="flex flex-wrap items-start gap-3 font-sans text-sm leading-[150%] text-[#777774]">
                    <span className="flex items-center gap-1">
                      <Clock className="h-4 w-4" />
                      {t('relatedTitle.duration')}
                    </span>
                    <span className="flex items-center gap-1">
                      <MonitorPlay className="h-4 w-4" />
                      {t('relatedTitle.videos')}
                    </span>
                    <span className="flex items-center gap-1">
                      <Users className="h-4 w-4" />
                      {t('relatedTitle.liveClass')}
                    </span>
                  </div>

                  <div className="flex items-center justify-between">
                    <p className="font-sans text-lg leading-[18px] text-[#806600]">
                      <span className="font-semibold">{priceDisplay}</span>
                    </p>
                    <Link
                      href={`/courses/${course.slug}`}
                      className="inline-flex h-[46px] items-center justify-center gap-2 rounded-md bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white shadow-[inset_0px_1px_0px_#ffffff1f,inset_0px_-1px_0px_#0000002e] transition-all duration-200 hover:opacity-90 hover:shadow-lg no-underline"
                    >
                      {t('relatedTitle.viewDetails')}
                      <ChevronRight className="h-6 w-6 text-white" />
                    </Link>
                  </div>
                </div>
              </article>
            )
          })}
        </div>

        {/* Nav buttons */}
        <button
          type="button"
          onClick={() => handleNav('left')}
          className="absolute left-2 top-1/2 z-20 -translate-y-1/2 flex items-center justify-center rounded-xl bg-white/80 p-3 transition-all duration-200 hover:bg-white hover:shadow-md hover:scale-105 cursor-pointer"
          aria-label="Previous"
        >
          <ChevronLeft className="h-8 w-8 text-[#1c3873]" />
        </button>
        <button
          type="button"
          onClick={() => handleNav('right')}
          className="absolute right-2 top-1/2 z-20 -translate-y-1/2 flex items-center justify-center rounded-xl bg-white/80 p-3 transition-all duration-200 hover:bg-white hover:shadow-md hover:scale-105 cursor-pointer"
          aria-label="Next"
        >
          <ChevronRight className="h-8 w-8 text-[#1c3873]" />
        </button>
      </div>
      </div>
    </section>
  )
}
