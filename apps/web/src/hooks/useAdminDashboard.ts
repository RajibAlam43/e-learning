'use client'

/**
 * useAdminDashboard — composes the admin dashboard view from existing endpoints.
 *
 * The backend has no dedicated `/admin/dashboard` aggregate endpoint (yet), so
 * we fetch the four primary admin collections in parallel and derive the
 * dashboard stats client-side:
 *   - GET /admin/courses     → total courses, recent courses, top by enrollment
 *   - GET /admin/instructors → total teachers, top instructors
 *   - GET /admin/orders      → revenue (sum paid), paid orders, email-identified customers, activity feed
 *   - GET /admin/live-classes → live class count
 *
 * Widgets that cannot be derived from these (course-completion rates,
 * time-series analytics) display a "backend endpoint needed" banner — see
 * AdminCourseCompletion and the analytics time-series note.
 */

import { useMemo } from 'react'
import { useAdminCourses, useAdminInstructors, useAdminOrders, useAdminLiveClasses } from '@/hooks/useAdmin'
import type { AdminCourseSummary, AdminInstructorSummary } from '@/types'

import { orderTotals, revenueByDay, type RevenuePoint } from '@/lib/order-reporting'

export type DashboardRevenuePoint = RevenuePoint

export interface DashboardStats {
  totalCourses: number
  totalTeachers: number
  totalLiveClasses: number
  totalRevenue: number
  totalPaidOrders: number
  totalOrderCustomers: number
}

export interface DashboardActivity {
  id: string
  customerName: string
  customerEmail: string
  courseName: string
  amount: number
  status: string
  createdAt: string
}

export function useAdminDashboard() {
  const coursesQuery = useAdminCourses()
  const instructorsQuery = useAdminInstructors()
  const ordersQuery = useAdminOrders()
  const liveClassesQuery = useAdminLiveClasses()

  const isLoading =
    coursesQuery.isLoading ||
    instructorsQuery.isLoading ||
    ordersQuery.isLoading ||
    liveClassesQuery.isLoading

  const isFetching =
    coursesQuery.isFetching ||
    instructorsQuery.isFetching ||
    ordersQuery.isFetching ||
    liveClassesQuery.isFetching

  // First error wins — callers can branch on `isError` to show the error UI.
  const firstError =
    coursesQuery.error || instructorsQuery.error || ordersQuery.error || liveClassesQuery.error || null

  const isError = !!firstError

  const courses = useMemo(() => coursesQuery.data ?? [], [coursesQuery.data])
  const instructors = useMemo(() => instructorsQuery.data ?? [], [instructorsQuery.data])
  const orders = useMemo(() => ordersQuery.data ?? [], [ordersQuery.data])
  const liveClasses = useMemo(() => liveClassesQuery.data ?? [], [liveClassesQuery.data])

  // ─── Derived stats ───
  const stats: DashboardStats = useMemo(() => {
    return {
      totalCourses: courses.length,
      totalTeachers: instructors.length,
      totalLiveClasses: liveClasses.length,
      ...orderTotals(orders),
    }
  }, [courses, instructors, orders, liveClasses])

  // ─── Recent courses (already sorted by publishedAt desc on backend) ───
  const recentCourses: AdminCourseSummary[] = useMemo(() => courses.slice(0, 6), [courses])

  // ─── Top instructors: derive by slicing — backend order is unspecified,
  //     so this is a "first N" approximation until /admin/instructors?sort=courses exists. ───
  const topInstructors: AdminInstructorSummary[] = useMemo(() => instructors.slice(0, 5), [instructors])

  // ─── Activity feed: most recent orders, newest first ───
  const recentActivity: DashboardActivity[] = useMemo(() => {
    return orders
      .slice()
      .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''))
      .slice(0, 7)
      .map((o) => ({
        id: o.orderId ?? '',
        customerName: o.customerName ?? 'Unknown',
        customerEmail: o.customerEmail ?? '',
        // AdminOrderSummary has no items[] — we only know it was an order, not which course.
        courseName: 'Order',
        amount: o.totalAmount ?? 0,
        status: o.status ?? 'UNKNOWN',
        createdAt: o.createdAt ?? '',
      }))
  }, [orders])

  // ─── Analytics time-series (derived from orders; backend proper endpoint TBD) ───
  const revenueSeries = useMemo(() => revenueByDay(orders, 180), [orders]) // 6 months
  const last7Days = useMemo(() => revenueByDay(orders, 7), [orders])
  const last30Days = useMemo(() => revenueByDay(orders, 30), [orders])

  function refetchAll() {
    return Promise.all([
      coursesQuery.refetch(),
      instructorsQuery.refetch(),
      ordersQuery.refetch(),
      liveClassesQuery.refetch(),
    ])
  }

  return {
    // Raw data (for widgets that need fields beyond the derived views)
    courses,
    instructors,
    orders,
    liveClasses,
    // Derived
    stats,
    recentCourses,
    topInstructors,
    recentActivity,
    revenueSeries,
    last7Days,
    last30Days,
    // Status
    isLoading,
    isFetching,
    isError,
    error: firstError,
    refetch: refetchAll,
  }
}
