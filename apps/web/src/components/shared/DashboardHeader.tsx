'use client'

import { useTranslations } from 'next-intl'

import Image from 'next/image'
import Link from 'next/link'
import { useAuthStore } from '@/store/auth.store'
import { useLogout } from '@/hooks/useAuth'
import React, { useState, useRef, useEffect } from 'react'
import { usePathname, useRouter } from 'next/navigation'

interface DashboardHeaderProps {
  onMenuToggle: () => void
}

const DashboardHeader = ({ onMenuToggle }: DashboardHeaderProps) => {
  const ui = useTranslations('privateUi')

  const user = useAuthStore((s) => s.user)
  const router = useRouter()
  const pathname = usePathname()
  const dashboardBase = pathname.startsWith('/instructor') ? '/instructor' : '/student'
  const { mutate: logout, isPending } = useLogout()
  const [menuOpen, setMenuOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [search, setSearch] = useState('')
  const menuRef = useRef<HTMLDivElement>(null)
  const searchInputRef = useRef<HTMLInputElement>(null)
  const menuButtonRef = useRef<HTMLButtonElement>(null)
  const searchButtonRef = useRef<HTMLButtonElement>(null)

  function submitSearch(event: React.FormEvent) {
    event.preventDefault()
    const query = search.trim()
    router.push(query ? `/courses?search=${encodeURIComponent(query)}` : '/courses')
    setSearchOpen(false)
  }

  const menuItems = [
    { href: `${dashboardBase}/settings`, label: ui('settings') },
  ]

  // Close dropdowns on outside click
  useEffect(() => {
    if (!menuOpen && !searchOpen) return
    const handleClickOutside = (e: MouseEvent | TouchEvent) => {
      if (menuOpen && menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('touchstart', handleClickOutside)
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      if (menuOpen) menuButtonRef.current?.focus()
      else if (searchOpen) searchButtonRef.current?.focus()
      setMenuOpen(false)
      setSearchOpen(false)
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('touchstart', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [menuOpen, searchOpen])

  // Auto-focus search input when opened
  useEffect(() => {
    if (searchOpen) {
      setTimeout(() => searchInputRef.current?.focus(), 100)
    }
  }, [searchOpen])

  return (
    <header className='w-full bg-white shadow-sm shadow-gray-200 relative z-10'>
      {/* ── Mobile Header ── */}
      <div className='flex items-center justify-between px-4 py-2 lg:hidden'>
        {/* Left: Logo */}
        <Link href='/' aria-label={ui('globalIslamicInstituteHome')}>
          <Image src='/images/logo.svg' width={120} height={36} alt='GII Logo' className='h-9 w-auto object-contain' />
        </Link>

        {/* Right: Search + Hamburger */}
        <nav className='inline-flex items-center gap-1.5' aria-label={ui('headerActions')}>
          <button
            type='button'
            ref={searchButtonRef}
            aria-label={ui('search')}
            onClick={() => setSearchOpen((prev) => !prev)}
            className={`inline-flex items-center justify-center w-10 h-10 rounded-full transition-colors ${searchOpen ? 'bg-[#eff6ff]' : 'hover:bg-gray-50'}`}
          >
            <svg
              className={`w-5 h-5 transition-colors ${searchOpen ? 'text-[#3b82f6]' : 'text-[#1c3873]'}`}
              viewBox='0 0 24 24'
              fill='none'
              stroke='currentColor'
              strokeWidth='2'
              strokeLinecap='round'
              strokeLinejoin='round'
            >
              <circle cx='11' cy='11' r='8' />
              <line x1='21' y1='21' x2='16.65' y2='16.65' />
            </svg>
          </button>
          <button
            onClick={onMenuToggle}
            className='flex flex-col justify-center items-center w-10 h-10 gap-[5px] rounded-full hover:bg-gray-50 transition-colors'
            aria-label={ui('toggleMenu')}
          >
            <span className='block w-5 h-[2px] bg-[#1c3873] rounded-full' />
            <span className='block w-5 h-[2px] bg-[#1c3873] rounded-full' />
            <span className='block w-5 h-[2px] bg-[#1c3873] rounded-full' />
          </button>
        </nav>
      </div>

      {/* Mobile Search Dropdown */}
      <div
        className={`lg:hidden overflow-hidden transition-all duration-200 ease-in-out ${searchOpen ? 'max-h-20 opacity-100' : 'max-h-0 opacity-0'}`}
      >
        <div className='pt-2 px-4 pb-3'>
          <form onSubmit={submitSearch} className='relative'>
            <svg
              className='absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#97a3bf]'
              viewBox='0 0 24 24'
              fill='none'
              stroke='currentColor'
              strokeWidth='2'
              strokeLinecap='round'
              strokeLinejoin='round'
            >
              <circle cx='11' cy='11' r='8' />
              <line x1='21' y1='21' x2='16.65' y2='16.65' />
            </svg>
            <input
              ref={searchInputRef}
              type='text'
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder={ui('searchCoursesTeachersLessons2')}
              className='w-full h-10 pl-10 pr-4 rounded-lg border border-gray-200 bg-[#f9fafb] text-sm text-[#1b1c1a] placeholder:text-[#97a3bf] focus:outline-none focus:ring-2 focus:ring-[#3b82f6] focus:border-transparent focus:bg-white transition-all'
            />
          </form>
        </div>
      </div>

      {/* ── Desktop Header ── */}
      <div className='hidden lg:flex items-center justify-between px-16 py-5'>
        {/* Search */}
        <form onSubmit={submitSearch} className='relative'>
          <Image
            src='/images/search-icon.png'
            width={16}
            height={16}
            className='absolute top-1/2 -translate-y-1/2 left-3 object-contain'
            alt={ui('search')}
          />
          <input
            type='text'
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder={ui('searchCoursesTeachersLessons')}
            className='w-96 h-10 pl-10 pr-2 py-2 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:shadow-md focus:-translate-y-px transition-all duration-300 ease-out placeholder:text-sm placeholder:font-normal placeholder:font-switzer placeholder:text-[#97a3bf]'
          />
        </form>

        {/* Right side */}
        <div className='flex items-center gap-6'>
          <Link href={`${dashboardBase}/announcements`} className='notification-bell relative w-4 h-4 flex items-center justify-center cursor-pointer transition-all duration-200 hover:scale-110 hover:opacity-80'>
            <Image src='/images/bell-icon.png' fill sizes='16px' alt={ui('notifications')} className='object-contain' />
          </Link>

          <Link href='/contact' className='support relative w-4 h-4 flex items-center justify-center cursor-pointer transition-all duration-200 hover:scale-110 hover:opacity-80'>
            <Image src='/images/question-icon.png' fill sizes='16px' alt={ui('support')} className='object-contain' />
          </Link>

          {/* Profile + dropdown */}
          <div
            className='relative'
            ref={menuRef}
          >
            <button
              type="button"
              ref={menuButtonRef}
              aria-expanded={menuOpen}
              aria-controls="dashboard-profile-actions"
              className='flex items-center gap-3 cursor-pointer select-none transition-opacity duration-200 hover:opacity-80'
              onClick={() => setMenuOpen((prev) => !prev)}
            >
              <div className='flex flex-col text-right'>
                <p className='text-sm font-medium font-switzer leading-4 text-[#49608f]'>{user?.fullName || ui('student')}</p>
                <span className='text-[10px] font-inter leading-4 font-normal text-[#5e5f5c] uppercase'>
                  {user?.roles?.[0] || ui('student')}
                </span>
              </div>
              <div className='relative w-10 h-10 rounded-full'>
                {user?.avatar ? (
                  <Image src={user.avatar} fill className='rounded-full object-cover' alt={user.fullName} />
                ) : (
                  <div className='w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-sm font-medium text-[#1c3873] font-switzer'>
                    {user?.fullName?.charAt(0)?.toUpperCase() || ui('s')}
                  </div>
                )}
              </div>
            </button>

            {/* Dropdown */}
            {menuOpen && (
              <div id="dashboard-profile-actions" className='absolute right-0 top-full pt-2 z-50'>
                <div className='w-52 bg-white rounded-xl shadow-[0px_8px_24px_0px_rgba(16,24,40,0.12)] border border-gray-100 py-2'>
                  <div className='px-4 py-3 border-b border-gray-100'>
                    <p className='text-sm font-medium font-switzer text-[#1c3873] truncate'>{user?.fullName || ui('student')}</p>
                    <p className='text-xs text-[#5e5f5c] truncate'>{user?.email || ''}</p>
                  </div>

                  {menuItems.map((item) => (
                    <Link
                      key={item.href}
                      href={item.href}
                      className='flex items-center px-4 py-2.5 text-sm font-normal font-switzer text-[#1c3873] hover:bg-gray-50 transition-colors'
                    >
                      {item.label}
                    </Link>
                  ))}

                  <button
                    onClick={() => logout()}
                    disabled={isPending}
                    className='w-full flex items-center px-4 py-2.5 text-sm font-normal font-switzer text-red-700 hover:bg-red-50 transition-colors disabled:opacity-50 cursor-pointer'
                  >
                    {isPending ? ui('loggingOut') : ui('logout')}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}

export default DashboardHeader
