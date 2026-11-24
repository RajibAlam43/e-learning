import type { Metadata } from 'next'
import { generatePageMetadata } from '@/config/seo.config'
import CoursesClient from './CoursesClient'

export const metadata: Metadata = generatePageMetadata('courses')

export default function CoursesPage() {
  return <CoursesClient />
}
