'use client'

import DashboardSidebar from '@/components/shared/DashboardSidebar'
import DashboardHeader from '@/components/shared/DashboardHeader'
import MobileDrawer from '@/components/shared/MobileDrawer'
import BottomNavBar from '@/components/shared/BottomNavBar'
import React, { useState } from 'react'
import { Home, BookOpen, Video } from 'lucide-react'
import { AuthGate } from '@/components/auth/AuthGate'
import { useTranslations } from 'next-intl'

export default function InstructorLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const t = useTranslations('privateNav')
  const instructorNavItems = [
    { label: t('home'), href: '/instructor/dashboard', icon: Home },
    { label: t('courses'), href: '/instructor/courses', icon: BookOpen },
    { label: t('live'), href: '/instructor/live-classes', icon: Video },
  ]

  const instructorMenuItems = [
    { href: '/instructor/dashboard', icon: '/images/dashboard-icon.png', label: t('dashboard') },
    { href: '/instructor/courses', icon: '/images/book-icon.png', label: t('myCourses') },
    { href: '/instructor/live-classes', icon: '/images/live-icon.png', label: t('liveClasses') },
    { href: '/instructor/announcements', icon: '/images/bell-icon.png', label: t('announcements') },
    { href: '/instructor/messages', icon: '/images/envelope-icon.png', label: t('messages') },
    { href: '/instructor/settings', icon: '/images/gear-icon.png', label: t('settings') },
  ]
  return (
    <AuthGate roles={['INSTRUCTOR', 'TEACHER', 'ADMIN', 'SUPER_ADMIN']}>
      <>
      <div className='flex h-screen overflow-hidden'>
        <DashboardSidebar primaryMenu={instructorMenuItems} logoHref="/" />
        <div className='flex flex-1 flex-col overflow-hidden min-w-0'>
          <DashboardHeader onMenuToggle={() => setSidebarOpen((prev) => !prev)} />
          <main className='flex-1 overflow-y-auto bg-[#f5f9ff] p-4 lg:p-6 pb-24 lg:pb-6'>
            {children}
          </main>
        </div>
      </div>
        <MobileDrawer isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} primaryMenu={instructorMenuItems} logoHref="/" />

      {/* Mobile Bottom Navigation */}
      <BottomNavBar items={instructorNavItems} />
      </>
    </AuthGate>
  )
}
