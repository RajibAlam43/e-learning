'use client'

import { useTranslations } from 'next-intl'

import { useState } from 'react'
import { Calendar, CircleDot, Clock, ExternalLink, Video } from 'lucide-react'
import { toast } from 'sonner'
import { Breadcrumb } from '@/components/shared/Breadcrumb'
import { useJoinLiveClass } from '@/hooks/useLearning'
import type { StudentLiveClassSummary } from '@/types'

type LiveClassDisplay = StudentLiveClassSummary & {
  provider?: string
  durationMinutes?: number
}

interface LiveClassOverviewSectionProps {
  liveClass: LiveClassDisplay
  courseName: string
  moduleName: string
}

export default function LiveClassOverviewSection({ liveClass, courseName, moduleName }: LiveClassOverviewSectionProps) {
  const ui = useTranslations('privateUi')

  const join = useJoinLiveClass()
  const [actionError, setActionError] = useState('')
  const status = liveClass.status === 'LIVE' ? 'LIVE' : liveClass.status === 'COMPLETED' ? 'COMPLETED' : 'UPCOMING'

  function openClassDestination() {
    if (!liveClass.liveClassId) return
    join.mutate(liveClass.liveClassId, {
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

  const startsAt = formatDateTime(liveClass.startsAt)
  const endsAt = formatTime(liveClass.endsAt)
  const duration = formatDuration(liveClass.durationMinutes)

  return (
    <div className='flex flex-col gap-6'>
      <Breadcrumb items={[
        { label: ui('myCourses'), href: '/student/courses' },
        { label: courseName },
        { label: moduleName || 'Live Class' },
      ]} />

      <section className='overflow-hidden rounded-2xl border border-blue-100 bg-white'>
        <div className='flex min-h-52 flex-col justify-between gap-6 bg-gradient-to-br from-[#1c3873] to-[#3b82f6] p-6 text-white md:p-8'>
          <div className='flex items-start justify-between gap-4'>
            <div className='flex items-center gap-3'>
              <span className='grid h-11 w-11 place-items-center rounded-xl bg-white/15'>
                <Video className='h-5 w-5' aria-hidden='true' />
              </span>
              <span className='text-sm font-medium uppercase tracking-wide text-white/80'>{ui('liveClass')}</span>
            </div>
            <span className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold ${
              status === 'LIVE'
                ? 'border-red-200 bg-red-50 text-red-600'
                : status === 'COMPLETED'
                  ? 'border-green-200 bg-green-50 text-green-700'
                  : 'border-blue-200 bg-blue-50 text-blue-700'
            }`}>
              {status === 'LIVE' && <CircleDot className='h-3 w-3 animate-pulse' aria-hidden='true' />}
              {status === 'LIVE' ? ui('liveNow2') : status === 'COMPLETED' ? ui('completed2') : ui('scheduled')}
            </span>
          </div>
          <h1 className='max-w-3xl text-2xl font-medium leading-tight text-white md:text-3xl'>{liveClass.title ?? ui('liveClass')}</h1>
        </div>

        <div className='flex flex-col gap-6 p-6 md:p-8'>
          {liveClass.description && <p className='max-w-3xl text-base leading-7 text-[#5e5f5c]'>{liveClass.description}</p>}

          <div className='grid gap-3 sm:grid-cols-2 lg:grid-cols-4'>
            <Detail icon={<Calendar className='h-4 w-4' aria-hidden='true' />} label={ui('starts')} value={startsAt} />
            <Detail icon={<Clock className='h-4 w-4' aria-hidden='true' />} label={ui('ends')} value={endsAt || 'Not specified'} />
            <Detail icon={<Clock className='h-4 w-4' aria-hidden='true' />} label={ui('duration')} value={duration} />
            <Detail icon={<Video className='h-4 w-4' aria-hidden='true' />} label={ui('provider')} value={formatProvider(liveClass.provider)} />
          </div>

          {actionError && <p role='alert' className='text-sm text-red-600'>{actionError}</p>}

          {status === 'LIVE' && liveClass.canJoin !== false && (
            <button
              type='button'
              onClick={openClassDestination}
              disabled={join.isPending}
              className='inline-flex h-11 w-full items-center justify-center gap-2 rounded-md bg-red-500 px-5 text-sm font-semibold text-white transition-colors hover:bg-red-600 disabled:opacity-50 sm:w-fit'
            >
              <CircleDot className='h-4 w-4 animate-pulse' aria-hidden='true' />
              {join.isPending ? ui('joining') : ui('joinLiveClass')}
            </button>
          )}

          {status === 'LIVE' && liveClass.canJoin === false && (
            <p className='text-sm font-medium text-[#5e5f5c]'>{ui('joiningIsNotAvailableForThisAccount')}</p>
          )}

          {status === 'COMPLETED' && liveClass.recordingUrl && (
            <a
              href={liveClass.recordingUrl}
              target='_blank'
              rel='noopener noreferrer'
              className='inline-flex h-11 w-full items-center justify-center gap-2 rounded-md bg-green-50 px-5 text-sm font-semibold text-green-700 transition-colors hover:bg-green-100 sm:w-fit'
            >
              <ExternalLink className='h-4 w-4' aria-hidden='true' />
              {ui('watchRecording2')}</a>
          )}

          {status === 'COMPLETED' && !liveClass.recordingUrl && liveClass.hasRecording && (
            <button
              type='button'
              onClick={openClassDestination}
              disabled={join.isPending}
              className='inline-flex h-11 w-full items-center justify-center gap-2 rounded-md bg-green-50 px-5 text-sm font-semibold text-green-700 transition-colors hover:bg-green-100 disabled:opacity-50 sm:w-fit'
            >
              <ExternalLink className='h-4 w-4' aria-hidden='true' />
              {join.isPending ? ui('opening') : ui('watchRecording2')}
            </button>
          )}

          {status === 'COMPLETED' && !liveClass.recordingUrl && !liveClass.hasRecording && (
            <p className='text-sm font-medium text-[#5e5f5c]'>{ui('theRecordingIsNotAvailableYet')}</p>
          )}

          {status === 'UPCOMING' && (
            <p className='text-sm font-medium text-[#5e5f5c]'>{ui('thisClassWillBecomeJoinableWhenTheHostStartsIt')}</p>
          )}
        </div>
      </section>
    </div>
  )
}

function Detail({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className='flex min-w-0 items-start gap-3 rounded-xl bg-[#f4f9ff] p-4'>
      <span className='mt-0.5 text-[#3b82f6]'>{icon}</span>
      <span className='min-w-0'>
        <span className='block text-xs text-[#5e5f5c]'>{label}</span>
        <span className='mt-1 block truncate text-sm font-medium text-[#1c3873]'>{value}</span>
      </span>
    </div>
  )
}

function formatDateTime(value?: string): string {
  if (!value) return 'Not specified'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? 'Not specified'
    : date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

function formatTime(value?: string): string {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })
}

function formatDuration(minutes?: number): string {
  if (!minutes || minutes <= 0) return 'Not specified'
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return remainder ? `${hours} hr ${remainder} min` : `${hours} hr`
}

function formatProvider(provider?: string): string {
  return provider === 'GOOGLE_MEET' ? 'Google Meet' : provider === 'ZOOM' ? 'Zoom' : provider === 'TEAMS' ? 'Microsoft Teams' : provider === 'OTHER' ? 'Other' : 'Not specified'
}
