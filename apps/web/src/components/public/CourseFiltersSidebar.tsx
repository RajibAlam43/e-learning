'use client'

import { useState, useMemo, useRef, useEffect, type KeyboardEvent } from 'react'
import { X } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { usePublicCategories } from '@/hooks/useCourses'
import { FilterSection, RadioOption, CheckboxOption } from './CourseFilterOptions'

const MIN_VALUE = 0
const MAX_VALUE = 10000
const STEP = 100

function PriceRangeSlider({ min, max, onMinChange, onMaxChange }: {
  min: number; max: number; onMinChange: (v: number) => void; onMaxChange: (v: number) => void
}) {
  const t = useTranslations('courses')
  const trackRef = useRef<HTMLDivElement>(null)
  const [localMin, setLocalMin] = useState(min)
  const [localMax, setLocalMax] = useState(max)
  const dragging = useRef<'min' | 'max' | null>(null)
  const minRef = useRef(localMin)
  const maxRef = useRef(localMax)
  const callbacksRef = useRef({ onMinChange, onMaxChange })

  useEffect(() => {
    minRef.current = localMin
    maxRef.current = localMax
    callbacksRef.current = { onMinChange, onMaxChange }
  }, [localMin, localMax, onMinChange, onMaxChange])

  useEffect(() => {
    if (!dragging.current) setLocalMin(min)
  }, [min])

  useEffect(() => {
    if (!dragging.current) setLocalMax(max)
  }, [max])

  useEffect(() => {
    const onMove = (e: PointerEvent) => {
      if (!dragging.current || !trackRef.current) return
      const rect = trackRef.current.getBoundingClientRect()
      const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
      const raw = Math.round((ratio * (MAX_VALUE - MIN_VALUE) + MIN_VALUE) / STEP) * STEP
      const clamped = Math.max(MIN_VALUE, Math.min(MAX_VALUE, raw))
      if (dragging.current === 'min') {
        const v = Math.min(clamped, maxRef.current)
        setLocalMin(v)
        callbacksRef.current.onMinChange(v)
      } else {
        const v = Math.max(clamped, minRef.current)
        setLocalMax(v)
        callbacksRef.current.onMaxChange(v)
      }
    }
    const onUp = () => { dragging.current = null }
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    return () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
    }
  }, [])

  const leftPercent = useMemo(() => ((localMin - MIN_VALUE) / (MAX_VALUE - MIN_VALUE)) * 100, [localMin])
  const rightPercent = useMemo(() => ((localMax - MIN_VALUE) / (MAX_VALUE - MIN_VALUE)) * 100, [localMax])
  const rangeWidth = Math.max(rightPercent - leftPercent, 0)
  const updateMin = (value: number) => {
    const nextValue = Math.max(MIN_VALUE, Math.min(value, localMax))
    setLocalMin(nextValue)
    onMinChange(nextValue)
  }
  const updateMax = (value: number) => {
    const nextValue = Math.min(MAX_VALUE, Math.max(value, localMin))
    setLocalMax(nextValue)
    onMaxChange(nextValue)
  }
  const handleSliderKeyDown = (handle: 'min' | 'max') => (event: KeyboardEvent<HTMLDivElement>) => {
    const current = handle === 'min' ? localMin : localMax
    let nextValue: number | undefined
    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') nextValue = current - STEP
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') nextValue = current + STEP
    if (event.key === 'PageDown') nextValue = current - STEP * 10
    if (event.key === 'PageUp') nextValue = current + STEP * 10
    if (event.key === 'Home') nextValue = MIN_VALUE
    if (event.key === 'End') nextValue = MAX_VALUE
    if (nextValue === undefined) return
    event.preventDefault()
    if (handle === 'min') updateMin(nextValue)
    else updateMax(nextValue)
  }

  return (
    <div className="flex self-stretch flex-col gap-2">
      <span className="text-sm leading-[21px] text-[#a3a3a3]">{t('filters.priceRange')}</span>
      <div className="relative h-[75px] w-full select-none">
        {/* Track background */}
        <div ref={trackRef} className="pointer-events-none absolute left-0 top-[61px] h-2 w-full rounded-[1000px] bg-[#e8ebf1]" />
        {/* Active range */}
        <div className="pointer-events-none absolute top-[61px] h-2 rounded-[1000px] bg-[#1c3873]" style={{ left: `${leftPercent}%`, width: `${rangeWidth}%` }} />
        {/* Left value label */}
        <div className="pointer-events-none absolute top-0" style={{ left: `${leftPercent}%`, transform: 'translateX(-50%)' }}>
          <div className="rounded bg-white px-2 py-1 shadow-[0px_6px_15px_-2px_rgba(16,24,40,0.08)]">
            <span className="whitespace-nowrap font-sans text-sm font-medium leading-[150%] text-[#5e5f5c]">{localMin.toLocaleString()}</span>
          </div>
          <div className="flex justify-center">
            <svg width="14" height="6" viewBox="0 0 14 6" fill="none"><path d="M7 6L0 0H14L7 6Z" fill="white"/></svg>
          </div>
        </div>
        {/* Right value label */}
        <div className="pointer-events-none absolute top-0" style={{ left: `${rightPercent}%`, transform: 'translateX(-50%)' }}>
          <div className="rounded bg-white px-2 py-1 shadow-[0px_6px_15px_-2px_rgba(16,24,40,0.08)]">
            <span className="whitespace-nowrap font-sans text-sm font-medium leading-[150%] text-[#5e5f5c]">{localMax.toLocaleString()}{localMax === MAX_VALUE ? '+' : ''}</span>
          </div>
          <div className="flex justify-center">
            <svg width="14" height="6" viewBox="0 0 14 6" fill="none"><path d="M7 6L0 0H14L7 6Z" fill="white"/></svg>
          </div>
        </div>
        {/* Left thumb */}
        <div
          role="slider"
          tabIndex={0}
          aria-label={`${t('filters.priceRange')} minimum`}
          aria-valuemin={MIN_VALUE}
          aria-valuemax={localMax}
          aria-valuenow={localMin}
          aria-valuetext={localMin.toLocaleString()}
          className="absolute top-[55px] z-10 h-5 w-5 cursor-grab rounded-[10px] border-4 border-solid border-white bg-[#1c3873] shadow-[0px_4px_4px_-2px_rgba(24,39,75,0.08),0px_2px_4px_-2px_rgba(24,39,75,0.12)] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1c3873] active:cursor-grabbing"
          style={{ left: `calc(${leftPercent}% - 0px)` }}
          onPointerDown={() => { dragging.current = 'min' }}
          onKeyDown={handleSliderKeyDown('min')}
        />
        {/* Right thumb */}
        <div
          role="slider"
          tabIndex={0}
          aria-label={`${t('filters.priceRange')} maximum`}
          aria-valuemin={localMin}
          aria-valuemax={MAX_VALUE}
          aria-valuenow={localMax}
          aria-valuetext={localMax === MAX_VALUE ? 'No upper price limit' : localMax.toLocaleString()}
          className="absolute top-[55px] z-10 h-5 w-5 cursor-grab rounded-[10px] border-4 border-solid border-white bg-[#1c3873] shadow-[0px_4px_4px_-2px_rgba(24,39,75,0.08),0px_2px_4px_-2px_rgba(24,39,75,0.12)] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1c3873] active:cursor-grabbing"
          style={{ left: `calc(${rightPercent}% - 20px)` }}
          onPointerDown={() => { dragging.current = 'max' }}
          onKeyDown={handleSliderKeyDown('max')}
        />
      </div>
    </div>
  )
}

const DURATIONS = ['Under 2 weeks', '2-4 weeks', '4-8 weeks'] as const
const LANGUAGES_LIST = ['English', 'Bengali', 'Arabic'] as const

interface CourseFiltersSidebarProps {
  level: string
  onLevelChange: (value: string) => void
  category: string[]
  onCategoryChange: (value: string[]) => void
  duration: string
  onDurationChange: (value: string) => void
  language: string
  onLanguageChange: (value: string) => void
  isFree: boolean
  onIsFreeChange: (value: boolean) => void
  isPaid: boolean
  onIsPaidChange: (value: boolean) => void
  priceMin: number
  onPriceMinChange: (value: number) => void
  priceMax: number
  onPriceMaxChange: (value: number) => void
  mobileOpen: boolean
  onMobileClose: () => void
}

function FilterContent({
  level, onLevelChange, category, onCategoryChange,
  duration, onDurationChange, language, onLanguageChange,
  isFree, onIsFreeChange, isPaid, onIsPaidChange,
  priceMin, onPriceMinChange, priceMax, onPriceMaxChange,
}: Omit<CourseFiltersSidebarProps, 'mobileOpen' | 'onMobileClose'>) {
  const t = useTranslations('courses')
  const categories = usePublicCategories()

  return (
    <>
      <FilterSection key="level" title={t('filters.level')} iconSrc="/images/level.svg">
        <RadioOption label={t('filters.beginner')} checked={level === 'BEGINNER'} onChange={() => onLevelChange(level === 'BEGINNER' ? '' : 'BEGINNER')} />
        <RadioOption label={t('filters.intermediate')} checked={level === 'INTERMEDIATE'} onChange={() => onLevelChange(level === 'INTERMEDIATE' ? '' : 'INTERMEDIATE')} />
        <RadioOption label={t('filters.advanced')} checked={level === 'ADVANCED'} onChange={() => onLevelChange(level === 'ADVANCED' ? '' : 'ADVANCED')} />
      </FilterSection>

      <FilterSection key="category" title={t('filters.category')} iconSrc="/images/category.svg">
        {categories.isLoading && <span className="text-xs text-[#71717a]">Loading categories...</span>}
        {categories.isError && <button type="button" className="text-left text-xs font-medium text-red-700 underline" onClick={() => categories.refetch()}>Retry categories</button>}
        {(categories.data ?? []).map((cat) => (
          <CheckboxOption
            key={cat.id ?? cat.slug}
            label={cat.name ?? cat.slug ?? 'Category'}
            checked={Boolean(cat.slug && category.includes(cat.slug))}
            onChange={() => {
              const slug = cat.slug
              if (!slug) return
              onCategoryChange(
                category.includes(slug)
                  ? category.filter((item) => item !== slug)
                  : [...category, slug],
              )
            }}
          />
        ))}
        {!categories.isLoading && !categories.isError && (categories.data ?? []).length === 0 && <span className="text-xs text-[#71717a]">No categories available.</span>}
      </FilterSection>

      <FilterSection key="duration" title={t('filters.duration')} iconSrc="/images/duration.svg" defaultOpen={false}>
        {DURATIONS.map((dur) => (
          <RadioOption key={dur} label={dur} checked={duration === dur} onChange={() => onDurationChange(duration === dur ? '' : dur)} />
        ))}
      </FilterSection>

      <FilterSection key="language" title={t('filters.language')} iconSrc="/images/language.svg" defaultOpen={false}>
        {LANGUAGES_LIST.map((lang) => (
          <RadioOption key={lang} label={lang} checked={language === lang} onChange={() => onLanguageChange(language === lang ? '' : lang)} />
        ))}
      </FilterSection>

      <FilterSection key="price" title={t('filters.price')} iconSrc="/images/money.svg" defaultOpen={false}>
        <div className="flex flex-col gap-[16px]">
          <fieldset className="inline-flex flex-col items-start gap-2 border-0 p-0 m-0">
            <legend className="sr-only">Price type</legend>
            <CheckboxOption label={t('filters.free')} checked={isFree} onChange={() => onIsFreeChange(!isFree)} />
            <CheckboxOption label={t('filters.paid')} checked={isPaid} onChange={() => onIsPaidChange(!isPaid)} />
          </fieldset>
          <PriceRangeSlider min={priceMin} max={priceMax} onMinChange={onPriceMinChange} onMaxChange={onPriceMaxChange} />
        </div>
      </FilterSection>
    </>
  )
}

export function CourseFiltersSidebar({
  level, onLevelChange, category, onCategoryChange,
  duration, onDurationChange, language, onLanguageChange,
  isFree, onIsFreeChange, isPaid, onIsPaidChange,
  priceMin, onPriceMinChange, priceMax, onPriceMaxChange,
  mobileOpen, onMobileClose,
}: CourseFiltersSidebarProps) {
  const t = useTranslations('courses')
  const hasActiveFilters = !!(
    level || category.length > 0 || duration || language || isFree || isPaid ||
    priceMin !== MIN_VALUE || priceMax !== MAX_VALUE
  )

  const clearAll = () => {
    onLevelChange('')
    onCategoryChange([])
    onDurationChange('')
    onLanguageChange('')
    onIsFreeChange(false)
    onIsPaidChange(false)
    onPriceMinChange(MIN_VALUE)
    onPriceMaxChange(MAX_VALUE)
  }

  const filterProps = {
    level, onLevelChange, category, onCategoryChange,
    duration, onDurationChange, language, onLanguageChange,
    isFree, onIsFreeChange, isPaid, onIsPaidChange,
    priceMin, onPriceMinChange, priceMax, onPriceMaxChange,
  }

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden w-[220px] shrink-0 flex-col gap-[12px] lg:flex">
        <div className="flex flex-col gap-[12px]">
          <FilterContent {...filterProps} />
        </div>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={clearAll}
            className="flex w-full items-center justify-center gap-2 rounded-lg border border-[#e5e5e5] px-4 py-2.5 font-sans text-sm font-medium text-[#5e5f5c] transition-colors hover:bg-[#f5f5f5] cursor-pointer"
          >
            <X className="h-4 w-4" />
            {t('filters.clear')}
          </button>
        )}
      </aside>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-50 bg-black/40 transition-opacity duration-300 lg:hidden"
          onClick={onMobileClose}
        />
      )}

      {/* Mobile drawer */}
      {mobileOpen && (
        <div
          id="mobile-course-filters"
          role="dialog"
          aria-modal="true"
          aria-label={t('filters.title')}
          className="fixed inset-y-0 left-0 z-50 w-[280px] flex-col gap-4 overflow-y-auto bg-white p-5 shadow-xl lg:hidden animate-slide-in-left"
        >
          <div className="flex items-center justify-between">
            <span className="font-sans text-base font-semibold text-[#1b1c1a]">{t('filters.title')}</span>
            <button
              type="button"
              onClick={onMobileClose}
              aria-label={`Close ${t('filters.title')}`}
              className="cursor-pointer transition-colors hover:text-[#1b1c1a] text-[#5e5f5c]"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
          <div className="flex flex-col gap-4">
            <FilterContent {...filterProps} />
          </div>
          {hasActiveFilters && (
            <button
              type="button"
              onClick={() => { clearAll(); onMobileClose() }}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-[#e5e5e5] px-4 py-2.5 font-sans text-sm font-medium text-[#5e5f5c] transition-colors hover:bg-[#f5f5f5] cursor-pointer"
            >
              <X className="h-4 w-4" />
              {t('filters.clear')}
            </button>
          )}
        </div>
      )}
    </>
  )
}
