'use client'

import Image from 'next/image'
import Link from 'next/link'
import { ChevronRight } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { usePublicCollections } from '@/hooks/useCourses'
import { Skeleton } from '@/components/ui/skeleton'

function ProgramCard({
  image,
  badge,
  title,
  description,
  ctaText,
  href,
}: {
  image: string
  badge: string
  title: string
  description: string
  ctaText: string
  href?: string
}) {
  return (
    <article className="flex flex-1 flex-col overflow-hidden rounded-lg bg-white transition-all duration-300 hover:scale-[1.02] hover:shadow-lg">
      <Link href={href ?? '#'} className="no-underline">
        <div className="relative h-[249px] shrink-0 overflow-hidden">
          <Image
            src={image}
            alt={title}
            fill
            className="object-cover"
            sizes="(max-width: 768px) 100vw, 33vw"
          />
          <span className="absolute left-4 top-4 z-10 rounded bg-[#b45309] px-2 py-1 font-sans text-xs font-medium leading-5 text-[var(--color-gold-50)]">
            {badge}
          </span>
        </div>
        <div className="flex flex-col gap-4 p-6">
          <div className="flex flex-col gap-1">
            <h3 className="font-heading text-xl leading-5 text-[var(--color-secondary-blue-500)]">
              {title}
            </h3>
            <p className="line-clamp-2 font-sans text-sm leading-[150%] text-[var(--color-text-body-secondary)]">
              {description}
            </p>
          </div>
        </div>
      </Link>
      <div className="px-6 pb-6">
        <Link
          href={href ?? '/courses'}
          className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-solid border-[rgba(37,99,235,0.3)] bg-white px-5 py-2.5 font-sans text-base font-semibold leading-6 text-[var(--color-secondary-blue-600)] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]"
        >
          {ctaText}
          <ChevronRight className="h-5 w-5" />
        </Link>
      </div>
    </article>
  )
}

export function ProgramsSection() {
  const t = useTranslations('programs')
  // Programs are modelled as "collections" in the backend — /public/collections.
  const { data, isLoading, isError, refetch } = usePublicCollections({ page: 0, size: 3 })

  const programs = (data?.content ?? []).slice(0, 3).map((c) => ({
    image: c.thumbnailUrl ?? '/images/manuscript-0.png',
    badge: c.collectionType ?? t('popular'),
    title: c.title ?? '',
    description: c.shortDescription ?? '',
    ctaText: t('learnMore'),
    href: `/collections/${c.slug ?? c.id}`,
  }))

  return (
    <section className="content-auto-section relative bg-[var(--color-neutral-100)] py-12 max-md:py-8">
      {/* Dotted pattern background */}
      <div
        className="absolute inset-0 z-0"
        style={{
          backgroundImage: 'radial-gradient(circle, #d5d5d5 1px, transparent 1px)',
          backgroundSize: '16px 16px',
        }}
        aria-hidden="true"
      />
      <div className="relative z-10 container mx-auto px-5">
        <div className="mb-8 flex items-center justify-between gap-5 max-md:flex-wrap">
          <div className="flex flex-col gap-2">
          <h2 className="font-heading text-[40px] font-semibold leading-[48px] tracking-[-1px] text-[var(--color-secondary-blue-500)] max-md:text-[32px] max-md:leading-[38px] max-sm:text-[24px] max-sm:leading-[29px]">
            {t('title')}
          </h2>
          <p className="max-w-[586px] font-sans text-base leading-6 text-[var(--color-text-body-secondary)]">
            {t('description')}
          </p>
          </div>
          <Link
            href="/programs"
            className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-md border border-solid border-[rgba(37,99,235,0.3)] bg-white px-5 py-2.5 font-sans text-base font-semibold leading-6 text-[var(--color-secondary-blue-600)] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]"
          >
            {t('exploreAll')}
            <ChevronRight className="h-5 w-5" />
          </Link>
        </div>
        <div className=" gap-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
          {isLoading ? (
            <>
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="flex-1 h-[400px] rounded-lg" />
              ))}
            </>
          ) : isError ? (
            <div role="alert" className="col-span-full rounded-lg border border-red-200 bg-red-50 p-6 text-center text-sm text-red-800">
              Programs could not be loaded.{' '}
              <button type="button" className="font-semibold underline" onClick={() => refetch()}>
                Try again
              </button>
            </div>
          ) : programs.length > 0 ? (
            programs.map((program, i) => (
              <ProgramCard key={program.title || i} {...program} />
            ))
          ) : null}
        </div>
      </div>
    </section>
  )
}
