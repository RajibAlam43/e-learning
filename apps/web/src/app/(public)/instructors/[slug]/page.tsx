import type { Metadata } from 'next'
import { notFound } from 'next/navigation'
import { getLocale, getTranslations } from 'next-intl/server'
import { getServerInstructor, isNotFoundError } from '@/services/public-server.service'
import { detailMetadata } from '@/lib/detail-metadata'
import { instructorJsonLd, serializeJsonLd } from '@/lib/json-ld'
import { PublicDetailUnavailable } from '@/components/public/PublicDetailUnavailable'
import InstructorDetailClient from './InstructorDetailClient'

type Props = { params: Promise<{ slug: string }> }

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params
  const locale = await getLocale()
  const t = await getTranslations('publicDetail')
  try {
    const instructor = await getServerInstructor(slug, locale)
    return detailMetadata(instructor.displayName || instructor.fullName || t('instructor'), instructor.headline || instructor.about, `/instructors/${encodeURIComponent(slug)}`, instructor.avatarUrl, 'profile')
  } catch (error) {
    return { title: isNotFoundError(error) ? t('instructorNotFound') : t('unavailable'), robots: { index: false, follow: false } }
  }
}

export default async function InstructorDetailPage({ params }: Props) {
  const { slug } = await params
  const locale = await getLocale()
  let instructor
  try {
    instructor = await getServerInstructor(slug, locale)
  } catch (error) {
    if (isNotFoundError(error)) notFound()
    return <PublicDetailUnavailable />
  }
  if (!instructor) notFound()
  return (
    <>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: serializeJsonLd(instructorJsonLd(instructor)) }} />
      <InstructorDetailClient slug={slug} initialData={instructor} />
    </>
  )
}
