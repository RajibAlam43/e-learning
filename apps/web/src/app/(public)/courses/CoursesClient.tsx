'use client'

import { useState, useEffect, useCallback, type Dispatch, type SetStateAction } from 'react'
import { useSearchParams } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { SlidersHorizontal, ChevronDown, Search } from 'lucide-react'
import { useCourses } from '@/hooks/useCourses'
import { CourseCard } from '@/components/public/CourseCard'
import { CourseCardSkeleton } from '@/components/public/CourseCardSkeleton'
import { CourseFiltersSidebar } from '@/components/public/CourseFiltersSidebar'
import { Pagination } from '@/components/shared/Pagination'
import { ErrorState } from '@/components/shared/ErrorState'
import { EmptyState } from '@/components/shared/EmptyState'
import type { CourseFilters } from '@/services/course.service'

const SORT_OPTIONS = [
  { value: 'publishedAt,desc', labelKey: 'sort.newest' },
  { value: 'title,asc', labelKey: 'sort.alphabeticalAZ' },
  { value: 'title,desc', labelKey: 'sort.alphabeticalZA' },
  { value: 'priceBdt,asc', labelKey: 'sort.priceLow' },
  { value: 'priceBdt,desc', labelKey: 'sort.priceHigh' },
] as const

export default function CoursesClient() {
  const searchParams = useSearchParams()
  const initialSearch = searchParams.get('search') ?? ''
  return <CoursesCatalog key={initialSearch} initialSearch={initialSearch} />
}

function CoursesCatalog({ initialSearch }: { initialSearch: string }) {
  const t = useTranslations('courses')
  const [sortValue, setSortValue] = useState('publishedAt,desc')
  const [sortOpen, setSortOpen] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const ITEMS_PER_PAGE = 9

  const [searchInput, setSearchInput] = useState(initialSearch)
  const [debouncedSearch, setDebouncedSearch] = useState(initialSearch)

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchInput), 400)
    return () => clearTimeout(timer)
  }, [searchInput])

  const [level, setLevel] = useState('')
  const [categorySlugs, setCategorySlugs] = useState<string[]>([])
  const [duration, setDuration] = useState('')
  const [language, setLanguage] = useState('')
  const [isFree, setIsFree] = useState(false)
  const [isPaid, setIsPaid] = useState(false)
  const [priceMin, setPriceMin] = useState(0)
  const [priceMax, setPriceMax] = useState(10000)
  const [debouncedPriceMin, setDebouncedPriceMin] = useState(0)
  const [debouncedPriceMax, setDebouncedPriceMax] = useState(10000)
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false)
  const resetPage = <T,>(setter: Dispatch<SetStateAction<T>>) => (value: T) => { setter(value); setCurrentPage(0) }

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedPriceMin(priceMin)
      setDebouncedPriceMax(priceMax)
      setCurrentPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [priceMin, priceMax])

  const apiLevel = level ? (level as 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED') : undefined
  const apiLanguage = language === 'English' ? 'EN' as const : language === 'Bengali' ? 'BN' as const : undefined

  const filters: CourseFilters = {
    page: currentPage,
    size: ITEMS_PER_PAGE,
    sort: [sortValue],
    level: apiLevel,
    language: apiLanguage,
    categorySlugs: categorySlugs.length > 0 ? categorySlugs : undefined,
    search: debouncedSearch || undefined,
    duration: duration ? duration as CourseFilters['duration'] : undefined,
    isFree,
    isPaid,
    priceMin: debouncedPriceMin,
    priceMax: debouncedPriceMax,
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

  const sortLabel =
    SORT_OPTIONS.find((s) => s.value === sortValue)?.labelKey
      ? t(SORT_OPTIONS.find((s) => s.value === sortValue)!.labelKey)
      : t('sort.newest')

  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [])

  return (
    <main className="w-full relative min-h-screen bg-[#f5f5f5]">
      <div
        className="w-full h-[200px] bg-cover bg-center bg-no-repeat sm:h-[240px] md:h-[282px]"
        style={{ backgroundImage: "url('/images/Header-Background-Image-Container.png')" }}
      >
        <div className="mx-auto max-w-[1440px] flex flex-col gap-2 px-4 pt-14 sm:gap-3 sm:px-5 sm:pt-16 md:pt-20 lg:px-10">
          <h1 className="text-[28px] font-medium leading-[34px] tracking-[-1px] text-white font-heading sm:text-[36px] sm:leading-[42px] md:text-[48px] md:leading-[56px]">
            {t('title')}
          </h1>
          <p className="text-sm leading-5 text-white/80 font-sans sm:text-base sm:leading-6">
            {t('subtitle')}
          </p>
        </div>
      </div>

      <div className="w-full -mt-6 pb-12 sm:-mt-8 md:-mt-10 md:pb-20">
        <div className="mx-auto max-w-[1440px] flex flex-row gap-3 px-4 sm:px-5 lg:px-10">
          <CourseFiltersSidebar
            level={level}
            onLevelChange={resetPage(setLevel)}
            category={categorySlugs}
            onCategoryChange={resetPage(setCategorySlugs)}
            duration={duration}
            onDurationChange={resetPage(setDuration)}
            language={language}
            onLanguageChange={resetPage(setLanguage)}
            isFree={isFree}
            onIsFreeChange={resetPage(setIsFree)}
            isPaid={isPaid}
            onIsPaidChange={resetPage(setIsPaid)}
            priceMin={priceMin}
            onPriceMinChange={resetPage(setPriceMin)}
            priceMax={priceMax}
            onPriceMaxChange={resetPage(setPriceMax)}
            mobileOpen={mobileFiltersOpen}
            onMobileClose={() => setMobileFiltersOpen(false)}
          />

          <section className="flex min-w-0 flex-1 flex-col gap-3 md:gap-4">
            <header className="flex w-full items-center justify-between rounded-lg bg-white py-2 pl-4 pr-2 shadow-[0px_2px_6px_rgba(16,24,40,0.06)] sticky top-0 z-10 sm:pl-6">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setMobileFiltersOpen(true)}
                  aria-expanded={mobileFiltersOpen}
                  aria-controls="mobile-course-filters"
                  className="flex h-9 items-center justify-center gap-1.5 rounded-lg border border-[#e5e5e5] bg-[#f5f5f5] px-3 font-sans text-sm font-medium text-[#1b1c1a] transition-colors hover:bg-[#e8ebf1] cursor-pointer lg:hidden"
                >
                  <SlidersHorizontal className="h-4 w-4" />
                  <span>{t('filters.title')}</span>
                </button>
                <span className="hidden text-base font-medium leading-6 text-[#71717a] sm:inline">
                  {t('showing', { count: totalElements })}
                </span>
              </div>
              <div className="flex items-center gap-2 sm:gap-3">
                <div className="relative hidden sm:flex items-center">
                  <Search className="absolute left-3 h-4 w-4 text-[#71717a]" />
                  <input
                    type="text"
                    value={searchInput}
                    onChange={(e) => { setSearchInput(e.target.value); setCurrentPage(0) }}
                    aria-label={t('filters.title')}
                    placeholder={t('filters.title') + '...'}
                    className="h-9 w-[180px] rounded-lg border border-[#e5e5e5] bg-[#f5f5f5] pl-9 pr-3 text-sm text-[#1b1c1a] outline-none focus:border-[#1c3873] transition-colors"
                  />
                </div>
                <div className="relative">
                  <button
                    type="button"
                    onClick={(e) => { e.stopPropagation(); setSortOpen(!sortOpen) }}
                    aria-expanded={sortOpen}
                    aria-haspopup="menu"
                    className="flex items-center gap-2 rounded-lg border border-[#e5e5e5] bg-[#f5f5f5] px-3 py-2 font-sans text-sm text-[#1b1c1a] transition-colors hover:bg-[#e8ebf1] cursor-pointer sm:text-base sm:px-[11px]"
                  >
                    {sortLabel}
                    <ChevronDown className={`h-4 w-4 text-[#5e5f5c] transition-transform duration-200 ${sortOpen ? 'rotate-180' : ''}`} />
                  </button>
                  {sortOpen && (
                    <div role="menu" onClick={(e) => e.stopPropagation()} className="absolute right-0 top-full z-50 mt-1 min-w-[180px] rounded-lg border border-[#e5e5e5] bg-white py-1 shadow-lg">
                      {SORT_OPTIONS.map((opt) => (
                        <button
                          key={opt.value}
                          type="button"
                          role="menuitemradio"
                          aria-checked={sortValue === opt.value}
                          onClick={() => { setSortValue(opt.value); setSortOpen(false); setCurrentPage(0) }}
                          className={`block w-full px-4 py-2 text-left font-sans text-sm transition-colors cursor-pointer ${sortValue === opt.value ? 'bg-[#1c3873] text-white' : 'text-[#1b1c1a] hover:bg-[#f5f5f5]'}`}
                        >
                          {t(opt.labelKey)}
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
                  {isLoading
                    ? Array.from({ length: ITEMS_PER_PAGE }).map((_, i) => <CourseCardSkeleton key={i} />)
                    : courses.map((course) => <CourseCard key={course.id} course={course} />)}
                </div>

                {!isLoading && courses.length === 0 && (
                  <EmptyState title={t('empty')} />
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
      </div>
    </main>
  )
}
