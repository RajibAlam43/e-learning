'use client'

import { useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { Search, SlidersHorizontal } from 'lucide-react'
import type { CourseFilters as Filters } from '@/services/course.service'

const levels = [
  { value: '', label: 'All Levels' },
  { value: 'BEGINNER', label: 'Beginner' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
] as const

const languages = [
  { value: '', label: 'All Languages' },
  { value: 'BN', label: 'Bangla' },
  { value: 'EN', label: 'English' },
] as const

export function CourseFilters() {
  const router = useRouter()
  const searchParams = useSearchParams()

  const [search, setSearch] = useState(searchParams.get('search') ?? '')
  const [level, setLevel] = useState(searchParams.get('level') ?? '')
  const [language, setLanguage] = useState(searchParams.get('language') ?? '')

  const updateFilters = (updates: Partial<Filters>) => {
    const params = new URLSearchParams(searchParams.toString())

    if (updates.level) {
      params.set('level', updates.level)
    } else {
      params.delete('level')
    }

    if (updates.language) {
      params.set('language', updates.language)
    } else {
      params.delete('language')
    }

    if (updates.search !== undefined) {
      if (updates.search.trim()) params.set('search', updates.search.trim())
      else params.delete('search')
    }

    router.push(`/courses?${params.toString()}`)
  }

  return (
    <div className="flex flex-col gap-4">
      {/* Search bar */}
      <form
        role="search"
        onSubmit={(event) => {
          event.preventDefault()
          updateFilters({ search })
        }}
        className="relative"
      >
        <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[var(--color-neutral-400)]" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search courses..."
          className="h-12 w-full rounded-lg border border-[var(--color-neutral-200)] bg-white pl-12 pr-4 font-sans text-base text-[var(--color-text-headline-primary)] placeholder:text-[var(--color-neutral-400)] focus:border-[var(--color-secondary-blue-500)] focus:outline-none"
        />
      </form>

      {/* Filter row */}
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2 text-[var(--color-text-body-secondary)]">
          <SlidersHorizontal className="h-4 w-4" />
          <span className="font-sans text-sm font-medium">Filters:</span>
        </div>

        <select
          value={level}
          onChange={(e) => {
            setLevel(e.target.value)
            updateFilters({ level: e.target.value as Filters['level'], language: language as Filters['language'] })
          }}
          className="h-10 rounded-lg border border-[var(--color-neutral-200)] bg-white px-4 font-sans text-sm text-[var(--color-text-headline-primary)] focus:border-[var(--color-secondary-blue-500)] focus:outline-none"
        >
          {levels.map((l) => (
            <option key={l.value} value={l.value}>
              {l.label}
            </option>
          ))}
        </select>

        <select
          value={language}
          onChange={(e) => {
            setLanguage(e.target.value)
            updateFilters({ language: e.target.value as Filters['language'], level: level as Filters['level'] })
          }}
          className="h-10 rounded-lg border border-[var(--color-neutral-200)] bg-white px-4 font-sans text-sm text-[var(--color-text-headline-primary)] focus:border-[var(--color-secondary-blue-500)] focus:outline-none"
        >
          {languages.map((l) => (
            <option key={l.value} value={l.value}>
              {l.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  )
}
