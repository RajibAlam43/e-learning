'use client'

import { useState } from 'react'
import { ChevronRight } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { useMutation } from '@tanstack/react-query'
import { collectionService } from '@/services/collection.service'
import { toast } from 'sonner'
import { usePublicSettings } from '@/hooks/useSettings'

const SOCIAL_LINKS = [
  {
    label: 'Facebook',
    svg: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-[18px] w-[26px]">
        <path d="M9.101 23.691v-7.98H6.627v-3.667h2.474v-1.58c0-4.085 1.848-5.978 5.858-5.978.401 0 1.094.042 1.541.083v3.387h-1.127c-1.485 0-2.048.714-2.048 2.041v2.048h3.1l-.532 3.667h-2.568v7.98C18.503 22.905 24 17.554 24 11.025 24 4.932 18.627 0 12 0S0 4.932 0 11.025c0 5.863 4.192 10.726 9.101 11.666z" />
      </svg>
    ),
  },
  {
    label: 'Instagram',
    svg: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
        <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z" />
      </svg>
    ),
  },
  {
    label: 'YouTube',
    svg: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
        <path d="M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
      </svg>
    ),
  },
  {
    label: 'WhatsApp',
    svg: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
        <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
      </svg>
    ),
  },
] as const

const INPUT_CLASSES =
  'h-[52px] w-full rounded-[4px] border border-solid border-[#e5e5e5] bg-white pt-3.5 pb-[13px] px-[17px] font-sans text-sm leading-[150%] text-[#5e5f5c] outline-none transition-all duration-200 focus:border-[#1c3873] focus:shadow-[0_0_0_3px_rgba(28,56,115,0.15)]'

export function ContactHeaderForm() {
  const t = useTranslations('contact')
  const settingsQuery = usePublicSettings()
  const [form, setForm] = useState({
    fullName: '', email: '', phone: '', message: '',
  })
  const submitTicket = useMutation({ mutationFn: collectionService.createSupportTicket })
  const settings = Object.fromEntries(
    (settingsQuery.data ?? []).flatMap((setting) =>
      setting.key && setting.value && typeof setting.value === 'object' && !Array.isArray(setting.value)
        ? Object.entries(setting.value).map(([key, value]) => [`${setting.key}.${key}`, value])
        : [],
    ),
  ) as Record<string, unknown>
  const settingValue = (key: string) => typeof settings[key] === 'string' ? settings[key] as string : ''
  const whatsappNumber = settingValue('site.contact.whatsappNumber').replace(/[^\d]/g, '')
  const socialHrefs: Record<string, string> = {
    Facebook: settingValue('site.social.facebookUrl'),
    Instagram: settingValue('site.social.instagramUrl'),
    YouTube: settingValue('site.social.youtubeUrl'),
    WhatsApp: settingValue('site.social.whatsappUrl') || (whatsappNumber ? `https://wa.me/${whatsappNumber}` : ''),
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await submitTicket.mutateAsync({ name: form.fullName, email: form.email, phone: form.phone || undefined, subject: 'Website contact request', message: form.message })
      setForm({ fullName: '', email: '', phone: '', message: '' })
      toast.success('Your message has been sent.')
    } catch (error) {
      toast.error((error as { message?: string }).message ?? 'Unable to send your message. Please try again.')
    }
  }

  return (
    <section className="w-full bg-white">
      <div className="container mx-auto px-5">
        {/* Header row */}
        <div className="flex items-start justify-between gap-5 border-b border-solid border-[#b9c1d4] pb-[22px] pt-6 md:gap-10 lg:items-center lg:gap-5">
          <h1 className="text-[28px] font-medium leading-[34px] tracking-[-1px] text-[#1c3873] font-heading max-md:text-[31px] max-md:leading-[34px] md:text-[42px] md:leading-[45px] lg:text-[52px] lg:leading-[56px]">
            {t('headingLine1')} <br className="hidden sm:block lg:block" />
            {t('headingLine2')}
          </h1>
          {/* Social icons — horizontal on mobile, vertical on desktop */}
          <div className="flex shrink-0 flex-col items-center gap-6 md:flex md:flex-row md:gap-4 lg:flex-col lg:gap-6">
            {SOCIAL_LINKS.map(({ svg, label }) => {
              const href = socialHrefs[label]
              if (!href) return null
              return (
                <a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={label}
                  className="flex h-[50px] w-[50px] items-center justify-center text-[#5e5f5c] transition-all duration-200 hover:text-[#1c3873] hover:scale-110"
                >
                  {svg}
                </a>
              )
            })}
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-8 py-12 max-md:gap-6 max-md:py-8">
          <div className="grid grid-cols-1 gap-8 max-md:gap-4 md:grid-cols-2 lg:grid-cols-3">
            <div className="flex min-w-0 flex-1 flex-col gap-2">
              <label htmlFor="fullName" className="font-sans text-base font-medium leading-6 text-[#1b1c1a]">
                {t('fullName')} <span className="text-[#ef4444]">*</span>
              </label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                required
                value={form.fullName}
                onChange={handleChange}
                placeholder="Md. Abdur Rahman"
                className={INPUT_CLASSES}
              />
            </div>
            <div className="flex min-w-0 flex-1 flex-col gap-2">
              <label htmlFor="email" className="font-sans text-base font-medium leading-6 text-[#1b1c1a]">
                {t('email')} <span className="text-[#ef4444]">*</span>
              </label>
              <input
                id="email"
                name="email"
                type="email"
                required
                value={form.email}
                onChange={handleChange}
                placeholder="rahman1234@gmail.com"
                className={INPUT_CLASSES}
              />
            </div>
            <div className="flex min-w-0 flex-1 flex-col gap-2 md:col-span-2 lg:col-span-1">
              <label htmlFor="phone" className="font-sans text-base font-medium leading-6 text-[#1b1c1a]">
                {t('phone')}
              </label>
              <input
                id="phone"
                name="phone"
                type="tel"
                value={form.phone}
                onChange={handleChange}
                placeholder="Enter your phone number"
                className={INPUT_CLASSES}
              />
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="message" className="font-sans text-base font-medium leading-6 text-[#1b1c1a]">
              <span>{t('message')}</span> <span className="text-[#ef4444]">*</span>
            </label>
            <textarea
              id="message"
              name="message"
              required
              rows={4}
              value={form.message}
              onChange={handleChange}
              placeholder="Type your message here"
              className="h-[111px] w-full resize-none rounded-[4px] border border-solid border-[#e5e5e5] bg-white p-[17px] font-sans text-sm leading-[150%] text-[#5e5f5c] outline-none transition-all duration-200 focus:border-[#1c3873] focus:shadow-[0_0_0_3px_rgba(28,56,115,0.15)]"
            />
          </div>

          <button
            type="submit"
            disabled={submitTicket.isPending}
            className="flex h-11 w-fit items-center justify-center gap-2 rounded-[6px] border-none px-5 py-2.5 font-sans text-base font-semibold leading-6 text-white transition-all duration-200 hover:scale-[1.02] hover:shadow-[0px_4px_12px_rgba(79,103,216,0.3)] disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:scale-100 disabled:hover:shadow-none"
            style={{
              background: 'linear-gradient(19.99deg, #4f67d8, #3a4fc2 60%, #2e3fa8)',
              boxShadow: submitTicket.isPending ? undefined : 'inset 0px -1px 0px rgba(0,0,0,0.18), inset 0px 1px 0px rgba(255,255,255,0.12)',
            }}
          >
            {submitTicket.isPending ? 'Sending…' : t('sendMessage')}
            {!submitTicket.isPending && <ChevronRight className="h-6 w-6" />}
          </button>
        </form>
      </div>
    </section>
  )
}
