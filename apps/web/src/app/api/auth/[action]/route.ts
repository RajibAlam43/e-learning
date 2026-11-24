import { NextRequest, NextResponse } from 'next/server'
import { NETWORK } from '@/config/network.config'
import { requestOrigin } from '@/lib/request-origin'

const API_URL = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || 'https://stage-api.globalislamicinstitute.com'
const ALLOWED_ACTIONS = new Set([
  'login',
  'google',
  'register',
  'send-code',
  'verify-code',
  'forgot-password',
  'reset-password',
  'refresh',
  'logout',
])

type CookieHeaders = Headers & { getSetCookie?: () => string[] }

const SESSION_MAX_AGE_SECONDS = 60 * 60 * 24 * 365

function getSetCookieHeaders(headers: Headers): string[] {
  const cookieHeaders = headers as CookieHeaders
  const values = cookieHeaders.getSetCookie?.()
  if (values?.length) return values
  const combined = headers.get('set-cookie')
  return combined ? [combined] : []
}

function refreshTokenValue(headers: Headers): string | null {
  for (const header of getSetCookieHeaders(headers)) {
    // Some runtimes expose multiple Set-Cookie values as one comma-joined
    // header. Do not require refresh_token to be the first cookie.
    const match = header.match(/(?:^|,\s*)refresh_token=([^;]+)/i)
    if (match?.[1]) return match[1]
  }
  return null
}

function cookieMaxAge(headers: Headers): number | undefined {
  for (const header of getSetCookieHeaders(headers)) {
    if (!/(?:^|,\s*)refresh_token=/i.test(header)) continue
    const match = header.match(/(?:^|;)\s*Max-Age=(\d+)/i)
    if (match?.[1]) return Number(match[1])
    const expires = header.match(/(?:^|;)\s*Expires=([^;]+)/i)?.[1]
    if (expires) {
      const seconds = Math.floor((Date.parse(expires) - Date.now()) / 1000)
      if (Number.isFinite(seconds)) return Math.max(0, seconds)
    }
  }
  return undefined
}

function isSecureRequest(request: NextRequest): boolean {
  const forwardedProto = request.headers.get('x-forwarded-proto')
    ?.split(',')[0]
    ?.trim()
  return forwardedProto === 'https' || request.nextUrl.protocol === 'https:'
}

function sessionCookieOptions(request: NextRequest) {
  return {
    secure: isSecureRequest(request),
    sameSite: 'lax' as const,
    path: '/',
    priority: 'high' as const,
  }
}

function setSessionCookies(
  response: NextResponse,
  request: NextRequest,
  payload: unknown,
): void {
  const data = payload && typeof payload === 'object'
    ? payload as { accessToken?: unknown; roles?: unknown }
    : null
  if (typeof data?.accessToken !== 'string' || !data.accessToken) return

  response.cookies.set({
    name: 'session',
    value: '1',
    httpOnly: false,
    maxAge: SESSION_MAX_AGE_SECONDS,
    ...sessionCookieOptions(request),
  })
  if (Array.isArray(data.roles)) {
    const roles = data.roles.filter((role): role is string => typeof role === 'string')
    response.cookies.set({
      name: 'user_roles',
      value: JSON.stringify(roles),
      httpOnly: false,
      maxAge: SESSION_MAX_AGE_SECONDS,
      ...sessionCookieOptions(request),
    })
  }
}

function clearSessionCookies(response: NextResponse, request: NextRequest): void {
  for (const name of ['session', 'user_roles']) {
    response.cookies.set({
      name,
      value: '',
      httpOnly: false,
      maxAge: 0,
      ...sessionCookieOptions(request),
    })
  }
}

async function readPayload(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) return null

  // Spring may advertise successful auth responses as */* even when the
  // body is JSON. Parse the body itself so refresh/login never lose the token
  // merely because the upstream content type is non-specific.
  try { return JSON.parse(text) as unknown } catch { return { message: text } }
}

function clearRefreshCookie(response: NextResponse, request: NextRequest): void {
  response.cookies.set({
    name: 'refresh_token',
    value: '',
    httpOnly: true,
    maxAge: 0,
    ...sessionCookieOptions(request),
  })
}

function isSameOrigin(request: NextRequest): boolean {
  const site = request.headers.get('sec-fetch-site')
  if (site && site !== 'same-origin' && site !== 'none') return false
  const origin = request.headers.get('origin')
  if (!origin) return site === 'same-origin'
  try { return new URL(origin).origin === requestOrigin(request) } catch { return false }
}

export async function POST(
  request: NextRequest,
  context: { params: Promise<{ action: string }> },
) {
  const { action } = await context.params
  if (!ALLOWED_ACTIONS.has(action)) {
    return NextResponse.json({ message: 'Not found' }, { status: 404 })
  }

  if (!isSameOrigin(request)) {
    const response = NextResponse.json({ message: 'Cross-origin authentication request rejected.' }, { status: 403 })
    response.headers.set('Cache-Control', 'no-store')
    return response
  }

  const body = action === 'refresh' || action === 'logout' ? undefined : await request.text()
  const refreshToken = request.cookies.get('refresh_token')?.value
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'Accept-Language': request.headers.get('accept-language') ?? 'en',
  }
  if (body) headers['Content-Type'] = request.headers.get('content-type') ?? 'application/json'
  if (refreshToken) headers.Cookie = `refresh_token=${refreshToken}`

  let upstream: Response
  let payload: unknown = null
  try {
    upstream = await fetch(`${API_URL}/public/auth/${action}`, {
      method: 'POST',
      headers,
      body,
      cache: 'no-store',
      signal: AbortSignal.any([request.signal, AbortSignal.timeout(NETWORK.authUpstreamTimeoutMs)]),
    })
    if (![204, 205, 304].includes(upstream.status)) payload = await readPayload(upstream)
  } catch (error) {
    const timedOut = error instanceof Error && (error.name === 'TimeoutError' || error.name === 'AbortError')
    const response = NextResponse.json({ message: 'Authentication service is unavailable.' }, { status: timedOut ? 504 : 503 })
    response.headers.set('Cache-Control', 'no-store')
    if (action === 'logout') {
      clearRefreshCookie(response, request)
      clearSessionCookies(response, request)
    }
    return response
  }

  if (upstream.status === 204 || upstream.status === 205 || upstream.status === 304) {
    const response = new NextResponse(null, { status: upstream.status })
    response.headers.set('Cache-Control', 'no-store')
    if (action === 'logout') {
      clearRefreshCookie(response, request)
      clearSessionCookies(response, request)
    }
    return response
  }

  // The API currently reports a missing refresh cookie as 400. Normalize that
  // to an authentication failure so the client clears the stale session marker
  // instead of leaving the user in an endless verification state.
  const status = action === 'refresh' && upstream.status === 400 ? 401 : upstream.status
  const response = NextResponse.json(payload, { status })
  response.headers.set('Cache-Control', 'no-store')
  const token = refreshTokenValue(upstream.headers)
  if (token) {
    response.cookies.set({
      name: 'refresh_token',
      value: token,
      httpOnly: true,
      maxAge: cookieMaxAge(upstream.headers),
      ...sessionCookieOptions(request),
    })
  }
  if (upstream.ok) {
    setSessionCookies(response, request, payload)
  }
  if (action === 'logout') {
    clearRefreshCookie(response, request)
    clearSessionCookies(response, request)
  } else if (action === 'refresh' && (upstream.status === 400 || upstream.status === 401 || upstream.status === 403)) {
    clearRefreshCookie(response, request)
    clearSessionCookies(response, request)
  }
  return response
}
