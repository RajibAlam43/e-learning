'use client'

import { useEffect } from 'react'
import { AlertCircle, RefreshCw } from 'lucide-react'

/**
 * Route-level error boundary for /admin/dashboard.
 * Catches uncaught errors during render and offers a retry.
 */
export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    // Log to monitoring (Sentry, etc.) when wired up.
    console.error('Dashboard error:', error)
  }, [error])

  return (
    <div className='flex flex-col items-center justify-center gap-4 py-20 px-4 max-w-[1152px] mx-auto'>
      <AlertCircle className='h-12 w-12 text-red-400' aria-hidden='true' />
      <h2 className='text-lg font-heading font-medium text-[#1c3873] m-0'>
        Something went wrong loading the dashboard
      </h2>
      <p className='text-sm text-[#5e5f5c] text-center max-w-md m-0'>
        {error.message || 'An unexpected error occurred.'}
      </p>
      {error.digest && (
        <p className='text-[11px] text-gray-400 font-mono m-0'>Error ID: {error.digest}</p>
      )}
      <button
        type='button'
        onClick={reset}
        className='inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#1c3873] text-white text-sm font-medium hover:bg-[#15294f] transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2'
      >
        <RefreshCw className='h-4 w-4' aria-hidden='true' />
        Try again
      </button>
    </div>
  )
}
