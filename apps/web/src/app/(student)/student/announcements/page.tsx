'use client'

import { useTranslations } from 'next-intl'

import { Megaphone, RefreshCw } from 'lucide-react'
import { useStudentAnnouncements } from '@/hooks/useStudentDashboard'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function StudentAnnouncementsPage() {
  const ui = useTranslations('privateUi')

  const query = useStudentAnnouncements(0, 20)
  const announcements = query.data?.content ?? []

  return <div className='flex flex-col gap-6'>
    <header className='flex items-center justify-between gap-4'>
      <div><h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('announcements')}</h1><p className='text-sm text-[#5e5f5c]'>{ui('updatesFromYourEnrolledCourses')}</p></div>
      <button type='button' onClick={() => query.refetch()} disabled={query.isFetching} className='rounded-lg border bg-white p-2.5 text-[#1c3873] disabled:opacity-50' aria-label={ui('refreshAnnouncements')}><RefreshCw className={`h-4 w-4 ${query.isFetching ? 'animate-spin' : ''}`} /></button>
    </header>
    {query.isError ? <ErrorState message={ui('unableToLoadAnnouncements')} onRetry={() => query.refetch()} /> : query.isLoading ? <div className='grid gap-3'>{[1, 2, 3].map(item => <div key={item} className='h-24 animate-pulse rounded-2xl bg-white' />)}</div> : announcements.length === 0 ? <EmptyState icon={<Megaphone className='h-10 w-10' />} title={ui('noAnnouncementsYet')} description={ui('courseUpdatesWillAppearHere')} /> : <div className='grid gap-3'>{announcements.map((item, index) => <article key={item.announcementId ?? `${item.courseId}-${index}`} className='rounded-2xl bg-white p-5 shadow-sm'><div className='flex items-start justify-between gap-3'><div><p className='text-xs font-semibold uppercase tracking-wide text-[#8c7355]'>{item.courseTitle ?? ui('courseUpdate')}</p><h2 className='mt-1 font-heading text-lg text-[#1c3873]'>{item.title ?? ui('announcement')}</h2></div><time className='shrink-0 text-xs text-slate-500'>{formatDate(item.createdAt)}</time></div><p className='mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-600'>{item.content}</p></article>)}</div>}
  </div>
}
