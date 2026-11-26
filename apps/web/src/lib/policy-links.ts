interface PublicSetting { key?: string; value?: unknown }

function policyUrl(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined
  try {
    const url = new URL(value)
    if (url.protocol === 'https:' || url.protocol === 'http:') return url.href
  } catch { /* Missing or invalid policy configuration. */ }
  return undefined
}

export function getPolicyLinks(settings: readonly PublicSetting[] = []) {
  const value = settings.find(setting => setting.key === 'site.legal')?.value
  const legal = value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
  return { terms: policyUrl(legal.termsUrl) ?? '/terms', privacy: policyUrl(legal.privacyPolicyUrl) ?? '/privacy-policy' }
}
