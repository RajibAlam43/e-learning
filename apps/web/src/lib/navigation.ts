/** Accept only local absolute paths, including after URL normalization. */
export function safeRedirectPath(value: string | null | undefined): string | undefined {
  if (!value || !value.startsWith('/') || value.startsWith('//') || /[\\\u0000-\u0020]/.test(value)) return undefined
  try {
    const base = 'https://local.invalid'
    const url = new URL(value, base)
    const path = decodeURIComponent(url.pathname)
    if (path.startsWith('//') || /[\\\u0000-\u0020]/.test(path)) return undefined
    return url.origin === base ? `${url.pathname}${url.search}${url.hash}` : undefined
  } catch {
    return undefined
  }
}

export function parseRoleCookie(value: string | undefined): string[] {
  try {
    const roles: unknown = JSON.parse(decodeURIComponent(value ?? '[]'))
    return Array.isArray(roles) ? roles.filter((role): role is string => typeof role === 'string') : []
  } catch {
    return []
  }
}

export function resolveLocale(value: string | undefined): 'en' | 'bn' {
  return value === 'en' ? 'en' : 'bn'
}
