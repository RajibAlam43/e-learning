'use client'

import { useRouter } from 'next/navigation'
import { useLocale } from 'next-intl'
import { setLocaleCookie } from '@/lib/locale-cookie'

const LOCALES = [
  { code: 'bn', label: 'বাংলা' },
  { code: 'en', label: 'English' },
]

export function LanguageSwitcher() {
  const router = useRouter()
  const currentLocale = useLocale()

  const switchLocale = (locale: string) => {
    setLocaleCookie(locale)
    router.refresh()
  }

  return (
    <div className="flex items-center gap-1">
      {LOCALES.map((l) => (
        <button
          key={l.code}
          type="button"
          lang={l.code}
          aria-pressed={currentLocale === l.code}
          onClick={() => switchLocale(l.code)}
          className={`rounded px-2 py-1 text-xs font-sans transition-colors hover:bg-[var(--color-neutral-100)] ${
            currentLocale === l.code
              ? 'font-semibold text-[#1d4ed8] bg-[var(--color-brand-primary-50)]'
              : 'text-[var(--color-text-body)]'
          }`}
        >
          {l.label}
        </button>
      ))}
    </div>
  )
}
