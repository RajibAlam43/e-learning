import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import type { UserRole } from '@/types'
import {
  setAccessToken,
  clearAccessToken,
} from '@/lib/api-client'
import { clearAuthCookie } from '@/lib/auth-cookie'
import { del } from 'idb-keyval'
import { normalizeAuthUser } from '@/lib/auth-user'
import type { AuthUser } from '@/lib/auth-user'

export type { AuthUser } from '@/lib/auth-user'

// Persistence is optional: blocked storage, corrupt JSON or quota errors must
// never prevent the HttpOnly session from being restored.
const storage = createJSONStorage(() => ({
  getItem: (name: string) => {
    try {
      const value = localStorage.getItem(name)
      if (value) JSON.parse(value)
      return value
    } catch {
      try { localStorage.removeItem(name) } catch { /* Storage is unavailable. */ }
      return null
    }
  },
  setItem: (name: string, value: string) => {
    try { localStorage.setItem(name, value) } catch { /* Continue without persistence. */ }
  },
  removeItem: (name: string) => {
    try { localStorage.removeItem(name) } catch { /* Continue without persistence. */ }
  },
}))

interface AuthState {
  user: AuthUser | null
  isAuthenticated: boolean
  hasHydrated: boolean
  isSessionReady: boolean
  sessionError: boolean

  login: (accessToken: string, user: AuthUser) => void
  logout: () => void
  setUser: (user: AuthUser) => void
  setHasHydrated: (hasHydrated: boolean) => void
  setSessionReady: (isSessionReady: boolean) => void
  setSessionError: (sessionError: boolean) => void
  hasRole: (role: UserRole) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,
      hasHydrated: false,
      isSessionReady: false,
      sessionError: false,

      login: (accessToken, user) => {
        const profile = normalizeAuthUser(user)
        if (!profile) throw new Error('Invalid authentication profile.')
        setAccessToken(accessToken)
        set({ user: profile, isAuthenticated: true, isSessionReady: true, sessionError: false })
      },

      logout: () => {
        clearAccessToken()
        clearAuthCookie()
        del('lms-offline-cache').catch(console.error)
        set({ user: null, isAuthenticated: false, isSessionReady: true, sessionError: false })
      },

      setUser: (user) => {
        const profile = normalizeAuthUser(user)
        set({ user: profile, isAuthenticated: Boolean(profile) })
      },

      hasRole: (role) => {
        const { user } = get()
        return user?.roles?.includes(role) ?? false
      },
      setHasHydrated: (hasHydrated: boolean) => set({ hasHydrated }),
      setSessionReady: (isSessionReady: boolean) => set({ isSessionReady }),
      setSessionError: (sessionError: boolean) => set({ sessionError }),
    }),
    {
      name: 'lms-auth',
      storage,
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      merge: (persisted, current) => {
        const profile = normalizeAuthUser((persisted as { user?: unknown } | undefined)?.user)
        return { ...current, user: profile, isAuthenticated: Boolean(profile) }
      },
      onRehydrateStorage: () => state => { state?.setHasHydrated(true) },
    }
  )
)
