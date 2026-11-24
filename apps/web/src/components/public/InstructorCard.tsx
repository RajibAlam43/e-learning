'use client'

import Image from 'next/image'
import Link from 'next/link'
import { BookOpen } from 'lucide-react'
import type { components } from '@/types/api'

type InstructorSummary = components['schemas']['InstructorSummaryResponse']

interface InstructorCardProps {
  instructor: InstructorSummary
}

export function InstructorCard({ instructor }: InstructorCardProps) {
  return (
    <article className="flex flex-col items-center overflow-hidden rounded-2xl bg-white p-8 text-center transition-all duration-300 hover:scale-[1.02] hover:shadow-lg">
      <div className="relative mb-4 h-24 w-24 overflow-hidden rounded-full bg-[var(--color-neutral-100)]">
        {instructor.avatarUrl ? (
          <Image
            src={instructor.avatarUrl}
            alt={instructor.fullName ?? 'Instructor'}
            fill
            className="object-cover"
            sizes="96px"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-[var(--color-secondary-blue-50)]">
            <span className="font-heading text-2xl font-semibold text-[var(--color-secondary-blue-500)]">
              {instructor.fullName?.charAt(0)?.toUpperCase() ?? '?'}
            </span>
          </div>
        )}
      </div>

      <h2 className="font-heading text-xl font-semibold text-[var(--color-text-headline-primary)]">
        {instructor.fullName}
      </h2>

      {instructor.credentials && (
        <p className="mt-1 font-sans text-sm text-[var(--color-secondary-blue-600)]">
          {instructor.credentials}
        </p>
      )}

      {instructor.shortBio && (
        <p className="mt-2 line-clamp-3 font-sans text-sm leading-[150%] text-[var(--color-text-body-secondary)]">
          {instructor.shortBio}
        </p>
      )}

      <div className="mt-4 flex items-center gap-4 font-sans text-sm text-[#6b6b68]">
        {instructor.yearsExperience != null && (
          <span>{instructor.yearsExperience}+ yrs exp</span>
        )}
        {instructor.publishedCoursesCount != null && (
          <span className="flex items-center gap-1">
            <BookOpen className="h-4 w-4" />
            {instructor.publishedCoursesCount} courses
          </span>
        )}
      </div>

      <Link
        href={`/instructors/${instructor.slug}`}
        className="btn-blue-gradient mt-6 inline-flex h-10 items-center justify-center gap-2 rounded-md px-5 font-sans text-sm font-semibold text-white transition-all duration-200 hover:opacity-90"
      >
        View Profile
      </Link>
    </article>
  )
}
