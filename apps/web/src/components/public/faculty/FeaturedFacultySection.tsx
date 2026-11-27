'use client'
import Link from 'next/link'
import { useCallback, useEffect, useRef, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { useFeaturedFaculty } from '@/services/faculty.service'
import { FacultyPersonCard } from './FacultyPersonCard'

export function FeaturedFacultySection() {
  const t = useTranslations('faculty'), query = useFeaturedFaculty()
  const people = query.data ?? []
  const viewport = useRef<HTMLDivElement>(null)
  const [scroll, setScroll] = useState({ left: false, right: false, overflow: false })
  const updateScroll = useCallback(() => {
    const el = viewport.current
    if (!el) return
    const max = el.scrollWidth - el.clientWidth
    const next = { left: el.scrollLeft > 2, right: el.scrollLeft < max - 2, overflow: max > 2 }
    setScroll(old => old.left === next.left && old.right === next.right && old.overflow === next.overflow ? old : next)
  }, [])
  useEffect(() => {
    const el = viewport.current
    if (!el) return
    const observer = new ResizeObserver(updateScroll)
    observer.observe(el)
    el.addEventListener('scroll', updateScroll, { passive: true })
    return () => { observer.disconnect(); el.removeEventListener('scroll', updateScroll) }
  }, [query.data?.length, query.isError, updateScroll])
  function move(direction: -1 | 1) {
    const el = viewport.current, card = el?.firstElementChild
    if (!el || !card) return
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    el.scrollBy({ left: direction * (card.getBoundingClientRect().width + 20), behavior: reducedMotion ? 'instant' : 'smooth' })
  }
  if (!people.length || query.isError) return null
  const arrow = 'grid h-10 w-10 shrink-0 place-items-center rounded-full border border-[#b8c8e8] bg-white text-[#1c3873] transition-colors hover:bg-[#e9f0ff] focus-visible:outline-2 focus-visible:outline-offset-4 disabled:cursor-default disabled:opacity-30'
  return <section aria-labelledby="featured-faculty-heading" aria-roledescription="carousel" className="w-full bg-[#f5f9ff] py-12 max-md:py-8"><div className="container mx-auto px-4 sm:px-5">
    <h2 id="featured-faculty-heading" className="mb-8 text-center font-heading text-3xl font-semibold text-[#1c3873] md:text-[40px]">{t('featuredTitle')}</h2>
    <div className="mx-auto flex max-w-6xl items-center gap-2 sm:gap-4">
      <button type="button" aria-label={t('previousProfiles')} aria-controls="featured-faculty-profiles" disabled={!scroll.left} onClick={() => move(-1)} className={arrow}><ChevronLeft className="h-5 w-5" /></button>
      <div id="featured-faculty-profiles" ref={viewport} role="group" aria-label={t('featuredTitle')} tabIndex={0} className={`flex min-w-0 flex-1 snap-x snap-mandatory gap-5 overflow-x-auto rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden ${scroll.overflow ? '' : 'justify-center'}`}>
        {people.map((p, index) => <div key={p.id} role="group" aria-roledescription="slide" aria-label={`${index + 1} / ${people.length}`} className="flex min-w-0 shrink-0 basis-full snap-start [&>article]:w-full sm:basis-[calc((100%_-_1.25rem)/2)] xl:basis-[calc((100%_-_2.5rem)/3)]"><FacultyPersonCard person={p} /></div>)}
      </div>
      <button type="button" aria-label={t('nextProfiles')} aria-controls="featured-faculty-profiles" disabled={!scroll.right} onClick={() => move(1)} className={arrow}><ChevronRight className="h-5 w-5" /></button>
    </div>
    <div className="mt-6 flex justify-center"><Link href="/faculty" className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-solid border-[rgba(37,99,235,0.3)] bg-white px-5 py-2.5 font-sans text-base font-semibold leading-6 text-[var(--color-secondary-blue-600)] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]">{t('viewAll')}<ChevronRight aria-hidden="true" className="h-5 w-5" /></Link></div>
  </div></section>
}
