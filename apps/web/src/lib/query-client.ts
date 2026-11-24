import { QueryClient } from '@tanstack/react-query'

export const CACHE_BUSTER = `public-only-v2-${process.env.NEXT_PUBLIC_BUILD_ID ?? 'dev'}`

export function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 5 * 60 * 1000,
        gcTime: 1000 * 60 * 60 * 24,
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  })
}

let browserQueryClient: QueryClient | undefined

export function getQueryClient() {
  if (typeof window === 'undefined') {
    return makeQueryClient()
  }
  if (!browserQueryClient) {
    browserQueryClient = makeQueryClient()
  }
  return browserQueryClient
}
