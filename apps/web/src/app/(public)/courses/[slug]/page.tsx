import type { Metadata } from 'next'
import { notFound } from 'next/navigation'
import { getLocale, getTranslations } from 'next-intl/server'
import { getServerCourse, isNotFoundError } from '@/services/public-server.service'
import { detailMetadata } from '@/lib/detail-metadata'
import { courseJsonLd, serializeJsonLd } from '@/lib/json-ld'
import { PublicDetailUnavailable } from '@/components/public/PublicDetailUnavailable'
import CourseDetailClient from './CourseDetailClient'

type Props = { params: Promise<{ slug: string }> }

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params
  const locale = await getLocale()
  const t = await getTranslations('publicDetail')
  try {
    const course = await getServerCourse(slug, locale)
    return detailMetadata(course.title || t('course'), course.shortDescription || course.description, `/courses/${encodeURIComponent(slug)}`, course.thumbnailUrl)
  } catch (error) {
    return { title: isNotFoundError(error) ? t('courseNotFound') : t('unavailable'), robots: { index: false, follow: false } }
  }
}

export default async function CourseDetailPage({ params }: Props) {
  const { slug } = await params
  const locale = await getLocale()
  let course
  try {
    course = await getServerCourse(slug, locale)
  } catch (error) {
    if (isNotFoundError(error)) notFound()
    return <PublicDetailUnavailable />
  }
  if (!course) notFound()
  return (
    <>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: serializeJsonLd(courseJsonLd(course)) }} />
      <CourseDetailClient slug={slug} initialData={course} />
    </>
  )
}
