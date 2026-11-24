'use client'

import { useEffect, useRef, type RefObject } from 'react'
import { useSaveLessonProgress } from '@/hooks/useLearning'

/** Resume native/HLS media and serialize saves so older requests cannot win. */
export function useVideoProgress(videoRef: RefObject<HTMLVideoElement | null>, courseId?: string, lessonId?: string, initialPosition = 0) {
  const progress = useSaveLessonProgress(courseId)
  const saveRef = useRef(progress.mutateAsync)
  useEffect(() => { saveRef.current = progress.mutateAsync }, [progress.mutateAsync])

  useEffect(() => {
    const video = videoRef.current
    if (!video || !courseId || !lessonId) return
    let lastPosition = initialPosition
    let lastSaved = initialPosition
    let lastRequestAt = Date.now()
    let pending: number | null = null
    let saving = false
    let resumed = false

    async function flush() {
      if (saving || pending === null) return
      saving = true
      while (pending !== null) {
        const position: number = pending
        pending = null
        try {
          await saveRef.current({ lessonId: lessonId!, lastPositionSec: position })
          lastSaved = position
        } catch {
          // Retry on the next playback/pause event; keep the last confirmed position.
        }
      }
      saving = false
    }

    function save() {
      if (!resumed || !Number.isFinite(lastPosition) || lastPosition === lastSaved) return
      pending = Math.max(0, Math.floor(lastPosition))
      lastRequestAt = Date.now()
      void flush()
    }
    function resume() {
      if (resumed) return
      resumed = true
      if (initialPosition > 0 && Number.isFinite(video!.duration) && initialPosition < video!.duration) {
        video!.currentTime = initialPosition
      }
    }
    function track() {
      if (!resumed || video!.readyState === 0) return
      lastPosition = Math.floor(video!.currentTime)
      if (Date.now() - lastRequestAt >= 15_000) save()
    }
    function pause() { track(); save() }
    function hidden() { if (document.visibilityState === 'hidden') pause() }
    video.addEventListener('loadedmetadata', resume)
    video.addEventListener('timeupdate', track)
    video.addEventListener('pause', pause)
    video.addEventListener('ended', pause)
    document.addEventListener('visibilitychange', hidden)
    if (video.readyState >= 1) resume()
    return () => {
      video.removeEventListener('loadedmetadata', resume)
      video.removeEventListener('timeupdate', track)
      video.removeEventListener('pause', pause)
      video.removeEventListener('ended', pause)
      document.removeEventListener('visibilitychange', hidden)
      save()
    }
    // A confirmed save updates the cache; it must not re-seek the playing video.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [videoRef, courseId, lessonId])

  return progress.isError
}
