'use client'

import { useState, useEffect, useCallback } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { ChevronDown, ChevronRight } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { useCourses } from '@/hooks/useCourses'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorState } from '@/components/shared/ErrorState'
import type { CourseFilters } from '@/services/course.service'
import type { components } from '@/types/api'

type CourseSummary = components['schemas']['CourseSummaryResponse']

const SORT_OPTIONS = [
  { value: 'publishedAt,desc', labelKey: 'newest' },
  { value: 'title,asc', labelKey: 'alphabeticalAZ' },
  { value: 'title,desc', labelKey: 'alphabeticalZA' },
  { value: 'priceBdt,asc', labelKey: 'priceLow' },
  { value: 'priceBdt,desc', labelKey: 'priceHigh' },
] as const

interface FilterGroupProps {
  iconSrc: string
  title: string
  options: readonly string[]
  value: string
  onChange: (val: string) => void
}

function FilterGroup({ iconSrc, title, options, value, onChange }: FilterGroupProps) {
  const [open, setOpen] = useState(true)

  return (
    <div className="self-stretch rounded-lg bg-white flex flex-col items-start p-4 gap-4 transition-all duration-200 hover:shadow-[0px_2px_6px_rgba(16,24,40,0.04)]">
      <div className="self-stretch flex items-center justify-between gap-5">
        <div className="flex items-center gap-2">
          <Image src={iconSrc} alt="" width={24} height={24} className="h-6 w-6" />
          <span className="leading-6 font-semibold text-[#262626] font-sans">{title}</span>
        </div>
        <button
          type="button"
          onClick={() => setOpen(!open)}
          className="cursor-pointer border-none p-0 bg-transparent h-6 w-6 flex items-center justify-center transition-transform duration-200"
        >
          <ChevronDown className={`h-6 w-6 text-[#71717a] transition-transform duration-200 ${open ? '' : '-rotate-90'}`} />
        </button>
      </div>
      <div className="self-stretch h-px bg-[#e8f0ff]" />
      {open && (
        <div className="self-stretch flex flex-col items-start gap-3">
          {options.map((opt) => (
            <button
              key={opt}
              type="button"
              onClick={() => onChange(value === opt ? '' : opt)}
              className="flex items-center gap-3 cursor-pointer border-none bg-transparent p-0 group"
            >
              <input type="radio" name={title} checked={value === opt} readOnly className="m-0 h-4 w-4 cursor-pointer accent-[#1c3873]" />
              <span className="text-sm leading-[150%] text-[#667085] font-sans transition-colors group-hover:text-[#1c3873]">{opt}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function ProgramCard({ course }: { course: CourseSummary }) {
  const t = useTranslations('courses')
  const badgeText = course.categoryNames?.[0] ?? 'Program'
  const isPopular = course.level === 'BEGINNER'
  const badgeStyle = isPopular ? 'bg-[#d97706] text-[#fffbeb]' : 'bg-[#f5f3ff] text-[#8b5cf6]'
  const badgeLabel = isPopular ? 'POPULAR' : badgeText

  return (
    <article className="group flex flex-1 flex-col items-start bg-white overflow-hidden min-w-[232px] max-w-[402px] transition-all duration-300 hover:shadow-[0px_8px_24px_rgba(16,24,40,0.12)] hover:-translate-y-1">
      <div className="relative h-[249px] w-full shrink-0 overflow-hidden">
        {course.thumbnailUrl ? (
          <Image src={course.thumbnailUrl} alt={course.title ?? 'Course'} fill className="object-cover transition-transform duration-500 group-hover:scale-105" sizes="(max-width: 700px) 100vw, (max-width: 900px) 50vw, 33vw" />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-[#e5e5e5]" />
        )}
        <span className={`absolute top-4 left-4 z-[1] rounded-[4px] flex items-center justify-center py-1 px-2 text-xs leading-5 font-medium font-sans transition-transform duration-300 group-hover:scale-105 ${badgeStyle}`}>
          {badgeLabel}
        </span>
      </div>
      <div className="flex w-full flex-col items-start p-6 gap-4">
        <div className="self-stretch flex flex-col items-start gap-1">
          <h3 className="m-0 self-stretch text-xl leading-5 font-normal text-[#1c3873] font-heading">{course.title}</h3>
          {course.shortDescription && (
            <p className="self-stretch text-sm leading-[150%] text-[#5e5f5c] font-sans overflow-hidden text-ellipsis line-clamp-2">{course.shortDescription}</p>
          )}
        </div>
        <Link
          href={`/courses/${course.slug}`}
          className="self-stretch h-11 shrink-0 flex items-center justify-center gap-2 rounded-md border border-solid border-[rgba(59,130,246,0.3)] bg-white text-base leading-6 font-semibold text-[#3b82f6] no-underline font-sans cursor-pointer transition-all duration-200 hover:bg-[#eff6ff] hover:shadow-[0px_4px_12px_rgba(59,130,246,0.15)]"
        >
          {t('learnMore')}
          <ChevronRight className="h-6 w-6 transition-transform duration-200 group-hover:translate-x-0.5" />
        </Link>
      </div>
    </article>
  )
}

function ProgramCardSkeleton() {
  return (
    <div className="flex flex-1 flex-col items-start bg-white overflow-hidden min-w-[232px] max-w-[402px]">
      <div className="skeleton-shimmer h-[249px] w-full" />
      <div className="flex flex-col gap-4 p-6 w-full">
        <div className="skeleton-shimmer h-5 w-[220px] rounded" />
        <div className="skeleton-shimmer h-4 w-full rounded" />
        <div className="skeleton-shimmer h-4 w-[160px] rounded" />
        <div className="skeleton-shimmer h-11 w-full rounded-md" />
      </div>
    </div>
  )
}

export default function ProgramsClient() {
  const t = useTranslations('courses')
  const [sortValue, setSortValue] = useState('publishedAt,desc')
  const [sortOpen, setSortOpen] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const ITEMS_PER_PAGE = 9

  const [typeFilter, setTypeFilter] = useState('')
  const [levelFilter, setLevelFilter] = useState('')
  const [durationFilter, setDurationFilter] = useState('')
  const [studyModeFilter, setStudyModeFilter] = useState('')

  const filters: CourseFilters = {
    page: currentPage,
    size: ITEMS_PER_PAGE,
    sort: [sortValue],
    level: levelFilter ? (levelFilter as 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED') : undefined,
  }

  const { data, isLoading, isError, error, refetch } = useCourses(filters)
  const courses = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0

  useEffect(() => {
    if (!sortOpen) return
    const handler = () => setSortOpen(false)
    document.addEventListener('click', handler)
    return () => document.removeEventListener('click', handler)
  }, [sortOpen])

  // Filter changes reset pagination so users never land on an empty later page.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { setCurrentPage(0) }, [levelFilter, typeFilter, durationFilter, studyModeFilter, sortValue])

  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])

  const hasActiveFilters = typeFilter || levelFilter || durationFilter || studyModeFilter
  const clearFilters = useCallback(() => { setTypeFilter(''); setLevelFilter(''); setDurationFilter(''); setStudyModeFilter('') }, [])

  const sortLabel = SORT_OPTIONS.find((s) => s.value === sortValue)
    ? t(`sort.${SORT_OPTIONS.find((s) => s.value === sortValue)!.labelKey}`)
    : t('sort.newest')

  return (
    <div className="w-full relative min-h-screen bg-[#f5f5f5]">
      <div className="w-full h-[200px] bg-cover bg-center bg-no-repeat sm:h-[240px] md:h-[282px]" style={{ backgroundImage: "url('/images/programs-header-bg.png')" }}>
        <div className="mx-auto max-w-[1440px] flex flex-col gap-2 px-4 pt-14 sm:gap-3 sm:px-5 sm:pt-16 md:pt-20 lg:px-10">
          <h1 className="text-[28px] font-medium leading-[34px] tracking-[-1px] text-white font-heading sm:text-[36px] sm:leading-[42px] md:text-[48px] md:leading-[56px]">{t('programs.title')}</h1>
          <p className="text-sm leading-5 text-white/80 font-sans sm:text-base sm:leading-6">{t('programs.subtitle')}</p>
        </div>
      </div>

      <main className="w-full -mt-6 pb-12 sm:-mt-8 md:-mt-10 md:pb-20">
        <div className="mx-auto max-w-[1440px] flex flex-row gap-3 px-4 sm:px-5 lg:px-10">
          <aside className="w-[220px] shrink-0 flex flex-col items-start gap-3 text-base text-[#262626] font-sans max-[750px]:w-full max-[750px]:flex-row max-[750px]:flex-wrap max-[750px]:gap-3">
            <FilterGroup iconSrc="/images/certificate.png" title={t('programs.filters.type')} options={['Diploma', 'Certification', 'Advanced'] as const} value={typeFilter} onChange={setTypeFilter} />
            <FilterGroup iconSrc="/images/level-program.png" title={t('filters.level')} options={['BEGINNER', 'INTERMEDIATE', 'ADVANCED'] as const} value={levelFilter} onChange={setLevelFilter} />
            <FilterGroup iconSrc="/images/duration-program.png" title={t('filters.duration')} options={['3 months', '6 months', '1 years'] as const} value={durationFilter} onChange={setDurationFilter} />
            <FilterGroup iconSrc="/images/calendar-program.png" title={t('programs.filters.studyMode')} options={['Fixed Schedule', 'Self-paced'] as const} value={studyModeFilter} onChange={setStudyModeFilter} />
            {hasActiveFilters && (
              <button type="button" onClick={clearFilters} className="self-stretch h-11 flex items-center justify-center rounded-lg border border-solid border-[#e5e5e5] bg-white text-sm leading-5 font-medium text-[#667085] cursor-pointer transition-all duration-200 hover:bg-[#f5f5f5] hover:text-[#1c3873] font-sans">{t('filters.clear')}</button>
            )}
          </aside>

          <section className="flex min-w-0 flex-1 flex-col gap-3 md:gap-4">
            <header className="self-stretch shadow-[0px_2px_6px_rgba(16,24,40,0.06)] rounded-lg bg-white flex items-center justify-between py-2 pl-6 pr-2 gap-5 sticky top-0 z-10 max-[700px]:flex-col max-[700px]:items-start max-[700px]:gap-2 max-[700px]:p-4">
              <span className="text-base leading-6 font-medium text-[#71717a] font-sans">{t('showing', { count: totalElements })}</span>
              <div className="flex items-center gap-3 text-sm font-sans">
                <span className="leading-5 text-[#71717a]">{t('sort.label')}</span>
                <div className="relative">
                  <button type="button" onClick={(e) => { e.stopPropagation(); setSortOpen(!sortOpen) }} className="flex items-center gap-2 rounded-lg bg-[#f5f5f5] border border-[#e5e5e5] px-[11px] py-2 text-base text-[#1b1c1a] cursor-pointer transition-colors hover:bg-[#e8ebf1]">
                    {sortLabel}
                    <ChevronDown className={`h-4 w-4 text-[#5e5f5c] transition-transform duration-200 ${sortOpen ? 'rotate-180' : ''}`} />
                  </button>
                  {sortOpen && (
                    <div onClick={(e) => e.stopPropagation()} className="absolute right-0 top-full z-50 mt-1 min-w-[200px] rounded-lg border border-[#e5e5e5] bg-white py-1 shadow-lg">
                      {SORT_OPTIONS.map((opt) => (
                        <button key={opt.value} type="button" onClick={() => { setSortValue(opt.value); setSortOpen(false) }} className={`block w-full px-4 py-2 text-left font-sans text-sm transition-colors cursor-pointer ${sortValue === opt.value ? 'bg-[#1c3873] text-white' : 'text-[#1b1c1a] hover:bg-[#f5f5f5]'}`}>
                          {t(`sort.${opt.labelKey}`)}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </header>

            {isError && !data ? (
              <ErrorState message={error?.message ?? t('errors.loadFailed')} onRetry={() => refetch()} />
            ) : (
              <>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:gap-[12px] xl:grid-cols-3">
                  {isLoading ? Array.from({ length: 6 }).map((_, i) => <ProgramCardSkeleton key={i} />) : courses.map((course) => <ProgramCard key={course.id} course={course} />)}
                </div>
                {!isLoading && courses.length === 0 && (
                  <div className="flex flex-col items-center justify-center gap-4 py-20">
                    <p className="font-sans text-lg text-[#71717a]">{t('empty')}</p>
                  </div>
                )}
                {!isLoading && totalPages > 1 && (
                  <div className="flex justify-center pt-6 md:pt-8">
                    <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={handlePageChange} />
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}
