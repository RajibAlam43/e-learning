'use client'

import { useTranslations } from 'next-intl'

import { useInstructorDashboard } from '@/hooks/useInstructorDashboard'
import { Skeleton } from '@/components/ui/skeleton'
import { ErrorState } from '@/components/shared/ErrorState'
import { EmptyState } from '@/components/shared/EmptyState'
import { Video, Calendar } from 'lucide-react'

function formatDateTime(iso?: string): { date: string; time: string } {
  if (!iso) return { date: '—', time: '' }
  try {
    const d = new Date(iso)
    return {
      date: d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true }),
    }
  } catch {
    return { date: '—', time: '' }
  }
}

export default function InstructorLiveClassesPage() {
  const ui = useTranslations('privateUi')

  const { data: dashboard, isLoading, isError, refetch } = useInstructorDashboard()
  const liveClasses = dashboard?.upcomingLiveClasses ?? []

  const live = liveClasses.filter((c) => c.status === 'LIVE')
  const upcoming = liveClasses.filter((c) => c.status === 'SCHEDULED')
  const completed = liveClasses.filter((c) => c.status === 'COMPLETED')

  return (
    <div className='flex flex-col gap-6'>
      <div className='flex flex-col gap-1'>
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('liveClasses')}</h1>
        <p className='text-sm text-[#5e5f5c]'>
          {!isLoading && !isError ? `${liveClasses.length} classes total` : ''}
        </p>
      </div>

      {isLoading && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className='h-48 rounded-2xl' />
          ))}
        </div>
      )}

      {isError && !isLoading && (
        <ErrorState message={ui('failedToLoadLiveClassesPleaseTryAgain')} onRetry={() => refetch()} />
      )}

      {!isLoading && !isError && liveClasses.length === 0 && (
        <EmptyState
          icon={<Video className='h-10 w-10' />}
          title={ui('noLiveClasses')}
          description={ui('yourScheduledLiveClassesWillAppearHere')}
        />
      )}

      {!isLoading && !isError && liveClasses.length > 0 && (
        <>
          {live.length > 0 && (
            <section className='flex flex-col gap-4'>
              <h2 className='text-lg font-heading font-medium text-[#1c3873]'>{ui('liveNow')}</h2>
              <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
                {live.map((lc) => {
                  const { date, time } = formatDateTime(lc.startsAt)
                  return <ClassCard key={lc.liveClassId} title={lc.title} date={date} time={time} status='LIVE' joinUrl={lc.startUrl} />
                })}
              </div>
            </section>
          )}
          {upcoming.length > 0 && (
            <section className='flex flex-col gap-4'>
              <h2 className='text-lg font-heading font-medium text-[#1c3873]'>{ui('upcoming')}</h2>
              <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
                {upcoming.map((lc) => {
                  const { date, time } = formatDateTime(lc.startsAt)
                  return <ClassCard key={lc.liveClassId} title={lc.title} date={date} time={time} status='SCHEDULED' />
                })}
              </div>
            </section>
          )}
          {completed.length > 0 && (
            <section className='flex flex-col gap-4'>
              <h2 className='text-lg font-heading font-medium text-[#1c3873]'>{ui('completed2')}</h2>
              <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
                {completed.map((lc) => {
                  const { date, time } = formatDateTime(lc.startsAt)
                  return <ClassCard key={lc.liveClassId} title={lc.title} date={date} time={time} status='COMPLETED' />
                })}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}

function ClassCard({ title, date, time, status, joinUrl, recordingUrl }: {
  title?: string
  date: string
  time: string
  status: string
  joinUrl?: string
  recordingUrl?: string
}) {
  const ui = useTranslations('privateUi')

  return (
    <div className='bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] overflow-hidden'>
      <div className='relative h-24 bg-gradient-to-br from-[#1c3873] to-[#3b82f6] p-3'>
        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium border ${
          status === 'LIVE' ? 'bg-red-50 text-red-600 border-red-200' :
          status === 'SCHEDULED' ? 'bg-blue-50 text-blue-600 border-blue-200' :
          'bg-green-50 text-green-600 border-green-200'
        }`}>
          {status === 'LIVE' && <span className='w-2 h-2 rounded-full bg-red-500 animate-pulse' />}
          {status}
        </span>
      </div>
      <div className='p-4 flex flex-col gap-3'>
        <h3 className='text-base font-medium text-[#1c3873] line-clamp-2'>{title ?? ui('untitled')}</h3>
        <div className='flex items-center gap-2 text-sm text-[#5e5f5c]'>
          <Calendar className='w-4 h-4 text-[#3b82f6]' />
          <span className='font-medium text-[#1c3873]'>{date}</span>
          <span>{ui('at')}{' '}{time}</span>
        </div>
        {status === 'LIVE' && joinUrl && (
          <a href={joinUrl} target='_blank' rel='noopener noreferrer'
            className='w-full h-10 flex items-center justify-center gap-2 rounded-md bg-red-500 text-white text-sm font-semibold hover:bg-red-600 transition-colors'>
            {ui('joinNow')}</a>
        )}
        {status === 'COMPLETED' && recordingUrl && (
          <a href={recordingUrl} target='_blank' rel='noopener noreferrer'
            className='w-full h-10 flex items-center justify-center gap-2 rounded-md bg-green-50 text-green-600 text-sm font-semibold hover:bg-green-100 transition-colors'>
            {ui('watchRecording')}</a>
        )}
      </div>
    </div>
  )
}
