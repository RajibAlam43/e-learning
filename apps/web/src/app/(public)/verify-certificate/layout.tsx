import type { Metadata } from 'next'
import { generatePageMetadata } from '@/config/seo.config'

export const metadata: Metadata = generatePageMetadata('verifyCertificate')

export default function VerifyCertificateLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return children
}
