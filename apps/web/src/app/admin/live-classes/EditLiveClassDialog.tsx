'use client'

import { useState, useEffect, useMemo } from 'react'
import { toast } from 'sonner'
import { useUpdateLiveClass } from '@/hooks/useAdmin'
import type { AdminLiveClassSummary, UpdateLiveClassRequest } from '@/types'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectTrigger,
  SelectContent,
  SelectItem,
  SelectValue,
} from '@/components/ui/select'

interface EditLiveClassDialogProps {
  liveClass: AdminLiveClassSummary | null
  open: boolean
  onClose: () => void
}

const STATUS_OPTIONS = [
  { value: 'SCHEDULED', label: 'Scheduled' },
  { value: 'LIVE', label: 'Live' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

function toDateTimeLocal(iso: string | undefined): string {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    const offset = d.getTimezoneOffset()
    const local = new Date(d.getTime() - offset * 60000)
    return local.toISOString().slice(0, 16)
  } catch {
    return ''
  }
}

export function EditLiveClassDialog({ liveClass, open, onClose }: EditLiveClassDialogProps) {
  const updateMutation = useUpdateLiveClass()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [status, setStatus] = useState('SCHEDULED')
  const [submitError, setSubmitError] = useState('')

  // Snapshot initial values from the summary — used for dirty detection.
  // Note: summary has no `description` or `endsAt`, so those can't be
  // pre-filled (no detail endpoint exists for live classes).
  const initial = useMemo(() => ({
    title: liveClass?.title ?? '',
    description: '',
    startsAt: toDateTimeLocal(liveClass?.startsAt),
    endsAt: '',
    status: liveClass?.status ?? 'SCHEDULED',
  }), [liveClass])

  useEffect(() => {
    // Hydrate the edit form from the selected class.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setTitle(initial.title)
    setDescription(initial.description)
    setStartsAt(initial.startsAt)
    setEndsAt(initial.endsAt)
    setStatus(initial.status)
  }, [initial])

  const isDirty = useMemo(() => {
    return title !== initial.title ||
      description !== initial.description ||
      startsAt !== initial.startsAt ||
      endsAt !== initial.endsAt ||
      status !== initial.status
  }, [title, description, startsAt, endsAt, status, initial])

  const isSaving = updateMutation.isPending

  function handleOpenChange(next: boolean) {
    if (next) return
    if (isSaving) return
    if (isDirty) {
      const ok = window.confirm('You have unsaved changes. Discard them?')
      if (!ok) return
    }
    onClose()
  }

  function handleSave() {
    if (!liveClass?.liveClassId) return
    if (!title.trim()) {
      setSubmitError('Title is required.')
      return
    }
    if (startsAt && endsAt && new Date(endsAt) <= new Date(startsAt)) {
      setSubmitError('End time must be later than start time.')
      return
    }
    setSubmitError('')

    const data: UpdateLiveClassRequest = {
      title: title || undefined,
      description: description || undefined,
      startsAt: startsAt ? new Date(startsAt).toISOString() : undefined,
      endsAt: endsAt ? new Date(endsAt).toISOString() : undefined,
      status: status as UpdateLiveClassRequest['status'],
    }

    updateMutation.mutate(
      { id: liveClass.liveClassId, data },
      {
        onSuccess: () => {
          toast.success('Live class updated successfully')
          onClose()
        },
        onError: (err) => {
          const e = err as { message?: string; status?: number }
          setSubmitError(e?.message || 'Failed to update live class')
          toast.error(e?.message || 'Failed to update live class')
          if (e?.status === 422) {
            toast.warning('Some fields need attention. Check the form and try again.')
          }
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="font-heading text-[#1c3873]">
            Edit Live Class
          </DialogTitle>
        </DialogHeader>

        <div className="mt-4 space-y-4">
          {submitError && <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{submitError}</div>}
          <div className="space-y-1.5">
            <Label htmlFor="edit-title">Title</Label>
            <Input
              id="edit-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Class title"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="edit-description">Description</Label>
            <Textarea
              id="edit-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Class description"
              rows={3}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="edit-starts">Starts At</Label>
              <Input
                id="edit-starts"
                type="datetime-local"
                value={startsAt}
                onChange={(e) => setStartsAt(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="edit-ends">Ends At</Label>
              <Input
                id="edit-ends"
                type="datetime-local"
                value={endsAt}
                onChange={(e) => setEndsAt(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>Status</Label>
            <Select value={status} onValueChange={(v) => setStatus(v ?? 'SCHEDULED')}>
              <SelectTrigger>
                <SelectValue placeholder="Select status" />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={isSaving}>
            Cancel
          </Button>
          <Button
            className="btn-blue-gradient text-white"
            onClick={handleSave}
            disabled={isSaving || !isDirty}
          >
            {isSaving ? 'Saving…' : 'Save Changes'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
