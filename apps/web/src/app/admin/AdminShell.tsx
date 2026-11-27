'use client'

import DashboardSidebar from '@/components/shared/DashboardSidebar'
import DashboardHeader from '@/components/shared/DashboardHeader'
import MobileDrawer from '@/components/shared/MobileDrawer'
import BottomNavBar from '@/components/shared/BottomNavBar'
import React, { useState } from 'react'
import { Users, SquareUser, Home, BookOpen, Star } from 'lucide-react'
import { AuthGate } from '@/components/auth/AuthGate'

const adminPrimaryMenu = [
  { href: '/admin/dashboard', icon: '/images/dashboard-icon.png', label: 'Dashboard' },
  { href: '/admin/students', icon: Users, label: 'Students' },
  { href: '/admin/teachers', icon: SquareUser, label: 'Teachers' },
  { href: '/admin/faculty', icon: Users, label: 'Faculty' },
  { href: '/admin/courses', icon: '/images/book-icon.png', label: 'Courses' },
  { href: '/admin/categories', icon: BookOpen, label: 'Categories' },
  { href: '/admin/semesters', icon: BookOpen, label: 'Semesters' },
  { href: '/admin/waivers', icon: BookOpen, label: 'Credit Waivers' },
  { href: '/admin/programs', icon: '/images/calendar-program.png', label: 'Programs' },
  { href: '/admin/live-classes', icon: '/images/live-icon.png', label: 'Live Classes' },
  { href: '/admin/payments', icon: '/images/price.png', label: 'Payments' },
  { href: '/admin/analytics', icon: '/images/dashboard-icon.png', label: 'Analytics' },
  { href: '/admin/reviews', icon: Star, label: 'Reviews' },
]

const adminSecondaryMenu = [
  { href: '/admin/announcements', icon: '/images/bell-icon.png', label: 'Announcements' },
  { href: '/admin/support', icon: '/images/question-icon.png', label: 'Support' },
  { href: '/admin/settings', icon: '/images/gear-icon.png', label: 'Settings' },
]

const adminBottomNavItems = [
  { label: 'Dashboard', href: '/admin/dashboard', icon: Home },
  { label: 'Courses', href: '/admin/courses', icon: BookOpen },
  { label: 'Students', href: '/admin/students', icon: Users },
  { label: 'Teachers', href: '/admin/teachers', icon: SquareUser },
]

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  return (
    <AuthGate roles={['ADMIN', 'SUPER_ADMIN']}>
      <>
      <div className='flex h-screen overflow-hidden'>
        <DashboardSidebar primaryMenu={adminPrimaryMenu} secondaryMenu={adminSecondaryMenu} logoHref="/" />
        <div className='flex flex-1 flex-col overflow-hidden min-w-0'>
          <DashboardHeader onMenuToggle={() => setSidebarOpen((prev) => !prev)} />
          <main className='flex-1 overflow-y-auto bg-[#f5f9ff] p-6 lg:pb-6 pb-24'>
            {children}
          </main>
        </div>
      </div>
      <MobileDrawer isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} primaryMenu={adminPrimaryMenu} secondaryMenu={adminSecondaryMenu} logoHref="/" />
      <BottomNavBar items={adminBottomNavItems} />
      </>
    </AuthGate>
  )
}
