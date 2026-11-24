'use client'

import { useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'

export function PublicDetailUnavailable() {
  const router = useRouter()
  const t = useTranslations('publicDetail')
  return (
    <main className="mx-auto flex min-h-[60vh] max-w-3xl flex-col items-center justify-center gap-4 px-5 py-20 text-center">
      <h1 className="font-heading text-2xl text-[#1c3873]">{t('unavailable')}</h1>
      <p className="text-sm text-slate-600">{t('retryDescription')}</p>
      <button type="button" onClick={() => router.refresh()} className="rounded-lg bg-[#1c3873] px-4 py-2 text-sm font-semibold text-white">{t('retry')}</button>
    </main>
  )
}
