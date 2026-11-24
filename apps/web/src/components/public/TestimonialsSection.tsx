'use client'

import { useRef, useEffect, useCallback, useState, forwardRef } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { Star, ChevronRight, ChevronLeft } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { usePublicReviews } from '@/hooks/useCourses'

const TESTIMONIAL_AVATARS = [
  '/images/testimonial-1.png',
  '/images/testimonial-2.png',
  '/images/testimonial-3.png',
] as const

const TestimonialCard = forwardRef<HTMLDivElement, {
  quote: string
  author: string
  role: string
  avatar: string
  variant: 'primary' | 'secondary'
}>(({ quote, author, role, avatar, variant }, ref) => {
  return (
    <section ref={ref}
      className={`flex min-h-[350px] w-[340px] shrink-0 flex-col items-center justify-center gap-[60px] rounded-3xl px-8 py-10 text-center transition-all duration-300 hover:scale-[1.02] hover:shadow-lg sm:h-[468px] sm:w-auto sm:p-[60px] ${
        variant === 'primary'
          ? 'bg-white'
          : 'bg-white shadow-[0px_6px_15px_-2px_rgba(16,24,40,0.08)]'
      }`}
    >
      <div className="flex items-center gap-1">
        {Array.from({ length: 5 }, (_, i) => (
          <Star key={i} className="h-5 w-5 fill-[#BFDBFE] text-[#BFDBFE]" />
        ))}
      </div>
      <p
        className={`w-[900px] max-w-full font-sans text-[28px] leading-[36px] tracking-[-1px] max-sm:text-[22px] max-sm:leading-[29px] ${
          variant === 'primary' ? 'text-black' : 'text-[#1b1c1a]'
        }`}
      >
        {quote}
      </p>
      <div className="flex flex-col items-center gap-3">
        <Image
          src={avatar}
          alt={author}
          width={40}
          height={40}
          className="h-10 w-10 rounded-full object-cover"
        />
        <div className="flex flex-col items-center">
          <span
            className={`font-sans text-lg font-semibold leading-6 ${
              variant === 'primary'
                ? 'tracking-[-0.5px]'
                : ''
            }`}
          >
            {author}
          </span>
          <span className="font-sans text-sm font-normal leading-5 text-[var(--color-text-body-secondary)]">
            {role}
          </span>
        </div>
      </div>
    </section>
  )
})
TestimonialCard.displayName = 'TestimonialCard'

export function TestimonialsSection() {
  const t = useTranslations('testimonials')
  const { data: publicReviews } = usePublicReviews()
  const title = t('title')
  const ctaText = t('cta')
  const ctaLink = '/signup'
  const sectionRef = useRef<HTMLElement>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const cardRefs = useRef<(HTMLDivElement | null)[]>([])
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const isTeleporting = useRef(false)

  // activeIdx tracks the REAL card index (0..total-1)
  const [activeIdx, setActiveIdx] = useState(0)
  const [isVisible, setIsVisible] = useState(false)

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
  }, [])

  const fallbackTestimonials = [
    {
      quote: t('items.0.quote'),
      author: t('items.0.author'),
      role: t('items.0.role'),
      avatar: TESTIMONIAL_AVATARS[0],
    },
    {
      quote: t('items.1.quote'),
      author: t('items.1.author'),
      role: t('items.1.role'),
      avatar: TESTIMONIAL_AVATARS[1],
    },
    {
      quote: t('items.2.quote'),
      author: t('items.2.author'),
      role: t('items.2.role'),
      avatar: TESTIMONIAL_AVATARS[2],
    },
  ]

  const testimonials = publicReviews && publicReviews.length > 0
    ? publicReviews.slice(0, 6).map((review, index) => ({
        quote: review.reviewText ?? '',
        author: review.studentName ?? 'Student',
        role: 'GII learner',
        avatar: TESTIMONIAL_AVATARS[index % TESTIMONIAL_AVATARS.length]!,
      }))
    : fallbackTestimonials

  const total = testimonials.length

  // Extended slides: [clone-last, ...real slides, clone-first]
  // Display indices: 0=clone-last, 1..total=real, total+1=clone-first
  const extendedSlides = [
    testimonials[testimonials.length - 1],
    ...testimonials,
    testimonials[0],
  ]

  const getScrollLeftForSlot = useCallback((slotIdx: number) => {
    const el = scrollRef.current
    const card = cardRefs.current[slotIdx]
    if (!el || !card) return 0
    return card.offsetLeft - (el.clientWidth / 2) + (card.offsetWidth / 2)
  }, [])

  const scrollToSlot = useCallback((slotIdx: number, smooth = true) => {
    const el = scrollRef.current
    if (!el) return
    const target = getScrollLeftForSlot(slotIdx)
    el.scrollTo({ left: target, behavior: smooth ? 'smooth' : 'instant' })
  }, [getScrollLeftForSlot])

  // Map real index to display slot index (real 0 -> slot 1, real 1 -> slot 2, etc.)
  const realToSlot = useCallback((realIdx: number) => realIdx + 1, [])

  // Map display slot index to real index
  const slotToReal = useCallback((slotIdx: number) => {
    if (slotIdx <= 0) return total - 1
    if (slotIdx > total) return 0
    return slotIdx - 1
  }, [total])

  // Center the first real card on mount
  useEffect(() => {
    if (!isVisible) return
    const timer = setTimeout(() => {
      scrollToSlot(realToSlot(0), false)
    }, 100)
    return () => clearTimeout(timer)
  }, [isVisible, scrollToSlot, realToSlot])

  const stopAutoplay = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const goNext = useCallback(() => {
    const el = scrollRef.current
    if (!el) return
    isTeleporting.current = false

    // Scroll to next slot (always forward)
    const currentSlot = realToSlot(activeIdx)
    const nextSlot = currentSlot + 1
    scrollToSlot(nextSlot, true)

    // If we scrolled to the clone-first (slot total+1), teleport after animation
    if (nextSlot > total) {
      isTeleporting.current = true
      setTimeout(() => {
        if (!isTeleporting.current) return
        scrollToSlot(realToSlot(0), false)
        isTeleporting.current = false
      }, 500)
      setActiveIdx(0)
    } else {
      setActiveIdx(slotToReal(nextSlot))
    }
  }, [activeIdx, total, scrollToSlot, realToSlot, slotToReal])

  const goPrev = useCallback(() => {
    const el = scrollRef.current
    if (!el) return
    isTeleporting.current = false

    const currentSlot = realToSlot(activeIdx)
    const prevSlot = currentSlot - 1
    scrollToSlot(prevSlot, true)

    // If we scrolled to the clone-last (slot 0), teleport after animation
    if (prevSlot <= 0) {
      isTeleporting.current = true
      setTimeout(() => {
        if (!isTeleporting.current) return
        scrollToSlot(realToSlot(total - 1), false)
        isTeleporting.current = false
      }, 500)
      setActiveIdx(total - 1)
    } else {
      setActiveIdx(slotToReal(prevSlot))
    }
  }, [activeIdx, total, scrollToSlot, realToSlot, slotToReal])

  const startAutoplay = useCallback(() => {
    stopAutoplay()
    timerRef.current = setInterval(goNext, 4000)
  }, [stopAutoplay, goNext])

  useEffect(() => {
    if (isVisible) startAutoplay()
    else stopAutoplay()
    return stopAutoplay
  }, [isVisible, startAutoplay, stopAutoplay])

  const scroll = (dir: 'left' | 'right') => {
    if (dir === 'right') {
      goNext()
    } else {
      goPrev()
    }
    if (isVisible) startAutoplay()
  }

  const onDotClick = (realIdx: number) => {
    isTeleporting.current = false
    scrollToSlot(realToSlot(realIdx), true)
    setActiveIdx(realIdx)
    if (isVisible) startAutoplay()
  }

  return (
    <section ref={sectionRef} className="relative isolate overflow-hidden bg-[#f5f5f5] py-12 sm:py-16 lg:py-20">
      <div className="container mx-auto px-5">
        <div className="relative z-10 flex items-center justify-between gap-5 max-lg:flex-wrap max-lg:gap-5">
          <h2 className="w-[544px] max-w-full font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[#2563eb] max-sm:text-[24px] max-sm:leading-[29px] max-md:text-[32px] max-md:leading-[38px]">
            {title}
          </h2>
          <Link
            href={ctaLink}
            className="btn-blue-gradient inline-flex h-[46px] items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white no-underline transition-all duration-200 hover:opacity-90 hover:scale-[1.02]"
          >
            {ctaText}
            <ChevronRight className="h-6 w-6" />
          </Link>
        </div>
      </div>

      {/* Dot indicators */}
      <div className="mt-8 flex items-center justify-center gap-2">
        {testimonials.map((_, idx) => (
          <button
            key={idx}
            type="button"
            onClick={() => onDotClick(idx)}
            className="flex h-6 w-6 cursor-pointer items-center justify-center rounded-full"
            aria-label={`Go to testimonial ${idx + 1}`}
            aria-current={idx === activeIdx ? 'true' : undefined}
          >
            <span
              aria-hidden="true"
              className={`h-2 rounded-full transition-all duration-300 ${
                idx === activeIdx ? 'w-6 bg-[#2563eb]' : 'w-2 bg-[#bfdbfe]'
              }`}
            />
          </button>
        ))}
      </div>

      <div className="relative mt-6">
        <div className="testimonial-fade-left pointer-events-none absolute bottom-0 left-0 z-[2] h-full w-20 sm:h-[468px] sm:w-[361px]" />
        <div className="testimonial-fade-right pointer-events-none absolute bottom-0 right-0 z-[3] h-full w-20 sm:h-[468px] sm:w-[361px]" />

        <button
          type="button"
          onClick={() => scroll('left')}
          className="absolute left-0 top-1/2 z-[5] -translate-y-1/2 flex items-center rounded-xl bg-white p-6 transition-all duration-200 hover:shadow-md hover:scale-105 cursor-pointer"
          aria-label="Previous"
        >
          <ChevronLeft className="h-9 w-9" />
        </button>
        <button
          type="button"
          onClick={() => scroll('right')}
          className="absolute right-0 top-1/2 z-[4] -translate-y-1/2 flex items-center rounded-xl bg-white p-6 transition-all duration-200 hover:shadow-md hover:scale-105 cursor-pointer"
          aria-label="Next"
        >
          <ChevronRight className="h-9 w-9" />
        </button>

        <div
          ref={scrollRef}
          tabIndex={0}
          role="region"
          aria-label="Student testimonials"
          className="relative z-[1] flex items-center justify-center gap-6 overflow-x-auto"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
          onMouseEnter={stopAutoplay}
          onMouseLeave={() => { if (isVisible) startAutoplay() }}
          onFocus={stopAutoplay}
          onBlur={() => { if (isVisible) startAutoplay() }}
        >
          {extendedSlides.map((item, slotIdx) => {
            if (!item) return null
            const realIdx = slotToReal(slotIdx)
            return (
              <TestimonialCard
                key={slotIdx}
                ref={(el) => { cardRefs.current[slotIdx] = el }}
                quote={item.quote}
                author={item.author}
                role={item.role}
                avatar={item.avatar}
                variant={realIdx === activeIdx ? 'secondary' : 'primary'}
              />
            )
          })}
        </div>
      </div>
    </section>
  )
}
