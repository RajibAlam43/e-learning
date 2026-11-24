'use client'

import { useState, useEffect, useRef } from 'react'
import dynamic from 'next/dynamic'
import { useLocale } from 'next-intl'
import { useQueryClient } from '@tanstack/react-query'
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client'
import { getQueryClient, CACHE_BUSTER } from '@/lib/query-client'
import { getQueryPersister } from '@/lib/query-persister'
import { setApiLocale } from '@/lib/api-client'
import { AuthProvider } from '@/providers/auth.provider'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'

const ReactQueryDevtools = process.env.NODE_ENV === 'development'
  ? dynamic(
      () => import('@tanstack/react-query-devtools').then((module) => module.ReactQueryDevtools),
      { ssr: false },
    )
  : null

/** Keeps the apiClient's Accept-Language header in sync with next-intl's locale.
 *  When locale changes, invalidates ALL queries so data refetches in the new language. */
function LocaleSync() {
  const locale = useLocale()
  const queryClient = useQueryClient()
  const previousLocale = useRef(locale)

  useEffect(() => {
    setApiLocale(locale)
    if (previousLocale.current !== locale) {
      previousLocale.current = locale
      queryClient.invalidateQueries()
    }
  }, [locale, queryClient])

  return null
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => getQueryClient())
  const [persister] = useState(() => getQueryPersister())

  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{
        persister: persister!,
        buster: CACHE_BUSTER,
        maxAge: 1000 * 60 * 60 * 24,
        dehydrateOptions: {
          // Keep TanStack's default success-only rule alongside the key filter.
          // Persisting `pending` queries makes a later rejection surface as
          // "a query that was dehydrated as pending ended up rejecting", and
          // persisting errors would cache a failure for the full 24h maxAge.
          shouldDehydrateQuery: (query) => {
            const [scope, kind] = query.queryKey
            return query.state.status === 'success' && (
              scope === 'public' || scope === 'categories' || scope === 'instructors' ||
              (scope === 'courses' && (kind === 'public' || kind === 'featured'))
            )
          },
        },
      }}
    >
      <AuthProvider>
        <TooltipProvider>
          <LocaleSync />
          {children}
          <Toaster position="top-right" richColors />
        </TooltipProvider>
      </AuthProvider>
      {ReactQueryDevtools && <ReactQueryDevtools initialIsOpen={false} />}
    </PersistQueryClientProvider>
  )
}
