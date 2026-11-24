'use client'

import { useMemo, useState, useCallback } from 'react'
import dynamic from 'next/dynamic'
import { format } from 'date-fns'
import { toast } from 'sonner'
import { Users, RefreshCw, AlertCircle } from 'lucide-react'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { SearchInput } from '@/components/admin/shared/SearchInput'
import { DataTable, type Column, type RowAction } from '@/components/admin/shared/DataTable'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import { useAdminInstructors, useUpdateInstructor, useDeleteInstructor } from '@/hooks/useAdmin'
import type { AdminInstructorSummary } from '@/types'
import { CreateTeacherDialog } from './CreateTeacherDialog'

// Lazy-load the edit dialog — it has a detail-fetch hook + 13 fields.
const EditTeacherDialog = dynamic(
  () => import('./EditTeacherDialog').then((m) => m.EditTeacherDialog),
  { ssr: false },
)

export default function TeachersPage() {
  const { data: instructors, isLoading, isError, isFetching, refetch } = useAdminInstructors()
  const updateInstructor = useUpdateInstructor()
  const deleteInstructor = useDeleteInstructor()
  const [search, setSearch] = useState('')
  const [editId, setEditId] = useState<string | null>(null)

  const filtered = useMemo(() => {
    if (!instructors) return []
    const q = search.toLowerCase()
    if (!q) return instructors
    return instructors.filter(
      (t) =>
        t.fullName?.toLowerCase().includes(q) ||
        t.email?.toLowerCase().includes(q)
    )
  }, [instructors, search])

  const handleTogglePublic = useCallback(
    async (teacher: AdminInstructorSummary) => {
      if (!teacher.userId) return
      try {
        await updateInstructor.mutateAsync({
          id: teacher.userId,
          data: { isPublic: !teacher.isPublic },
        })
        toast.success(
          teacher.isPublic
            ? 'Teacher set to private'
            : 'Teacher set to public'
        )
      } catch (err) {
        const e = err as { message?: string }
        toast.error(e?.message || 'Failed to update teacher')
      }
    },
    [updateInstructor]
  )

  const columns: Column<AdminInstructorSummary>[] = useMemo(
    () => [
      {
        key: 'name',
        header: 'Name',
        render: (t) => (
          <span className="font-medium text-[#1c3873]">
            {t.fullName ?? '-'}
          </span>
        ),
      },
      {
        key: 'email',
        header: 'Email',
        render: (t) => t.email ?? '-',
        hideOnMobile: true,
      },
      {
        key: 'headline',
        header: 'Headline',
        render: (t) => (
          <span className="line-clamp-1">{t.headline ?? '-'}</span>
        ),
        hideOnMobile: true,
      },
      {
        key: 'courses',
        header: 'Courses',
        render: (t) => t.assignedCoursesCount ?? 0,
      },
      {
        key: 'public',
        header: 'Public',
        render: (t) => (
          <StatusBadge status={t.isPublic ? 'ACTIVE' : 'INACTIVE'} />
        ),
      },
      {
        key: 'created',
        header: 'Created',
        render: (t) =>
          t.createdAt
            ? format(new Date(t.createdAt), 'MMM d, yyyy')
            : '-',
        hideOnMobile: true,
      },
    ],
    []
  )

  const actions: RowAction<AdminInstructorSummary>[] = useMemo(
    () => [
      {
        label: 'Edit',
        onClick: (t) => setEditId(t.userId ?? null),
      },
      {
        label: teacherIsPublicLabel(),
        onClick: (t) => handleTogglePublic(t),
      },
      {
        label: 'Remove instructor role',
        onClick: (t) => {
          if (!t.userId || !window.confirm('Remove the instructor role? The user account will remain.')) return
          deleteInstructor.mutate(t.userId, { onSuccess: () => { toast.success('Instructor role removed'); refetch() }, onError: e => toast.error(e.message) })
        },
      },
    ],
    [handleTogglePublic, deleteInstructor, refetch]
  )

  function teacherIsPublicLabel() {
    return 'Toggle Public/Private'
  }

  const editTeacher = useMemo(
    () => instructors?.find((t) => t.userId === editId) ?? null,
    [instructors, editId]
  )

  return (
    <div className="mx-auto flex w-full max-w-[1152px] flex-col gap-6 px-4 sm:px-6 pb-10">
      <AdminPageHeader
        title="Teachers"
        action={
          <div className='flex items-center gap-2'>
            <button
              type='button'
              onClick={() => refetch()}
              disabled={isFetching}
              aria-label="Refresh teacher list"
              className="inline-flex items-center justify-center h-10 w-10 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-50 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2"
            >
              <RefreshCw className={`h-4 w-4 text-[#5e5f5c] ${isFetching ? 'animate-spin' : ''}`} />
            </button>
            <CreateTeacherDialog />
          </div>
        }
      />

      {isError && (
        <div
          role="alert"
          className="flex flex-col items-center justify-center gap-4 py-16 rounded-2xl bg-white border border-red-100"
        >
          <AlertCircle className="h-10 w-10 text-red-400" aria-hidden="true" />
          <div className="text-center">
            <h3 className="text-base font-medium text-[#1c3873] m-0 mb-1">Couldn&apos;t load teachers</h3>
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
          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Search by name or email..."
            className="max-w-sm"
          />
          <DataTable
            columns={columns}
            data={filtered}
            keyExtractor={(t) => t.userId ?? ''}
            isLoading={isLoading}
            actions={actions}
            emptyIcon={<Users className="h-10 w-10" />}
            emptyTitle="No teachers found"
            emptyDescription="Add a teacher to get started."
          />
          <EditTeacherDialog teacher={editTeacher} onClose={() => setEditId(null)} />
        </>
      )}
    </div>
  )
}
