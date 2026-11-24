const LOCALE_COOKIE_OPTIONS = 'path=/; max-age=31536000; SameSite=Lax'

export function setLocaleCookie(locale: string): void {
  if (typeof document !== 'undefined') {
    document.cookie = `locale=${encodeURIComponent(locale)}; ${LOCALE_COOKIE_OPTIONS}`
  }
}
