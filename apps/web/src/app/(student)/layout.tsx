'use client'

import DashboardSidebar from '@/components/shared/DashboardSidebar'
import DashboardHeader from '@/components/shared/DashboardHeader'
import MobileDrawer from '@/components/shared/MobileDrawer'
import BottomNavBar from '@/components/shared/BottomNavBar'
import React, { useState } from 'react'
import { Home, BookOpen, Video, Library, Megaphone } from 'lucide-react'
import { AuthGate } from '@/components/auth/AuthGate'
import { useTranslations } from 'next-intl'

export default function StudentLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const t = useTranslations('privateNav')
  const studentNavItems = [
    { label: t('home'), href: '/student/dashboard', icon: Home },
    { label: t('courses'), href: '/student/courses', icon: BookOpen },
    { label: t('live'), href: '/student/live-classes', icon: Video },
  ]

  const studentMenuItems = [
    { href: '/student/dashboard', icon: '/images/dashboard-icon.png', label: t('dashboard') },
    { href: '/student/courses', icon: '/images/book-icon.png', label: t('myCourses') },
    { href: '/student/collections', icon: Library, label: t('myPrograms') },
    { href: '/student/waivers', icon: Library, label: 'Credit Waivers' },
    { href: '/student/live-classes', icon: '/images/live-icon.png', label: t('liveClasses') },
    { href: '/student/assignments', icon: '/images/clipboard-icon.png', label: t('assignments') },
    { href: '/student/certificates', icon: '/images/badge-icon.png', label: t('certificates') },
    { href: '/student/announcements', icon: Megaphone, label: t('announcements') },
    { href: '/student/messages', icon: '/images/envelope-icon.png', label: t('messages') },
    { href: '/student/settings', icon: '/images/gear-icon.png', label: t('settings') },
  ]
  return (
    <AuthGate roles={['STUDENT']}>
      <>
      <div className='w-full flex flex-row'>
        {/* Desktop Sidebar */}
        <DashboardSidebar primaryMenu={studentMenuItems} logoHref="/" />

        {/* Main content */}
        <div className='main-content flex-1 min-w-0 w-full'>
          {/* Top bar */}
          <DashboardHeader onMenuToggle={() => setSidebarOpen((prev) => !prev)} />

          <div className='w-full px-5 lg:px-16 py-5 pb-24 lg:pb-5 bg-[#f5f9ff] min-h-screen'>
            {children}
          </div>
        </div>
      </div>

      {/* Mobile Drawer — outside flex container */}
      <MobileDrawer isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} primaryMenu={studentMenuItems} logoHref="/" />

      {/* Mobile Bottom Navigation */}
      <BottomNavBar items={studentNavItems} />
      </>
    </AuthGate>
  )
}
