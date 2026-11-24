'use client'

import { useRef, useEffect, useState, useCallback, useMemo } from 'react'
import Image from 'next/image'
import { useTranslations } from 'next-intl'
import { usePlatformStats } from '@/hooks/useCourses'

const iconMap = {
  BookOpen: '/images/book-icon.svg',
  Users: '/images/users-icon.svg',
  GraduationCap: '/images/identity.svg',
  Award: '/images/graduate.svg',
} as const
type StatIcon = keyof typeof iconMap

function parseQuantity(raw: string) {
  const match = raw.match(/^(\d+)(.*)/)
  if (!match) return { target: 0, suffix: '' }
  return { target: Number(match[1]), suffix: match[2] }
}

function useCounter(target: number, shouldStart: boolean) {
  const [count, setCount] = useState(0)

  useEffect(() => {
    if (!shouldStart || target === 0) return
    const duration = 2000
    const frameDuration = 1000 / 60
    const totalFrames = Math.round(duration / frameDuration)
    let frame = 0
    const raf = requestAnimationFrame(function tick() {
      frame++
      const progress = frame / totalFrames
      const eased = 1 - Math.pow(1 - progress, 3)
      setCount(Math.round(eased * target))
      if (frame < totalFrames) {
        requestAnimationFrame(tick)
      } else {
        setCount(target)
      }
    })
    return () => cancelAnimationFrame(raf)
  }, [target, shouldStart])

  return count
}

function StatCard({
  icon,
  quantity,
  label,
  shouldAnimate,
}: {
  icon: StatIcon
  quantity: string
  label: string
  shouldAnimate: boolean
}) {
  const iconSrc = iconMap[icon]
  const { target, suffix } = parseQuantity(quantity)
  const count = useCounter(target, shouldAnimate)

  return (
    <article className="flex flex-1 items-center gap-6 rounded-lg bg-white/80 px-6 py-6 backdrop-blur-md max-sm:gap-4 max-sm:px-4 max-sm:py-4">
      <div className="flex h-[72px] w-[72px] shrink-0 items-center justify-center rounded-lg bg-white max-sm:h-12 max-sm:w-12">
        <Image
          src={iconSrc}
          alt=""
          width={36}
          height={36}
          className="h-9 w-9 object-contain max-sm:h-6 max-sm:w-6"
          aria-hidden="true"
        />
      </div>
      <div className="flex flex-1 flex-col justify-center gap-2 max-sm:gap-1">
        <p className="font-sans text-[32px] font-semibold leading-8 tracking-[-1px] text-[var(--color-text-headline-primary)] max-sm:text-xl max-sm:leading-6">
          {shouldAnimate ? `${count}${suffix}` : quantity}
        </p>
        <p className="font-sans text-lg font-medium leading-7 text-[var(--color-text-body-secondary)] max-sm:text-sm max-sm:leading-5">
          {label}
        </p>
      </div>
    </article>
  )
}

export function StatsSection() {
  const t = useTranslations('stats')
  const { data: platformStats } = usePlatformStats()
  const sectionRef = useRef<HTMLElement>(null)
  const [isVisible, setIsVisible] = useState(false)

  const statsData = useMemo(
    () => [
      { icon: 'BookOpen' as const, quantity: platformStats?.courses != null ? `${platformStats.courses}+` : '20+', label: t('courses') },
      { icon: 'Users' as const, quantity: platformStats?.students != null ? `${platformStats.students}+` : '10k+', label: t('students') },
      { icon: 'GraduationCap' as const, quantity: platformStats?.instructors != null ? `${platformStats.instructors}+` : '10+', label: t('scholars') },
      { icon: 'Award' as const, quantity: platformStats?.programs != null ? `${platformStats.programs}+` : '5+', label: t('diplomas') },
    ],
    [t, platformStats]
  )

  const handleIntersection = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const [entry] = entries
      if (entry?.isIntersecting) {
        setIsVisible(true)
      }
    },
    []
  )

  useEffect(() => {
    const el = sectionRef.current
    if (!el) return
    const observer = new IntersectionObserver(handleIntersection, {
      threshold: 0.3,
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [handleIntersection])

  return (
    <section
      ref={sectionRef}
      aria-label="Platform statistics"
      className="content-auto-section stats-gradient relative z-10 -mt-24 w-full flex items-end justify-center pt-24 pb-12 max-md:-mt-16 max-md:pt-16 max-md:pb-8"
    >
      <div className="grid w-full container mx-auto px-5 grid-cols-2 gap-4 lg:flex">
        {statsData.map((stat) => (
          <StatCard
            key={stat.label}
            icon={stat.icon}
            quantity={stat.quantity}
            label={stat.label}
            shouldAnimate={isVisible}
          />
        ))}
      </div>
    </section>
  )
}
