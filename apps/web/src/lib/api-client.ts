import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from '@/types'
import { clearAuthCookie } from '@/lib/auth-cookie'
import { NETWORK } from '@/config/network.config'

interface SessionRequest extends InternalAxiosRequestConfig {
  _retry?: boolean
  _requiresAuth?: boolean
  _sessionGeneration?: number
  _releaseAuthRequest?: () => void
}

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  withCredentials: true,
  timeout: NETWORK.requestTimeoutMs,
  headers: { 'Content-Type': 'application/json' },
})

// Never adopt an independent API-origin cookie, which may belong to another account.
export const authClient = axios.create({
  baseURL: '',
  withCredentials: true,
  timeout: NETWORK.requestTimeoutMs,
  headers: { 'Content-Type': 'application/json' },
})

let currentLocale: 'en' | 'bn' = 'bn'
let accessToken: string | null = null
let sessionGeneration = 0
let sessionController = new AbortController()
let refreshEntry: {
  generation: number
  controller: AbortController
  promise: Promise<string | null>
} | null = null

export function setApiLocale(locale: string | undefined): void {
  if (locale === 'en' || locale === 'bn') currentLocale = locale
}

export function getApiLocale(): 'en' | 'bn' {
  if (typeof document !== 'undefined') {
    const locale = document.cookie.split(';').find(cookie => cookie.trim().startsWith('locale='))?.trim().slice(7)
    if (locale === 'en' || locale === 'bn') return locale
  }
  return currentLocale
}

export function getSessionGeneration(): number {
  return sessionGeneration
}

export function getSessionSignal(): AbortSignal {
  return sessionController.signal
}

export function getAccessToken(): string | null {
  return accessToken
}

function invalidateSessionRequests(): void {
  sessionGeneration++
  sessionController.abort()
  sessionController = new AbortController()
  const previous = refreshEntry
  refreshEntry = null
  previous?.controller.abort()
}

// Only explicit sign-in changes the generation; a same-session refresh does not.
export function setAccessToken(token: string): void {
  invalidateSessionRequests()
  accessToken = token
}

export function clearAccessToken(): void {
  invalidateSessionRequests()
  accessToken = null
}

function expireBrowserSession(): void {
  clearAccessToken()
  clearAuthCookie()
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('auth-session-expired'))
    window.location.replace(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
  }
}

// Serialize cookie-affecting responses so an earlier logout cannot clear the
// cookie established by a later login. Aborted refreshes also release the queue.
let authQueue: Promise<void> = Promise.resolve()
let activeAuthRelease: (() => void) | null = null
authClient.interceptors.request.use(async request => {
  const config = request as SessionRequest
  const previous = authQueue
  let release!: () => void
  authQueue = new Promise<void>(resolve => { release = resolve })
  await previous
  if (config.signal?.aborted) {
    release()
    throw new axios.CanceledError('Authentication request cancelled', config)
  }
  const releaseRequest = () => {
    if (activeAuthRelease === releaseRequest) activeAuthRelease = null
    release()
  }
  config._releaseAuthRequest = releaseRequest
  activeAuthRelease = releaseRequest
  config.headers['Accept-Language'] = getApiLocale()
  return config
})
authClient.interceptors.response.use(
  response => {
    (response.config as SessionRequest)._releaseAuthRequest?.()
    return response
  },
  error => {
    if (error.config) (error.config as SessionRequest)._releaseAuthRequest?.()
    else activeAuthRelease?.()
    return Promise.reject(axios.isCancel(error) ? error : toApiError(error))
  },
)

export function refreshAccessToken(): Promise<string | null> {
  if (refreshEntry?.generation === sessionGeneration) return refreshEntry.promise
  const generation = sessionGeneration
  const controller = new AbortController()
  const entry = { generation, controller, promise: Promise.resolve<string | null>(null) }
  entry.promise = authClient
    .post<{ accessToken?: unknown }>('/api/auth/refresh', undefined, { signal: controller.signal })
    .then(response => {
      if (generation !== sessionGeneration) return null
      const token = response.data?.accessToken
      if (typeof token !== 'string' || !token) {
        throw {
          message: 'Refresh response did not include an access token.',
          code: 'INVALID_REFRESH_RESPONSE',
          status: 401,
        } satisfies ApiError
      }
      accessToken = token
      return token
    })
    .catch((error: unknown) => {
      if (generation !== sessionGeneration) return null
      if (error && typeof error === 'object' && 'status' in error) throw error
      throw toApiError(error)
    })
    .finally(() => {
      if (refreshEntry === entry) refreshEntry = null
    })
  refreshEntry = entry
  return entry.promise
}

function requiresAuthentication(url?: string): boolean {
  if (!url) return false
  const path = url.startsWith('http') ? new URL(url).pathname : (url.split('?')[0] ?? '')
  return !path.startsWith('/public/')
}

apiClient.interceptors.request.use(request => {
  const config = request as SessionRequest
  config._requiresAuth = requiresAuthentication(config.url)
  if (config._requiresAuth) {
    config._sessionGeneration ??= sessionGeneration
    if (config._sessionGeneration !== sessionGeneration) {
      throw new axios.CanceledError('Session changed', config)
    }
  }
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  const isAdminPage = typeof window !== 'undefined' && window.location.pathname.startsWith('/admin')
  config.headers['Accept-Language'] = isAdminPage ? 'en' : config.headers['Accept-Language'] ?? getApiLocale()
  return config
})

apiClient.interceptors.response.use(
  response => {
    const config = response.config as SessionRequest
    if (config._requiresAuth && config._sessionGeneration !== sessionGeneration) {
      throw new axios.CanceledError('Session changed', config)
    }
    if (typeof response.data === 'string') {
      try { response.data = JSON.parse(response.data) } catch { /* Non-JSON response. */ }
    }
    return response
  },
  async error => {
    const original = error.config as SessionRequest | undefined
    if (axios.isCancel(error)) return Promise.reject(error)
    if (!original) return Promise.reject(toApiError(error))
    if (original._requiresAuth && original._sessionGeneration !== sessionGeneration) {
      return Promise.reject(new axios.CanceledError('Session changed', original))
    }
    const status = error.response?.status
    if (original._requiresAuth && (status === 401 || status === 403) && !original._retry) {
      original._retry = true
      try {
        const token = await refreshAccessToken()
        if (original._sessionGeneration !== sessionGeneration) {
          return Promise.reject(new axios.CanceledError('Session changed', original))
        }
        if (token) {
          original.headers.Authorization = `Bearer ${token}`
          return apiClient(original)
        }
        expireBrowserSession()
      } catch (refreshError) {
        const refreshStatus = (refreshError as ApiError)?.status
        if (original._sessionGeneration === sessionGeneration && (refreshStatus === 401 || refreshStatus === 403)) {
          expireBrowserSession()
        }
      }
    }
    return Promise.reject(toApiError(error))
  },
)

function toApiError(error: unknown): ApiError {
  const err = error as { response?: { data?: Record<string, unknown>; status?: number }; message?: string; code?: string }
  return {
    message: (err.response?.data?.detail as string) || (err.response?.data?.message as string) || err.message || 'Request failed',
    code: (err.response?.data?.code as string) || err.code || 'UNKNOWN_ERROR',
    status: err.response?.status || (err.code === 'ECONNABORTED' || err.code === 'ETIMEDOUT' ? 504 : 500),
  }
}
