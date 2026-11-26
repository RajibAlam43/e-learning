import type { Metadata } from 'next'
import { PolicyPage } from '@/components/public/legal/PolicyPage'

export const metadata: Metadata = {
  title: 'Terms of Service',
  description: 'Account eligibility, course access, payments and live-class terms for Global Islamic Institute.',
  alternates: { canonical: '/terms' },
}

export default function TermsPage() { return <PolicyPage kind='terms' /> }
