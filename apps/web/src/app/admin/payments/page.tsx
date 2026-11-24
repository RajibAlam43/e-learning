'use client'

import { useState, useMemo, useCallback } from 'react'
import dynamic from 'next/dynamic'
import { CreditCard, RefreshCw, AlertCircle } from 'lucide-react'
import { format } from 'date-fns'
import StatCard from '@/components/shared/StatCard'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { SearchInput } from '@/components/admin/shared/SearchInput'
import { FilterBar } from '@/components/admin/shared/FilterBar'
import { DataTable, type Column } from '@/components/admin/shared/DataTable'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import { useAdminOrders } from '@/hooks/useAdmin'
import type { AdminOrderSummary } from '@/types'

// Lazy-load the detail dialog — it fetches order detail and has status mutation.
const OrderDetailDialog = dynamic(
  () => import('./OrderDetailDialog').then((m) => m.OrderDetailDialog),
  { ssr: false },
)

const STATUS_FILTER_OPTIONS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'PAID', label: 'Paid' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'REFUNDED', label: 'Refunded' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const PROVIDER_FILTER_OPTIONS = [
  { value: 'SSLCOMMERZ', label: 'SSLCommerz' },
  { value: 'BKASH', label: 'bKash' },
]

function formatDate(date: string | undefined): string {
  if (!date) return '—'
  try {
    return format(new Date(date), 'MMM d, yyyy')
  } catch {
    return '—'
  }
}

export default function AdminPaymentsPage() {
  const { data: orders, isLoading, isError, isFetching, refetch } = useAdminOrders()

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [providerFilter, setProviderFilter] = useState('all')
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null)

  const stats = useMemo(() => {
    if (!orders) return { revenue: 0, pending: 0, total: 0 }

    let revenue = 0
    let pending = 0

    for (const o of orders) {
      if (o.status === 'PAID') revenue += o.totalAmount ?? 0
      if (o.status === 'PENDING') pending++
    }

    return { revenue, pending, total: orders.length }
  }, [orders])

  const filtered = useMemo(() => {
    if (!orders) return []

    return orders.filter((o) => {
      if (statusFilter !== 'all' && o.status !== statusFilter) return false
      if (providerFilter !== 'all' && o.provider !== providerFilter) return false
      if (search) {
        const q = search.toLowerCase()
        const matchesName = o.customerName?.toLowerCase().includes(q)
        const matchesEmail = o.customerEmail?.toLowerCase().includes(q)
        const matchesId = o.orderId?.toLowerCase().includes(q)
        if (!matchesName && !matchesEmail && !matchesId) return false
      }
      return true
    })
  }, [orders, statusFilter, providerFilter, search])

  const handleSearch = useCallback((value: string) => {
    setSearch(value)
  }, [])

  const handleClearFilters = useCallback(() => {
    setStatusFilter('all')
    setProviderFilter('all')
  }, [])

  const columns: Column<AdminOrderSummary>[] = [
    {
      key: 'orderId',
      header: 'Order ID',
      render: (o) => (
        <span className="font-mono text-xs">{o.orderId?.slice(0, 8)}...</span>
      ),
    },
    {
      key: 'customer',
      header: 'Customer',
      render: (o) => (
        <div>
          <p className="font-medium text-[#1b1c1a]">{o.customerName ?? '—'}</p>
          <p className="text-xs text-[#71717a]">{o.customerEmail ?? ''}</p>
        </div>
      ),
    },
    {
      key: 'amount',
      header: 'Amount',
      render: (o) => (
        <span className="font-medium">৳{(o.totalAmount ?? 0).toLocaleString()}</span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (o) => <StatusBadge status={o.status ?? 'PENDING'} />,
    },
    {
      key: 'provider',
      header: 'Provider',
      render: (o) => o.provider ?? '—',
      hideOnMobile: true,
    },
    {
      key: 'date',
      header: 'Date',
      render: (o) => formatDate(o.createdAt),
      hideOnMobile: true,
    },
  ]

  return (
    <div className="flex flex-col gap-6 w-full max-w-[1152px] mx-auto px-4 sm:px-6">
      {/* Stat cards */}
      <div className="grid grid-cols-2 xl:grid-cols-3 gap-3">
        <StatCard
          icon="/images/ion_book.svg"
          iconBg="bg-emerald-50"
          iconAlt="Total revenue"
          value={isLoading ? '—' : `৳${stats.revenue.toLocaleString()}`}
          label="Total Revenue"
        />
        <StatCard
          icon="/images/mdi_clock.svg"
          iconBg="bg-amber-50"
          iconAlt="Pending orders"
          value={isLoading ? '—' : stats.pending}
          label="Pending Orders"
        />
        <StatCard
          icon="/images/ph_video-fill.svg"
          iconBg="bg-blue-50"
          iconAlt="Total orders"
          value={isLoading ? '—' : stats.total}
          label="Total Orders"
        />
      </div>

      <AdminPageHeader
        title="Payments"
        action={
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            aria-label="Refresh payment list"
            className="inline-flex items-center justify-center h-10 w-10 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-50 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2"
          >
            <RefreshCw className={`h-4 w-4 text-[#5e5f5c] ${isFetching ? 'animate-spin' : ''}`} />
          </button>
        }
      />

      {/* Error state — orders endpoint failed */}
      {isError && (
        <div
          role="alert"
          className="flex flex-col items-center justify-center gap-4 py-16 rounded-2xl bg-white border border-red-100"
        >
          <AlertCircle className="h-10 w-10 text-red-400" aria-hidden="true" />
          <div className="text-center">
            <h3 className="text-base font-medium text-[#1c3873] m-0 mb-1">Couldn&apos;t load payments</h3>
            <p className="text-sm text-[#5e5f5c] m-0">Check your connection and try again.</p>
          </div>
          <button
            type="button"
            onClick={() => refetch()}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#1c3873] text-white text-sm font-medium hover:bg-[#15294f] transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2"
          >
            <RefreshCw className="h-4 w-4" /> Try again
          </button>
        </div>
      )}

      {!isError && (
        <>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <SearchInput
          value={search}
          onChange={handleSearch}
          placeholder="Search orders..."
          className="max-w-sm"
        />
        <FilterBar
          filters={[
            {
              key: 'status',
              label: 'Status',
              options: STATUS_FILTER_OPTIONS,
              value: statusFilter,
              onChange: setStatusFilter,
            },
            {
              key: 'provider',
              label: 'Provider',
              options: PROVIDER_FILTER_OPTIONS,
              value: providerFilter,
              onChange: setProviderFilter,
            },
          ]}
          showClear={statusFilter !== 'all' || providerFilter !== 'all'}
          onClear={handleClearFilters}
        />
      </div>

      <DataTable
        columns={columns}
        data={filtered}
        keyExtractor={(o) => o.orderId ?? ''}
        isLoading={isLoading}
        onRowClick={(o) => setSelectedOrderId(o.orderId ?? null)}
        emptyIcon={<CreditCard className="h-10 w-10" />}
        emptyTitle="No orders found"
        emptyDescription="Payment records will appear here once orders are placed."
      />

      <OrderDetailDialog
        orderId={selectedOrderId}
        open={!!selectedOrderId}
        onClose={() => setSelectedOrderId(null)}
      />
        </>
      )}
    </div>
  )
}
