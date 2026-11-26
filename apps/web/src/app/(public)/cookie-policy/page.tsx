import type { Metadata } from 'next'
import { PolicyPage } from '@/components/public/legal/PolicyPage'

export const metadata: Metadata = {
  title: 'Cookie Policy',
  description: 'Cookies, authentication, preferences and local browser storage used by Global Islamic Institute.',
  alternates: { canonical: '/cookie-policy' },
}

export default function CookiePolicyPage() { return <PolicyPage kind='cookies' /> }
