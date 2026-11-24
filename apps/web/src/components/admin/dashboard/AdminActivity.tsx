'use client'

import { Activity as ActivityIcon } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState } from '@/components/shared/EmptyState'
import type { DashboardActivity } from '@/hooks/useAdminDashboard'

interface AdminActivityProps {
  items: DashboardActivity[]
  isLoading: boolean
}

function formatRelative(iso: string): string {
  if (!iso) return '—'
  try {
    const date = new Date(iso)
    const diffMs = Date.now() - date.getTime()
    const diffMin = Math.floor(diffMs / 60000)
    if (diffMin < 1) return 'just now'
    if (diffMin < 60) return `${diffMin}m ago`
    const diffHr = Math.floor(diffMin / 60)
    if (diffHr < 24) return `${diffHr}h ago`
    const diffDay = Math.floor(diffHr / 24)
    if (diffDay < 7) return `${diffDay}d ago`
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  } catch {
    return '—'
  }
}

function initials(name: string): string {
  return name
    .split(' ')
    .map((p) => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

export function AdminActivity({ items, isLoading }: AdminActivityProps) {
  return (
    <section
      className='w-full lg:w-[340px] shrink-0 bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-5 flex flex-col gap-4'
      aria-label='Recent activity'
    >
      <header className='flex items-center justify-between'>
        <h3 className='text-base font-heading font-medium text-[#1c3873] m-0'>Recent Activity</h3>
        <span className='inline-flex items-center gap-1 text-[11px] text-[#71717a]'>
          <ActivityIcon className='h-3 w-3' /> Last 7 orders
        </span>
      </header>

      {/* Loading */}
      {isLoading && (
        <ul className='flex flex-col gap-3 m-0 p-0 list-none' aria-busy='true'>
          {Array.from({ length: 5 }).map((_, i) => (
            <li key={i} className='flex items-center gap-3'>
              <Skeleton className='h-9 w-9 rounded-full shrink-0' />
              <div className='flex-1 flex flex-col gap-1.5'>
                <Skeleton className='h-3 w-3/4' />
                <Skeleton className='h-2.5 w-1/2' />
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* Empty */}
      {!isLoading && items.length === 0 && (
        <EmptyState
          icon={<ActivityIcon className='h-8 w-8' />}
          title='No activity yet'
          description='New orders will appear here.'
        />
      )}

      {/* List */}
      {!isLoading && items.length > 0 && (
        <ul className='flex flex-col gap-3 m-0 p-0 list-none'>
          {items.map((a) => (
            <li key={a.id} className='flex items-start gap-3'>
              <div
                className='h-9 w-9 rounded-full bg-blue-50 flex items-center justify-center text-xs font-medium text-[#3b82f6] shrink-0'
                aria-hidden='true'
              >
                {initials(a.customerName) || '?'}
              </div>
              <div className='flex-1 min-w-0'>
                <p className='text-sm text-[#1b1c1a] m-0'>
                  <span className='font-medium'>{a.customerName}</span>{' '}
                  <span className='text-[#5e5f5c]'>placed an order</span>
                </p>
                <p className='text-xs text-[#71717a] m-0'>
                  ৳{(a.amount ?? 0).toLocaleString()} · {formatRelative(a.createdAt)}
                </p>
              </div>
              <span
                className={`text-[10px] font-medium px-1.5 py-0.5 rounded-full shrink-0 ${
                  a.status === 'PAID' || a.status === 'COMPLETED'
                    ? 'bg-green-100 text-green-700'
                    : a.status === 'PENDING'
                      ? 'bg-amber-100 text-amber-700'
                      : 'bg-gray-100 text-gray-600'
                }`}
              >
                {(a.status ?? 'UNKNOWN').toLowerCase()}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
