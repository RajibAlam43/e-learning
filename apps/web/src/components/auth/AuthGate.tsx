'use client'

import { useEffect } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/auth.store'
import { useTranslations } from 'next-intl'

export function AuthGate({ children, roles }: { children: React.ReactNode; roles?: string[] }) {
  const router = useRouter()
  const pathname = usePathname()
  const user = useAuthStore((state) => state.user)
  const isSessionReady = useAuthStore((state) => state.isSessionReady)
  const sessionError = useAuthStore((state) => state.sessionError)
  const t = useTranslations('auth.session')

  const userRoles = user?.roles
  const hasRequiredRole = isSessionReady && (!roles || roles.some(role => Array.isArray(userRoles) && userRoles.some(userRole => userRole === role)))

  useEffect(() => {
    if (!isSessionReady) return
    if (!user) {
      router.replace(`/login?redirect=${encodeURIComponent(pathname || '/')}`)
      return
    }
    if (!hasRequiredRole) {
      const destination = Array.isArray(userRoles) && userRoles.some((role) => ['ADMIN', 'SUPER_ADMIN'].includes(role))
        ? '/admin/dashboard'
        : Array.isArray(userRoles) && userRoles.some((role) => ['INSTRUCTOR', 'TEACHER'].includes(role))
          ? '/instructor/dashboard'
          : '/student/dashboard'
      router.replace(destination)
    }
  }, [hasRequiredRole, isSessionReady, pathname, router, user, userRoles])

  if (!isSessionReady && sessionError) {
    return <div role="alert" className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center"><p className="text-sm text-slate-700">{t('unavailable')}</p><button type="button" onClick={() => window.dispatchEvent(new Event('auth-session-retry'))} className="rounded-md bg-[#1c3873] px-4 py-2 text-sm font-medium text-white">{t('retry')}</button></div>
  }
  if (!isSessionReady || !user || !hasRequiredRole) {
    return <div className="min-h-[60vh] w-full animate-pulse bg-[#f5f9ff]" aria-busy="true" />
  }

  return <>{children}</>
}
