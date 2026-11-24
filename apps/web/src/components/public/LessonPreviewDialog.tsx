'use client'

import { useTranslations } from 'next-intl'
import { VideoOff } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { getLessonEmbedUrl } from '@/lib/video-embed'
import type { components } from '@/types/api'

type Lesson = components['schemas']['LessonSummaryResponse']

interface LessonPreviewDialogProps {
  /** The lesson being previewed, or null when the dialog is closed. */
  lesson: Lesson | null
  onClose: () => void
}

export function LessonPreviewDialog({ lesson, onClose }: LessonPreviewDialogProps) {
  const t = useTranslations('courses.detail.curriculumTitle')
  const embedUrl = getLessonEmbedUrl(lesson?.video)
  const title = lesson?.title ?? t('preview')

  return (
    <Dialog
      open={lesson !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle className="pr-8 text-[#1c3873]">{title}</DialogTitle>
        </DialogHeader>

        {embedUrl ? (
          <div className="aspect-video w-full overflow-hidden rounded-lg bg-black">
            <iframe
              src={embedUrl}
              title={title}
              className="h-full w-full"
              allowFullScreen
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              referrerPolicy="strict-origin-when-cross-origin"
            />
          </div>
        ) : (
          <div
            role="status"
            className="flex aspect-video w-full flex-col items-center justify-center gap-3 rounded-lg bg-[#f5f5f5] px-6 text-center"
          >
            <VideoOff className="h-10 w-10 text-[#a3a3a3]" aria-hidden="true" />
            <p className="font-sans text-sm text-[#5e5f5c]">{t('previewUnavailable')}</p>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
