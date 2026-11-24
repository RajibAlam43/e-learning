// Shared auth cookie utilities — single source of truth.
// Cookie attributes must match between set and clear, otherwise browsers
// (especially Chrome) may refuse to delete the cookie.

const COOKIE_NAME = 'session'
const COOKIE_OPTIONS = (() => {
  const isSecure = typeof window !== 'undefined' && window.location.protocol === 'https:'
  return `path=/; max-age=31536000; SameSite=Lax${isSecure ? '; Secure' : ''}`
})()

const CLEAR_OPTIONS = (() => {
  const isSecure = typeof window !== 'undefined' && window.location.protocol === 'https:'
  return `path=/; max-age=0; SameSite=Lax${isSecure ? '; Secure' : ''}`
})()

export function setAuthCookie(_token: string, roles: string[] = []): void {
  if (typeof document === 'undefined') return
  // The access token must never be readable by JavaScript. The backend refresh
  // token is HttpOnly; this marker is only a routing hint and never authorizes API calls.
  document.cookie = `auth=; ${CLEAR_OPTIONS}`
  document.cookie = `${COOKIE_NAME}=1; ${COOKIE_OPTIONS}`
  if (roles.length > 0) {
    document.cookie = `user_roles=${encodeURIComponent(JSON.stringify(roles))}; ${COOKIE_OPTIONS}`
  }
}

export function clearAuthCookie(): void {
  if (typeof document === 'undefined') return
  document.cookie = `${COOKIE_NAME}=; ${CLEAR_OPTIONS}`
  document.cookie = `auth=; ${CLEAR_OPTIONS}`
  document.cookie = `user_roles=; ${CLEAR_OPTIONS}`
}
