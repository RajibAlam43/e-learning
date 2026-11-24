'use client'

import { Suspense, useState, useCallback, useEffect, useRef } from 'react'
import { useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import { Mail, Phone, Lock, Eye, EyeOff } from 'lucide-react'
import { AuthShell } from '@/components/auth/AuthShell'
import { useGoogleLogin, useLogin } from '@/hooks/useAuth'
import { ROUTES } from '@/constants/routes'
import { safeRedirectPath } from '@/lib/navigation'
import { startGoogleSignIn } from '@/lib/google-signin'

type LoginChannel = 'EMAIL' | 'PHONE'

function LoginForm() {
  const t = useTranslations('auth.login')
  const searchParams = useSearchParams()
  const [channel, setChannel] = useState<LoginChannel>('EMAIL')
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const getRedirectUrl = useCallback(() => {
    const redirect = searchParams.get('redirect')
    return safeRedirectPath(redirect) ?? ROUTES.student.dashboard
  }, [searchParams])

  const loginMutation = useLogin(getRedirectUrl())
  const googleMutation = useGoogleLogin(getRedirectUrl())
  const [googleError, setGoogleError] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!identifier.trim() || !password.trim()) return
    loginMutation.mutate({ channel, identifier: identifier.trim(), password })
  }

  const allFilled = identifier.trim() !== '' && password.trim() !== ''

  const [googleLoading, setGoogleLoading] = useState(false)
  const googleController = useRef<AbortController | null>(null)
  useEffect(() => () => googleController.current?.abort(), [])
  const handleGoogleLogin = useCallback(async () => {
    setGoogleError('')
    setGoogleLoading(true)
    googleController.current?.abort()
    const controller = new AbortController()
    googleController.current = controller
    try {
      await startGoogleSignIn(credential => googleMutation.mutate({ credential }), controller.signal)
    } catch (error) {
      setGoogleError(error instanceof Error && error.message === 'GOOGLE_NOT_CONFIGURED' ? t('googleNotConfigured') : t('googleFailed'))
    } finally {
      setGoogleLoading(false)
    }
  }, [googleMutation, t])

  return (
    <AuthShell showFooter={false}>
      <form onSubmit={handleSubmit} className="flex w-full flex-col items-start gap-6">
        <div className="flex w-full flex-col items-start gap-1">
          <h3 className="m-0 w-full font-heading text-2xl font-semibold leading-8 text-[#1c3873] max-md:text-xl max-md:leading-7">
            {t('title')}
          </h3>
          <p className="w-full text-sm leading-[150%] text-[#5e5f5c]">{t('subtitle')}</p>
        </div>

        <div className="flex w-full gap-6">
          <button type="button" onClick={() => { setChannel('EMAIL'); setIdentifier('') }} className="flex flex-1 flex-col items-center gap-2">
            <span className={`text-sm leading-5 ${channel === 'EMAIL' ? 'font-semibold text-[#1c3873]' : 'leading-[150%] text-[#5e5f5c]'}`}>{t('loginWithEmail')}</span>
            <div className={`h-px w-full ${channel === 'EMAIL' ? 'border-t border-[#1c3873]' : 'border-t border-[#d4d4d4]'}`} />
          </button>
          <button type="button" onClick={() => { setChannel('PHONE'); setIdentifier('') }} className="flex flex-1 flex-col items-center gap-2">
            <span className={`text-sm leading-5 ${channel === 'PHONE' ? 'font-semibold text-[#1c3873]' : 'leading-[150%] text-[#5e5f5c]'}`}>{t('loginWithPhone')}</span>
            <div className={`h-px w-full ${channel === 'PHONE' ? 'border-t border-[#1c3873]' : 'border-t border-[#d4d4d4]'}`} />
          </button>
        </div>

        <div className="flex w-full flex-col items-start gap-4">
          <div className="flex w-full flex-col items-start gap-1">
            <div className="w-full flex items-center shrink-0">
              <span className="flex-1 text-sm font-semibold leading-5 text-[#1b1c1a]">{channel === 'EMAIL' ? t('email') : t('phone')}</span>
            </div>
            <div className="flex h-11 w-full shrink-0 items-center gap-2 rounded-sm border border-[#e5e7eb] bg-white px-3 py-2 focus-within:border-[#1c3873] transition-all duration-200">
              <span className="flex h-[22px] w-[22px] shrink-0 items-center text-[#667085]">
                {channel === 'EMAIL' ? <Mail className="h-[22px] w-[22px]" /> : <Phone className="h-[22px] w-[22px]" />}
              </span>
              <input
                type={channel === 'EMAIL' ? 'email' : 'tel'}
                aria-label={channel === 'EMAIL' ? t('email') : t('phone')}
                placeholder={channel === 'EMAIL' ? 'your@email.com' : '+880 XXX XXX XXXX'}
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                className="flex-1 border-none bg-transparent font-sans text-sm leading-[150%] text-[#5e5f5c] outline-none"
                autoComplete={channel === 'EMAIL' ? 'email' : 'tel'}
              />
            </div>
          </div>

          <div className="flex w-full flex-col items-start gap-1">
            <div className="flex w-full items-center justify-between">
              <span className="flex-1 text-sm font-semibold leading-5 text-[#1b1c1a]">{t('password')}</span>
              <Link href={ROUTES.forgotPassword} className="text-sm leading-[150%] text-[#1c3873]">{t('forgotPassword')}</Link>
            </div>
            <div className="flex h-11 w-full shrink-0 items-center gap-2 rounded-sm border border-[#e5e7eb] bg-white px-[11px] py-2 focus-within:border-[#1c3873] transition-all duration-200">
              <span className="flex h-[22px] w-[22px] shrink-0 items-center text-[#667085]"><Lock className="h-[22px] w-[22px]" /></span>
              <input
                type={showPassword ? 'text' : 'password'}
                aria-label={t('password')}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="flex-1 border-none bg-transparent font-sans text-sm leading-[150%] text-[#71717a] outline-none"
                autoComplete="current-password"
              />
              <button type="button" onClick={() => setShowPassword(!showPassword)} className="flex h-[22px] w-[22px] shrink-0 cursor-pointer items-center border-none bg-transparent p-0 text-[#667085]" aria-label={showPassword ? t('hidePassword') : t('showPassword')}>
                {showPassword ? <EyeOff className="h-[22px] w-[22px]" /> : <Eye className="h-[22px] w-[22px]" />}
              </button>
            </div>
          </div>
        </div>

        <div className="flex w-full flex-col items-start gap-3">
          <button type="submit" disabled={!allFilled || loginMutation.isPending} className={`flex h-11 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 text-base font-semibold leading-6 text-white transition-all duration-200 ${allFilled && !loginMutation.isPending ? 'btn-blue-gradient hover:opacity-90' : 'cursor-not-allowed bg-[#d0d5dd]'}`}>
            {loginMutation.isPending ? t('signingIn') : t('submit')}
          </button>

          <button type="button" onClick={handleGoogleLogin} disabled={googleLoading || googleMutation.isPending} className="flex h-11 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-none bg-white px-5 py-2.5 text-base font-semibold leading-6 text-[#1d4ed8] shadow-[0px_1px_0px_rgba(255,255,255,0.8)_inset,_0px_1px_2px_rgba(0,0,0,0.05),_0px_3px_8px_rgba(0,0,0,0.08)] transition-all duration-200 hover:bg-[#f9fafb] disabled:opacity-50">
            <svg viewBox="0 0 24 24" className="h-6 w-6">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            {googleLoading || googleMutation.isPending ? t('signingIn') : t('continueWithGoogle')}
          </button>

          {loginMutation.isError && (
            <div className="w-full rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-600">
              {loginMutation.error?.message || t('loginFailed')}
            </div>
          )}
          {(googleError || googleMutation.isError) && <div className="w-full rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-600">{googleError || googleMutation.error?.message || t('googleFailed')}</div>}
        </div>

        <div className="w-full text-center text-sm text-[#5e5f5c]">
          <span className="font-sans leading-5">{t('noAccount')} </span>
          <Link href={ROUTES.signup} className="font-sans text-sm font-semibold leading-5 text-[#1c3873] hover:underline">{t('signupLink')}</Link>
        </div>
      </form>
    </AuthShell>
  )
}

export default function LoginClient() {
  return (
    <Suspense fallback={
      <div className="flex w-full items-center justify-center overflow-auto bg-[#e8ebf1] p-4 md:p-5">
        <div className="flex w-full max-w-[562px] items-center justify-center rounded-3xl bg-white px-16 py-10">
          <div className="flex flex-col items-center gap-6">
            <div className="h-14 w-14 animate-pulse rounded-xl bg-[#f3f4f6]" />
            <div className="h-6 w-48 animate-pulse rounded bg-[#f3f4f6]" />
          </div>
        </div>
      </div>
    }>
      <LoginForm />
    </Suspense>
  )
}
