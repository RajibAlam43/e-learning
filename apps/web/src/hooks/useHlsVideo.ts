'use client'

import { useEffect, useRef } from 'react'
import Hls from 'hls.js'
import type { ErrorData } from 'hls.js'

const HLS_MIME = 'application/vnd.apple.mpegurl'

export interface VideoLoadError {
  /** Where the failure originated. */
  source: 'native' | 'hls.js'
  /** Machine-readable reason (hls.js details or MediaError code). */
  code: string
  /** HTTP status when the failure was a network response. */
  status?: number
}

const NATIVE_ERROR_NAMES: Record<number, string> = {
  1: 'MEDIA_ERR_ABORTED',
  2: 'MEDIA_ERR_NETWORK',
  3: 'MEDIA_ERR_DECODE',
  4: 'MEDIA_ERR_SRC_NOT_SUPPORTED',
}

/**
 * Desktop Chrome/Edge/Firefox cannot play HLS (.m3u8 — the format Mux serves)
 * in a plain <video src>; only Safari supports it natively. Attach hls.js via
 * MSE for manifests; keep native playback for direct files (mp4) and Safari.
 *
 * The hook owns the src: the <video> element must NOT set a src attribute.
 * Failures are reported through `onError` — element errors (native path) and
 * fatal hls.js errors (manifest/segment fetch, MSE issues).
 *
 * hls.js is imported statically on purpose: it is required for the platform's
 * core feature (Mux lessons) and a failed dynamic import previously degraded
 * to an unplayable native manifest.
 */
export function useHlsVideo(src: string | undefined, onError?: (error: VideoLoadError) => void) {
  const videoRef = useRef<HTMLVideoElement>(null)
  // Keep the callback fresh without re-running the playback effect on every render.
  const onErrorRef = useRef(onError)
  useEffect(() => {
    onErrorRef.current = onError
  }, [onError])

  useEffect(() => {
    const video = videoRef.current
    if (!video || !src) return

    let hls: Hls | null = null

    const reportNativeError = () => {
      const code = video.error?.code
      onErrorRef.current?.({
        source: 'native',
        code: NATIVE_ERROR_NAMES[code ?? -1] ?? `MEDIA_ERROR_${code ?? 'UNKNOWN'}`,
      })
    }
    video.addEventListener('error', reportNativeError)

    const isHlsManifest = /\.m3u8($|\?)/i.test(src)
    const supportsNativeHls = !!video.canPlayType(HLS_MIME)

    if (!isHlsManifest) {
      // Direct file (mp4) — every browser handles this natively.
      video.src = src
    } else if (Hls.isSupported()) {
      hls = new Hls()
      hls.on(Hls.Events.ERROR, (_event, data: ErrorData) => {
        if (!data.fatal) return
        onErrorRef.current?.({
          source: 'hls.js',
          code: data.details,
          status: data.response?.code,
        })
      })
      hls.loadSource(src)
      hls.attachMedia(video)
    } else if (supportsNativeHls) {
      // Safari / iOS — native HLS playback.
      video.src = src
    } else {
      // No MSE (e.g. insecure origin) and no native HLS: nothing can play it.
      onErrorRef.current?.({ source: 'hls.js', code: 'HLS_NO_PLAYBACK_SUPPORT' })
    }

    return () => {
      hls?.destroy()
      video.removeEventListener('error', reportNativeError)
      video.removeAttribute('src')
      video.load()
    }
  }, [src])

  return videoRef
}
