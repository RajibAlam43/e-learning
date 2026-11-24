'use client'

import { useTranslations } from 'next-intl'

import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import { Skeleton } from '@/components/ui/skeleton'
import { useLiveClasses } from '@/hooks/useStudentDashboard'
import type { StudentLiveClassSummary } from '@/types'
import { Video, Clock, Calendar, CircleDot, ExternalLink } from 'lucide-react'
import { useJoinLiveClass } from '@/hooks/useLearning'
import { toast } from 'sonner'
import { useState } from 'react'

type DisplayStatus = 'LIVE' | 'UPCOMING' | 'COMPLETED'

interface LiveClassDisplay {
  id: string
  title: string
  instructor: string
  date: string
  time: string
  duration: string
  status: DisplayStatus
  courseId?: string
  joinUrl?: string
  canJoin?: boolean
  hasRecording?: boolean
  recordingUrl?: string
}

function mapStatus(apiStatus: string | undefined): DisplayStatus | null {
  switch (apiStatus) {
    case 'LIVE': return 'LIVE'
    case 'SCHEDULED': return 'UPCOMING'
    case 'COMPLETED': return 'COMPLETED'
    default: return null // CANCELLED, FAILED — skip
  }
}

function formatDateTime(iso?: string): { date: string; time: string } {
  if (!iso) return { date: '—', time: '' }
  try {
    const d = new Date(iso)
    const now = new Date()
    const isToday = d.toDateString() === now.toDateString()
    const tomorrow = new Date(now)
    tomorrow.setDate(tomorrow.getDate() + 1)
    const isTomorrow = d.toDateString() === tomorrow.toDateString()

    return {
      date: isToday
        ? 'TODAY'
        : isTomorrow
          ? 'Tomorrow'
          : d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true }),
    }
  } catch {
    return { date: '—', time: '' }
  }
}

function formatDuration(minutes?: number): string {
  if (!minutes || minutes <= 0) return '1 hr'
  if (minutes < 60) return `${minutes} min`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h}.${Math.round(m / 6) / 10} hrs` : `${h} hr`
}

function mapToDisplay(c: StudentLiveClassSummary): LiveClassDisplay | null {
  const status = mapStatus(c.status)
  if (!status) return null
  const { date, time } = formatDateTime(c.startsAt)
  return {
    id: c.liveClassId ?? '',
    title: c.title ?? 'Untitled class',
    instructor: c.instructorName ?? 'Unknown instructor',
    date,
    time,
    duration: formatDuration(c.durationMinutes),
    status,
    courseId: c.courseId,
    joinUrl: c.joinUrl,
    canJoin: c.canJoin,
    hasRecording: c.hasRecording,
    recordingUrl: c.recordingUrl,
  }
}

function StatusBadge({ status }: { status: DisplayStatus }) {
  const styles = {
    LIVE: 'bg-red-50 text-red-600 border-red-200',
    UPCOMING: 'bg-blue-50 text-blue-600 border-blue-200',
    COMPLETED: 'bg-green-50 text-green-600 border-green-200',
  }

  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium border ${styles[status]}`}>
      {status === 'LIVE' && <CircleDot className='w-3 h-3 animate-pulse' />}
      {status}
    </span>
  )
}

function LiveClassCard({ cls }: { cls: LiveClassDisplay }) {
  const ui = useTranslations('privateUi')

  const join = useJoinLiveClass()
  const [actionError, setActionError] = useState('')

  function openClassDestination() {
    join.mutate(cls.id, {
      onSuccess: (data) => {
        const destination = data.joinUrl ?? data.recordingUrl
        if (!destination) {
          setActionError(ui('theMeetingOrRecordingLinkIsNotAvailableYet'))
          return
        }
        setActionError('')
        window.open(destination, '_blank', 'noopener,noreferrer')
      },
      onError: (error) => {
        const message = error instanceof Error ? error.message : 'Unable to open this live class.'
        setActionError(message)
        toast.error(message)
      },
    })
  }

  return (
    <div className='bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0px_8px_20px_0px_rgba(20,26,184,0.12)]'>
      {/* Thumbnail — gradient fallback (no thumbnail field in API) */}
      <div className='relative w-full h-36 bg-gradient-to-br from-[#1c3873] to-[#3b82f6]'>
        <div className='absolute top-3 left-3'>
          <StatusBadge status={cls.status} />
        </div>
        <div className='absolute bottom-3 left-3 right-3 flex items-center gap-2'>
          <div className='flex items-center gap-1 text-white/90 text-xs'>
            <Clock className='w-3.5 h-3.5' />
            {cls.duration}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className='p-4 flex flex-col gap-3'>
        <h3 className='text-base font-medium font-switzer leading-6 text-[#1c3873] line-clamp-2'>
          {cls.title}
        </h3>

        <div className='flex items-center gap-2'>
          <div className='w-7 h-7 rounded-full bg-blue-100 flex items-center justify-center text-xs font-medium text-[#1c3873] shrink-0'>
            {cls.instructor.charAt(0)}
          </div>
          <span className='text-sm text-[#5e5f5c] truncate'>{cls.instructor}</span>
        </div>

        <div className='flex items-center gap-2 text-sm text-[#5e5f5c]'>
          <Calendar className='w-4 h-4 text-[#3b82f6] shrink-0' />
          <span className='font-medium text-[#1c3873]'>{cls.date}</span>
          <span>{ui('at')}{' '}{cls.time}</span>
        </div>

        {/* Action */}
        {actionError && <p role='alert' className='text-xs text-red-600'>{actionError}</p>}
        {cls.status === 'LIVE' && cls.canJoin !== false ? (
          <button
            type='button'
            onClick={openClassDestination}
            disabled={join.isPending}
            className='w-full h-10 flex items-center justify-center gap-2 rounded-md bg-red-500 text-white text-sm font-semibold font-switzer hover:bg-red-600 transition-colors'
          >
            <CircleDot className='w-4 h-4 animate-pulse' />
            {join.isPending ? ui('joining2') : ui('joinNow')}
          </button>
        ) : cls.status === 'LIVE' ? (
          <div className='flex h-10 w-full items-center justify-center rounded-md bg-gray-50 text-sm font-medium text-gray-500'>
            {ui('joinUnavailable')}</div>
        ) : cls.status === 'UPCOMING' ? (
          <div className='w-full h-10 flex items-center justify-center gap-2 rounded-md shadow-[0px_3px_8px_0px_rgba(0,0,0,0.08)] bg-white outline outline-1 outline-blue-500/30 text-blue-500 text-sm font-semibold font-switzer'>
            <Calendar className='w-4 h-4' />
            {ui('starts')}{' '}{cls.time || ui('soon')}
          </div>
        ) : cls.status === 'COMPLETED' && cls.recordingUrl ? (
          <a
            href={cls.recordingUrl}
            target='_blank'
            rel='noopener noreferrer'
            className='w-full h-10 flex items-center justify-center gap-2 rounded-md bg-[#f0fdf4] text-green-600 text-sm font-semibold font-switzer hover:bg-green-100 transition-colors'
          >
            <ExternalLink className='w-4 h-4' />
            {ui('watchRecording')}</a>
        ) : cls.status === 'COMPLETED' && cls.hasRecording ? (
          <button
            type='button'
            onClick={openClassDestination}
            disabled={join.isPending}
            className='w-full h-10 flex items-center justify-center gap-2 rounded-md bg-[#f0fdf4] text-green-600 text-sm font-semibold font-switzer hover:bg-green-100 transition-colors disabled:opacity-50'
          >
            <ExternalLink className='w-4 h-4' />
            {join.isPending ? ui('opening') : ui('watchRecording')}
          </button>
        ) : cls.status === 'COMPLETED' ? (
          <div className='w-full h-10 flex items-center justify-center rounded-md bg-gray-50 text-gray-400 text-sm font-medium'>
            {ui('completed2')}</div>
        ) : null}
      </div>
    </div>
  )
}

export default function LiveClassesPage() {
  const ui = useTranslations('privateUi')

  const { data: apiClasses, isLoading, isError, refetch } = useLiveClasses()

  const classes: LiveClassDisplay[] = (apiClasses ?? [])
    .map(mapToDisplay)
    .filter((c): c is LiveClassDisplay => Boolean(c?.id))

  const liveNow = classes.filter((c) => c.status === 'LIVE')
  const upcoming = classes.filter((c) => c.status === 'UPCOMING')
  const completed = classes.filter((c) => c.status === 'COMPLETED')

  return (
    <div className='flex flex-col gap-6'>
      <div className='flex flex-col gap-1'>
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('liveClasses')}</h1>
        <p className='text-sm text-[#5e5f5c]'>
          {!isLoading ? `${classes.length} classes total` : ''}
        </p>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className='h-72 rounded-2xl' />
          ))}
        </div>
      )}

      {/* Error */}
      {isError && !isLoading && (
        <ErrorState message={ui('failedToLoadLiveClassesPleaseTryAgain')} onRetry={() => refetch()} />
      )}

      {/* Empty */}
      {!isLoading && !isError && classes.length === 0 && (
        <EmptyState
          icon={<Video className='h-10 w-10' />}
          title={ui('noLiveClasses')}
          description={ui('liveClassesWillAppearHereWhenScheduled')}
        />
      )}

      {/* Live Now */}
      {!isLoading && !isError && liveNow.length > 0 && (
        <section className='flex flex-col gap-4'>
          <div className='flex items-center gap-2'>
            <CircleDot className='w-5 h-5 text-red-500 animate-pulse' />
            <h2 className='text-lg font-heading font-medium text-[#1c3873]'>{ui('liveNow')}</h2>
          </div>
          <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
            {liveNow.map((cls) => (
              <LiveClassCard key={cls.id} cls={cls} />
            ))}
          </div>
        </section>
      )}

      {/* Upcoming */}
      {!isLoading && !isError && upcoming.length > 0 && (
        <section className='flex flex-col gap-4'>
          <h2 className='text-lg font-heading font-medium text-[#1c3873]'>{ui('upcoming')}</h2>
          <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
            {upcoming.map((cls) => (
              <LiveClassCard key={cls.id} cls={cls} />
            ))}
          </div>
        </section>
      )}

      {/* Completed */}
      {!isLoading && !isError && completed.length > 0 && (
        <section className='flex flex-col gap-4'>
          <h2 className='text-lg font-heading font-medium text-[#1c3873]'>{ui('completed2')}</h2>
          <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
            {completed.map((cls) => (
              <LiveClassCard key={cls.id} cls={cls} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
