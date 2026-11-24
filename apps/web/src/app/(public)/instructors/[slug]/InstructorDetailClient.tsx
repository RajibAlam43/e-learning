'use client'

import { useTranslations } from 'next-intl'

import Image from 'next/image'
import { useInstructor } from '@/hooks/useInstructors'
import { BookOpen, Clock } from 'lucide-react'
import { ErrorState } from '@/components/shared/ErrorState'
import { CourseCard } from '@/components/public/CourseCard'
import type { components } from '@/types/api'

interface InstructorDetailClientProps {
  slug: string
  initialData: components['schemas']['InstructorDetailsResponse']
}

export default function InstructorDetailClient({ slug, initialData }: InstructorDetailClientProps) {
  const ui = useTranslations('privateUi')

  const { data: instructor, isLoading, isError, error } = useInstructor(slug, initialData)

  if (isLoading) {
    return (
      <main className="min-h-screen bg-white">
        <div className="container mx-auto px-5 py-12">
          <div className="animate-pulse space-y-6">
            <div className="flex gap-8">
              <div className="h-32 w-32 rounded-full bg-[var(--color-neutral-100)]" />
              <div className="flex-1 space-y-3">
                <div className="h-8 w-64 rounded bg-[var(--color-neutral-100)]" />
                <div className="h-5 w-48 rounded bg-[var(--color-neutral-100)]" />
                <div className="h-4 w-32 rounded bg-[var(--color-neutral-100)]" />
              </div>
            </div>
          </div>
        </div>
      </main>
    )
  }

  if (isError && !instructor) {
    return (
      <main className="min-h-screen bg-white">
        <div className="container mx-auto px-5 py-12">
          <ErrorState message={error?.message ?? ui('instructorNotFound')} />
        </div>
      </main>
    )
  }

  if (!instructor) return null

  return (
    <main className="min-h-screen bg-white">
      <section className="bg-[var(--color-secondary-blue-50)] py-16">
        <div className="container mx-auto px-5">
          <div className="flex flex-col items-center gap-8 sm:flex-row sm:items-start">
            <div className="relative h-32 w-32 shrink-0 overflow-hidden rounded-full bg-white shadow-md">
              {instructor.avatarUrl ? (
                <Image src={instructor.avatarUrl} alt={instructor.fullName ?? ui('instructor2')} fill className="object-cover" sizes="128px" />
              ) : (
                <div className="flex h-full w-full items-center justify-center bg-[var(--color-secondary-blue-100)]">
                  <span className="font-heading text-4xl font-semibold text-[var(--color-secondary-blue-500)]">
                    {instructor.fullName?.charAt(0)?.toUpperCase() ?? '?'}
                  </span>
                </div>
              )}
            </div>

            <div className="flex flex-col items-center gap-3 text-center sm:items-start sm:text-left">
              <h1 className="font-heading text-[36px] font-semibold leading-[44px] tracking-[-1px] text-[var(--color-text-headline-primary)] max-md:text-[28px] max-md:leading-[34px]">
                {instructor.displayName ?? instructor.fullName}
              </h1>
              {instructor.headline && (
                <p className="font-sans text-lg text-[var(--color-secondary-blue-500)]">{instructor.headline}</p>
              )}
              {(instructor.institution || instructor.expertiseArea) && (
                <p className="font-sans text-base text-[var(--color-text-body-secondary)]">
                  {[instructor.expertiseArea, instructor.institution].filter(Boolean).join(' at ')}
                </p>
              )}
              <div className="flex items-center gap-6 font-sans text-sm text-[var(--color-neutral-600)]">
                {instructor.yearsExperience != null && (
                  <span className="flex items-center gap-1"><Clock className="h-4 w-4" />{instructor.yearsExperience}{ui('yearsExperience')}</span>
                )}
                {instructor.publishedCourses && (
                  <span className="flex items-center gap-1"><BookOpen className="h-4 w-4" />{instructor.publishedCourses.length} {ui('courses2')}</span>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="py-12">
        <div className="container mx-auto px-5">
          {instructor.about && (
            <div className="mb-12">
              <h2 className="mb-4 font-heading text-2xl font-semibold text-[var(--color-text-headline-primary)]">{ui('about')}</h2>
              <div className="max-w-[800px] font-sans text-base leading-7 text-[var(--color-text-body-secondary)] whitespace-pre-line">
                {instructor.about}
              </div>
            </div>
          )}

          {instructor.credentialsText && (
            <div className="mb-12">
              <h2 className="mb-4 font-heading text-2xl font-semibold text-[var(--color-text-headline-primary)]">{ui('credentials')}</h2>
              <p className="max-w-[800px] font-sans text-base leading-7 text-[var(--color-text-body-secondary)]">{instructor.credentialsText}</p>
            </div>
          )}

          {instructor.specialties && instructor.specialties.length > 0 && (
            <div className="mb-12">
              <h2 className="mb-4 font-heading text-2xl font-semibold text-[var(--color-text-headline-primary)]">{ui('specialties')}</h2>
              <div className="flex flex-wrap gap-3">
                {instructor.specialties.map((spec) => (
                  <span key={spec} className="rounded-full bg-[var(--color-secondary-blue-50)] px-4 py-2 font-sans text-sm font-medium text-[var(--color-secondary-blue-600)]">{spec}</span>
                ))}
              </div>
            </div>
          )}

          {instructor.publishedCourses && instructor.publishedCourses.length > 0 && (
            <div>
              <h2 className="mb-6 font-heading text-2xl font-semibold text-[var(--color-text-headline-primary)]">
                {ui('coursesBy')}{' '}{instructor.displayName ?? instructor.fullName}
              </h2>
              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                {instructor.publishedCourses.map((course) => <CourseCard key={course.id} course={course} />)}
              </div>
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
