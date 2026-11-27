'use client'

import { CourseOfferings } from '@/components/admin/course/CourseOfferings'

import { use } from 'react'
import { useState } from 'react'
import { toast } from 'sonner'
import { ArrowLeft, BookOpen } from 'lucide-react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import { Skeleton } from '@/components/ui/skeleton'
import {
  useAdminCourse,
  usePublishCourse,
  useUnpublishCourse,
  useAdminInstructors,
  useAssignInstructor,
} from '@/hooks/useAdmin'
import { ROUTES } from '@/constants/routes'
import { CourseOverviewTab } from './CourseOverviewTab'
import { CourseCurriculumTab } from './CourseCurriculumTab'
import type { AdminInstructorSummary } from '@/types'

export default function CourseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: course, isLoading, isError } = useAdminCourse(id)
  const publishCourse = usePublishCourse()
  const unpublishCourse = useUnpublishCourse()

  async function handleTogglePublish() {
    if (!course?.courseId) return
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
  }

  if (isLoading) {
    return (
      <div className="mx-auto flex w-full max-w-[1152px] flex-col gap-6 px-4 sm:px-6 pb-10">
        <Skeleton className="h-8 w-40" />
        <Skeleton className="h-10 w-full max-w-md" />
        <Skeleton className="h-[400px] w-full rounded-2xl" />
      </div>
    )
  }

  if (isError || !course) {
    return (
      <div className="mx-auto flex w-full max-w-[1152px] flex-col items-center gap-4 py-20 px-4 sm:px-6">
        <BookOpen className="h-12 w-12 text-[#a3a3a3]" />
        <p className="font-heading text-lg font-medium text-[#1c3873]">
          Course not found
        </p>
        <Link href={ROUTES.admin.courses}>
          <Button variant="outline">Back to Courses</Button>
        </Link>
      </div>
    )
  }

  const isPublished = course.status === 'PUBLISHED'

  return (
    <div className="mx-auto flex w-full max-w-[1152px] flex-col gap-6 px-4 sm:px-6 pb-10">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Link href={ROUTES.admin.courses} aria-label="Back to courses">
            <Button variant="ghost" size="icon" aria-label="Back to courses">
              <ArrowLeft className="h-5 w-5" aria-hidden="true" />
            </Button>
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="font-heading text-2xl font-semibold text-[#1c3873]">
              {course.title}
            </h1>
            <StatusBadge status={course.status ?? 'DRAFT'} />
          </div>
        </div>
        <Button
          onClick={handleTogglePublish}
          disabled={publishCourse.isPending || unpublishCourse.isPending}
          className={isPublished ? '' : 'btn-blue-gradient text-white'}
          variant={isPublished ? 'outline' : 'default'}
        >
          {publishCourse.isPending
            ? 'Publishing…'
            : unpublishCourse.isPending
              ? 'Unpublishing…'
              : isPublished
                ? 'Unpublish'
                : 'Publish'}
        </Button>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="overview">
        <TabsList variant="line">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="curriculum">Curriculum</TabsTrigger>
          <TabsTrigger value="instructors">Instructors</TabsTrigger>
          <TabsTrigger value="cohorts">Cohorts</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <CourseOverviewTab course={course} />
        </TabsContent>

        <TabsContent value="curriculum">
          <CourseCurriculumTab course={course} />
        </TabsContent>

        <TabsContent value="cohorts"><CourseOfferings course={course} /></TabsContent>

        <TabsContent value="instructors">
          <InstructorsTabContent courseId={course.courseId ?? id} instructors={course.instructors ?? []} />
        </TabsContent>
      </Tabs>
    </div>
  )
}

function InstructorsTabContent({
  courseId,
  instructors,
}: {
  courseId: string
  instructors: AdminInstructorSummary[]
}) {
  const [selected, setSelected] = useState('')
  const { data: allInstructors } = useAdminInstructors()
  const assign = useAssignInstructor()
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end gap-2 rounded-2xl bg-white p-5 shadow-sm">
        <label className="grid gap-1 text-sm font-medium text-[#1c3873]">Assign instructor
          <select value={selected} onChange={(e)=>setSelected(e.target.value)} className="h-10 min-w-64 rounded-lg border px-3">
            <option value="">Select instructor</option>
            {(allInstructors??[]).filter((i)=>!instructors.some((a)=>a.userId===i.userId)).map((i)=><option key={i.userId} value={i.userId}>{i.displayName??i.fullName} — {i.email}</option>)}
          </select>
        </label>
        <Button type="button" disabled={!selected||assign.isPending} onClick={()=>assign.mutate({courseId,data:{instructorUserId:selected,role:'PRIMARY'}},{onSuccess:()=>{toast.success('Instructor assigned');setSelected('')},onError:e=>toast.error(e.message)})}>Assign</Button>
      </div>
      <div className="rounded-2xl bg-white shadow-[0px_2px_6px_rgba(16,24,40,0.06)]">
      <div className="border-b border-[#f0f0f0] px-6 py-3.5">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-[#71717a]">
          Assigned Instructors
        </h3>
      </div>
      {instructors.length===0?<p className="px-6 py-10 text-sm text-gray-500">No instructors assigned yet.</p>:<ul className="divide-y divide-[#f0f0f0]">
        {instructors.map((inst) => (
          <li
            key={inst.userId}
            className="flex items-center justify-between px-6 py-4"
          >
            <div className="flex flex-col">
              <span className="text-sm font-medium text-[#1c3873]">
                {inst.fullName ?? inst.displayName ?? '-'}
              </span>
              <span className="text-xs text-[#71717a]">{inst.email ?? '-'}</span>
            </div>
            <StatusBadge status={inst.isPublic ? 'ACTIVE' : 'INACTIVE'} />
          </li>
        ))}
      </ul>}
    </div>
    </div>
  )
}
