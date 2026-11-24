'use client'

import { useInstructors } from '@/hooks/useInstructors'
import { InstructorCard } from '@/components/public/InstructorCard'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import { GraduationCap } from 'lucide-react'

export default function InstructorsClient() {
  const { data: instructors, isLoading, isError, error } = useInstructors()

  return (
    <main className="min-h-screen bg-white">
      <PageHeader
        title="Our Instructors"
        description="Learn from qualified scholars and experienced educators who are passionate about spreading authentic Islamic knowledge."
      />

      <div className="container mx-auto px-5 py-12">
        {isLoading && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="h-[350px] animate-pulse rounded-2xl bg-[var(--color-neutral-100)]" />
            ))}
          </div>
        )}

        {isError && <ErrorState message={error?.message ?? 'Failed to load instructors'} />}

        {!isLoading && !isError && (!instructors || instructors.length === 0) && (
          <EmptyState icon={<GraduationCap className="h-12 w-12" />} title="No instructors found" description="Check back later for our instructor profiles." />
        )}

        {!isLoading && instructors && instructors.length > 0 && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {instructors.map((instructor) => <InstructorCard key={instructor.id} instructor={instructor} />)}
          </div>
        )}
      </div>
    </main>
  )
}
