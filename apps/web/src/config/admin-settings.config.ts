import { Building2, FileText, Globe2, Mail, Search, Share2 } from 'lucide-react'

export type SettingsGroup = 'general' | 'contact' | 'social' | 'seo' | 'legal'
export type SettingsValues = Record<string, string | boolean>

export interface FieldDefinition {
  name: string
  label: string
  type?: 'text' | 'email' | 'tel' | 'url' | 'textarea'
  placeholder?: string
  required?: boolean
}

export interface GroupDefinition {
  id: SettingsGroup
  key: string
  label: string
  description: string
  icon: typeof Globe2
  fields: FieldDefinition[]
  defaults: SettingsValues
}

export const GROUPS: GroupDefinition[] = [
  {
    id: 'general',
    key: 'site.general',
    label: 'General',
    description: 'Institute identity and regional preferences',
    icon: Building2,
    defaults: {
      instituteName: 'Global Islamic Institute',
      instituteNameBn: '',
      shortName: 'GII',
      tagline: '',
      taglineBn: '',
      timezone: 'Asia/Dhaka',
      currency: 'BDT',
    },
    fields: [
      { name: 'instituteName', label: 'Institute name', required: true },
      { name: 'instituteNameBn', label: 'Institute name (Bangla)' },
      { name: 'shortName', label: 'Short name', required: true },
      { name: 'tagline', label: 'Tagline', type: 'textarea' },
      { name: 'taglineBn', label: 'Tagline (Bangla)', type: 'textarea' },
      { name: 'timezone', label: 'Timezone', required: true },
      { name: 'currency', label: 'Currency', required: true },
    ],
  },
  {
    id: 'contact',
    key: 'site.contact',
    label: 'Contact',
    description: 'Public contact and office information',
    icon: Mail,
    defaults: {
      primaryEmail: '',
      supportEmail: '',
      primaryPhone: '',
      whatsappNumber: '',
      addressDhaka: '',
      addressLondon: '',
      officeHours: '',
      mapUrl: '',
    },
    fields: [
      { name: 'primaryEmail', label: 'Primary email', type: 'email', required: true },
      { name: 'supportEmail', label: 'Support email', type: 'email' },
      { name: 'primaryPhone', label: 'Primary phone', type: 'tel', required: true },
      { name: 'whatsappNumber', label: 'WhatsApp number', type: 'tel' },
      { name: 'addressDhaka', label: 'Dhaka office address', type: 'textarea' },
      { name: 'addressLondon', label: 'London office address', type: 'textarea' },
      { name: 'officeHours', label: 'Office hours' },
      { name: 'mapUrl', label: 'Map URL', type: 'url' },
    ],
  },
  {
    id: 'social',
    key: 'site.social',
    label: 'Social links',
    description: 'Official social media destinations',
    icon: Share2,
    defaults: {
      facebookUrl: '',
      instagramUrl: '',
      youtubeUrl: '',
      linkedinUrl: '',
      whatsappUrl: '',
      telegramUrl: '',
    },
    fields: [
      { name: 'facebookUrl', label: 'Facebook URL', type: 'url' },
      { name: 'instagramUrl', label: 'Instagram URL', type: 'url' },
      { name: 'youtubeUrl', label: 'YouTube URL', type: 'url' },
      { name: 'linkedinUrl', label: 'LinkedIn URL', type: 'url' },
      { name: 'whatsappUrl', label: 'WhatsApp URL', type: 'url' },
      { name: 'telegramUrl', label: 'Telegram URL', type: 'url' },
    ],
  },
  {
    id: 'seo',
    key: 'site.seo',
    label: 'SEO',
    description: 'Default search and sharing metadata',
    icon: Search,
    defaults: {
      defaultTitle: '',
      defaultDescription: '',
      defaultKeywords: '',
      canonicalUrl: '',
      socialImageUrl: '',
      googleSiteVerification: '',
    },
    fields: [
      { name: 'defaultTitle', label: 'Default page title', required: true },
      { name: 'defaultDescription', label: 'Default meta description', type: 'textarea', required: true },
      { name: 'defaultKeywords', label: 'Default keywords' },
      { name: 'canonicalUrl', label: 'Canonical website URL', type: 'url' },
      { name: 'socialImageUrl', label: 'Social sharing image URL', type: 'url' },
      { name: 'googleSiteVerification', label: 'Google site verification' },
    ],
  },
  {
    id: 'legal',
    key: 'site.legal',
    label: 'Legal',
    description: 'Public policy and legal document links',
    icon: FileText,
    defaults: {
      privacyPolicyUrl: '',
      termsUrl: '',
      refundPolicyUrl: '',
      cookiePolicyUrl: '',
      copyrightText: '',
    },
    fields: [
      { name: 'privacyPolicyUrl', label: 'Privacy policy URL', type: 'url' },
      { name: 'termsUrl', label: 'Terms and conditions URL', type: 'url' },
      { name: 'refundPolicyUrl', label: 'Refund policy URL', type: 'url' },
      { name: 'cookiePolicyUrl', label: 'Cookie policy URL', type: 'url' },
      { name: 'copyrightText', label: 'Copyright text' },
    ],
  },
]

export function valueObject(value: unknown): SettingsValues {
  if (!value || Array.isArray(value) || typeof value !== 'object') return {}
  return Object.fromEntries(
    Object.entries(value).filter((entry): entry is [string, string | boolean] =>
      typeof entry[1] === 'string' || typeof entry[1] === 'boolean',
    ),
  )
}

export function validateGroup(group: GroupDefinition, values: SettingsValues) {
  const errors: Record<string, string> = {}
  for (const field of group.fields) {
    const value = String(values[field.name] ?? '').trim()
    if (field.required && !value) errors[field.name] = `${field.label} is required`
    if (value && field.type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      errors[field.name] = 'Enter a valid email address'
    }
    if (value && field.type === 'url') {
      try {
        const url = new URL(value)
        if (!['http:', 'https:'].includes(url.protocol)) throw new Error()
      } catch {
        errors[field.name] = 'Enter a complete http or https URL'
      }
    }
  }
  return errors
}
