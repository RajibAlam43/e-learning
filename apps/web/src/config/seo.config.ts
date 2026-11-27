import type { Metadata } from 'next'
import { APP_CONFIG } from './app.config'

type SeoPageKey =
  | 'faculty'
  | 'home'
  | 'courses'
  | 'instructors'
  | 'login'
  | 'signup'
  | 'blog'
  | 'contact'
  | 'programs'
  | 'verifyCertificate'

interface SeoDefaults {
  title: string
  description: string
  path: string
}

const seoDefaults: Record<SeoPageKey, SeoDefaults> = {
  home: {
    title: `${APP_CONFIG.name} - Online Islamic Education`,
    description: 'Learn Quran, Arabic, Islamic Studies and more from qualified instructors.',
    path: '/',
  },
  courses: {
    title: 'Courses',
    description: 'Browse Islamic courses at Global Islamic Institute - Quran, Arabic, Fiqh, and more.',
    path: '/courses',
  },
  faculty: { title: 'Faculty', description: 'Meet the teachers, staff and boards of Global Islamic Institute.', path: '/faculty' },
  instructors: {
    title: 'Our Instructors',
    description: 'Meet our qualified and experienced Islamic education instructors.',
    path: '/instructors',
  },
  login: {
    title: 'Log In',
    description: 'Sign in to your account to continue learning.',
    path: '/login',
  },
  signup: {
    title: 'Create Account',
    description: 'Join our platform and start your learning journey.',
    path: '/signup',
  },
  blog: {
    title: 'Blog',
    description: 'Read articles and insights on Islamic education.',
    path: '/blog',
  },
  contact: {
    title: 'Contact Us',
    description: 'Get in touch with Global Islamic Institute - we are here to help.',
    path: '/contact',
  },
  programs: {
    title: 'Programs',
    description: 'Explore our comprehensive diploma programs in Islamic disciplines.',
    path: '/programs',
  },
  verifyCertificate: {
    title: 'Verify a Certificate',
    description: 'Verify a Global Islamic Institute certificate using its certificate code.',
    path: '/verify-certificate',
  },
}

export function generatePageMetadata(
  page: SeoPageKey,
  overrides?: Partial<Metadata>,
): Metadata {
  const defaults = seoDefaults[page]
  return {
    title: defaults.title,
    description: defaults.description,
    alternates: {
      canonical: defaults.path,
    },
    openGraph: {
      title: defaults.title,
      description: defaults.description,
      siteName: APP_CONFIG.name,
      url: defaults.path,
      type: 'website',
      images: [
        {
          url: '/images/hero-bg.png',
          width: 1440,
          height: 897,
          alt: APP_CONFIG.name,
        },
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title: defaults.title,
      description: defaults.description,
      images: ['/images/hero-bg.png'],
    },
    ...overrides,
  }
}
