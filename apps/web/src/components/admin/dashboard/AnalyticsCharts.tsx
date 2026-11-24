'use client'

/**
 * AnalyticsCharts — extracted so the recharts library can be lazy-loaded via
 * next/dynamic. This keeps recharts (~95 KB gzipped) out of the main bundle
 * until the dashboard actually renders.
 */
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
} from 'recharts'
import type { DashboardRevenuePoint } from '@/hooks/useAdminDashboard'

interface AnalyticsChartsProps {
  data: Array<DashboardRevenuePoint & { label?: string }>
  isMonthly: boolean
}

function formatDay(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('en-US', { timeZone: 'UTC', month: 'short', day: 'numeric' })
  } catch {
    return iso
  }
}

function formatMonth(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('en-US', { timeZone: 'UTC', month: 'short', year: '2-digit' })
  } catch {
    return iso
  }
}

function ChartTooltip({
  active,
  payload,
  label,
  isMonthly,
}: {
  active?: boolean
  payload?: { value: number; name: string }[]
  label?: string
  isMonthly?: boolean
}) {
  if (!active || !payload || payload.length === 0) return null
  return (
    <div className='rounded-lg bg-white shadow-lg border border-gray-100 p-3 text-xs'>
      <p className='font-medium text-[#1b1c1a] m-0 mb-1'>
        {isMonthly ? formatMonth(label ?? '') : formatDay(label ?? '')}
      </p>
      {payload.map((p) => (
        <p key={p.name} className='text-[#5e5f5c] m-0'>
          {p.name === 'revenue' ? 'Revenue' : 'Paid orders'}:{' '}
          <span className='font-medium text-[#1b1c1a]'>
            {p.name === 'revenue' ? `৳${p.value.toLocaleString()}` : p.value}
          </span>
        </p>
      ))}
    </div>
  )
}

export default function AnalyticsCharts({ data, isMonthly }: AnalyticsChartsProps) {
  return (
    <>
      {/* Bar chart — revenue per period bucket */}
      <div className='h-64 w-full' aria-label='Revenue bar chart'>
        <ResponsiveContainer width='100%' height='100%'>
          <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray='3 3' stroke='#f0f0f0' vertical={false} />
            <XAxis
              dataKey='date'
              tickFormatter={(d) => (isMonthly ? formatMonth(d) : formatDay(d))}
              tick={{ fontSize: 10, fill: '#71717a' }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis tick={{ fontSize: 10, fill: '#71717a' }} tickLine={false} axisLine={false} width={40} />
            <Tooltip cursor={{ fill: 'rgba(59,130,246,0.05)' }} content={<ChartTooltip isMonthly={isMonthly} />} />
            <Bar dataKey='revenue' fill='#3b82f6' radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Line chart — paidOrders trend */}
      <div className='h-40 w-full mt-4' aria-label='Paid orders line chart'>
        <ResponsiveContainer width='100%' height='100%'>
          <LineChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
            <XAxis
              dataKey='date'
              tickFormatter={(d) => (isMonthly ? formatMonth(d) : formatDay(d))}
              tick={{ fontSize: 10, fill: '#71717a' }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              tick={{ fontSize: 10, fill: '#71717a' }}
              tickLine={false}
              axisLine={false}
              width={40}
              allowDecimals={false}
            />
            <Tooltip content={<ChartTooltip isMonthly={isMonthly} />} />
            <Line
              type='monotone'
              dataKey='paidOrders'
              stroke='#10b981'
              strokeWidth={2}
              dot={{ r: 2, fill: '#10b981' }}
              activeDot={{ r: 4 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </>
  )
}
