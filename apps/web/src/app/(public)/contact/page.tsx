import type { Metadata } from 'next'
import { ContactHeaderForm } from '@/components/public/contact/ContactHeaderForm'
import { ContactInfoBanner } from '@/components/public/contact/ContactInfoBanner'
import { ContactFAQ } from '@/components/public/contact/ContactFAQ'
import { generatePageMetadata } from '@/config/seo.config'

export const metadata: Metadata = generatePageMetadata('contact')

export default function ContactPage() {
  return (
    <main className="w-full min-h-screen bg-[#f5f5f5]">
      <ContactHeaderForm />
      <ContactInfoBanner />
      <ContactFAQ />
    </main>
  )
}
