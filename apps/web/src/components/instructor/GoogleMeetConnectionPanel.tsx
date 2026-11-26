'use client'

import { useSyncExternalStore } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocale } from 'next-intl'
import { recordingService } from '@/services/recording.service'
import { message } from '@/lib/recording/browser-recorder'
import { useAuthStore } from '@/store/auth.store'

const subscribeLocation = (changed: () => void) => {
  window.addEventListener('popstate', changed)
  return () => window.removeEventListener('popstate', changed)
}

export function GoogleMeetConnectionPanel() {
  const bn = useLocale() === 'bn'
  const qc = useQueryClient()
  const ownerId = useAuthStore(state => state.user?.id)
  const callbackResult = useSyncExternalStore(subscribeLocation,
    () => new URLSearchParams(window.location.search).get('googleMeet'), () => null)
  const connection = useQuery({ queryKey: ['google-meet-connection', ownerId], queryFn: recordingService.connection, enabled: Boolean(ownerId), retry: false })
  const authorize = useMutation({ mutationFn: recordingService.authorize,
    onSuccess: result => { window.location.assign(result.authorizationUrl) } })
  const disconnect = useMutation({ mutationFn: recordingService.disconnect,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['google-meet-connection'] }) })
  const failure = authorize.error ?? disconnect.error ?? connection.error
  return <section className='rounded-xl border border-slate-200 bg-white p-5' aria-label={bn ? 'Google সংযোগ' : 'Google connection'}>
    <div className='flex flex-wrap items-start justify-between gap-4'>
      <div className='max-w-prose space-y-1'>
        <h2 className='text-lg font-medium'>{bn ? 'ক্লাসের জন্য Google অ্যাকাউন্ট' : 'Google account for classes'}</h2>
        <p className='text-sm leading-relaxed text-slate-600'>
          {connection.data?.sharedOrganizerEnabled
            ? (bn ? 'প্রতিষ্ঠানের অ্যাকাউন্ট মিটিং তৈরি করে। কোর্সের শিক্ষকরা কো-হোস্ট হন।' : 'The institute account creates meetings. Course teachers are added as co-hosts.')
            : (bn ? 'কোর্সের প্রধান শিক্ষক নিজের Google অ্যাকাউন্ট যুক্ত করলে সেই অ্যাকাউন্টে মিটিং তৈরি হবে।' : 'The course’s primary teacher connects their own Google account to host its meetings.')}
        </p>
        {connection.isPending ? <p className='text-sm text-slate-600' aria-busy='true'>{bn ? 'সংযোগ পরীক্ষা হচ্ছে…' : 'Checking connection…'}</p>
          : connection.data?.connected && <p className='text-sm font-medium text-green-800'>{bn ? 'সংযুক্ত:' : 'Connected:'} {connection.data.email}</p>}
        {connection.data?.calendarSyncEnabled && <p className='text-sm text-slate-600'>{bn ? 'ক্যালেন্ডারে ক্লাস যোগ হবে এবং অন্য শিক্ষকরা আমন্ত্রণ পাবেন। আমন্ত্রণ কো-হোস্ট অনুমতি নয়।' : 'Classes appear on the host’s Calendar; other teachers receive invitations. A Calendar invitation does not grant co-host permissions.'}</p>}
      </div>
      <div className='flex flex-wrap gap-2'>
        <button type='button' onClick={() => authorize.mutate()} disabled={authorize.isPending || connection.isPending}
          className='min-h-11 rounded-md bg-[#1c3873] px-4 py-2 text-sm font-medium text-white hover:bg-[#0f1f3f] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:opacity-50'>
          {authorize.isPending ? (bn ? 'খুলছে…' : 'Opening…') : connection.data?.connected ? (bn ? 'Google পুনরায় যুক্ত করুন' : 'Reconnect Google') : (bn ? 'Google যুক্ত করুন' : 'Connect Google')}
        </button>
        {connection.data?.connected && <button type='button' disabled={disconnect.isPending} onClick={() => {
          if (window.confirm(bn ? 'সংযোগ বিচ্ছিন্ন করলে এই অ্যাকাউন্টের মিটিং পরিচালনা করতে আবার সংযোগ দিতে হবে। চালিয়ে যাবেন?' : 'Disconnecting prevents the LMS managing meetings owned by this account until you reconnect. Continue?')) disconnect.mutate()
        }} className='min-h-11 rounded-md border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-blue-600 disabled:opacity-50'>
          {bn ? 'বিচ্ছিন্ন করুন' : 'Disconnect'}
        </button>}
      </div>
    </div>
    {callbackResult === 'failed' && <p role='alert' className='mt-3 text-sm text-red-700'>{bn ? 'Google সংযোগ সম্পূর্ণ হয়নি। আবার সংযোগ দিন এবং অনুমতিগুলো গ্রহণ করুন।' : 'Google connection was not completed. Reconnect and approve the requested permissions.'}</p>}
    {failure && <p role='alert' className='mt-3 text-sm text-red-700'>{message(failure)} <button type='button' onClick={() => void connection.refetch()} className='underline underline-offset-4'>{bn ? 'আবার চেষ্টা করুন' : 'Retry'}</button></p>}
  </section>
}
