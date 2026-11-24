'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import { X } from 'lucide-react'
import type { AdminCourseSection, CreateLiveClassItemRequest } from '@/types'
import { useCreateLiveClassItem } from '@/hooks/useAdmin'

export function LiveClassItemDialog({ courseId, sections, onClose }: { courseId: string; sections: AdminCourseSection[]; onClose: () => void }) {
  const create = useCreateLiveClassItem()
  const [form, setForm] = useState<CreateLiveClassItemRequest>({
    sectionId: sections[0]?.sectionId ?? '',
    title: '',
    titleEn: '',
    expectedDurationMinutes: 60,
    isMandatory: true,
  })
  const [error, setError] = useState('')

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    if (!form.sectionId) {
      setError('Choose a section for this live class.')
      return
    }
    if (!form.title.trim()) {
      setError('Enter a live class title.')
      return
    }
    if (!form.expectedDurationMinutes || form.expectedDurationMinutes < 1) {
      setError('Expected duration must be at least 1 minute.')
      return
    }

    try {
      await create.mutateAsync({
        courseId,
        data: {
          ...form,
          title: form.title.trim(),
          titleEn: form.titleEn?.trim() || undefined,
        },
      })
      toast.success('Live class item created')
      onClose()
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Could not create live class item'
      setError(message)
      toast.error(message)
    }
  }

  return (
    <div role='dialog' aria-modal='true' aria-labelledby='add-live-class-title' className='fixed inset-0 z-50 grid place-items-center bg-black/40 p-4'>
      <form onSubmit={submit} className='w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl'>
        <div className='mb-5 flex items-center justify-between'>
          <h2 id='add-live-class-title' className='font-heading text-xl text-[#1c3873]'>Add live class</h2>
          <button type='button' onClick={onClose} aria-label='Close' className='rounded-lg p-2 text-slate-500 hover:bg-slate-100'>
            <X className='h-5 w-5' />
          </button>
        </div>

        {error && <div role='alert' className='mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800'>{error}</div>}

        <div className='grid gap-3'>
          <label className='grid gap-1 text-sm font-medium'>
            Section
            <select required value={form.sectionId} onChange={(event) => setForm({ ...form, sectionId: event.target.value })} className='h-10 rounded-lg border px-3'>
              {sections.map((section) => <option key={section.sectionId} value={section.sectionId}>{section.title ?? section.titleEn ?? 'Section'}</option>)}
            </select>
          </label>
          <label className='grid gap-1 text-sm font-medium'>
            Title (Bangla)
            <input required value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} className='h-10 rounded-lg border px-3' />
          </label>
          <label className='grid gap-1 text-sm font-medium'>
            Title (English)
            <input value={form.titleEn ?? ''} onChange={(event) => setForm({ ...form, titleEn: event.target.value })} className='h-10 rounded-lg border px-3' />
          </label>
          <label className='grid gap-1 text-sm font-medium'>
            Expected duration (minutes)
            <input required min={1} type='number' value={form.expectedDurationMinutes ?? 60} onChange={(event) => setForm({ ...form, expectedDurationMinutes: Math.max(1, Number(event.target.value) || 1) })} className='h-10 rounded-lg border px-3' />
          </label>
          <label className='flex items-center gap-2 text-sm'>
            <input type='checkbox' checked={form.isMandatory ?? true} onChange={(event) => setForm({ ...form, isMandatory: event.target.checked })} />
            Mandatory item
          </label>
        </div>

        <div className='mt-6 flex justify-end gap-3'>
          <button type='button' onClick={onClose} className='rounded-lg border px-4 py-2 text-sm'>Cancel</button>
          <button disabled={create.isPending} className='rounded-lg bg-[#1c3873] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50'>
            {create.isPending ? 'Creating...' : 'Create live class'}
          </button>
        </div>
      </form>
    </div>
  )
}
