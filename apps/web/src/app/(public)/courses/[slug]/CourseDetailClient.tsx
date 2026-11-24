'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useCourse, useCourses, usePublicCourseReviews } from '@/hooks/useCourses'
import { useMyCourses } from '@/hooks/useStudentDashboard'
import { useAuthStore } from '@/store/auth.store'
import type { CourseDetails } from '@/types'
import { getLessonEmbedUrl } from '@/lib/video-embed'
import { ChevronRight, Play, PlayCircle } from 'lucide-react'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { ErrorState } from '@/components/shared/ErrorState'
import { CourseOverviewRequirements } from '@/components/public/CourseOverviewRequirements'
import { CourseOutcomes } from '@/components/public/CourseOutcomes'
import { CourseContents } from '@/components/public/CourseContents'
import { CurriculumStructure } from '@/components/public/CurriculumStructure'
import { ExpertInstructor } from '@/components/public/ExpertInstructor'
import { RelatedCoursesSection } from '@/components/public/RelatedCoursesSection'
import { TestimonialsSection } from '@/components/public/TestimonialsSection'
import { CTASection } from '@/components/public/CTASection'
import { Star } from 'lucide-react'
import { useCreateCourseReview } from '@/hooks/useStudentDashboard'
import { useState } from 'react'
import { useTranslations } from 'next-intl'
import {
  HeroSkeleton,
  OverviewSkeleton,
  OutcomesSkeleton,
  ContentsSkeleton,
  CurriculumSkeleton,
  InstructorSkeleton,
  RelatedCoursesSkeleton,
} from '@/components/public/CourseDetailSkeletons'

interface CourseDetailClientProps {
  slug: string
  initialData: CourseDetails
}

export default function CourseDetailClient({ slug, initialData }: CourseDetailClientProps) {
  const ui = useTranslations('privateUi')

  const { data: course, isLoading, isPending, isFetching, isError, error, refetch } = useCourse(slug, initialData)
  const { data: relatedData } = useCourses({ level: course?.level, size: 10 })
  const reviews = usePublicCourseReviews(slug)
  const t = useTranslations()

  // Check if the logged-in user is already enrolled in this course.
  const user = useAuthStore((s) => s.user)
  const { data: myCourses } = useMyCourses()
  const isEnrolled = !!(user && myCourses?.some(
    (c) =>
      (c.courseSlug === slug || c.courseId === course?.id) &&
      c.enrollmentStatus === 'ACTIVE',
  ))
  const [rating, setRating] = useState(5)
  const [reviewText, setReviewText] = useState('')
  const [isVideoOpen, setIsVideoOpen] = useState(false)
  const createReview = useCreateCourseReview()
  const submitReview = async () => {
    if (!course?.id || reviewText.trim().length < 10) return
    try {
      await createReview.mutateAsync({ courseId: course.id, data: { rating, reviewText: reviewText.trim() } })
      setReviewText('')
    } catch { /* toast handled by API boundary */ }
  }

  if (!course && (isPending || isLoading || isFetching)) {
    return (
      <main className="min-h-screen bg-white">
        <HeroSkeleton />
        <OverviewSkeleton />
        <OutcomesSkeleton />
        <ContentsSkeleton />
        <CurriculumSkeleton />
        <InstructorSkeleton />
        <RelatedCoursesSkeleton />
      </main>
    )
  }

  if (!course && isError) {
    return (
      <main className="min-h-screen bg-white">
        <section
          className="pt-[48px] h-[458px] bg-cover bg-center"
          style={{ backgroundImage: "url('/images/course-detail-page-hero.png')" }}
        >
          <div className="mx-auto max-w-[1440px] px-4 py-10 sm:px-6 lg:px-16">
            <ErrorState message={error?.message ?? t('courses.detail.notFound')} onRetry={() => refetch()} />
          </div>
        </section>
      </main>
    )
  }

  if (!course) return null

  const isFree = (course.priceBdt ?? -1) === 0
  const introVideoUrl = getLessonEmbedUrl(course.video)
  const priceDisplay = isFree
    ? t('courses.detail.relatedTitle.free')
    : course.priceBdt != null
      ? `${course.priceBdt.toLocaleString()} BDT`
      : t('courses.detail.relatedTitle.tba')

  const relatedCourses = relatedData?.content
    ?.filter((c) => c.id !== course.id)
    .map((c) => ({
      id: c.id,
      title: c.title,
      slug: c.slug,
      thumbnailUrl: c.thumbnailUrl,
      priceBdt: c.priceBdt,
      level: c.level,
    })) ?? []

  return (
    <main className="min-h-screen bg-white">
      <section
        className="pt-[48px] pb-16 bg-cover bg-center"
        style={{ backgroundImage: "url('/images/course-detail-page-hero.png')" }}
      >
        <div className="container mx-auto px-5 flex flex-col items-start gap-20">
          <section className="flex w-full flex-col items-start gap-12">
            <div className="flex items-center gap-2">
              <Link href="/courses" className="group flex items-center gap-2 no-underline hover:opacity-80 transition-all duration-200 flex gap-2 items-center">
                <Image src="/Back-Icon.svg" alt="" width={24} height={24} className="h-6 w-6 cursor-pointer transition-transform duration-200 group-hover:-translate-x-1" />
              <span className="underline leading-6 font-medium text-white font-sans">{t('courses.detail.breadcrumbCourses')}</span>
              </Link>
            </div>

            <div className="flex w-full flex-col items-start gap-4">
              {course.level && (
                <span className="inline-flex items-center justify-center rounded-[4px] bg-[#d97706] px-2 py-1 font-sans text-xs leading-5 font-medium text-[#fffbeb]">
                  {course.level}
                </span>
              )}
              <h1 className="m-0 w-full text-[52px] font-medium leading-[56px] tracking-[-1px] text-white font-heading max-[750px]:text-[42px] max-[750px]:leading-[45px] max-[450px]:text-[31px] max-[450px]:leading-[34px]">
                {course.title}
              </h1>
              {course.shortDescription && (
                <p className="text-lg leading-7 text-[#d4d4d4] font-sans max-[675px]:w-full">
                  {course.shortDescription}
                </p>
              )}
            </div>
          </section>

          <section className="flex w-full h-[220px] items-start flex-wrap overflow-hidden rounded-xl border-2 border-white gap-y-5 max-lg:h-[84px] max-lg:flex-nowrap max-lg:items-stretch mb-[-105px] lg:mb-[-170px]">
            <div className="relative h-[220px] w-[220px] shrink-0 bg-[#e8ebf1] max-lg:h-[84px] max-lg:w-[118px]">
              {course.thumbnailUrl && (
                <Image src={course.thumbnailUrl} alt={course.title ?? ui('course')} fill className="object-cover" />
              )}
              {introVideoUrl && (
                <button
                  type="button"
                  onClick={() => setIsVideoOpen(true)}
                  aria-label={ui('playCoursePreviewVideo')}
                  className="group absolute inset-0 flex cursor-pointer items-center justify-center bg-black/10 transition-colors duration-200 hover:bg-black/30"
                >
                  <span className="flex h-16 w-16 items-center justify-center rounded-full bg-white/90 shadow-lg transition-transform duration-200 group-hover:scale-110 max-lg:h-11 max-lg:w-11">
                    <Play className="ml-1 h-7 w-7 fill-[#1c3873] text-[#1c3873] max-lg:h-5 max-lg:w-5" />
                  </span>
                </button>
              )}
            </div>

            <div className="flex flex-1 flex-col items-start justify-between gap-5 bg-white p-8 min-w-[419px] max-[750px]:min-w-full max-[450px]:p-5 h-full max-lg:hidden">
              <h3 className="m-0 w-full text-2xl font-medium leading-8 text-[#1c3873] font-heading max-[450px]:text-[19px] max-[450px]:leading-[26px]">
                {t('courses.detail.secureYourSeat')}
              </h3>
              <div className="flex w-full items-start gap-[11.5px] opacity-[0.9] text-base text-[#5e5f5c] font-sans max-lg:flex-row max-lg:flex-wrap">
                <div className="flex flex-1 flex-col gap-3 min-w-[185px]">
                  <div className="flex w-full items-center gap-2 max-[450px]:flex-wrap">
                    <Image src="/images/check-mark.png" alt="" width={18} height={18} className="h-[18px] w-[18px] shrink-0" />
                    <span className="flex-1 leading-6 font-medium inline-block min-w-[168px]">{t('courses.detail.includes.dashboard')}</span>
                  </div>
                  <div className="flex w-full items-center gap-2 max-[450px]:flex-wrap">
                    <Image src="/images/check-mark.png" alt="" width={18} height={18} className="h-[18px] w-[18px] shrink-0" />
                    <span className="flex-1 leading-6 font-medium inline-block min-w-[139px]">{t('courses.detail.includes.liveFeedback')}</span>
                  </div>
                </div>
                <div className="flex flex-1 flex-col gap-3 min-w-[185px]">
                  <div className="flex w-full items-center gap-2 max-[450px]:flex-wrap">
                    <Image src="/images/check-mark.png" alt="" width={18} height={18} className="h-[18px] w-[18px] shrink-0" />
                    <span className="flex-1 leading-6 font-medium inline-block min-w-[134px]">{t('courses.detail.includes.audioPortal')}</span>
                  </div>
                  <div className="flex w-full items-center gap-2 max-[450px]:flex-wrap">
                    <Image src="/images/check-mark.png" alt="" width={18} height={18} className="h-[18px] w-[18px] shrink-0" />
                    <span className="flex-1 leading-6 font-medium inline-block min-w-[118px]">{t('courses.detail.includes.certificate')}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex w-[319px] flex-col items-center justify-center gap-8 bg-[#ffefcc] p-8 text-center max-lg:w-auto max-lg:flex-1 max-lg:flex-row max-lg:items-center max-lg:justify-between max-lg:gap-3 max-lg:p-3">
              <div className="flex w-full flex-col items-center justify-center gap-2 max-lg:w-auto">
                <div className="opacity-[0.6] max-lg:hidden">
                  <span className="text-lg leading-7 font-medium text-[#5e5f5c] font-sans">{t('courses.detail.completeCourse')}</span>
                </div>
                <h2 className="m-0 text-[18px] leading-[26px] lg:text-[36px] tracking-[-1px] lg:leading-[44px] font-semibold text-[#1b1c1a] md:text-[29px] md:leading-[35px]">
                  {priceDisplay}
                </h2>
              </div>
              <Link
                href={isEnrolled ? `/student/courses/${course?.id ?? slug}` : `/courses/${slug}/checkout`}
                className="flex h-11 w-full items-center justify-center gap-2 rounded-[6px] px-5 py-2.5 text-base font-semibold text-white no-underline transition-all duration-200 hover:opacity-90 hover:shadow-lg hover:scale-[1.02] cursor-pointer max-lg:w-auto max-lg:shrink-0 max-lg:px-4 max-lg:text-sm"
                style={{ background: 'linear-gradient(19.99deg, #4f67d8, #3a4fc2 60%, #2e3fa8)', boxShadow: '0 -1px 0 0 rgba(0,0,0,0.18) inset, 0 1px 0 0 rgba(255,255,255,0.12) inset' }}
              >
                {isEnrolled ? (
                  <>
                    <PlayCircle className="h-5 w-5" />
                    {t('courses.detail.continueLearning')}
                  </>
                ) : (
                  <>
                    <span className="max-lg:hidden">{t('courses.detail.enroll')}</span>
                    <span className="lg:hidden">{t('courses.detail.enrollShort')}</span>
                    <ChevronRight className="h-6 w-6" />
                  </>
                )}
              </Link>
            </div>
          </section>
        </div>
      </section>

      {introVideoUrl && (
        <Dialog open={isVideoOpen} onOpenChange={setIsVideoOpen}>
          <DialogContent className="w-full gap-0 bg-black p-2 sm:max-w-4xl">
            <DialogTitle className="sr-only">{ui('coursePreview')}</DialogTitle>
            <div className="aspect-video w-full overflow-hidden rounded-lg bg-black">
              <iframe
                src={introVideoUrl}
                title={`${course.title ?? 'Course'} preview video`}
                className="h-full w-full"
                allowFullScreen
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                referrerPolicy="strict-origin-when-cross-origin"
              />
            </div>
          </DialogContent>
        </Dialog>
      )}
      <CourseOverviewRequirements description={course.description ?? undefined} requirements={course.requirements ?? undefined} />
      <CourseOutcomes outcomes={course.courseOutcomes ?? undefined} />
      <CourseContents recordedHoursCount={course.recordedHoursCount ?? undefined} liveSessionCount={course.liveSessionCount ?? undefined} />
      <CurriculumStructure sections={course.sections ?? undefined} />
      <ExpertInstructor instructors={course.instructors ?? undefined} />
      <section className="container mx-auto px-5 py-12">
        <div className="w-full">
          <h2 className="font-heading text-2xl font-semibold text-[#1c3873]">{ui('studentReviews')}</h2>
          {reviews.isLoading && <p className="mt-4 text-sm text-slate-500">{ui('loadingReviews')}</p>}
          {reviews.isError && <p className="mt-4 text-sm text-red-700">{ui('reviewsAreTemporarilyUnavailable')}</p>}
          {!reviews.isLoading && !reviews.isError && (reviews.data ?? []).length === 0 && <p className="mt-4 text-sm text-slate-500">{ui('noPublishedReviewsYet')}</p>}
          <div className="mt-5 grid gap-4">{(reviews.data ?? []).map(review => <article key={review.reviewId} className="rounded-xl border bg-white p-5"><div className="flex items-center gap-1 text-amber-500">{Array.from({length:5},(_,i)=><Star key={i} className={i < (review.rating ?? 0) ? 'h-4 w-4 fill-current' : 'h-4 w-4'} />)}</div><p className="mt-3 text-sm text-slate-700">{review.reviewText}</p><p className="mt-3 text-xs text-slate-500">{review.studentName ?? ui('student')}</p></article>)}</div>
          {isEnrolled && user && <form onSubmit={event => { event.preventDefault(); void submitReview() }} className="mt-8 rounded-xl border bg-slate-50 p-5"><h3 className="font-semibold text-[#1c3873]">{ui('shareYourExperience')}</h3><div className="mt-4 flex gap-1">{Array.from({length:5},(_,i)=><button type="button" key={i} aria-label={`${i+1} stars`} onClick={() => setRating(i+1)}><Star className={i < rating ? 'h-6 w-6 fill-amber-400 text-amber-400' : 'h-6 w-6 text-slate-300'} /></button>)}</div><textarea required minLength={10} value={reviewText} onChange={event => setReviewText(event.target.value)} rows={4} className="mt-4 w-full rounded-lg border bg-white p-3 text-sm" placeholder={ui('tellOtherLearnersWhatYouFoundUseful')} /><div className="mt-3 flex justify-end"><button disabled={createReview.isPending || reviewText.trim().length < 10} className="rounded-lg bg-[#1c3873] px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{createReview.isPending ? ui('submitting') : ui('submitReview')}</button></div></form>}
        </div>
      </section>
      <RelatedCoursesSection courses={relatedCourses} />
      <TestimonialsSection />
      <CTASection />
    </main>
  )
}
