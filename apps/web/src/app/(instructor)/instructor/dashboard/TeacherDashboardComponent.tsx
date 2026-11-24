'use client'

import { useLocale, useTranslations } from 'next-intl'

import { useAuthStore } from '@/store/auth.store'
import { useInstructorDashboard } from '@/hooks/useInstructorDashboard'
import { ErrorState } from '@/components/shared/ErrorState'
import TeacherScheduledClasses from '@/components/shared/TeacherScheduledClasses'
import TeacherScheduledClassesEmpty from '@/components/shared/TeacherScheduledClassesEmpty'
import StatCards from '@/components/shared/StatCards'
import CourseCardList from '@/components/shared/CourseCardList'
import UpcomingLiveClasses from '@/components/shared/UpcomingLiveClasses'
import LatestAnnouncements from '@/components/shared/LatestAnnouncements'
import EmptyCoursesCard from '@/components/shared/EmptyCoursesCard'
import LiveClassesEmpty from '@/components/shared/LiveClassesEmpty'
import DailyAyah from '@/components/shared/DailyAyah'
import LearningStreak from '@/components/shared/LearningStreak'
import WeeklyTimeChart from '@/components/shared/WeeklyTimeChart'
import SupportCard from '@/components/shared/SupportCard'
import Divider from '@/components/shared/Divider'
import type { StudentCourseSummary } from '@/types'

function formatLiveClassTime(startsAt: string | undefined, locale: string, todayLabel: string, tomorrowLabel: string) {
  if (!startsAt) return { day: '', time: '', isToday: false }
  const date = new Date(startsAt)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()

  const tomorrow = new Date(now)
  tomorrow.setDate(tomorrow.getDate() + 1)
  const isTomorrow = date.toDateString() === tomorrow.toDateString()

  const timeStr = date.toLocaleTimeString(locale, { hour: 'numeric', minute: '2-digit', hour12: true })

  return {
    day: isToday ? todayLabel : isTomorrow ? tomorrowLabel : date.toLocaleDateString(locale, { weekday: 'short', month: 'short', day: 'numeric' }),
    time: timeStr,
    isToday,
  }
}

export default function TeacherDashboardComponent() {
  const ui = useTranslations('privateUi')
  const locale = useLocale()

  const user = useAuthStore((s) => s.user)
  const userName = user?.fullName?.split(' ')[0] || ui('teacher')

  const { data: dashboard, isLoading, isError, refetch } = useInstructorDashboard()

  const assignedCourses = dashboard?.assignedCourses ?? []
  const upcomingClasses = dashboard?.upcomingLiveClasses ?? []

  // Count today's live classes
  const now = new Date()
  const liveClassesToday = upcomingClasses.filter(
    (c) => c.startsAt && new Date(c.startsAt).toDateString() === now.toDateString()
  ).length

  const dashboardStats = [
    { icon: '/images/ion_book.svg', iconBg: 'bg-blue-50', iconAlt: ui('assignedCourses2'), value: dashboard?.totalCoursesAssigned ?? 0, label: ui('assignedCourses2') },
    { icon: '/images/ph_video-fill.svg', iconBg: 'bg-indigo-50', iconAlt: ui('liveClassesToday'), value: liveClassesToday, label: ui('liveClassesToday') },
    { icon: '/images/user-icon.png', iconBg: 'bg-orange-50', iconAlt: ui('totalStudents'), value: dashboard?.totalStudentsAcrossAllCourses ?? 0, label: ui('totalStudents') },
  ]

  // Map assigned courses to the shape CourseCardList expects.
  // InstructorCourseSnapshotResponse has no thumbnailUrl or completion %,
  // so we omit progress/lessonsLeft (both optional on CourseCardList).
  const coursesList = assignedCourses.map((c) => ({
    title: c.courseName ?? '',
    thumbnail: '/images/course-quran.jpg',
    href: c.courseSlug ? `/courses/${encodeURIComponent(c.courseSlug)}` : '/courses',
  }))

  // TeacherScheduledClasses expects StudentCourseSummary[]; the underlying
  // TeacherCourseStrip only renders a few fields, so we map minimally.
  // TODO: refactor TeacherScheduledClasses to accept a generic course shape.
  const scheduledForStrip = assignedCourses.map((c) => ({
    courseId: c.courseId,
    courseName: c.courseName,
    courseSlug: c.courseSlug,
    courseThumbnailUrl: '/images/course-quran.jpg',
  })) as unknown as StudentCourseSummary[]

  const liveClassesFormatted = upcomingClasses
    .filter((c) => c.status !== 'COMPLETED' && c.status !== 'CANCELLED')
    .slice(0, 4)
    .map((c) => {
      const { day, time, isToday } = formatLiveClassTime(c.startsAt, locale, ui('today'), ui('tomorrow'))
      return {
        title: c.title ?? '',
        instructor: dashboard?.instructorName ?? '',
        day,
        time,
        isToday,
      }
    })

  // No announcements endpoint exists yet — pass empty array instead of fake data.
  const announcements: { title: string; description: string; time: string }[] = []

  // Show error state if the dashboard fetch fails
  if (isError && !isLoading) {
    return (
      <div className='flex items-center justify-center py-20'>
        <ErrorState
          message={ui('failedToLoadYourDashboardPleaseTryAgain')}
          onRetry={() => refetch()}
        />
      </div>
    )
  }

  return (
    <div className='flex flex-col gap-6'>
      <div className='flex flex-col md:flex-row gap-6'>
        <div className='flex-1 flex flex-col 2xl:w-2/3 gap-6'>
          {/* Teacher Scheduled Classes */}
          {isLoading ? (
            <TeacherScheduledClasses courses={[]} userName={userName} isLoading />
          ) : scheduledForStrip.length > 0 ? (
            <TeacherScheduledClasses courses={scheduledForStrip} userName={userName} />
          ) : (
            <TeacherScheduledClassesEmpty userName={userName} />
          )}

          <StatCards stats={dashboardStats} isLoading={isLoading} />

          <Divider />

          {/* Assigned Courses */}
          {isLoading ? (
            <CourseCardList title={ui('assignedCourses')} viewAllHref='/instructor/courses' courses={[]} isLoading />
          ) : coursesList.length > 0 ? (
            <CourseCardList title={ui('assignedCourses')} viewAllHref='/instructor/courses' courses={coursesList} />
          ) : (
            <EmptyCoursesCard />
          )}

          <Divider />

          {/* Live Classes + Announcements */}
          <div className='flex flex-col lg:flex-row gap-3'>
            {isLoading ? (
              <UpcomingLiveClasses classes={[]} isLoading />
            ) : liveClassesFormatted.length > 0 ? (
              <UpcomingLiveClasses classes={liveClassesFormatted} />
            ) : (
              <LiveClassesEmpty />
            )}
            <LatestAnnouncements announcements={announcements} />
          </div>
        </div>

        {/* Right section */}
        <div className='w-full md:w-80 2xl:w-1/3 flex flex-col gap-6'>
          <DailyAyah isLoading={isLoading} />
          {/* Learning streak / weekly time aren't in InstructorDashboardResponse —
              keeping the widgets with neutral defaults until backend adds them. */}
          <LearningStreak days={0} isLoading={false} />
          <WeeklyTimeChart isLoading={false} />
          <SupportCard isLoading={isLoading} />
        </div>
      </div>
    </div>
  )
}
