'use client'

import { useTranslations } from 'next-intl'

import { EmptyState } from '@/components/shared/EmptyState'
import { Megaphone } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { useCreateInstructorAnnouncement, useInstructorDashboard } from '@/hooks/useInstructorDashboard'

export default function InstructorAnnouncementsPage() {
  const ui = useTranslations('privateUi')

  const dashboard = useInstructorDashboard()
  const create = useCreateInstructorAnnouncement()
  const [courseId, setCourseId] = useState('')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const courses = dashboard.data?.assignedCourses ?? []
  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!courseId || !title.trim() || !content.trim()) return
    try { await create.mutateAsync({ courseId, data: { title: title.trim(), content: content.trim() } }); setTitle(''); setContent(''); toast.success(ui('announcementPublished')) }
    catch (error) { toast.error((error as { message?: string }).message ?? ui('unableToPublishAnnouncement')) }
  }
  return (
    <div className='flex flex-col gap-6'>
      <div className='flex flex-col gap-1'>
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('announcements')}</h1>
        <p className='text-sm text-[#5e5f5c]'>{ui('courseAnnouncements')}</p>
      </div>

      {dashboard.isLoading ? <div className='h-40 animate-pulse rounded-2xl bg-white' /> : courses.length === 0 ? <EmptyState
        icon={<Megaphone className='h-10 w-10' />}
        title={ui('noCoursesAssigned')}
        description={ui('announcementsCanBePublishedAfterACourseIsAssignedToYou')}
      /> : <form onSubmit={submit} className='max-w-2xl rounded-2xl bg-white p-6 shadow-sm'><div className='grid gap-4'><label className='grid gap-1 text-sm font-medium'>{ui('course')}<select required value={courseId} onChange={e => setCourseId(e.target.value)} className='h-11 rounded-lg border px-3'><option value=''>{ui('selectACourse')}</option>{courses.map(course => <option key={course.courseId} value={course.courseId}>{course.courseName ?? course.courseSlug ?? course.courseId}</option>)}</select></label><label className='grid gap-1 text-sm font-medium'>{ui('title')}<input required value={title} onChange={e => setTitle(e.target.value)} className='h-11 rounded-lg border px-3' maxLength={200} /></label><label className='grid gap-1 text-sm font-medium'>{ui('message')}<textarea required value={content} onChange={e => setContent(e.target.value)} className='min-h-32 rounded-lg border px-3 py-2' maxLength={5000} /></label></div><button type='submit' disabled={create.isPending} className='mt-5 rounded-lg bg-[#1c3873] px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-50'>{create.isPending ? ui('publishing') : ui('publishAnnouncement')}</button></form>}
    </div>
  )
}
