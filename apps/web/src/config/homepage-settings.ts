export const HOMEPAGE_SETTING_KEY = 'site.homepage'
export interface HomepagePartner { name: string; logoUrl: string; websiteUrl: string }
export const HOMEPAGE_DEFAULTS = { showCoursesCount: true, showStudentsCount: true, showScholarsCount: true, showDiplomasCount: true, partners: [] as HomepagePartner[] }
export type HomepageSettings = typeof HOMEPAGE_DEFAULTS
export function partnerDrafts(value: unknown): HomepagePartner[] {
  if (!Array.isArray(value)) return []
  return value.map(item => {
    const row = item && typeof item === 'object' ? item as Record<string, unknown> : {}
    return { name: typeof row.name === 'string' ? row.name : '', logoUrl: typeof row.logoUrl === 'string' ? row.logoUrl : '', websiteUrl: typeof row.websiteUrl === 'string' ? row.websiteUrl : '' }
  })
}
export function validPublicUrl(value: string, allowRelative = false): boolean {
  if (allowRelative && /^\/(?!\/)/.test(value)) return true
  try { const url = new URL(value); return url.protocol === 'https:' && !!url.hostname } catch { return false }
}
export function homepageSettings(value: unknown): HomepageSettings {
  const row = value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
  const flags = Object.fromEntries(Object.entries(HOMEPAGE_DEFAULTS).filter(([key]) => key !== 'partners').map(([key, fallback]) => [key, typeof row[key] === 'boolean' ? row[key] : fallback]))
  return { ...HOMEPAGE_DEFAULTS, ...flags, partners: partnerDrafts(row.partners).map(p => ({ name: p.name.trim(), logoUrl: p.logoUrl.trim(), websiteUrl: p.websiteUrl.trim() })).filter(p => p.name && validPublicUrl(p.logoUrl, true) && (!p.websiteUrl || validPublicUrl(p.websiteUrl))) }
}
