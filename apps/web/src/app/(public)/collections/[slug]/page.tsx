import type { Metadata } from 'next'
import { notFound } from 'next/navigation'
import { getLocale, getTranslations } from 'next-intl/server'
import { getServerCollection, isNotFoundError } from '@/services/public-server.service'
import { detailMetadata } from '@/lib/detail-metadata'
import { PublicDetailUnavailable } from '@/components/public/PublicDetailUnavailable'

type Props = { params: Promise<{ slug: string }> }

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params
  const locale = await getLocale()
  const t = await getTranslations('publicDetail')
  try {
    const item = await getServerCollection(slug, locale)
    return detailMetadata(item.title || t('program'), item.shortDescription || item.description, `/collections/${encodeURIComponent(slug)}`, item.thumbnailUrl)
  } catch (error) {
    return { title: isNotFoundError(error) ? t('programNotFound') : t('unavailable'), robots: { index: false, follow: false } }
  }
}

export default async function CollectionDetailPage({ params }: Props) {
  const { slug } = await params
  const locale = await getLocale()
  const t = await getTranslations('publicDetail')
  let item
  try {
    item = await getServerCollection(slug, locale)
  } catch (error) {
    if (isNotFoundError(error)) notFound()
    return <PublicDetailUnavailable />
  }
  if (!item) notFound()
  return (
    <main className="mx-auto max-w-5xl px-4 py-12 sm:py-20">
      <p className="text-xs font-semibold uppercase tracking-wide text-[#8c7355]">{item.collectionType}</p>
      <h1 className="mt-2 font-heading text-4xl text-[#1c3873]">{item.title}</h1>
      <p className="mt-4 max-w-3xl text-slate-600">{item.description ?? item.shortDescription}</p>
      <div className="mt-8 rounded-2xl bg-slate-50 p-6">
        <h2 className="font-heading text-2xl text-[#1c3873]">{t('includedCourses')}</h2>
        <ul className="mt-4 divide-y divide-slate-200">
          {(item.courses ?? []).map(course => <li key={course.id} className="py-3"><span className="font-medium">{course.title}</span></li>)}
        </ul>
      </div>
    </main>
  )
}
