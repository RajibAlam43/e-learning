'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import { useScheduleExternalLiveClass, useScheduleLiveClass } from '@/hooks/useAdmin'
import type { AdminSectionItem, ScheduleExternalLiveClassRequest, ScheduleLiveClassRequest } from '@/types'

type Provider = ScheduleLiveClassRequest['provider']
type Mode = 'PROVISIONED' | 'EXTERNAL_URL'

interface LiveClassScheduleDialogProps {
  courseId: string
  item: AdminSectionItem
  onClose: () => void
}

const providers: { value: Provider; label: string }[] = [
  { value: 'ZOOM', label: 'Zoom' },
  { value: 'GOOGLE_MEET', label: 'Google Meet' },
  { value: 'TEAMS', label: 'Microsoft Teams' },
  { value: 'OTHER', label: 'Other' },
]

export function LiveClassScheduleDialog({ courseId, item, onClose }: LiveClassScheduleDialogProps) {
  const liveClass = item.liveClass
  const liveClassId = liveClass?.liveClassId ?? liveClass?.liveClassItemId
  const schedule = useScheduleLiveClass()
  const scheduleExternal = useScheduleExternalLiveClass()
  const [mode, setMode] = useState<Mode>('PROVISIONED')
  const [startsAt, setStartsAt] = useState(toDateTimeLocal(liveClass?.startsAt))
  const [endsAt, setEndsAt] = useState(toDateTimeLocal(liveClass?.endsAt))
  const [provider, setProvider] = useState<Provider>((liveClass?.provider as Provider | undefined) ?? 'ZOOM')
  const [maxCapacity, setMaxCapacity] = useState('0')
  const [participantJoinUrl, setParticipantJoinUrl] = useState('')
  const [hostStartUrl, setHostStartUrl] = useState('')
  const [meetingId, setMeetingId] = useState('')
  const [error, setError] = useState('')

  const isSaving = schedule.isPending || scheduleExternal.isPending

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')

    if (!liveClassId) {
      setError('This live class does not have a schedulable ID yet.')
      return
    }
    if (!startsAt || !endsAt) {
      setError('Start time and end time are required.')
      return
    }
    if (new Date(endsAt) <= new Date(startsAt)) {
      setError('End time must be later than start time.')
      return
    }
    if (mode === 'EXTERNAL_URL' && !participantJoinUrl.trim()) {
      setError('Participant join URL is required for an external meeting.')
      return
    }

    try {
      if (mode === 'EXTERNAL_URL') {
        const data: ScheduleExternalLiveClassRequest = {
          startsAt: new Date(startsAt).toISOString(),
          endsAt: new Date(endsAt).toISOString(),
          provider,
          participantJoinUrl: participantJoinUrl.trim(),
          hostStartUrl: hostStartUrl.trim() || undefined,
          meetingId: meetingId.trim() || undefined,
          maxCapacity: Math.max(0, Number(maxCapacity) || 0),
        }
        await scheduleExternal.mutateAsync({ courseId, liveClassId, data })
      } else {
        const data: ScheduleLiveClassRequest = {
          startsAt: new Date(startsAt).toISOString(),
          endsAt: new Date(endsAt).toISOString(),
          provider,
          maxCapacity: Math.max(0, Number(maxCapacity) || 0),
        }
        await schedule.mutateAsync({ courseId, liveClassId, data })
      }
      toast.success('Live class scheduled')
      onClose()
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Could not schedule the live class.'
      setError(message)
      toast.error(message)
    }
  }

  return (
    <div role='dialog' aria-modal='true' aria-labelledby='live-class-schedule-title' className='fixed inset-0 z-50 grid place-items-center bg-black/45 p-4'>
      <form onSubmit={submit} className='max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl'>
        <div className='mb-5 flex items-start justify-between gap-4'>
          <div>
            <h2 id='live-class-schedule-title' className='font-heading text-xl font-semibold text-[#1c3873]'>Schedule live class</h2>
            <p className='mt-1 text-sm text-slate-500'>{liveClass?.title ?? 'Live class'}</p>
          </div>
          <button type='button' onClick={onClose} className='rounded-lg p-2 text-slate-500 hover:bg-slate-100' aria-label='Close'>×</button>
        </div>

        {error && <div role='alert' className='mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800'>{error}</div>}

        <div className='grid gap-4 sm:grid-cols-2'>
          <Field label='Meeting mode'>
            <select value={mode} onChange={(event) => setMode(event.target.value as Mode)} className='h-10 rounded-lg border border-slate-200 px-3'>
              <option value='PROVISIONED'>Provision with provider</option>
              <option value='EXTERNAL_URL'>Use external meeting URL</option>
            </select>
          </Field>
          <Field label='Provider'>
            <select value={provider} onChange={(event) => setProvider(event.target.value as Provider)} className='h-10 rounded-lg border border-slate-200 px-3'>
              {providers.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </Field>
          <Field label='Start time' required>
            <input required type='datetime-local' value={startsAt} onChange={(event) => setStartsAt(event.target.value)} className='h-10 rounded-lg border border-slate-200 px-3' />
          </Field>
          <Field label='End time' required>
            <input required type='datetime-local' value={endsAt} onChange={(event) => setEndsAt(event.target.value)} className='h-10 rounded-lg border border-slate-200 px-3' />
          </Field>
          <Field label='Maximum capacity'>
            <input type='number' min={0} value={maxCapacity} onChange={(event) => setMaxCapacity(event.target.value)} className='h-10 rounded-lg border border-slate-200 px-3' />
          </Field>
          {mode === 'EXTERNAL_URL' && (
            <>
              <Field label='Participant join URL' required>
                <input required type='url' value={participantJoinUrl} onChange={(event) => setParticipantJoinUrl(event.target.value)} className='h-10 rounded-lg border border-slate-200 px-3' placeholder='https://...' />
              </Field>
              <Field label='Host start URL'>
                <input type='url' value={hostStartUrl} onChange={(event) => setHostStartUrl(event.target.value)} className='h-10 rounded-lg border border-slate-200 px-3' placeholder='https://...' />
              </Field>
              <Field label='Meeting ID'>
                <input value={meetingId} onChange={(event) => setMeetingId(event.target.value)} className='h-10 rounded-lg border border-slate-200 px-3' />
              </Field>
            </>
          )}
        </div>

        <div className='mt-6 flex justify-end gap-3 border-t pt-4'>
          <button type='button' onClick={onClose} className='rounded-lg border border-slate-200 px-4 py-2 text-sm' disabled={isSaving}>Cancel</button>
          <button type='submit' disabled={isSaving} className='inline-flex items-center justify-center rounded-lg bg-[#1c3873] px-5 py-2 text-sm font-semibold text-white disabled:opacity-50'>
            {isSaving ? 'Scheduling...' : 'Schedule live class'}
          </button>
        </div>
      </form>
    </div>
  )
}

function Field({ label, required, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return <label className='grid gap-1 text-sm font-medium text-slate-700'><span>{label}{required && <span className='ml-0.5 text-red-600' aria-hidden='true'>*</span>}</span>{children}</label>
}

function toDateTimeLocal(value?: string): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}
