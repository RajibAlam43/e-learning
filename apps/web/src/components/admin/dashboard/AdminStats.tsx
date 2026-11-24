'use client'

import { DollarSign, Users, UserPlus, BookOpen, GraduationCap, type LucideIcon } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import type { DashboardStats } from '@/hooks/useAdminDashboard'

interface AdminStatsProps {
  stats: DashboardStats
  isLoading: boolean
}

interface StatCardData {
  label: string
  value: string
  icon: LucideIcon
}

/** Format a number for display — 1234 → "1,234", 1500 → "1,500". */
function formatNumber(n: number): string {
  return n.toLocaleString('en-US')
}

/** Format revenue in BDT — 1500 → "৳1,500". */
function formatRevenue(n: number): string {
  if (n >= 100000) return `৳${(n / 1000).toFixed(0)}k`
  return `৳${formatNumber(n)}`
}

export function AdminStats({ stats, isLoading }: AdminStatsProps) {
  const cards: StatCardData[] = [
    { label: 'Revenue (paid orders)', value: formatRevenue(stats.totalRevenue), icon: DollarSign },
    { label: 'Order customers (email)', value: formatNumber(stats.totalOrderCustomers), icon: Users },
    { label: 'Paid orders', value: formatNumber(stats.totalPaidOrders), icon: UserPlus },
    { label: 'Total courses', value: formatNumber(stats.totalCourses), icon: BookOpen },
    { label: 'Total teachers', value: formatNumber(stats.totalTeachers), icon: GraduationCap },
  ]

  return (
    <div className='grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3'>
      {cards.map((card) => (
        <article
          key={card.label}
          className='shadow-[0px_2px_6px_rgba(16,24,40,0.06)] rounded-2xl bg-white flex flex-col p-5 gap-6 transition-all duration-300 hover:-translate-y-1 hover:shadow-[0px_4px_12px_rgba(16,24,40,0.1)]'
          aria-label={card.label}
        >
          <div className='flex items-start justify-between gap-3'>
            <div className='flex flex-col gap-3 min-w-0'>
              <span className='text-xs leading-4 text-[#5e5f5c] truncate'>{card.label}</span>
              {isLoading ? (
                <Skeleton className='h-8 w-20' />
              ) : (
                <span className='lg:text-[32px] lg:leading-9 font-medium text-[#1c3873] md:text-[26px] md:leading-[29px] text-[20px] leading-6'>
                  {card.value}
                </span>
              )}
            </div>
            <span className='p-3 bg-[#f4f9ff] rounded-lg shrink-0'>
              <card.icon className='h-5 w-5 text-[#3b82f6]' aria-hidden='true' />
            </span>
          </div>
          {/* Trend indicator omitted — requires historical data we don't have.
              Backend dashboard endpoint would provide this. */}
          <div className='text-[11px] text-gray-400 leading-4'>
            {isLoading ? 'Loading…' : card.label === 'Order customers (email)' ? 'Unique emails in order history' : 'Live from latest data'}
          </div>
        </article>
      ))}
    </div>
  )
}
