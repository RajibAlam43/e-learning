'use client'

import axios from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { authService } from '@/services/auth.service'
import { useAuthStore } from '@/store/auth.store'
import { QUERY_KEYS } from '@/constants/queryKeys'
import { ROUTES } from '@/constants/routes'
import { getSessionGeneration } from '@/lib/api-client'
import { setAuthCookie } from '@/lib/auth-cookie'
import { normalizeAuthUser } from '@/lib/auth-user'
import type { AuthUser } from '@/lib/auth-user'
import { safeRedirectPath } from '@/lib/navigation'
import type {
  AuthResponse, LoginRequest, RegisterRequest, ForgotPasswordRequest,
  ResetPasswordRequest, SendVerificationRequest, GoogleAuthRequest,
} from '@/types'

interface SessionContext { generation: number }

function destination(user: AuthUser, redirectTo?: string): string {
  const path = safeRedirectPath(redirectTo)
  if (path && path !== ROUTES.student.dashboard) return path
  if (user.roles.some(role => role === 'ADMIN' || role === 'SUPER_ADMIN')) return ROUTES.admin.dashboard
  if (user.roles.some(role => role === 'INSTRUCTOR' || role === 'TEACHER')) return ROUTES.instructor.dashboard
  return ROUTES.student.dashboard
}

function useSessionActions() {
  const { login, logout } = useAuthStore()
  const queryClient = useQueryClient()
  const router = useRouter()
  return {
    router,
    begin: async (): Promise<SessionContext> => {
      logout()
      const generation = getSessionGeneration()
      await queryClient.cancelQueries()
      if (generation !== getSessionGeneration()) throw new axios.CanceledError('Session changed')
      queryClient.clear()
      return { generation }
    },
    accept: (data: AuthResponse, context?: SessionContext): AuthUser | null => {
      if (!context || context.generation !== getSessionGeneration() || !data.accessToken) return null
      const user = normalizeAuthUser(data)
      if (!user) throw new Error('Invalid authentication profile.')
      login(data.accessToken, user)
      setAuthCookie(data.accessToken, user.roles)
      return user
    },
    isCurrent: (context?: SessionContext) => Boolean(context && context.generation === getSessionGeneration()),
  }
}

export function useLogin(redirectTo?: string) {
  const session = useSessionActions()
  return useMutation({
    mutationFn: (data: LoginRequest) => authService.login(data),
    onMutate: session.begin,
    onSuccess: (data, variables, context) => {
      const user = session.accept(data, context)
      if (user) {
        session.router.push(destination(user, redirectTo))
      } else if (session.isCurrent(context) && data.isVerified === false) {
        session.router.push(`${ROUTES.verifyAccount}?channel=${variables.channel}&identifier=${encodeURIComponent(variables.identifier)}`)
      }
    },
  })
}

export function useGoogleLogin(redirectTo?: string) {
  const session = useSessionActions()
  return useMutation({
    mutationFn: (data: GoogleAuthRequest) => authService.google(data),
    onMutate: session.begin,
    onSuccess: (data, _variables, context) => {
      const user = session.accept(data, context)
      if (user) session.router.push(destination(user, redirectTo))
    },
  })
}

export function useRegister() {
  return useMutation({ mutationFn: (data: RegisterRequest) => authService.register(data) })
}

export function useSendVerification() {
  return useMutation({ mutationFn: (data: SendVerificationRequest) => authService.sendVerification(data) })
}

export function useVerifyCode(redirectTo?: string) {
  const session = useSessionActions()
  return useMutation({
    mutationFn: (data: { channel: 'EMAIL' | 'PHONE'; identifier: string; code: string; purpose: string }) => authService.verifyCode(data),
    onMutate: session.begin,
    onSuccess: (data, _variables, context) => {
      const user = session.accept(data, context)
      if (user) session.router.push(destination(user, redirectTo))
    },
  })
}

export function useForgotPassword() {
  return useMutation({ mutationFn: (data: ForgotPasswordRequest) => authService.forgotPassword(data) })
}

export function useResetPassword() {
  const session = useSessionActions()
  return useMutation({
    mutationFn: (data: ResetPasswordRequest) => authService.resetPassword(data),
    onMutate: session.begin,
    onSuccess: (data, _variables, context) => {
      const user = session.accept(data, context)
      if (user) session.router.push(destination(user))
    },
  })
}

export function useLogout() {
  const session = useSessionActions()
  return useMutation({
    mutationFn: () => authService.logout(),
    onMutate: session.begin,
    onSettled: (_data, _error, _variables, context) => {
      if (session.isCurrent(context)) session.router.push(ROUTES.login)
    },
  })
}

export function useMe() {
  const ready = useAuthStore(state => state.isSessionReady && state.isAuthenticated)
  return useQuery({
    queryKey: QUERY_KEYS.user(),
    queryFn: ({ signal }) => authService.getMe(signal),
    enabled: ready,
    retry: false,
  })
}
