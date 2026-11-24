import type { NextRequest } from 'next/server'

/** Preserve the incoming host; NextURL normalizes loopback addresses to localhost. */
export function requestOrigin(request: NextRequest): string {
  const host = request.headers.get('x-forwarded-host')?.split(',')[0]?.trim()
    || request.headers.get('host') || request.nextUrl.host
  const forwardedProtocol = request.headers.get('x-forwarded-proto')?.split(',')[0]?.trim().toLowerCase()
  const protocol = forwardedProtocol === 'https' || forwardedProtocol === 'http'
    ? `${forwardedProtocol}:` : request.nextUrl.protocol
  return new URL(`${protocol}//${host}`).origin
}
