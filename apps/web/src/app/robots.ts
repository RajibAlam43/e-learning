import type { MetadataRoute } from 'next'
import { APP_CONFIG } from '@/config/app.config'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',
        allow: '/',
        disallow: [
          '/student/',
          '/instructor/',
          '/admin/',
          '/api/',
          '/profile/',
        ],
      },
    ],
    sitemap: `${APP_CONFIG.url}/sitemap.xml`,
  }
}
