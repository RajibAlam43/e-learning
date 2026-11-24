'use client'

import Image from 'next/image'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useLogout } from '@/hooks/useAuth'
import React from 'react'
import { useTranslations } from 'next-intl'

export interface MenuItem {
  href: string
  icon: string | React.ElementType
  label: string
}

export interface SidebarProps {
  primaryMenu: MenuItem[]
  secondaryMenu?: MenuItem[]
  logoHref?: string
  onNavigate?: () => void
}

const SidebarContent = ({ primaryMenu, secondaryMenu, logoHref = '/', onNavigate }: SidebarProps) => {
  const pathname = usePathname()
  const { mutate: logout, isPending } = useLogout()
  const t = useTranslations('privateNav')

  const handleLogout = () => {
    logout()
    onNavigate?.()
  }

  const renderMenu = (items: MenuItem[]) => (
    <nav className='flex flex-col gap-2 flex-shrink-0'>
      {items.map((item) => {
        // Special active logic for courses matching sub-routes
        const isActive = item.href.includes('/courses')
          ? pathname.startsWith(item.href)
          : pathname === item.href

        return (
          <Link
            key={item.label}
            href={item.href}
            onClick={onNavigate}
            className={`flex flex-row gap-3 items-center px-4 py-[10px] rounded-lg transition-all duration-200 ease-in-out hover:-translate-y-px hover:shadow-sm ${
              isActive
                ? 'font-medium bg-[#f3f4f6]'
                : 'hover:font-medium hover:bg-[#f3f4f6]'
            }`}
          >
            <div className='relative w-[15px] h-[15px] flex items-center justify-center shrink-0'>
              {typeof item.icon === 'string' ? (
                <Image src={item.icon} fill sizes='15px' className='object-contain' alt={item.label} />
              ) : (
                <item.icon className="w-4 h-4 text-inherit" />
              )}
            </div>
            <span className={`text-sm leading-5 font-switzer text-[#1c3873] ${isActive ? 'font-medium' : 'font-normal'}`}>
              {item.label}
            </span>
          </Link>
        )
      })}
    </nav>
  )

  return (
    <div className='flex flex-col h-full'>
      {/* Logo */}
      <Link href={logoHref} onClick={onNavigate} className='shrink-0'>
        <Image src="/images/logo.svg" width={200} height={32} alt='Global Islamic Institute' className='w-auto' unoptimized />
      </Link>

      {/* Divider */}
      <div className='w-48 h-[2px] shrink-0 mt-6' style={{ backgroundImage: "url('/images/Line1.png')" }}></div>

      {/* Scrollable Container */}
      <div className='flex flex-col gap-6 flex-1 overflow-y-auto overscroll-contain min-h-0 mt-6 pb-6'>
        {renderMenu(primaryMenu)}
        
        {secondaryMenu && secondaryMenu.length > 0 && (
          <div className='mt-auto flex flex-col gap-2'>
             <div className='w-full h-px bg-gray-100 mb-2' />
             {renderMenu(secondaryMenu)}
          </div>
        )}
      </div>

      {/* Logout */}
      <button
        onClick={handleLogout}
        disabled={isPending}
        className='shrink-0 flex flex-row gap-3 items-center px-6 py-4 bg-white border-t border-blue-100 transition-colors duration-200 hover:bg-red-50 disabled:opacity-50 cursor-pointer -mx-6'
      >
        <div className='relative w-4 h-4 flex items-center justify-center overflow-hidden shrink-0'>
          <Image src="/images/logout-icon.png" fill sizes='16px' className='object-contain' alt='' />
        </div>
        <span className='text-sm text-[#b91c1c] font-normal leading-5 font-switzer'>
          {isPending ? t('loggingOut') : t('logout')}
        </span>
      </button>
    </div>
  )
}

export { SidebarContent }

const DashboardSidebar = (props: Omit<SidebarProps, 'onNavigate'>) => {
  return (
    <div className='hidden lg:flex left-sidebar w-60 max-w-60 h-screen px-6 pt-6 flex-col sticky top-0 overflow-hidden bg-white border-r border-blue-100 shrink-0'>
      <SidebarContent {...props} />
    </div>
  )
}

export default DashboardSidebar
