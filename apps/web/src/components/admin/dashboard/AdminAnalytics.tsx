'use client'

import { useState } from 'react'
import dynamic from 'next/dynamic'
import { TrendingUp, AlertCircle, BarChart3 } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { SectionCard } from '@/components/admin/shared/SectionCard'
import type { DashboardRevenuePoint } from '@/hooks/useAdminDashboard'

// Lazy-load recharts (saves ~95 KB from the main bundle until dashboard renders).
const AnalyticsCharts = dynamic(() => import('./AnalyticsCharts'), {
  ssr: false,
  loading: () => <Skeleton className='h-64 rounded-xl' />,
})

type Period = '7d' | '30d' | '6m'

interface AdminAnalyticsProps {
  revenueSeries: DashboardRevenuePoint[] // 6 months
  last30Days: DashboardRevenuePoint[]
  last7Days: DashboardRevenuePoint[]
  totalRevenue: number
  totalPaidOrders: number
  isLoading: boolean
}

const PERIOD_LABELS: Record<Period, string> = {
  '7d': 'Last 7 days',
  '30d': 'Last 30 days',
  '6m': 'Last 6 months',
}

export function AdminAnalytics({
  revenueSeries,
  last30Days,
  last7Days,
  totalRevenue,
  totalPaidOrders,
  isLoading,
}: AdminAnalyticsProps) {
  const [period, setPeriod] = useState<Period>('30d')

  const rawData =
    period === '7d' ? last7Days : period === '30d' ? last30Days : revenueSeries
  const isMonthly = period === '6m'

  // The 6-month view buckets by day — that's 180 points, too many bars.
  // Collapse to monthly buckets for display.
  const chartData = isMonthly ? bucketMonthly(revenueSeries) : rawData

  const periodRevenue = rawData.reduce((s, p) => s + p.revenue, 0)
  const periodPaidOrders = rawData.reduce((s, p) => s + p.paidOrders, 0)

  return (
    <SectionCard
      title='Revenue & Paid orders'
      description={isLoading
        ? 'Loading dashboard data…'
        : `Derived from ${totalPaidOrders.toLocaleString()} paid orders · ৳${totalRevenue.toLocaleString()} total paid`}
      action={
        <div className='inline-flex rounded-lg border border-gray-200 p-0.5 bg-gray-50'>
          {(Object.keys(PERIOD_LABELS) as Period[]).map((p) => (
            <button
              key={p}
              type='button'
              onClick={() => setPeriod(p)}
              aria-pressed={period === p}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 ${
                period === p
                  ? 'bg-white text-[#1c3873] shadow-sm'
                  : 'text-[#71717a] hover:text-[#1b1c1a]'
              }`}
            >
              {PERIOD_LABELS[p]}
            </button>
          ))}
        </div>
      }
    >
      <p className='mb-3 text-xs text-slate-500'>Payment dates use Asia/Dhaka. Orders without a payment date are excluded from charts.</p>
      {/* Loading */}
      {isLoading && (
        <div className='flex flex-col gap-4'>
          <div className='grid grid-cols-2 gap-3'>
            <Skeleton className='h-16 rounded-xl' />
            <Skeleton className='h-16 rounded-xl' />
          </div>
          <Skeleton className='h-64 rounded-xl' />
        </div>
      )}

      {/* Loaded */}
      {!isLoading && (
        <>
          {/* Summary stats */}
          <div className='grid grid-cols-2 gap-3'>
            <div className='rounded-xl bg-green-50 p-3 flex items-center gap-3'>
              <span className='p-2 rounded-lg bg-green-100'>
                <TrendingUp className='h-4 w-4 text-green-600' aria-hidden='true' />
              </span>
              <div className='flex flex-col'>
                <span className='text-[11px] text-[#166534]'>Revenue ({PERIOD_LABELS[period]})</span>
                <span className='text-base font-medium text-[#14532d]'>
                  ৳{periodRevenue.toLocaleString()}
                </span>
              </div>
            </div>
            <div className='rounded-xl bg-blue-50 p-3 flex items-center gap-3'>
              <span className='p-2 rounded-lg bg-blue-100'>
                <BarChart3 className='h-4 w-4 text-blue-600' aria-hidden='true' />
              </span>
              <div className='flex flex-col'>
                <span className='text-[11px] text-[#1e40af]'>Paid orders ({PERIOD_LABELS[period]})</span>
                <span className='text-base font-medium text-[#1e3a8a]'>{periodPaidOrders}</span>
              </div>
            </div>
          </div>

          {/* Charts (lazy-loaded) */}
          {chartData.length > 0 ? (
            <AnalyticsCharts data={chartData} isMonthly={isMonthly} />
          ) : (
            <div className='h-64 flex items-center justify-center text-sm text-[#71717a] bg-gray-50 rounded-xl'>
              <div className='flex flex-col items-center gap-2'>
                <AlertCircle className='h-8 w-8 text-gray-300' aria-hidden='true' />
                <p className='m-0'>No orders in this period yet.</p>
              </div>
            </div>
          )}
        </>
      )}
    </SectionCard>
  )
}

/** Collapse daily buckets into monthly buckets for the 6-month view. */
function bucketMonthly(points: DashboardRevenuePoint[]) {
  const map = new Map<string, { revenue: number; paidOrders: number }>()
  for (const p of points) {
    const monthKey = p.date.slice(0, 7) // YYYY-MM
    const existing = map.get(monthKey) ?? { revenue: 0, paidOrders: 0 }
    existing.revenue += p.revenue
    existing.paidOrders += p.paidOrders
    map.set(monthKey, existing)
  }
  return Array.from(map.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, v]) => ({ date, ...v }))
}
