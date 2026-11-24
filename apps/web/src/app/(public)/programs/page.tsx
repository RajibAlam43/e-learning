import type { Metadata } from 'next'
import { generatePageMetadata } from '@/config/seo.config'
import CollectionsClient from '../collections/CollectionsClient'

export const metadata: Metadata = generatePageMetadata('programs')

export default function ProgramsPage() {
  return <CollectionsClient />
}
