import type { Metadata } from 'next'
import { PolicyPage } from '@/components/public/legal/PolicyPage'

export const metadata: Metadata = {
  title: 'Privacy Policy',
  description: 'How Global Islamic Institute handles account, learning, Google Calendar and Meet information.',
  alternates: { canonical: '/privacy-policy' },
}

export default function PrivacyPolicyPage() { return <PolicyPage kind='privacy' /> }
