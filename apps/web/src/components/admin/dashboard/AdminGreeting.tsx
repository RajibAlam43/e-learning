'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Plus, Video, Megaphone, ChevronRight, RefreshCw } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'

interface AdminGreetingProps {
  isFetching: boolean
  onRefresh: () => void
}

function greetingForHour(hour: number): string {
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export function AdminGreeting({ isFetching, onRefresh }: AdminGreetingProps) {
  const router = useRouter()
  const user = useAuthStore((s) => s.user)
  const firstName = user?.fullName?.split(' ')[0] ?? 'Admin'
  const hour = new Date().getHours()

  return (
    <section
      className='w-full rounded-2xl [background:linear-gradient(100.17deg,#684f17,#bf9843)] p-4 md:p-8 flex flex-col gap-4 shadow-[0px_4px_12px_rgba(191,152,67,0.2)]'
      aria-label='Dashboard greeting'
    >
      <div className='flex flex-wrap items-start justify-between gap-3'>
        <div className='flex flex-col gap-1'>
          <h3 className='m-0 text-2xl md:text-3xl font-medium text-white'>
            Assalamu Alaikum, {firstName}
          </h3>
          <p className='text-sm text-white/80 m-0'>
            {greetingForHour(hour)} — here&apos;s how your platform is performing today.
          </p>
        </div>
        <button
          type='button'
          onClick={onRefresh}
          disabled={isFetching}
          aria-label='Refresh dashboard data'
          className='inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-white/10 backdrop-blur text-white text-xs font-medium hover:bg-white/20 transition-colors disabled:opacity-60 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} aria-hidden='true' />
          {isFetching ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      <div className='flex flex-col sm:flex-row items-stretch sm:items-center gap-3 rounded-xl bg-white p-3 md:p-4'>
        <Link
          href='/admin/courses/create'
          className='flex-1 sm:flex-none inline-flex items-center justify-center gap-2 h-10 px-4 rounded-md [background:linear-gradient(19.99deg,#4f67d8,#3a4fc2_60%,#2e3fa8)] text-white text-sm font-medium no-underline hover:opacity-90 transition-opacity focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
        >
          <Plus className='h-4 w-4' aria-hidden='true' />
          Create a course
          <ChevronRight className='h-4 w-4 hidden sm:inline' aria-hidden='true' />
        </Link>
        <Link
          href='/admin/live-classes'
          className='flex-1 sm:flex-none inline-flex items-center justify-center gap-2 h-10 px-4 rounded-md [background:linear-gradient(19.99deg,#d0a518,#af840b_55%,#876103)] text-white text-sm font-medium no-underline hover:opacity-90 transition-opacity focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
        >
          <Video className='h-4 w-4' aria-hidden='true' />
          Live classes
          <ChevronRight className='h-4 w-4 hidden sm:inline' aria-hidden='true' />
        </Link>
        <button
          type='button'
          onClick={() => router.push('/admin/announcements')}
          className='flex-1 sm:flex-none inline-flex items-center justify-center gap-2 h-10 px-4 rounded-md border border-[rgba(59,130,246,0.3)] bg-white text-[#3b82f6] text-sm font-medium hover:bg-blue-50 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
        >
          <Megaphone className='h-4 w-4' aria-hidden='true' />
          Post announcement
          <ChevronRight className='h-4 w-4 hidden sm:inline' aria-hidden='true' />
        </button>
      </div>
    </section>
  )
}
