import type { NextConfig } from 'next'
import createNextIntlPlugin from 'next-intl/plugin'

const withNextIntl = createNextIntlPlugin()

// Only the institute's media stores and existing catalog/profile image hosts.
const imageHosts = new Set([
  'stage-assets.globalislamicinstitute.com',
  'assets.globalislamicinstitute.com',
  'quacklabs.in',
  'lh3.googleusercontent.com',
  ...(process.env.IMAGE_REMOTE_HOSTS ?? '').split(',').map(host => host.trim()).filter(Boolean),
])
for (const host of imageHosts) {
  if (!/^[a-z0-9.-]+$/i.test(host) || host.includes('..')) {
    throw new Error('IMAGE_REMOTE_HOSTS must contain explicit hostnames without wildcards or URLs.')
  }
}

const securityHeaders = [
  { key: 'Content-Security-Policy', value: "base-uri 'self'; form-action 'self' https:; frame-ancestors 'none'; object-src 'none'" },
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'X-Frame-Options', value: 'DENY' },
  { key: 'X-Permitted-Cross-Domain-Policies', value: 'none' },
  { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
  { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
  { key: 'Strict-Transport-Security', value: 'max-age=31536000; includeSubDomains' },
]

const noIndexRoutes = [
  '/login',
  '/signup',
  '/forgot-password',
  '/reset-password',
  '/verify-account',
  '/student/:path*',
  '/instructor/:path*',
  '/admin/:path*',
]

const nextConfig: NextConfig = {
  output: 'standalone',
  agentRules: false,
  poweredByHeader: false,
  images: {
    qualities: [75, 100],
    dangerouslyAllowSVG: false,
    contentSecurityPolicy: "default-src 'self'; script-src 'none'; sandbox;",
    remotePatterns: [...imageHosts].map(hostname => ({ protocol: 'https' as const, hostname })),
  },
  async headers() {
    return [
      { source: '/(.*)', headers: securityHeaders },
      ...noIndexRoutes.map((source) => ({
        source,
        headers: [{ key: 'X-Robots-Tag', value: 'noindex, nofollow' }],
      })),
    ]
  },
}

export default withNextIntl(nextConfig)
