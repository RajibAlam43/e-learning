'use client'

import { GraduationCap, BookOpen } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
import type { AdminInstructorSummary } from '@/types'

interface AdminTopInstructorsProps {
  instructors: AdminInstructorSummary[]
  isLoading: boolean
}

export function AdminTopInstructors({ instructors, isLoading }: AdminTopInstructorsProps) {
  return (
    <section
      className='bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-5 flex flex-col gap-4'
      aria-label='Instructors'
    >
      <header className='flex items-center justify-between'>
        <h3 className='text-base font-heading font-medium text-[#1c3873] m-0'>Instructors</h3>
        <span className='text-[11px] text-[#71717a]'>
          {instructors.length} {instructors.length === 1 ? 'teacher' : 'teachers'}
        </span>
      </header>

      {/* Loading */}
      {isLoading && (
        <ul className='flex flex-col gap-3 m-0 p-0 list-none' aria-busy='true'>
          {Array.from({ length: 3 }).map((_, i) => (
            <li key={i} className='flex items-center gap-3'>
              <Skeleton className='h-10 w-10 rounded-full shrink-0' />
              <div className='flex-1 flex flex-col gap-1.5'>
                <Skeleton className='h-3 w-2/3' />
                <Skeleton className='h-2.5 w-1/3' />
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* Empty */}
      {!isLoading && instructors.length === 0 && (
        <EmptyState
          icon={<GraduationCap className='h-8 w-8' />}
          title='No instructors yet'
          description='Add teachers from the Teachers page.'
        />
      )}

      {/* List — AdminInstructorSummary has no avatarUrl or rating in the spec,
          so we show initials + assigned course count instead. */}
      {!isLoading && instructors.length > 0 && (
        <ul className='flex flex-col gap-3 m-0 p-0 list-none'>
          {instructors.map((ins) => {
            const initials = (ins.fullName ?? '?')
              .split(' ')
              .map((p) => p[0])
              .filter(Boolean)
              .slice(0, 2)
              .join('')
              .toUpperCase()
            return (
              <li key={ins.userId ?? ins.email} className='flex items-center gap-3'>
                <div
                  className='h-10 w-10 rounded-full bg-blue-50 flex items-center justify-center text-xs font-medium text-[#3b82f6] shrink-0'
                  aria-hidden='true'
                >
                  {initials}
                </div>
                <div className='flex-1 min-w-0'>
                  <p className='text-sm font-medium text-[#1b1c1a] truncate m-0'>
                    {ins.fullName ?? ins.displayName ?? 'Unknown instructor'}
                  </p>
                  <p className='text-xs text-[#71717a] truncate m-0'>
                    {ins.headline ?? ins.email ?? 'No headline'}
                  </p>
                </div>
                {typeof ins.assignedCoursesCount === 'number' && (
                  <span
                    className='inline-flex items-center gap-1 text-xs font-medium text-[#49608f] shrink-0'
                    title={`${ins.assignedCoursesCount} assigned courses`}
                  >
                    <BookOpen className='h-3 w-3' aria-hidden='true' />
                    {ins.assignedCoursesCount}
                  </span>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
