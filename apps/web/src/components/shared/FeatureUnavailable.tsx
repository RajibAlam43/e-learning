'use client'

import Link from 'next/link'
import { ClipboardList, MessageSquare } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { EmptyState } from './EmptyState'

export function FeatureUnavailable({ feature }: { feature: 'assignments' | 'messages' }) {
  const t = useTranslations('featuresUnavailable')
  const Icon = feature === 'assignments' ? ClipboardList : MessageSquare
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-heading font-medium text-[#1c3873]">{t(`${feature}Title`)}</h1>
      <EmptyState icon={<Icon className="h-10 w-10" />} title={t('unavailable')} description={t(`${feature}Description`)} />
      <Link href="/contact" className="self-start text-sm font-semibold text-[#1d4ed8] hover:underline">{t('contactSupport')}</Link>
    </div>
  )
}
