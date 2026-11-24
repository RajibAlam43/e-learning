'use client'

import { useLocale, useTranslations } from 'next-intl'

import { useAuthStore } from '@/store/auth.store'
import { useStudentDashboard, useMyCourses, useLiveClasses, useStudentAnnouncements, useLearningStreak } from '@/hooks/useStudentDashboard'
import { ErrorState } from '@/components/shared/ErrorState'
import ContinuedCourses from '@/components/shared/ContinuedCourses'
import ContinuedCoursesEmpty from '@/components/shared/ContinuedCoursesEmpty'
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

export default function DashboardComponent() {
  const ui = useTranslations('privateUi')
  const locale = useLocale()

  const user = useAuthStore((s) => s.user)
  const userName = user?.fullName?.split(' ')[0] || ui('student')

  const { data: dashboard, isLoading: dashLoading, isError: dashError, refetch: dashRefetch } = useStudentDashboard()
  const { data: myCourses, isLoading: coursesLoading } = useMyCourses()
  const { data: liveClasses, isLoading: liveLoading } = useLiveClasses()
  const { data: announcementPage } = useStudentAnnouncements(0, 4)
  const { data: streakData } = useLearningStreak()

  const ongoingCourses = dashboard?.ongoingCourses ?? []
  const upcomingClasses = dashboard?.upcomingLiveClasses ?? liveClasses ?? []

  const dashboardStats = [
    { icon: '/images/ion_book.svg', iconBg: 'bg-blue-50', iconAlt: ui('activeCourses'), value: dashboard?.totalEnrolledCourses ?? 0, label: ui('activeCourses2') },
    { icon: '/images/ph_video-fill.svg', iconBg: 'bg-indigo-50', iconAlt: ui('lessonsCompleted'), value: dashboard?.completedCourses ?? 0, label: ui('lessonsCompleted2') },
    { icon: '/images/mdi_clock.svg', iconBg: 'bg-orange-50', iconAlt: ui('timeSpent'), value: dashLoading ? '—' : ui('dayCount', { count: streakData?.currentStreak ?? dashboard?.learningStreak ?? 0 }), label: ui('learningStreak') },
  ]

  const coursesList = (myCourses ?? []).filter((c) => c.enrollmentStatus === 'ACTIVE').map((c) => ({
    title: c.courseName ?? '',
    thumbnail: c.courseThumbnailUrl ?? '/images/course-quran.jpg',
    href: `/student/courses/${c.courseId}`,
    progress: Math.round(c.completionPercentage ?? 0),
    lessonsLeft: (c.totalLessons ?? 0) - (c.completedLessons ?? 0),
  }))

  const liveClassesFormatted = upcomingClasses
    .filter((c) => c.status !== 'COMPLETED' && c.status !== 'CANCELLED')
    .slice(0, 4)
    .map((c) => {
      const { day, time, isToday } = formatLiveClassTime(c.startsAt, locale, ui('today'), ui('tomorrow'))
      return {
        title: c.title ?? '',
        instructor: c.instructorName ?? '',
        day,
        time,
        isToday,
      }
    })

  const announcements = (announcementPage?.content ?? []).map((item) => ({
    title: item.title ?? ui('announcement'),
    description: item.content ?? '',
    time: item.createdAt ? new Date(item.createdAt).toLocaleDateString(locale, { month: 'short', day: 'numeric' }) : '',
  }))

  // Show error state if the main dashboard fetch fails
  if (dashError && !dashLoading) {
    return (
      <div className='flex items-center justify-center py-20'>
        <ErrorState
          message={ui('failedToLoadYourDashboardPleaseTryAgain')}
          onRetry={() => dashRefetch()}
        />
      </div>
    )
  }

  return (
    <div className='flex flex-col gap-6'>
      {/* Left Section  */}
      <div className='flex flex-col md:flex-row gap-6'>
        <div className='flex-1 flex flex-col 2xl:w-2/3 gap-6'>
          {/* Continued Courses */}
          {dashLoading ? (
            <ContinuedCourses courses={[]} userName={userName} isLoading />
          ) : ongoingCourses.length > 0 ? (
            <ContinuedCourses courses={ongoingCourses} userName={userName} />
          ) : (
            <ContinuedCoursesEmpty userName={userName} />
          )}

          <StatCards stats={dashboardStats} isLoading={dashLoading} />

          <Divider />

          {/* Your Courses */}
          {coursesLoading ? (
            <CourseCardList title={ui('yourCourses')} viewAllHref='/student/courses' courses={[]} isLoading />
          ) : coursesList.length > 0 ? (
            <CourseCardList title={ui('yourCourses')} viewAllHref='/student/courses' courses={coursesList} />
          ) : (
            <EmptyCoursesCard />
          )}

          <Divider />

          {/* Live Classes + Announcements */}
          <div className='flex flex-col lg:flex-row gap-3'>
            {liveLoading ? (
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
          <DailyAyah isLoading={dashLoading} />
          <LearningStreak days={streakData?.currentStreak ?? dashboard?.learningStreak ?? 0} isLoading={dashLoading} />
          <WeeklyTimeChart isLoading={dashLoading} />
          <SupportCard isLoading={dashLoading} />
        </div>
      </div>
    </div>
  )
}
