'use client'
import Image from 'next/image'
import Link from 'next/link'
import { ChevronRight } from 'lucide-react'
import { useLocale, useTranslations } from 'next-intl'
import { usePlatformStats } from '@/hooks/useCourses'

function useStudentMilestone() {
  const locale = useLocale(), t = useTranslations('studentMilestone')
  const { data, isError } = usePlatformStats()
  const students = data?.students
  if (isError || typeof students !== 'number' || !Number.isFinite(students) || students < 1000) return null
  const count = new Intl.NumberFormat(locale).format(Math.floor(students / 1000))
  return t('count', { count })
}

export function StudentMilestoneBadge() {
  const t = useTranslations('hero')
  return <div className="inline-flex items-center gap-2 rounded-full border border-solid border-white/20 bg-white/20 px-2 py-1 backdrop-blur-sm">
    <div className="flex items-center -space-x-[5px]" aria-hidden="true">{[1, 2, 3, 4].map(index => <Image key={index} src={`/images/avatar-${index}.jpg`} alt="" width={24} height={24} className="h-6 w-6 rounded-full border border-white object-cover" />)}</div>
    <p className="whitespace-nowrap font-sans text-sm font-medium leading-5 text-[var(--color-secondary-blue-50)]">{t('tagline')}</p>
  </div>
}

export function StudentMilestoneJoinLink() {
  const t = useTranslations('testimonials'), count = useStudentMilestone()
  return <Link href="/signup" className="btn-blue-gradient inline-flex h-[46px] items-center justify-center gap-2 rounded-md border-none px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white no-underline transition-all duration-200 hover:opacity-90 hover:scale-[1.02]">{count ? t('ctaWithCount', { count }) : t('cta')}<ChevronRight aria-hidden="true" className="h-6 w-6" /></Link>
}
