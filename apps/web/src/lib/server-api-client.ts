import axios from 'axios'
import { NETWORK } from '@/config/network.config'

/**
 * Server-side API client for SSR data fetching.
 * No interceptors — used in Server Components only.
 * Passes cookies from the incoming request for auth, and sends the user's
 * locale as Accept-Language so the backend returns localized content.
 */
export function createServerApiClient(cookieHeader?: string, locale?: string) {
  return axios.create({
    baseURL: process.env.API_URL || process.env.NEXT_PUBLIC_API_URL,
    timeout: NETWORK.requestTimeoutMs,
    headers: {
      'Content-Type': 'application/json',
      ...(cookieHeader ? { Cookie: cookieHeader } : {}),
      'Accept-Language': locale === 'en' ? 'en' : 'bn',
    },
    withCredentials: true,
  })
}
