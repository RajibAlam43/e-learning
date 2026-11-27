'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import { useCourses } from '@/hooks/useCourses'
import { Skeleton } from '@/components/ui/skeleton'
import { Star, Clock, MonitorPlay, Users, ChevronRight } from 'lucide-react'

interface CourseCardProps {
  image: string
  badge: string
  rating: number
  reviewCount: number
  title: string
  duration: string
  videos: number
  liveClasses: number
  price: string
  viewDetailsLabel: string
  videosLabel: string
  liveClassLabel: string
  href: string
}

function CourseCard({
  image,
  badge,
  rating,
  reviewCount,
  title,
  duration,
  videos,
  liveClasses,
  price,
  viewDetailsLabel,
  videosLabel,
  liveClassLabel,
  href,
}: CourseCardProps) {
  return (
    <article className="flex flex-1 flex-col overflow-hidden rounded-lg bg-white transition-all duration-300 hover:scale-[1.02] hover:shadow-lg">
      <div className="relative h-[274px] shrink-0 overflow-hidden">
        <Image
          src={image}
          alt={title}
          fill
          className="rounded-md object-cover"
          sizes="(max-width: 768px) 100vw, 33vw"
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
            {videos} {videosLabel}
          </span>
          <span className="flex items-center gap-1">
            <Users className="h-4 w-4" />
            {liveClasses} {liveClassLabel}
          </span>
        </div>
        <div className="flex items-center justify-between gap-5">
          <span className="font-sans text-lg font-medium leading-7 text-[#806600]">
            {price}
          </span>
          <Link
            href={href}
            className="btn-blue-gradient inline-flex h-[46px] items-center justify-center gap-2 rounded-md px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white transition-all duration-200 hover:opacity-90"
          >
            {viewDetailsLabel}
            <ChevronRight className="h-5 w-5" />
          </Link>
        </div>
      </div>
    </article>
  )
}

export function SpecialCoursesSection() {
  const t = useTranslations('specialCourses')
  const { data, isLoading, isError, refetch } = useCourses({ page: 0, size: 3 })
  const apiCourses = data?.content ?? []
  if (!isLoading && !isError && apiCourses.length === 0) return null

  return (
    <section className="content-auto-section bg-white py-12 max-md:py-8">
      <div className="container mx-auto px-5">
        <div className="mb-8 flex items-center justify-between gap-5 max-md:flex-wrap">
          <div className="flex flex-col gap-2">
            <h2 className="font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[var(--color-text-headline-primary)] max-md:text-[32px] max-md:leading-[38px]">
              {t('title')}
            </h2>
            <p className="max-w-[586px] font-sans text-base leading-6 text-[var(--color-text-body-secondary)]">
              {t('description')}
            </p>
          </div>
          <Link
            href="/courses"
            className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-md border border-solid border-[rgba(37,99,235,0.3)] bg-white px-5 py-2.5 font-sans text-base font-semibold leading-6 text-[var(--color-secondary-blue-600)] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]"
          >
            {t('exploreAll')}
            <ChevronRight className="h-5 w-5" />
          </Link>
        </div>

        <div className="gap-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
          {isLoading ? (
            <>
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="flex-1 h-[450px] rounded-lg" />
              ))}
            </>
          ) : isError ? (
            <div role="alert" className="col-span-full rounded-lg border border-red-200 bg-red-50 p-6 text-center text-sm text-red-800">Featured courses could not be loaded. <button type="button" className="font-semibold underline" onClick={() => refetch()}>Try again</button></div>
          ) : apiCourses.length > 0 ? (
            apiCourses.map((course, idx) => {
              const isFree = (course.priceBdt ?? -1) === 0
              return (
                <CourseCard
                  key={course.id ?? idx}
                  image={course.thumbnailUrl ?? '/images/special-course-1.png'}
                  badge={course.level ?? t('popular')}
                  rating={course.averageRating ?? 0}
                  reviewCount={Number(course.totalReviews ?? 0)}
                  duration="—"
                  videos={0}
                  liveClasses={0}
                  price={isFree ? 'Free' : `${course.priceBdt?.toLocaleString() ?? 0} BDT`}
                  title={course.title ?? ''}
                  viewDetailsLabel={t('viewDetails')}
                  videosLabel={t('videos')}
                  liveClassLabel={t('liveClass')}
                  href={`/courses/${course.slug ?? course.id}`}
                />
              )
            })
          ) : null}
        </div>
      </div>
    </section>
  )
}
