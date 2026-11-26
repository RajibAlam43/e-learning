'use client'

import { useLocale } from 'next-intl'

export function RecordClassLink({ classId, meetingUrl }: { classId?: string; meetingUrl?: string }) {
  const bn = useLocale() === 'bn'
  if (!classId || !meetingUrl?.startsWith('https://meet.google.com/')) return null

  // Load the document so the recorder route's capture Permissions-Policy takes effect.
  return <a href={`/instructor/live-classes/${classId}/record`}
    className='flex min-h-11 items-center justify-center rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-[#1c3873] hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-blue-600'>
    {bn ? 'ক্লাস রেকর্ড করুন' : 'Record class'}
  </a>
}
