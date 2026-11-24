import type { Metadata } from 'next'
import { generatePageMetadata } from '@/config/seo.config'
import CollectionsClient from './CollectionsClient'

export const metadata: Metadata = generatePageMetadata('programs', {
  alternates: { canonical: '/programs' },
  robots: { index: false, follow: true },
})
export default function CollectionsPage() { return <CollectionsClient /> }
