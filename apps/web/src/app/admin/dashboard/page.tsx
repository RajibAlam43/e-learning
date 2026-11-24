'use client'

import { AlertCircle, RefreshCw } from 'lucide-react'
import { useSyncExternalStore } from 'react'
import { AdminGreeting } from '@/components/admin/dashboard/AdminGreeting'
import { AdminStats } from '@/components/admin/dashboard/AdminStats'
import { AdminAnalytics } from '@/components/admin/dashboard/AdminAnalytics'
import { AdminDashboardGrid } from '@/components/admin/dashboard/AdminDashboardGrid'
import { useAdminDashboard } from '@/hooks/useAdminDashboard'

export default function AdminDashboardPage() {
  const dash = useAdminDashboard()
  const mounted = useSyncExternalStore(
    () => () => undefined,
    () => true,
    () => false,
  )

  // Top-level error UI — individual widgets receive their own loading/empty
  // states, but a complete backend failure (e.g., 403) is surfaced here.
  if (mounted && dash.isError && !dash.isFetching && dash.courses.length === 0 && dash.instructors.length === 0 && dash.orders.length === 0 && dash.liveClasses.length === 0) {
    // All dashboard sources failed on the initial load.
    return (
      <div className='flex flex-col items-center justify-center gap-4 py-20 max-w-[1152px] mx-auto px-4'>
        <AlertCircle className='h-12 w-12 text-red-400' />
        <h2 className='text-lg font-heading font-medium text-[#1c3873]'>Couldn&apos;t load the dashboard</h2>
        <p className='text-sm text-[#5e5f5c] text-center max-w-md'>
          We failed to fetch one or more admin endpoints. Check your connection
          and permissions, then try again.
        </p>
        <button
          type='button'
          onClick={() => dash.refetch()}
          className='inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#1c3873] text-white text-sm font-medium hover:bg-[#15294f] transition-colors'
        >
          <RefreshCw className='h-4 w-4' /> Try again
        </button>
      </div>
    )
  }

  return (
    <div className='flex flex-col gap-8 w-full max-w-[1152px] mx-auto pb-10 px-4 sm:px-6 leading-[normal] tracking-[normal]'>
      {mounted && dash.isError && (
        <div role='alert' className='flex items-center justify-between gap-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900'>
          <span>Some dashboard data could not be loaded. The available sections are still shown.</span>
          <button type='button' onClick={() => dash.refetch()} className='shrink-0 font-semibold underline'>Retry</button>
        </div>
      )}
      <AdminGreeting
        isFetching={mounted && dash.isFetching}
        onRefresh={() => dash.refetch()}
      />
      <AdminStats
        stats={mounted ? dash.stats : {
          totalCourses: 0,
          totalTeachers: 0,
          totalLiveClasses: 0,
          totalRevenue: 0,
          totalPaidOrders: 0,
          totalOrderCustomers: 0,
        }}
        isLoading={!mounted || dash.isLoading}
      />
      <AdminAnalytics
        revenueSeries={mounted ? dash.revenueSeries : []}
        last30Days={mounted ? dash.last30Days : []}
        last7Days={mounted ? dash.last7Days : []}
        totalRevenue={mounted ? dash.stats.totalRevenue : 0}
        totalPaidOrders={mounted ? dash.stats.totalPaidOrders : 0}
        isLoading={!mounted || dash.isLoading}
      />
      <AdminDashboardGrid
        recentCourses={mounted ? dash.recentCourses : []}
        topInstructors={mounted ? dash.topInstructors : []}
        recentActivity={mounted ? dash.recentActivity : []}
        liveClassCount={mounted ? dash.stats.totalLiveClasses : 0}
        isLoading={!mounted || dash.isLoading}
      />
    </div>
  )
}
