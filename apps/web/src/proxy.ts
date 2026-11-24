import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'
import { parseRoleCookie } from '@/lib/navigation'
import { requestOrigin } from '@/lib/request-origin'

const authPaths = ['/login', '/signup', '/forgot-password', '/reset-password', '/verify-account']
const isAuthPath = (path: string) => authPaths.includes(path)
const isRouteSegment = (path: string, segment: string) =>
  path === segment || path.startsWith(`${segment}/`)
const isDashboardPath = (path: string) =>
  isRouteSegment(path, '/student') ||
  isRouteSegment(path, '/instructor') ||
  isRouteSegment(path, '/admin')

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl
  const hasAuth = request.cookies.get('session')?.value === '1'
  if (isAuthPath(pathname) && hasAuth) {
    const roles = parseRoleCookie(request.cookies.get('user_roles')?.value)
    const destination = roles.some(role => role === 'ADMIN' || role === 'SUPER_ADMIN')
      ? '/admin/dashboard'
      : roles.some(role => role === 'INSTRUCTOR' || role === 'TEACHER')
        ? '/instructor/dashboard'
        : '/student/dashboard'
    return NextResponse.redirect(new URL(destination, requestOrigin(request)))
  }
  if (isDashboardPath(pathname) && !hasAuth) {
    const login = new URL('/login', requestOrigin(request))
    login.searchParams.set('redirect', pathname)
    return NextResponse.redirect(login)
  }
  return NextResponse.next()
}

export const config = { matcher: ['/((?!api|_next/static|_next/image|favicon.ico|images|robots.txt|sitemap.xml).*)'] }
