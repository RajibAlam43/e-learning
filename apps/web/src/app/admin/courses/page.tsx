'use client'

import { useMemo, useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { format } from 'date-fns'
import { toast } from 'sonner'
import { BookOpen, RefreshCw, AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { SearchInput } from '@/components/admin/shared/SearchInput'
import { FilterBar } from '@/components/admin/shared/FilterBar'
import { DataTable, type Column, type RowAction } from '@/components/admin/shared/DataTable'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import {
  useAdminCourses,
  usePublishCourse,
  useUnpublishCourse,
  useFeatureCourse,
  useUnfeatureCourse,
} from '@/hooks/useAdmin'
import { ROUTES } from '@/constants/routes'
import type { AdminCourseSummary } from '@/types'
import Link from 'next/link'

const STATUS_OPTIONS = [
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Published', value: 'PUBLISHED' },
  { label: 'Archived', value: 'ARCHIVED' },
]

export default function CoursesPage() {
  const router = useRouter()
  const { data: courses, isLoading, isError, isFetching, refetch } = useAdminCourses()
  const publishCourse = usePublishCourse()
  const unpublishCourse = useUnpublishCourse()
  const featureCourse = useFeatureCourse()
  const unfeatureCourse = useUnfeatureCourse()

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')

  const filtered = useMemo(() => {
    if (!courses) return []
    return courses.filter((c) => {
      if (statusFilter !== 'all' && c.status !== statusFilter) return false
      const q = search.toLowerCase()
      if (q && !c.title?.toLowerCase().includes(q) && !c.instructorName?.toLowerCase().includes(q)) return false
      return true
    })
  }, [courses, search, statusFilter])

  const handleTogglePublish = useCallback(
    async (course: AdminCourseSummary) => {
      if (!course.courseId) return
      try {
        if (course.status === 'PUBLISHED') {
          await unpublishCourse.mutateAsync(course.courseId)
          toast.success('Course unpublished')
        } else {
          await publishCourse.mutateAsync(course.courseId)
          toast.success('Course published')
        }
      } catch {
        toast.error('Failed to update course status')
      }
    },
    [publishCourse, unpublishCourse]
  )

  const columns: Column<AdminCourseSummary>[] = useMemo(
    () => [
      {
        key: 'title',
        header: 'Title',
        render: (c) => (
          <span className="font-medium text-[#1c3873]">{c.title ?? '-'}</span>
        ),
      },
      {
        key: 'instructor',
        header: 'Instructor',
        render: (c) => c.instructorName ?? '-',
        hideOnMobile: true,
      },
      {
        key: 'status',
        header: 'Status',
        render: (c) => <StatusBadge status={c.status ?? 'DRAFT'} />,
      },
      {
        key: 'enrolled',
        header: 'Enrolled',
        render: (c) => c.totalEnrolled ?? 0,
      },
      {
        key: 'price',
        header: 'Price',
        render: (c) => (c.isFree ? 'Free' : `${c.priceBdt ?? 0} BDT`),
        hideOnMobile: true,
      },
      {
        key: 'created',
        header: 'Created',
        render: (c) =>
          c.createdAt ? format(new Date(c.createdAt), 'MMM d, yyyy') : '-',
        hideOnMobile: true,
      },
    ],
    []
  )

  const actions: RowAction<AdminCourseSummary>[] = useMemo(
    () => [
      {
        label: 'View Details',
        onClick: (c) => {
          if (c.courseId) router.push(ROUTES.admin.courseDetail(c.courseId))
        },
      },
      {
        label: 'Publish / Unpublish',
        onClick: (c) => handleTogglePublish(c),
      },
      {
        label: 'Feature / Unfeature',
        onClick: (c) => {
          if (!c.courseId) return
          if (c.isFeatured) {
            unfeatureCourse.mutate(c.courseId, { onSuccess: () => toast.success('Course unfeatured'), onError: e => toast.error(e.message) })
            return
          }
          const raw = window.prompt('Featured position (positive number)', '1')
          const position = raw ? Number(raw) : NaN
          if (!Number.isInteger(position) || position < 1) { toast.error('Enter a positive whole-number position.'); return }
          featureCourse.mutate({ id: c.courseId, data: { position } }, { onSuccess: () => toast.success('Course featured'), onError: e => toast.error(e.message) })
        },
      },
    ],
    [featureCourse, handleTogglePublish, router, unfeatureCourse]
  )

  return (
    <div className="mx-auto flex w-full max-w-[1152px] flex-col gap-6 px-4 sm:px-6 pb-10">
      <AdminPageHeader
        title="Courses"
        action={
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => refetch()}
              disabled={isFetching}
              aria-label="Refresh course list"
              className="inline-flex items-center justify-center h-10 w-10 rounded-lg border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-50 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2"
            >
              <RefreshCw className={`h-4 w-4 text-[#5e5f5c] ${isFetching ? 'animate-spin' : ''}`} />
            </button>
            <Link href={ROUTES.admin.courseCreate}>
              <Button className="btn-blue-gradient text-white gap-1.5">
                <BookOpen className="h-4 w-4" />
                Create Course
              </Button>
            </Link>
          </div>
        }
      />

      {/* Error state — list endpoint failed */}
      {isError && (
        <div
          role="alert"
          className="flex flex-col items-center justify-center gap-4 py-16 rounded-2xl bg-white border border-red-100"
        >
          <AlertCircle className="h-10 w-10 text-red-400" aria-hidden="true" />
          <div className="text-center">
            <h3 className="text-base font-medium text-[#1c3873] m-0 mb-1">Couldn&apos;t load courses</h3>
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
              onChange={setSearch}
              placeholder="Search courses..."
              className="max-w-sm"
            />
            <FilterBar
              filters={[
                {
                  key: 'status',
                  label: 'Status',
                  options: STATUS_OPTIONS,
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
            keyExtractor={(c) => c.courseId ?? ''}
            isLoading={isLoading}
            actions={actions}
            onRowClick={(c) => {
              if (c.courseId) router.push(ROUTES.admin.courseDetail(c.courseId))
            }}
            emptyIcon={<BookOpen className="h-10 w-10" />}
            emptyTitle="No courses found"
            emptyDescription="Create a course to get started."
          />
        </>
      )}
    </div>
  )
}
