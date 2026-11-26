'use client'

import { useState, useCallback } from 'react'
import { Users, RefreshCw, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react'
import { format } from 'date-fns'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { SearchInput } from '@/components/admin/shared/SearchInput'
import { DataTable, type Column } from '@/components/admin/shared/DataTable'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import { useAdminUsers } from '@/hooks/useAdmin'
import type { AdminUserSummary, AdminUsersParams } from '@/types'

function formatDate(date: string): string {
  try {
    return format(new Date(date), 'MMM d, yyyy')
  } catch {
    return '—'
  }
}

const roleLabels: Record<string, string> = {
  STUDENT: 'Student',
  INSTRUCTOR: 'Teacher',
  TEACHER: 'Teacher',
  ADMIN: 'Admin',
  SUPER_ADMIN: 'Super admin',
}

const columns: Column<AdminUserSummary>[] = [
  {
    key: 'name', header: 'Name',
    render: (user) => <span className="font-medium [overflow-wrap:anywhere]">{user.fullName}</span>,
  },
  {
    key: 'email', header: 'Email',
    render: (user) => <span className="break-all">{user.email ?? '—'}</span>,
  },
  { key: 'phone', header: 'Phone', render: (user) => user.phone ?? '—' },
  {
    key: 'roles', header: 'Roles',
    render: (user) => user.roles.map((role) => roleLabels[role] ?? role).join(', ') || '—',
  },
  { key: 'status', header: 'Status', render: (user) => <StatusBadge status={user.status} /> },
  {
    key: 'joined', header: 'Registered', className: 'whitespace-nowrap',
    render: (user) => formatDate(user.createdAt),
  },
]

const PAGE_BUTTON = 'inline-flex h-10 items-center justify-center gap-1 rounded-lg border border-[#e5e5e5] bg-white px-3 text-sm font-medium text-[#1c3873] transition-colors hover:bg-[#f8faff] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50'

export default function AdminStudentsPage() {
  const [params, setParams] = useState<AdminUsersParams>({ search: '', page: 0, size: 20 })
  const { data, isLoading, isError, isFetching, refetch } = useAdminUsers(params)

  const handleSearch = useCallback((value: string) => {
    const search = value.trim()
    setParams((current) => current.search === search ? current : { ...current, search, page: 0 })
  }, [])

  const users = data?.content ?? []
  const total = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0
  const pageUnavailable = Boolean(data && params.page > 0 && params.page >= totalPages)

  return (
    <div className="flex flex-col gap-6 w-full max-w-[1152px] mx-auto px-4 sm:px-6">
      <AdminPageHeader
        title="Students"
        description="Accounts with the Student role, including those without orders or with additional roles."
        action={
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            aria-label="Refresh student list"
            className="inline-flex items-center justify-center h-10 w-10 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-50 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2"
          >
            <RefreshCw className={`h-4 w-4 text-[#5e5f5c] ${isFetching ? 'animate-spin' : ''}`} aria-hidden="true" />
          </button>
        }
      />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-4 flex items-center gap-3">
          <div className="p-3 rounded-lg bg-blue-50 flex items-center justify-center">
            <Users className="h-6 w-6 text-blue-600" aria-hidden="true" />
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-2xl font-medium font-heading text-[#1c3873]">
              {!data || isError ? '—' : total.toLocaleString()}
            </span>
            <span className="text-xs text-[#5e5f5c]">{params.search ? 'Matching students' : 'Total students'}</span>
          </div>
        </div>
      </div>

      <SearchInput
        onChange={handleSearch}
        placeholder="Search by name, email or phone..."
        ariaLabel="Search students by name, email or phone"
        className="w-full max-w-md"
      />

      {isError ? (
        <div role="alert" className="flex flex-col items-center justify-center gap-4 py-16 px-4 text-center rounded-2xl bg-white border border-red-100">
          <AlertCircle className="h-10 w-10 text-red-400" aria-hidden="true" />
          <div>
            <h3 className="text-base font-medium text-[#1c3873] m-0 mb-1">Couldn&apos;t load students</h3>
            <p className="text-sm text-[#5e5f5c] m-0">Check your connection and try again.</p>
          </div>
          <button type="button" onClick={() => refetch()} disabled={isFetching} className={PAGE_BUTTON}>
            <RefreshCw className="h-4 w-4" aria-hidden="true" /> Try again
          </button>
        </div>
      ) : (
        <section aria-label="Student directory" aria-busy={isFetching} className="flex flex-col gap-4">
          <DataTable
            columns={columns}
            data={users}
            keyExtractor={(user) => user.userId}
            isLoading={isLoading}
            emptyIcon={<Users className="h-10 w-10" aria-hidden="true" />}
            emptyTitle={pageUnavailable ? 'No students on this page' : params.search ? 'No matching students' : 'No students found'}
            emptyDescription={pageUnavailable ? 'Return to the previous page to see the remaining students.' : params.search ? 'Try a different name, email or phone number.' : 'Registered accounts with the Student role will appear here.'}
          />

          {data && (
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p role="status" className="text-sm text-[#5e5f5c] tabular-nums">
                {users.length > 0
                  ? `${params.page * data.size + 1}–${params.page * data.size + users.length} of ${total.toLocaleString()} students`
                  : '0 students on this page'}
              </p>
              {(totalPages > 1 || pageUnavailable) && (
                <nav aria-label="Student pagination" className="flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    disabled={isFetching || params.page === 0}
                    onClick={() => setParams((current) => ({ ...current, page: Math.max(0, Math.min(current.page - 1, totalPages - 1)) }))}
                    className={PAGE_BUTTON}
                  >
                    <ChevronLeft className="h-4 w-4" aria-hidden="true" /> Previous
                  </button>
                  <span className="text-sm text-[#5e5f5c] tabular-nums">
                    {pageUnavailable ? 'Page unavailable' : `Page ${params.page + 1} of ${totalPages}`}
                  </span>
                  <button
                    type="button"
                    disabled={isFetching || params.page >= totalPages - 1}
                    onClick={() => setParams((current) => ({ ...current, page: current.page + 1 }))}
                    className={PAGE_BUTTON}
                  >
                    Next <ChevronRight className="h-4 w-4" aria-hidden="true" />
                  </button>
                </nav>
              )}
            </div>
          )}
        </section>
      )}
    </div>
  )
}
