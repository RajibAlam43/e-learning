'use client'

import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { apiClient, getAccessToken, getSessionGeneration, refreshAccessToken } from '@/lib/api-client'
import { normalizeAuthUser } from '@/lib/auth-user'
import { QUERY_KEYS } from '@/constants/queryKeys'
import type { ApiError, UserResponse } from '@/types'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { setUser, logout, setSessionReady, setSessionError } = useAuthStore()
  const hasHydrated = useAuthStore(state => state.hasHydrated)
  const queryClient = useQueryClient()

  useEffect(() => {
    const handleSessionExpired = () => {
      logout()
      queryClient.clear()
    }
    window.addEventListener('auth-session-expired', handleSessionExpired)
    return () => window.removeEventListener('auth-session-expired', handleSessionExpired)
  }, [logout, queryClient])

  useEffect(() => {
    if (!hasHydrated) return
    let disposed = false
    let controller: AbortController | null = null

    async function fetchUser() {
      controller?.abort()
      const request = new AbortController()
      controller = request
      const generation = getSessionGeneration()
      const isCurrent = () => !disposed && !request.signal.aborted && generation === getSessionGeneration()
      const { user, isSessionReady } = useAuthStore.getState()
      if (isSessionReady && getAccessToken()) return
      const hasSessionMarker = document.cookie.split(';').some(cookie => cookie.trim() === 'session=1')
      if (!user && !hasSessionMarker) {
        setSessionError(false)
        setSessionReady(true)
        return
      }
      setSessionError(false)
      setSessionReady(false)
      try {
        const token = await refreshAccessToken()
        if (!isCurrent()) return
        if (!token) throw { status: 401 }
        const { data } = await apiClient.get<UserResponse>('/me', { signal: request.signal })
        if (!isCurrent()) return
        const profile = normalizeAuthUser(data)
        if (!profile) throw new Error('Invalid session profile.')
        setUser(profile)
        queryClient.setQueryData(QUERY_KEYS.user(), data)
        setSessionReady(true)
      } catch (error) {
        if (!isCurrent()) return
        const status = (error as ApiError)?.status
        if (status === 401 || status === 403) {
          logout()
          queryClient.clear()
        } else {
          setSessionError(true)
        }
      }
    }

    const retry = () => { void fetchUser() }
    window.addEventListener('auth-session-retry', retry)
    void fetchUser()
    return () => {
      disposed = true
      controller?.abort()
      window.removeEventListener('auth-session-retry', retry)
    }
  }, [hasHydrated, setUser, logout, queryClient, setSessionReady, setSessionError])

  return <>{children}</>
}
