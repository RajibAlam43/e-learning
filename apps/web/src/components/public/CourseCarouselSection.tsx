'use client'

import { useRef, useEffect, useCallback, useState } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import { useFeaturedCourses } from '@/hooks/useCourses'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Star,
  Clock,
  MonitorPlay,
  Users,
  ChevronRight,
  ChevronLeft,
} from 'lucide-react'

const AUTOPLAY_INTERVAL = 4000

function CarouselCard({
  image,
  badge,
  rating,
  reviewCount,
  title,
  duration,
  videos,
  liveClasses,
  price,
  buttonStyle,
  href,
  t,
}: {
  image: string
  badge: string
  rating: number
  reviewCount: number
  title: string
  duration: string
  videos: number
  liveClasses: number
  price: string
  buttonStyle: 'blue' | 'gold'
  href: string
  t: ReturnType<typeof useTranslations>
}) {
  return (
    <article
      className="shrink-0 w-full snap-center overflow-hidden rounded-lg bg-white transition-all duration-300 hover:scale-[1.02] hover:shadow-lg md:w-[400px]"
    >
      <div className="relative h-[274px] overflow-hidden">
        <Image
          src={image}
          alt={title}
          fill
          className="object-cover"
          sizes="400px"
        />
        <span className="absolute left-4 top-4 z-10 rounded bg-[#b45309] px-2 py-1 font-sans text-xs font-medium leading-5 text-[var(--color-gold-50)]">
          {badge}
        </span>
      </div>
      <div className="flex flex-col gap-4 p-6">
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-1">
            <Star className="h-4 w-4 fill-amber-500 text-amber-500" />
            <span className="font-sans text-sm leading-[150%] text-[var(--color-text-headline-primary)]">
              {rating} ({reviewCount})
            </span>
          </div>
          <h3 className="font-heading text-2xl font-medium leading-8 text-[var(--color-secondary-blue-500)]">
            {title}
          </h3>
        </div>
        <div className="flex flex-wrap gap-x-3 gap-y-2 font-sans text-sm leading-[150%] text-[#6b6b68]">
          <span className="flex items-center gap-1">
            <Clock className="h-4 w-4" />
            {duration}
          </span>
          <span className="flex items-center gap-1">
            <MonitorPlay className="h-4 w-4" />
            {videos} {t('videos')}
          </span>
          <span className="flex items-center gap-1">
            <Users className="h-4 w-4" />
            {liveClasses} {t('liveClass')}
          </span>
        </div>
        <div className="flex items-center justify-between gap-5">
          <span className="font-sans text-lg font-semibold leading-6 text-[#806600]">
            {price}
          </span>
          <Link
            href={href}
            className={`inline-flex h-[46px] items-center justify-center gap-2 rounded-md px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white transition-all duration-200 hover:opacity-90 ${
              buttonStyle === 'blue'
                ? 'btn-blue-gradient'
                : 'btn-gold-gradient'
            }`}
          >
            {t('seeDetails')}
            <ChevronRight className="h-5 w-5" />
          </Link>
        </div>
      </div>
    </article>
  )
}

export function CourseCarouselSection() {
  const t = useTranslations('courseCarousel')
  const { data: coursesData, isLoading, isError, refetch } = useFeaturedCourses(6)
  const sectionPresent = isLoading || isError || (coursesData?.length ?? 0) > 0
  const sectionRef = useRef<HTMLElement>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const [isVisible, setIsVisible] = useState(false)
  // Active card index — kept in refs because it changes inside timers/events.
  const activeIdxRef = useRef(0)
  const countRef = useRef(0)

  useEffect(() => {
    const section = sectionRef.current
    if (!section || typeof IntersectionObserver === 'undefined') {
      setIsVisible(true)
      return
    }
    const observer = new IntersectionObserver(
      ([entry]) => setIsVisible(entry?.isIntersecting ?? false),
      { rootMargin: '200px 0px', threshold: 0.01 },
    )
    observer.observe(section)
    return () => observer.disconnect()
  }, [sectionPresent])

  // Keep the card count in sync for timers and navigation.
  useEffect(() => {
    countRef.current = coursesData?.length ?? 0
  }, [coursesData])

  const stopAutoplay = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  // Scroll to center a card by index
  const scrollToCard = useCallback((idx: number) => {
    const el = scrollRef.current
    if (!el) return
    const cards = el.children as HTMLCollectionOf<HTMLElement>
    const card = cards[idx]
    if (!card) return
    const cardLeft = card.offsetLeft
    const cardWidth = card.offsetWidth
    const containerWidth = el.clientWidth
    el.scrollTo({ left: cardLeft - (containerWidth / 2) + (cardWidth / 2), behavior: 'smooth' })
  }, [])

  // Index of the card currently closest to the viewport centre.
  const nearestCenterIndex = useCallback(() => {
    const el = scrollRef.current
    if (!el) return 0
    const cards = el.children as HTMLCollectionOf<HTMLElement>
    const center = el.scrollLeft + el.clientWidth / 2
    let best = 0
    let bestDist = Infinity
    for (let i = 0; i < cards.length; i++) {
      const card = cards[i]
      if (!card) continue
      const dist = Math.abs(card.offsetLeft + card.offsetWidth / 2 - center)
      if (dist < bestDist) {
        bestDist = dist
        best = i
      }
    }
    return best
  }, [])

  // After a swipe, adopt the card the user landed on so autoplay continues from it.
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    let settleTimer: ReturnType<typeof setTimeout> | undefined
    const handleScroll = () => {
      clearTimeout(settleTimer)
      settleTimer = setTimeout(() => {
        activeIdxRef.current = nearestCenterIndex()
      }, 120)
    }
    el.addEventListener('scroll', handleScroll, { passive: true })
    return () => {
      el.removeEventListener('scroll', handleScroll)
      clearTimeout(settleTimer)
    }
  }, [nearestCenterIndex, sectionPresent])

  /** Move to an index (wraps in both directions), always centering the card. */
  const goTo = useCallback((idx: number) => {
    const el = scrollRef.current
    const count = countRef.current
    if (!el || !count) return
    const target = idx < 0 ? count - 1 : idx >= count ? 0 : idx

    if (idx >= count) {
      // Forward wrap: teleport home, then centre the first card.
      el.scrollTo({ left: 0, behavior: 'auto' })
      activeIdxRef.current = 0
      setTimeout(() => scrollToCard(0), 60)
      return
    }
    if (idx < 0) {
      // Backward wrap: teleport to the end, then centre the last card.
      el.scrollTo({ left: el.scrollWidth, behavior: 'auto' })
      activeIdxRef.current = count - 1
      setTimeout(() => scrollToCard(count - 1), 60)
      return
    }

    activeIdxRef.current = target
    scrollToCard(target)
  }, [scrollToCard])

  const startAutoplay = useCallback(() => {
    stopAutoplay()
    timerRef.current = setInterval(() => {
      if (!scrollRef.current || !countRef.current) return
      goTo(activeIdxRef.current + 1)
    }, AUTOPLAY_INTERVAL)
  }, [stopAutoplay, goTo])

  useEffect(() => {
    if (isVisible && coursesData?.length) startAutoplay()
    else stopAutoplay()
    return stopAutoplay
  }, [isVisible, coursesData?.length, startAutoplay, stopAutoplay])

  const handleNav = (dir: 'left' | 'right') => {
    goTo(nearestCenterIndex() + (dir === 'left' ? -1 : 1))
    startAutoplay()
  }

  if (!sectionPresent) return null

  return (
    <section ref={sectionRef} className="relative overflow-hidden bg-[var(--color-neutral-50)] py-12 px-5 xl:px-32 max-md:py-8">
      <div
        className="pointer-events-none absolute right-[-212px] top-[117px] h-[696px] w-[1797px] rounded-full bg-[var(--color-neutral-100)] opacity-50"
        style={{ filter: 'blur(138px)' }}
        aria-hidden="true"
      />

      <div  className="relative z-10 hard-w container mx-auto overflow-hidden rounded-xl bg-[var(--color-brand-primary-50)] py-10 px-5 shadow-[0px_25px_60px_-15px_rgba(16,24,40,0.2)] max-md:py-8 max-md:px-4">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <h2 className="font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[var(--color-text-headline-primary)] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[24px] max-sm:leading-[29px]">
            {t('title')}
          </h2>
          <p className="w-[432px] max-w-full font-sans text-base leading-6 text-[var(--color-text-body-secondary)]">
            {t('description')}
          </p>
        </div>

        <div className="relative">
          <div
            ref={scrollRef}
            tabIndex={0}
            role="region"
            aria-label="Course carousel"
            className="flex snap-x snap-mandatory items-center gap-4 overflow-x-auto"
            style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
            onMouseEnter={stopAutoplay}
            onMouseLeave={() => { if (isVisible) startAutoplay() }}
            onFocus={stopAutoplay}
            onBlur={() => { if (isVisible) startAutoplay() }}
          >
            {isLoading ? (
              <>
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="shrink-0 w-full snap-center md:w-[400px] h-[450px] rounded-lg" />
                ))}
              </>
            ) : isError ? <div role="alert" className="w-full rounded-lg border border-red-200 bg-red-50 p-6 text-center text-sm text-red-800">Special courses could not be loaded. <button type="button" className="font-semibold underline" onClick={() => refetch()}>Try again</button></div> : (coursesData ?? []).map((course, idx) => {
              const isFree = (course.priceBdt ?? -1) === 0
              return (
                <CarouselCard
                  key={course.id ?? idx}
                  image={course.thumbnailUrl ?? '/images/manuscript-7.png'}
                  badge={course.level ?? 'POPULAR'}
                  rating={0}
                  reviewCount={0}
                  title={course.title ?? ''}
                  duration="—"
                  videos={0}
                  liveClasses={0}
                  price={isFree ? 'Free' : `${course.priceBdt?.toLocaleString() ?? 0} BDT`}
                  buttonStyle="gold"
                  href={`/courses/${course.slug ?? course.id}`}
                  t={t}
                />
              )
            })}
          </div>

          <button
            type="button"
            onClick={() => handleNav('left')}
            className="absolute left-2 top-1/2 z-20 -translate-y-1/2 flex items-center justify-center rounded-xl bg-white/80 p-3 transition-all duration-200 hover:bg-white hover:shadow-md hover:scale-105 cursor-pointer"
            aria-label="Previous"
          >
            <ChevronLeft className="h-8 w-8 text-[var(--color-text-headline-primary)]" />
          </button>
          <button
            type="button"
            onClick={() => handleNav('right')}
            className="absolute right-2 top-1/2 z-20 -translate-y-1/2 flex items-center justify-center rounded-xl bg-white/80 p-3 transition-all duration-200 hover:bg-white hover:shadow-md hover:scale-105 cursor-pointer"
            aria-label="Next"
          >
            <ChevronRight className="h-8 w-8 text-[var(--color-text-headline-primary)]" />
          </button>
        </div>

        <div className="mt-6 flex justify-center">
          <Link
            href="/courses"
            className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-solid border-[rgba(37,99,235,0.3)] bg-white px-5 py-2.5 font-sans text-base font-semibold leading-6 text-[var(--color-secondary-blue-600)] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]"
          >
            {t('seeAll')}
            <ChevronRight className="h-5 w-5" />
          </Link>
        </div>
      </div>
    </section>
  )
}
