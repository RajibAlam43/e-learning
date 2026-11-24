'use client'

import { AlertTriangle } from 'lucide-react'
import { useTranslations } from 'next-intl'

interface ErrorStateProps {
  title?: string
  message?: string
  onRetry?: () => void
}

export function ErrorState({
  title,
  message,
  onRetry,
}: ErrorStateProps) {
  const t = useTranslations('common')
  return (
    <div className="flex flex-col items-center justify-center gap-4 py-16 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-50">
        <AlertTriangle className="h-8 w-8 text-[var(--destructive)]" />
      </div>
      <div className="flex flex-col gap-1">
        <h3 className="font-sans text-lg font-semibold text-[var(--color-text-headline-primary)]">
          {title ?? t('error')}
        </h3>
        <p className="text-sm text-[var(--color-text-body-secondary)]">
          {message ?? t('errorDetails')}
        </p>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-2 rounded-md bg-[var(--color-primary)] px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-[var(--color-primary)]/90"
        >
          {t('retry')}
        </button>
      )}
    </div>
  )
}
