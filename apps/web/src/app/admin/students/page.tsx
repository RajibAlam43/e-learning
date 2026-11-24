'use client'

import { useState, useMemo, useCallback } from 'react'
import { Users, RefreshCw, AlertCircle } from 'lucide-react'
import { format } from 'date-fns'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { SearchInput } from '@/components/admin/shared/SearchInput'
import { DataTable, type Column, type RowAction } from '@/components/admin/shared/DataTable'
import { useAdminOrders } from '@/hooks/useAdmin'
import { deriveOrderCustomers, customerEmail, type OrderCustomer } from '@/lib/order-reporting'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'

function formatDate(date: string): string {
  try {
    return format(new Date(date), 'MMM d, yyyy')
  } catch {
    return '—'
  }
}

export default function AdminStudentsPage() {
  const { data: orders, isLoading, isError, isFetching, refetch } = useAdminOrders()
  const [search, setSearch] = useState('')
  const [selectedStudent, setSelectedStudent] = useState<OrderCustomer | null>(null)

  const students = useMemo(() => {
    if (!orders) return []
    return deriveOrderCustomers(orders)
  }, [orders])

  const filtered = useMemo(() => {
    if (!search) return students
    const q = search.toLowerCase()
    return students.filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        s.email.toLowerCase().includes(q),
    )
  }, [students, search])

  const studentOrders = useMemo(() => {
    if (!selectedStudent || !orders) return []
    return orders.filter(
      (o) => customerEmail(o) === selectedStudent.email,
    )
  }, [selectedStudent, orders])

  const handleSearch = useCallback((value: string) => {
    setSearch(value)
  }, [])

  const columns: Column<OrderCustomer>[] = [
    { key: 'name', header: 'Name', render: (s) => <span className="font-medium">{s.name}</span> },
    { key: 'email', header: 'Email', render: (s) => s.email, hideOnMobile: true },
    { key: 'orders', header: 'Total Orders', render: (s) => s.orderCount },
    {
      key: 'spent',
      header: 'Paid Amount',
      render: (s) => <span className="font-medium">৳{s.totalSpent.toLocaleString()}</span>,
    },
    {
      key: 'lastOrder',
      header: 'Last Order Date',
      render: (s) => (s.lastOrder ? formatDate(s.lastOrder) : '—'),
      hideOnMobile: true,
    },
  ]

  const actions: RowAction<OrderCustomer>[] = [
    { label: 'View Orders', onClick: (s) => setSelectedStudent(s) },
  ]

  return (
    <div className="flex flex-col gap-6 w-full max-w-[1152px] mx-auto px-4 sm:px-6">
      <AdminPageHeader
        title="Order Customers"
        description="Customers identified by email in order history. Customers without an email and accounts with no orders are not included."
        action={
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            aria-label="Refresh customer list"
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
            <h3 className="text-base font-medium text-[#1c3873] m-0 mb-1">Couldn&apos;t load customers</h3>
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

      {/* Stat card + Search + Table — hidden when orders endpoint fails */}
      {!isError && (
        <>
          {/* Stat card */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-4 flex items-center gap-3">
              <div className="p-3 rounded-lg bg-blue-50 flex items-center justify-center">
                <Users className="h-6 w-6 text-blue-600" aria-hidden="true" />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-2xl font-medium font-heading text-[#1c3873]">
                  {isLoading ? '—' : students.length}
                </span>
                <span className="text-xs text-[#5e5f5c]">Order Customers (email)</span>
              </div>
            </div>
          </div>

          {/* Search */}
          <SearchInput
            value={search}
            onChange={handleSearch}
            placeholder="Search by name or email..."
            className="max-w-sm"
          />

          {/* Table */}
          <DataTable
            columns={columns}
            data={filtered}
            keyExtractor={(s) => s.email}
            isLoading={isLoading}
            actions={actions}
            onRowClick={(s) => setSelectedStudent(s)}
            emptyIcon={<Users className="h-10 w-10" />}
            emptyTitle="No order customers found"
            emptyDescription="Customers will appear here when an order includes their email."
          />
        </>
      )}

      {/* Student orders dialog */}
      <Dialog
        open={!!selectedStudent}
        onOpenChange={(open) => { if (!open) setSelectedStudent(null) }}
      >
        <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="font-heading text-[#1c3873]">
              {selectedStudent?.name}&apos;s Orders
            </DialogTitle>
          </DialogHeader>

          <div className="mt-2 space-y-1 text-sm text-[#5e5f5c]">
            <p>Email: {selectedStudent?.email}</p>
            <p>Paid amount: ৳{selectedStudent?.totalSpent.toLocaleString()}</p>
          </div>

          <div className="mt-4 space-y-3">
            {studentOrders.length === 0 && (
              <p className="text-sm text-[#71717a]">No orders found.</p>
            )}
            {studentOrders.map((o) => (
              <div
                key={o.orderId}
                className="rounded-xl border border-[#f0f0f0] p-3 text-sm"
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium text-[#1b1c1a]">
                    {o.orderId?.slice(0, 8)}...
                  </span>
                  <StatusBadge status={o.status ?? 'PENDING'} />
                </div>
                <div className="mt-1.5 flex items-center justify-between text-[#5e5f5c]">
                  <span>৳{(o.totalAmount ?? 0).toLocaleString()}</span>
                  <span>{o.createdAt ? formatDate(o.createdAt) : '—'}</span>
                </div>
              </div>
            ))}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
