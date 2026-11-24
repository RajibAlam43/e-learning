'use client'

import { useState, useEffect, useRef } from 'react'
import Link from 'next/link'
import Image from 'next/image'
import { useRouter } from 'next/navigation'
import { useTranslations, useLocale } from 'next-intl'
import { Search, Globe, ShoppingCart, Menu, X, LayoutDashboard, LogOut } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useLogout } from '@/hooks/useAuth'
import { ROUTES } from '@/constants/routes'
import { setLocaleCookie } from '@/lib/locale-cookie'
import { useDialogFocus } from '@/hooks/useDialogFocus'

const LOCALE_OPTIONS = [
  { code: 'en', label: 'En' },
  { code: 'bn', label: 'বাং' },
]

export function Navbar() {
  const t = useTranslations('nav')
  const locale = useLocale()
  const router = useRouter()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const [search, setSearch] = useState('')
  const { isAuthenticated, user, isSessionReady } = useAuthStore()
  const logoutMutation = useLogout()
  const [profileOpen, setProfileOpen] = useState(false)
  const ready = isSessionReady
  const profileRef = useRef<HTMLDivElement>(null)
  const navigationRef = useRef<HTMLDivElement>(null)
  useDialogFocus(mobileOpen, navigationRef, () => setMobileOpen(false))

  // Close profile dropdown on outside click
  useEffect(() => {
    if (!profileOpen) return
    const close = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setProfileOpen(false)
      }
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [profileOpen])

  const initials = user?.fullName
    ? user.fullName.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : '?'

  const switchLocale = (code: string) => {
    setLocaleCookie(code)
    router.refresh()
  }

  const submitSearch = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const query = search.trim()
    router.push(query ? `/courses?search=${encodeURIComponent(query)}` : '/courses')
    setMobileOpen(false)
  }

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 10)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileOpen])

  const navLinks = [
    { label: t('home'), href: ROUTES.home },
    { label: t('courses'), href: ROUTES.courses },
    { label: t('programs'), href: ROUTES.programs },
    { label: t('aboutUs'), href: '#about' },
    { label: t('contact'), href: ROUTES.contact },
  ]

  return (
    <>
      <header
        className={`sticky top-0 z-50 w-full bg-white transition-shadow duration-200 ${
          scrolled ? 'shadow-[0px_1px_3px_0px_rgba(0,0,0,0.08)]' : ''
        }`}
      >
        <div className="container mx-auto flex items-center justify-between px-5 py-2">
          {/* Left: Logo + Search */}
          <div className="flex items-center gap-4 min-w-0 md:gap-6">
            <Link
              href="/"
              className="flex items-center gap-2 shrink-0 transition-opacity duration-200 hover:opacity-80"
            >
              <Image
                src="/images/logo.svg"
                alt="GII Logo"
                width={83}
                height={50}
                className="h-auto shrink-0"
                sizes="83px"
                loading="eager"
              />
            </Link>

            <form
              role="search"
              action="/courses"
              method="get"
              onSubmit={submitSearch}
              className="flex items-center gap-2 rounded-xl border border-solid border-[var(--color-brand-primary-50)] p-2 transition-all duration-200 focus-within:border-[var(--color-secondary-blue-400)] focus-within:shadow-[0_0_0_3px_rgba(59,130,246,0.15)] w-[180px] sm:w-[200px] md:w-[200px] lg:w-[295px] max-md:flex-1"
            >
              <label htmlFor="course-search" className="sr-only">
                {t('searchCourses')}
              </label>
              <Search
                className="h-6 w-6 shrink-0 text-[var(--color-brand-primary-200)]"
                strokeWidth={1.5}
              />
              <input
                id="course-search"
                name="search"
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder={t('courses')}
                className="w-full bg-transparent font-sans text-sm leading-[150%] text-[var(--color-text-body-secondary)] placeholder:text-[var(--color-brand-primary-200)] outline-none"
              />
            </form>
          </div>

          {/* Middle: Nav links */}
          <nav aria-label="Primary" className="hidden items-center gap-6 xl:flex flex-row justify-center">
            {navLinks.map((link) => (
              <Link
                key={link.label}
                href={link.href}
                className="whitespace-nowrap font-sans text-sm leading-5 font-normal text-[var(--color-text-body-secondary)] transition-colors duration-200 hover:text-[#1d4ed8]"
              >
                {link.label}
              </Link>
            ))}
          </nav>

          {/* Right: Language + Cart + Auth */}
          <div className="hidden md:flex items-center justify-end gap-3 shrink-0">
            <button
              type="button"
              onClick={() => {
                const next = LOCALE_OPTIONS.find((l) => l.code !== locale) ?? { code: 'en', label: 'En' }
                switchLocale(next.code)
              }}
              className="hidden md:flex items-center gap-[3px] rounded-sm bg-[var(--color-secondary-blue-50)] p-2 font-sans text-base leading-none text-[#1d4ed8] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]/70 hover:text-[var(--color-secondary-blue-600)] cursor-pointer"
              aria-label="Change language"
            >
              <Globe className="h-5 w-5 text-[var(--color-secondary-blue-600)]" />
              <span>{LOCALE_OPTIONS.find((l) => l.code !== locale)?.label ?? 'En'}</span>
            </button>

            <button
              type="button"
              aria-label="Shopping cart"
              className="flex h-11 w-11 items-center justify-center rounded-sm transition-colors duration-200 hover:bg-[var(--color-neutral-100)] cursor-pointer"
            >
              <ShoppingCart
                className="h-5 w-5 text-[var(--color-secondary-blue-600)]"
                strokeWidth={1.5}
              />
            </button>

            {!ready ? (
              <div className="h-9 w-9 rounded-full bg-gray-200 animate-pulse" />
            ) : isAuthenticated && user ? (
              <div ref={profileRef} className="relative">
                <button
                  type="button"
                  onClick={() => setProfileOpen(!profileOpen)}
                  aria-label="Open account menu"
                  aria-expanded={profileOpen}
                  aria-controls="account-menu"
                  className="flex h-9 w-9 items-center justify-center rounded-full bg-[var(--color-secondary-blue-600)] text-sm font-semibold text-white transition-opacity duration-200 hover:opacity-90 cursor-pointer"
                >
                  {initials}
                </button>

                {profileOpen && (
                  <div id="account-menu" className="absolute right-0 top-full z-50 mt-2 w-64 overflow-hidden rounded-xl border border-[#e5e7eb] bg-white shadow-lg">
                    {/* User info */}
                    <div className="border-b border-[#e5e7eb] px-4 py-3">
                      <p className="m-0 text-sm font-semibold text-[#1b1c1a] truncate">
                        {user.fullName}
                      </p>
                      <p className="m-0 mt-0.5 text-xs text-[#71717a] truncate">
                        {user.email || user.phone}
                      </p>
                    </div>

                    {/* Actions */}
                    <div className="p-1.5">
                      <Link
                        href={ROUTES.student.dashboard}
                        onClick={() => setProfileOpen(false)}
                        className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-[#5e5f5c] transition-colors duration-150 hover:bg-[#f5f5f5] no-underline"
                      >
                        <LayoutDashboard className="h-4 w-4 text-[#71717a]" />
                        {t('dashboard')}
                      </Link>
                      <button
                        type="button"
                        onClick={() => logoutMutation.mutate()}
                        disabled={logoutMutation.isPending}
                        className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-red-600 transition-colors duration-150 hover:bg-red-50 cursor-pointer disabled:opacity-50"
                      >
                        <LogOut className="h-4 w-4" />
                        {t('logout')}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <>
                <Link
                  href={ROUTES.login}
                  className="hidden h-11 items-center justify-center gap-3 rounded-lg px-4 transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)] md:flex lg:flex"
                >
                  <span className="font-sans text-base font-medium leading-6 text-[var(--color-secondary-blue-600)]">
                    {t('login')}
                  </span>
                </Link>
                <Link
                  href={ROUTES.signup}
                  className="btn-gold-gradient flex h-11 items-center justify-center gap-2 overflow-hidden rounded-md px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white transition-all duration-200 hover:opacity-90 hover:scale-[1.02]"
                >
                  {t('getStarted')}
                </Link>
              </>
            )}
          </div>

          {/* Mobile hamburger */}
          <button
            type="button"
            onClick={() => setMobileOpen(true)}
            aria-label="Open menu"
            aria-expanded={mobileOpen}
            aria-controls="mobile-navigation"
            className="flex h-11 w-11 items-center justify-center rounded-sm transition-colors duration-200 hover:bg-[var(--color-neutral-100)] xl:hidden"
          >
            <Menu className="h-6 w-6 text-[var(--color-text-body-secondary)]" />
          </button>
        </div>
      </header>

      {/* Backdrop */}
      <div
        className={`fixed inset-0 z-50 bg-black/40 transition-opacity duration-300 xl:hidden ${
          mobileOpen ? 'opacity-100' : 'pointer-events-none opacity-0'
        }`}
        onClick={() => setMobileOpen(false)}
        aria-hidden="true"
      />

      {/* Mobile Drawer */}
      <div
        id="mobile-navigation"
        ref={navigationRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-label="Navigation menu"
        aria-hidden={!mobileOpen}
        inert={!mobileOpen}
        className={`fixed inset-y-0 left-0 z-50 w-[280px] bg-white shadow-2xl transition-transform duration-300 ease-in-out xl:hidden ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-16 items-center justify-between border-b border-neutral-100 px-5">
          <span className="font-sans text-sm font-semibold text-[var(--color-text-headline-primary)]">
            {t('menu')}
          </span>
          <button
            type="button"
            onClick={() => setMobileOpen(false)}
            aria-label="Close menu"
            className="flex h-10 w-10 items-center justify-center rounded-sm transition-colors duration-200 hover:bg-[var(--color-neutral-100)]"
          >
            <X className="h-5 w-5 text-[var(--color-text-body-secondary)]" />
          </button>
        </div>

        <nav className="flex flex-col gap-1 p-4">
          {navLinks.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              onClick={() => setMobileOpen(false)}
              className="rounded-lg px-4 py-3 font-sans text-base font-normal text-[var(--color-text-body-secondary)] transition-colors duration-200 hover:bg-[var(--color-neutral-100)] hover:text-[#1d4ed8]"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="mt-auto border-t border-neutral-100 p-4">
          {!ready ? (
            <div className="h-11 rounded-md bg-gray-200 animate-pulse" />
          ) : isAuthenticated ? (
            <div className="flex flex-col gap-2">
              <Link
                href={ROUTES.student.dashboard}
                onClick={() => setMobileOpen(false)}
                className="block w-full rounded-md bg-[var(--color-secondary-blue-50)] px-5 py-3 text-center font-sans text-base font-semibold text-[var(--color-secondary-blue-600)] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]/70"
              >
                {t('dashboard')}
              </Link>
              <button
                type="button"
                onClick={() => { setMobileOpen(false); logoutMutation.mutate() }}
                disabled={logoutMutation.isPending}
                className="block w-full rounded-md border border-red-200 px-5 py-3 text-center font-sans text-base font-semibold text-red-600 transition-colors duration-200 hover:bg-red-50 cursor-pointer disabled:opacity-50"
              >
                {t('logout')}
              </button>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              <Link
                href={ROUTES.login}
                onClick={() => setMobileOpen(false)}
                className="block w-full rounded-md border border-[var(--color-secondary-blue-200)] px-5 py-3 text-center font-sans text-base font-semibold text-[var(--color-secondary-blue-600)] transition-colors duration-200 hover:bg-[var(--color-secondary-blue-50)]"
              >
                {t('login')}
              </Link>
              <Link
                href={ROUTES.signup}
                onClick={() => setMobileOpen(false)}
                className="btn-gold-gradient block w-full rounded-md px-5 py-3 text-center font-sans text-base font-semibold text-white transition-all duration-200 hover:opacity-90"
              >
                {t('getStarted')}
              </Link>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
