import type { Metadata } from 'next'
import { HeroSection } from '@/components/public/HeroSection'
import { StatsSection } from '@/components/public/StatsSection'
import { AboutSection } from '@/components/public/AboutSection'
import { ProgramsSection } from '@/components/public/ProgramsSection'
import { SpecialCoursesSection } from '@/components/public/SpecialCoursesSection'
import { CourseCarouselSection } from '@/components/public/CourseCarouselSection'
import { LearningSystemSection } from '@/components/public/LearningSystemSection'
import { QuoteSection } from '@/components/public/QuoteSection'
import { PlatformFeaturesSection } from '@/components/public/PlatformFeaturesSection'
import { BlogSection } from '@/components/public/BlogSection'
import { TestimonialsSection } from '@/components/public/TestimonialsSection'
import { PartnersSection } from '@/components/public/PartnersSection'
import { CTASection } from '@/components/public/CTASection'
import { generatePageMetadata } from '@/config/seo.config'

export const metadata: Metadata = generatePageMetadata('home')

export default function HomePage() {
  return (
    <main>
      <HeroSection />
      <StatsSection />
      <AboutSection />
      <ProgramsSection />
      <SpecialCoursesSection />
      <CourseCarouselSection />
      <LearningSystemSection />
      <QuoteSection />
      <PartnersSection />
      <PlatformFeaturesSection />
      <BlogSection />
      <TestimonialsSection />
      <CTASection />
    </main>
  )
}
