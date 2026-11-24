'use client'

import { BarChart3 } from 'lucide-react'

interface AdminCourseCompletionProps {
  /** Number of live classes — shown as a substitute metric until backend adds
   *  a real course-completion endpoint. */
  liveClassCount: number
  /** Total course count (passed for context in the message). */
  courseCount: number
}

/**
 * AdminCourseCompletion — placeholder widget.
 *
 * The backend has no endpoint for per-course completion rates. Until one is
 * added, this widget shows a clear "backend endpoint needed" banner instead
 * of fake percentages. We surface the live-class count as a related metric so
 * the widget isn't empty.
 */
export function AdminCourseCompletion({ liveClassCount, courseCount }: AdminCourseCompletionProps) {
  return (
    <section
      className='h-full w-full bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-5 flex flex-col gap-4'
      aria-label='Course completion (pending backend)'
    >
      <header className='flex items-center justify-between'>
        <h3 className='text-base font-heading font-medium text-[#1c3873] m-0'>
          Course Completion Rate
        </h3>
        <BarChart3 className='h-4 w-4 text-[#71717a]' aria-hidden='true' />
      </header>

      {/* Real but limited data — live class count */}
      <div className='grid grid-cols-2 gap-3'>
        <div className='rounded-xl bg-blue-50 p-3 flex flex-col gap-1'>
          <span className='text-[11px] text-[#49608f]'>Total courses</span>
          <span className='text-xl font-medium text-[#1c3873]'>{courseCount}</span>
        </div>
        <div className='rounded-xl bg-amber-50 p-3 flex flex-col gap-1'>
          <span className='text-[11px] text-[#92400e]'>Live classes</span>
          <span className='text-xl font-medium text-[#92400e]'>{liveClassCount}</span>
        </div>
      </div>

    </section>
  )
}
