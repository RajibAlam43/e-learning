'use client'

import { useTranslations } from 'next-intl'

import { useState } from 'react'
import { Lock, VideoOff } from 'lucide-react'
import type { components } from '@/types/api'
import { getLessonEmbedUrl } from '@/lib/video-embed'
import { useHlsVideo, type VideoLoadError } from '@/hooks/useHlsVideo'

import { useVideoProgress } from '@/hooks/useVideoProgress'

type MediaPlayback = components['schemas']['MediaPlaybackResponse']

interface VideoPlayerProps {
  courseId?: string
  lessonId?: string
  initialPosition?: number
  playback: MediaPlayback | undefined
  thumbnailUrl?: string
  courseLabel?: string
  isAccessible?: boolean
  isLoading: boolean
  isExpanded?: boolean
  onToggleExpand?: () => void
}

export default function VideoPlayer({ playback, isAccessible = true, isLoading, courseId, lessonId, initialPosition }: VideoPlayerProps) {
  const ui = useTranslations('privateUi')

  if (isLoading) return <div className='aspect-video w-full animate-pulse rounded-2xl bg-gray-100' />

  if (!isAccessible) {
    return (
      <div className='flex aspect-video w-full flex-col items-center justify-center gap-3 rounded-2xl bg-gray-900'>
        <Lock className='h-10 w-10 text-white/50' />
        <p className='text-sm text-white/60'>{ui('thisLessonIsLocked')}</p>
      </div>
    )
  }

  // Signed Mux playback may deliver the JWT in a separate `token` field —
  // stream.mux.com URLs reject requests without it (manifest returns 403).
  const rawSrc = playback?.hlsUrl ?? playback?.playbackUrl
  const videoSrc = rawSrc ? withPlaybackToken(rawSrc, playback?.token) : undefined
  const youtubeEmbed = playback?.provider === 'YOUTUBE'
    ? getLessonEmbedUrl({ provider: 'YOUTUBE', sourceId: playback.playbackId ?? playback.playbackUrl ?? playback.embedUrl })
    : undefined
  const embedSrc = playback?.playbackMode === 'IFRAME'
    ? playback.embedUrl ?? youtubeEmbed
    : youtubeEmbed

  if (videoSrc || embedSrc) {
    return (
      <div className='relative aspect-video w-full overflow-hidden rounded-2xl bg-black'>
        {embedSrc ? (
          <iframe
            src={embedSrc}
            title={ui('lessonVideo')}
            className='h-full w-full'
            allowFullScreen
            allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture'
            referrerPolicy='strict-origin-when-cross-origin'
          />
        ) : (
          <HlsVideo key={`${lessonId}:${videoSrc}`} src={videoSrc} courseId={courseId} lessonId={lessonId} initialPosition={initialPosition} />
        )}
      </div>
    )
  }

  return (
    <div className='flex aspect-video w-full flex-col items-center justify-center gap-3 rounded-2xl bg-slate-100 text-center'>
      <VideoOff className='h-10 w-10 text-slate-400' />
      <p className='text-sm font-medium text-slate-600'>{ui('videoUnavailable')}</p>
      <p className='max-w-xs text-xs text-slate-500'>{ui('theCourseHasNotProvidedPlayableMediaForThisLessonYet')}</p>
    </div>
  )
}

/**
 * Native <video> element whose src is managed by useHlsVideo (handles .m3u8
 * via hls.js on browsers without native HLS). Must not set a src attribute.
 * Load failures are shown on the frame instead of failing silently.
 */
function HlsVideo({ src, courseId, lessonId, initialPosition }: { src: string | undefined } & Pick<VideoPlayerProps, 'courseId' | 'lessonId' | 'initialPosition'>) {
  const ui = useTranslations('privateUi')

  const [error, setError] = useState<VideoLoadError | null>(null)
  const videoRef = useHlsVideo(src, setError)
  const progressFailed = useVideoProgress(videoRef, courseId, lessonId, initialPosition)

  return (
    <div className='relative h-full w-full'>
      <video ref={videoRef} className='h-full w-full object-contain' controls controlsList='nodownload' preload='metadata' />
      {progressFailed && <p role='status' className='absolute top-2 left-2 rounded bg-black/80 px-3 py-1 text-xs text-white'>{ui('progressCouldNotBeSavedRetryingAsYouWatch')}</p>}
      {error && (
        <div className='pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-1 bg-black/80 p-4 text-center'>
          <p className='text-sm font-medium text-white'>{ui('videoFailedToLoad')}</p>
          <p className='text-xs text-white/70'>
            {error.code}
            {error.status ? ` (HTTP ${error.status})` : ''}
          </p>
        </div>
      )}
    </div>
  )
}

/** Append the signed-playback JWT unless the URL already carries it. */
function withPlaybackToken(url: string, token: string | undefined): string {
  if (!token || /[?&]token=/.test(url)) return url
  return `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`
}
