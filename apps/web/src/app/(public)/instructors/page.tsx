import type { Metadata } from 'next'
import { generatePageMetadata } from '@/config/seo.config'
import InstructorsClient from './InstructorsClient'

export const metadata: Metadata = generatePageMetadata('instructors')

export default function InstructorsPage() {
  return <InstructorsClient />
}
