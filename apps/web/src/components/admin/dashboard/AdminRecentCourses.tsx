'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import Image from 'next/image'
import { Pencil, MoreVertical, ChevronLeft, ChevronRight, BookOpen } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
import type { AdminCourseSummary } from '@/types'

const PAGE_SIZE = 5

interface AdminRecentCoursesProps {
  courses: AdminCourseSummary[]
  isLoading: boolean
}

function formatDate(iso?: string): string {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return '—'
  }
}

export function AdminRecentCourses({ courses, isLoading }: AdminRecentCoursesProps) {
  const router = useRouter()
  const [page, setPage] = useState(0)

  const totalPages = Math.max(1, Math.ceil(courses.length / PAGE_SIZE))
  const pageItems = courses.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  return (
    <section
      className='flex-1 w-full bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-5 flex flex-col gap-4'
      aria-label='Recent courses'
    >
      <header className='flex items-center justify-between'>
        <h3 className='text-base font-heading font-medium text-[#1c3873] m-0'>Recent Courses</h3>
        <Link
          href='/admin/courses'
          className='text-xs font-medium text-[#3b82f6] hover:text-[#2563eb] transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
        >
          See all courses →
        </Link>
      </header>

      {/* Loading */}
      {isLoading && (
        <ul className='flex flex-col gap-3 m-0 p-0 list-none' aria-busy='true'>
          {Array.from({ length: 4 }).map((_, i) => (
            <li key={i} className='flex items-center gap-3 p-3 rounded-xl border border-[#f0f0f0]'>
              <Skeleton className='h-10 w-10 rounded-lg shrink-0' />
              <div className='flex-1 flex flex-col gap-2'>
                <Skeleton className='h-4 w-3/4' />
                <Skeleton className='h-3 w-1/2' />
              </div>
              <Skeleton className='h-6 w-16' />
            </li>
          ))}
        </ul>
      )}

      {/* Empty */}
      {!isLoading && pageItems.length === 0 && (
        <EmptyState
          icon={<BookOpen className='h-10 w-10' />}
          title='No courses yet'
          description='Create your first course to see it here.'
        />
      )}

      {/* List */}
      {!isLoading && pageItems.length > 0 && (
        <>
          <ul className='flex flex-col gap-3 m-0 p-0 list-none'>
            {pageItems.map((c) => (
              <li
                key={c.courseId ?? c.slug}
                className='flex items-center gap-3 p-3 rounded-xl border border-[#f0f0f0] hover:border-blue-200 hover:bg-blue-50/30 transition-colors'
              >
                <div className='relative h-10 w-10 rounded-lg overflow-hidden bg-gray-100 shrink-0'>
                  {c.thumbnailUrl ? (
                    <Image
                      src={c.thumbnailUrl}
                      alt={`${c.title ?? 'Course'} thumbnail`}
                      fill
                      className='object-cover'
                      sizes='40px'
                    />
                  ) : (
                    <div className='flex items-center justify-center h-full'>
                      <BookOpen className='h-4 w-4 text-gray-400' />
                    </div>
                  )}
                </div>
                <div className='flex-1 min-w-0'>
                  <p className='text-sm font-medium text-[#1b1c1a] truncate m-0'>
                    {c.title || 'Untitled course'}
                  </p>
                  <p className='text-xs text-[#71717a] m-0'>
                    {c.instructorName ?? 'No instructor'} · Updated {formatDate(c.publishedAt)}
                  </p>
                </div>
                <span
                  className={`text-[11px] font-medium px-2 py-1 rounded-full shrink-0 ${
                    c.status === 'PUBLISHED'
                      ? 'bg-green-100 text-green-700'
                      : c.status === 'DRAFT'
                        ? 'bg-gray-100 text-gray-600'
                        : 'bg-amber-100 text-amber-700'
                  }`}
                >
                  {(c.status ?? 'UNKNOWN').toLowerCase()}
                </span>
                <button
                  type='button'
                  onClick={() => router.push(`/admin/courses/${c.courseId}`)}
                  aria-label={`Edit ${c.title ?? 'course'}`}
                  className='p-2 rounded-lg hover:bg-gray-100 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
                >
                  <Pencil className='h-4 w-4 text-[#5e5f5c]' />
                </button>
                <button
                  type='button'
                  aria-label={`More actions for ${c.title ?? 'course'}`}
                  className='p-2 rounded-lg hover:bg-gray-100 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
                >
                  <MoreVertical className='h-4 w-4 text-[#5e5f5c]' />
                </button>
              </li>
            ))}
          </ul>

          {/* Pagination */}
          {totalPages > 1 && (
            <nav className='flex items-center justify-between pt-2' aria-label='Pagination'>
              <button
                type='button'
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                aria-label='Previous page'
                className='inline-flex items-center gap-1 text-xs font-medium text-[#5e5f5c] disabled:opacity-40 disabled:cursor-not-allowed hover:text-[#1c3873] transition-colors'
              >
                <ChevronLeft className='h-3 w-3' /> Prev
              </button>
              <span className='text-xs text-[#71717a]'>
                Page {page + 1} of {totalPages}
              </span>
              <button
                type='button'
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                aria-label='Next page'
                className='inline-flex items-center gap-1 text-xs font-medium text-[#5e5f5c] disabled:opacity-40 disabled:cursor-not-allowed hover:text-[#1c3873] transition-colors'
              >
                Next <ChevronRight className='h-3 w-3' />
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
