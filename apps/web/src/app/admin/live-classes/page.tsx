'use client'

import { useState, useMemo, useCallback } from 'react'
import dynamic from 'next/dynamic'
import { Video, RefreshCw, AlertCircle } from 'lucide-react'
import { format } from 'date-fns'
import { toast } from 'sonner'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { SearchInput } from '@/components/admin/shared/SearchInput'
import { FilterBar } from '@/components/admin/shared/FilterBar'
import { DataTable, type Column, type RowAction } from '@/components/admin/shared/DataTable'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import { useAdminLiveClasses, useStartLiveClass } from '@/hooks/useAdmin'
import type { AdminLiveClassSummary } from '@/types'
import { CreateLiveClassDialog } from './CreateLiveClassDialog'

// Lazy-load the edit dialog — it has form state + mutation.
const EditLiveClassDialog = dynamic(
  () => import('./EditLiveClassDialog').then((m) => m.EditLiveClassDialog),
  { ssr: false },
)

const STATUS_FILTER_OPTIONS = [
  { value: 'SCHEDULED', label: 'Scheduled' },
  { value: 'LIVE', label: 'Live' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

function formatDateTime(date: string | undefined): string {
  if (!date) return '—'
  try {
    return format(new Date(date), 'MMM d, yyyy h:mm a')
  } catch {
    return '—'
  }
}

function isUpcomingWithin24h(dateStr: string | undefined): boolean {
  if (!dateStr) return false
  try {
    const d = new Date(dateStr)
    const now = new Date()
    const diff = d.getTime() - now.getTime()
    return diff > 0 && diff < 24 * 60 * 60 * 1000
  } catch {
    return false
  }
}

export default function AdminLiveClassesPage() {
  const { data: liveClasses, isLoading, isError, isFetching, refetch } = useAdminLiveClasses()
  const startMutation = useStartLiveClass()

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [editTarget, setEditTarget] = useState<AdminLiveClassSummary | null>(null)

  const filtered = useMemo(() => {
    if (!liveClasses) return []

    return liveClasses.filter((lc) => {
      if (statusFilter !== 'all' && lc.status !== statusFilter) return false
      if (search) {
        const q = search.toLowerCase()
        const matchesTitle = lc.title?.toLowerCase().includes(q)
        const matchesCourse = lc.courseName?.toLowerCase().includes(q)
        const matchesInstructor = lc.instructorName?.toLowerCase().includes(q)
        if (!matchesTitle && !matchesCourse && !matchesInstructor) return false
      }
      return true
    })
  }, [liveClasses, statusFilter, search])

  const handleSearch = useCallback((value: string) => {
    setSearch(value)
  }, [])

  function handleStartClass(lc: AdminLiveClassSummary) {
    if (!lc.liveClassId) return

    // Confirm before starting — this opens a real meeting room.
    const ok = window.confirm(
      `Start "${lc.title ?? 'this class'}" now? This will open the meeting room for ${lc.approvedRegistrants ?? 0} registered student(s).`,
    )
    if (!ok) return

    startMutation.mutate(lc.liveClassId, {
      onSuccess: (data) => {
        const url = data?.hostStartUrl
        toast.success(
          url
            ? 'Class started — opening meeting room…'
            : 'Class started successfully',
        )
        if (url) {
          window.open(url, '_blank', 'noopener,noreferrer')
        }
      },
      onError: (err) => {
        const e = err as { message?: string }
        toast.error(e?.message || 'Failed to start the live class')
      },
    })
  }

  const columns: Column<AdminLiveClassSummary>[] = [
    {
      key: 'title',
      header: 'Title',
      render: (lc) => {
        const upcoming = isUpcomingWithin24h(lc.startsAt)
        return (
          <div className={`flex items-center gap-2 ${upcoming ? 'pl-2 border-l-2 border-blue-500' : ''}`}>
            <span className="font-medium">{lc.title ?? '—'}</span>
          </div>
        )
      },
    },
    {
      key: 'course',
      header: 'Course',
      render: (lc) => lc.courseName ?? '—',
      hideOnMobile: true,
    },
    {
      key: 'instructor',
      header: 'Instructor',
      render: (lc) => lc.instructorName ?? '—',
      hideOnMobile: true,
    },
    {
      key: 'status',
      header: 'Status',
      render: (lc) => <StatusBadge status={lc.status ?? 'SCHEDULED'} />,
    },
    {
      key: 'scheduledTime',
      header: 'Scheduled Time',
      render: (lc) => formatDateTime(lc.startsAt),
    },
    {
      key: 'students',
      header: 'Students',
      render: (lc) => lc.approvedRegistrants ?? 0,
    },
  ]

  const actions: RowAction<AdminLiveClassSummary>[] = [
    {
      label: 'Start Class',
      onClick: (lc) => {
        if (lc.status !== 'SCHEDULED') {
          toast.error('Only scheduled classes can be started')
          return
        }
        handleStartClass(lc)
      },
    },
    {
      label: 'Edit',
      onClick: (lc) => setEditTarget(lc),
    },
  ]

  return (
    <div className="flex flex-col gap-6 w-full max-w-[1152px] mx-auto px-4 sm:px-6">
      <AdminPageHeader
        title="Live Classes"
        action={
          <div className='flex items-center gap-2'>
            <button
              type='button'
              onClick={() => refetch()}
              disabled={isFetching}
              aria-label="Refresh live classes list"
              className="inline-flex items-center justify-center h-10 w-10 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-50 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2"
            >
              <RefreshCw className={`h-4 w-4 text-[#5e5f5c] ${isFetching ? 'animate-spin' : ''}`} />
            </button>
            <CreateLiveClassDialog />
          </div>
        }
      />

      {/* Error state */}
      {isError && (
        <div
          role="alert"
          className="flex flex-col items-center justify-center gap-4 py-16 rounded-2xl bg-white border border-red-100"
        >
          <AlertCircle className="h-10 w-10 text-red-400" aria-hidden="true" />
          <div className="text-center">
            <h3 className="text-base font-medium text-[#1c3873] m-0 mb-1">Couldn&apos;t load live classes</h3>
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
              placeholder="Search classes..."
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
              ]}
              showClear={statusFilter !== 'all'}
              onClear={() => setStatusFilter('all')}
            />
          </div>

          <DataTable
            columns={columns}
            data={filtered}
            keyExtractor={(lc) => lc.liveClassId ?? ''}
            isLoading={isLoading}
            actions={actions}
            emptyIcon={<Video className="h-10 w-10" />}
            emptyTitle="No live classes found"
            emptyDescription="Live classes will appear here once created."
          />

          <EditLiveClassDialog
            liveClass={editTarget}
            open={!!editTarget}
            onClose={() => setEditTarget(null)}
          />
        </>
      )}
    </div>
  )
}
