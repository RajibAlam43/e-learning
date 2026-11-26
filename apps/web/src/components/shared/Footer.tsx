'use client'

import Link from 'next/link'
import Image from 'next/image'
import { ArrowUp } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { usePublicSettings } from '@/hooks/useSettings'

const LOCATIONS = ['London, United Kingdom', 'Dhaka, Bangladesh']

function SocialIcon({ icon }: { icon: string }) {
  switch (icon) {
    case 'email':
      return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4">
          <rect width="20" height="16" x="2" y="4" rx="2" />
          <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
        </svg>
      )
    case 'facebook':
      return (
        <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
          <path d="M9.101 23.691v-7.98H6.627v-3.667h2.474v-1.58c0-4.085 1.848-5.978 5.858-5.978.401 0 1.094.042 1.541.083v3.387h-1.127c-1.485 0-2.048.714-2.048 2.041v2.048h3.1l-.532 3.667h-2.568v7.98C18.503 22.905 24 17.554 24 11.025 24 4.932 18.627 0 12 0S0 4.932 0 11.025c0 5.863 4.192 10.726 9.101 11.666z" />
        </svg>
      )
    case 'instagram':
      return (
        <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
          <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z" />
        </svg>
      )
    case 'youtube':
      return (
        <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
          <path d="M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
        </svg>
      )
    case 'whatsapp':
      return (
        <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
          <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
        </svg>
      )
    default:
      return null
  }
}

export function Footer() {
  const t = useTranslations('footer')
  const settingsQuery = usePublicSettings()

  const settings = Object.fromEntries(
    (settingsQuery.data ?? []).flatMap((setting) =>
      setting.key && setting.value && typeof setting.value === 'object' && !Array.isArray(setting.value)
        ? Object.entries(setting.value).map(([key, value]) => [`${setting.key}.${key}`, value])
        : [],
    ),
  ) as Record<string, unknown>
  const value = (key: string) => typeof settings[key] === 'string' ? settings[key] as string : ''
  const email = value('site.contact.primaryEmail')
  const whatsappNumber = value('site.contact.whatsappNumber').replace(/[^\d]/g, '')
  const socialLinks = [
    email ? { label: 'Email', href: `mailto:${email}`, icon: 'email' } : null,
    value('site.social.facebookUrl') ? { label: 'Facebook', href: value('site.social.facebookUrl'), icon: 'facebook' } : null,
    value('site.social.instagramUrl') ? { label: 'Instagram', href: value('site.social.instagramUrl'), icon: 'instagram' } : null,
    value('site.social.youtubeUrl') ? { label: 'YouTube', href: value('site.social.youtubeUrl'), icon: 'youtube' } : null,
    value('site.social.whatsappUrl')
      ? { label: 'WhatsApp', href: value('site.social.whatsappUrl'), icon: 'whatsapp' }
      : whatsappNumber ? { label: 'WhatsApp', href: `https://wa.me/${whatsappNumber}`, icon: 'whatsapp' } : null,
  ].filter((item): item is { label: string; href: string; icon: string } => item !== null)
  const legalHref = (key: string) => value(key) || ({ 'site.legal.privacyPolicyUrl': '/privacy-policy', 'site.legal.termsUrl': '/terms', 'site.legal.cookiePolicyUrl': '/cookie-policy' } as Record<string, string>)[key] || '/contact'

  const footerSections = [
    {
      title: t('quickLinks'),
      items: [
        { label: t('courses'), href: '/courses' },
        { label: t('programs'), href: '/programs' },
        { label: t('scholarships'), href: '/programs' },
        { label: t('specialCourses'), href: '/courses' },
      ],
    },
    {
      title: t('support'),
      items: [
        { label: t('helpCenter'), href: '/contact' },
        { label: t('studentPortal'), href: '/login' },
        { label: t('admissions'), href: '/contact' },
      ],
    },
    {
      title: t('legal'),
      items: [
        { label: t('privacy'), href: legalHref('site.legal.privacyPolicyUrl') },
        { label: t('terms'), href: legalHref('site.legal.termsUrl') },
        { label: t('cookie'), href: legalHref('site.legal.cookiePolicyUrl') },
      ],
    },
  ]

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <footer className="relative flex w-full flex-col items-start bg-white">
      {/* Main content */}
      <div className="container mx-auto flex flex-col gap-6 md:gap-12 px-5 py-16 lg:flex-row lg:items-start lg:gap-[80px]">
        {/* Left column */}
        <div className="flex w-full flex-col items-start gap-4 md:gap-6 lg:w-[316px] lg:shrink-0 lg:gap-[23px]">
          <div className="flex flex-col items-start gap-3">
            <Image
              src="/images/logo.svg"
              alt="GII Logo"
              width={93}
              height={63}
              className="h-[63px] w-auto object-contain"
            />
            <span className="hidden md:hidden font-heading text-[20px] text-[var(--color-text-headline-primary)]">
              Global Islamic Institute
            </span>
          </div>
          <p className="self-stretch font-sans text-[14px] leading-[150%] text-[var(--color-text-body-secondary)]">
            {t('description')}
          </p>
          <nav aria-label="Social media" className="flex items-start gap-4 self-stretch">
            {socialLinks.map(({ label, href, icon }) => (
              <a
                key={label}
                href={href}
                aria-label={label}
                target={href.startsWith('http') ? '_blank' : undefined}
                rel={href.startsWith('http') ? 'noopener noreferrer' : undefined}
                className="flex h-10 w-10 items-center justify-center rounded-full bg-[#fff5eb] text-[var(--color-text-headline-primary)] transition-all duration-200 hover:scale-110 hover:opacity-70"
              >
                <SocialIcon icon={icon} />
              </a>
            ))}
          </nav>
        </div>

        {/* Right columns */}
        <div className="flex w-full flex-row justify-between gap-4 md:gap-8 sm:flex-row sm:gap-8 lg:flex-1 lg:flex-row lg:items-start lg:justify-between">
          {footerSections.map((section) => (
            <nav
              key={section.title}
              aria-label={section.title}
              className="flex flex-col items-start gap-4 sm:w-1/3 lg:gap-6"
            >
              <span className="self-stretch font-sans text-[16px] font-semibold leading-[24px] text-[var(--color-text-headline-primary)]">
                {section.title}
              </span>
              <div className="flex flex-col items-start gap-2 md:gap-4">
                {section.items.map((item) => (
                  <Link
                    key={item.label}
                    href={item.href}
                    className="self-stretch font-sans text-[14px] leading-[150%] text-[var(--color-text-body-secondary)] transition-colors duration-200 hover:text-[var(--color-secondary-blue-600)]"
                  >
                    {item.label}
                  </Link>
                ))}
              </div>
            </nav>
          ))}
        </div>

      </div>

      {/* Scroll to top */}
      <button
        type="button"
        onClick={scrollToTop}
        aria-label={t('scrollToTop')}
        className="absolute bottom-4 right-4 flex h-[54px] w-[49px] flex-col items-center rounded-lg bg-[var(--color-secondary-blue-600)] py-1 shadow-[0px_6px_15px_-2px_rgba(16,24,40,0.08)] transition-all duration-200 hover:scale-105 hover:shadow-lg cursor-pointer lg:right-8 lg:top-[263px] lg:bottom-auto"
      >
        <ArrowUp className="h-6 w-6 text-white" />
        <span className="font-sans text-center text-[12px] leading-[14px] text-white">
          {t('scrollToTop')}
        </span>
      </button>

      {/* Bottom bar */}
      <div className="flex w-full flex-col items-center justify-between gap-4 border-t border-[#f2f1ed] bg-[#f2f1ed] px-5 py-8 sm:flex-row sm:px-12">
        <p className="font-sans text-[12px] leading-5 text-[var(--color-text-headline-primary)]">
          {t('rights')}
        </p>
        <address className="not-italic">
          <div className="flex flex-col items-center gap-2 sm:flex-row sm:items-start sm:gap-8">
            {LOCATIONS.map((location) => (
              <span
                key={location}
                className="font-sans text-[12px] leading-5 text-[var(--color-text-headline-primary)]"
              >
                {location}
              </span>
            ))}
          </div>
        </address>
      </div>
    </footer>
  )
}
