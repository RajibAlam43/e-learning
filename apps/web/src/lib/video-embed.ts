import type { components } from '@/types/api'

type LessonVideo = components['schemas']['LessonVideoResponse']

const YOUTUBE_ID_PATTERN = /^[A-Za-z0-9_-]{11}$/

/** Return the canonical 11-character YouTube ID from a URL or ID. */
export function getYouTubeVideoId(value: string | null | undefined): string | null {
  const input = value?.trim()
  if (!input) return null
  if (YOUTUBE_ID_PATTERN.test(input)) return input

  try {
    const url = new URL(input)
    const host = url.hostname.toLowerCase().replace(/^www\./, '')
    if (!['youtube.com', 'm.youtube.com', 'youtube-nocookie.com', 'youtu.be'].includes(host)) return null

    const path = url.pathname.split('/').filter(Boolean)
    const candidate = host === 'youtu.be'
      ? path[0]
      : url.searchParams.get('v') ?? (['embed', 'shorts', 'live', 'v'].includes(path[0] ?? '') ? path[1] : undefined)
    return candidate && YOUTUBE_ID_PATTERN.test(candidate) ? candidate : null
  } catch {
    return null
  }
}

export function getYouTubeWatchUrl(value: string | null | undefined): string {
  const id = getYouTubeVideoId(value)
  return id ? `https://youtu.be/${id}` : ''
}

/**
 * Public course pages only receive `{ provider, sourceId }` for a lesson.
 * The signed playback endpoint (`GET /learn/lessons/{id}/playback`) requires a
 * bearer token, so an anonymous free-preview has to be rendered from the
 * provider's own public embed URL instead.
 *
 * BUNNY is deliberately unsupported: its embed URL needs a library id that the
 * public payload does not expose, and guessing one would produce a broken
 * player rather than an honest "unavailable" state.
 */
export function getLessonEmbedUrl(video: LessonVideo | undefined): string | null {
  const sourceId = video?.sourceId?.trim()
  if (!sourceId) return null

  const sourceUrl = safeProviderUrl(sourceId, video?.provider)
  if (sourceUrl) return sourceUrl

  const id = encodeURIComponent(sourceId)

  switch (video?.provider) {
    case 'YOUTUBE': {
      const youtubeId = getYouTubeVideoId(sourceId)
      return youtubeId ? `https://www.youtube-nocookie.com/embed/${encodeURIComponent(youtubeId)}?rel=0&modestbranding=1` : null
    }
    case 'MUX':
      return `https://player.mux.com/${id}`
    default:
      return null
  }
}

/**
 * Some API versions return the provider URL rather than only its identifier.
 * Preserve those URLs only for known provider hosts; never turn arbitrary API
 * data into an iframe source.
 */
function safeProviderUrl(sourceId: string, provider: LessonVideo['provider']): string | null {
  if (!/^https?:\/\//i.test(sourceId)) return null

  try {
    const url = new URL(sourceId)
    const host = url.hostname.toLowerCase()
    if (provider === 'MUX') {
      if (host === 'player.mux.com') return url.toString()
      if (host === 'stream.mux.com') {
        const id = url.pathname.split('/').filter(Boolean).at(-1)?.replace(/\.m3u8$/i, '')
        return id ? `https://player.mux.com/${encodeURIComponent(id)}${url.search}` : null
      }
      return null
    }
    if (provider === 'YOUTUBE') {
      const id = getYouTubeVideoId(sourceId)
      return id ? `https://www.youtube-nocookie.com/embed/${encodeURIComponent(id)}?rel=0&modestbranding=1` : null
    }
    return null
  } catch {
    return null
  }
}

/**
 * Formats a lesson length as `m:ss` (or `h:mm:ss`). Digit-only output is used
 * deliberately so the label reads correctly in both locales without pinning a
 * number format to en-US.
 */
export function formatLessonDuration(totalSeconds: number | undefined): string | null {
  if (typeof totalSeconds !== 'number' || !Number.isFinite(totalSeconds) || totalSeconds <= 0) {
    return null
  }

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = Math.floor(totalSeconds % 60)
  const paddedSeconds = String(seconds).padStart(2, '0')

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${paddedSeconds}`
  }

  return `${minutes}:${paddedSeconds}`
}
